package org.springframework.samples.petclinic.api

import com.fasterxml.jackson.annotation.JsonIgnore
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.function.Function
import java.util.stream.Collectors

@SpringBootApplication
class PetClinicApplication {
  @Bean
  fun webClient(builder: WebClient.Builder): WebClient = builder.build()
}

fun main(args: Array<String>) {
  runApplication<PetClinicApplication>(*args)
}

@Component
class CustomersServiceClient(private val webClient: WebClient) {

  @Value("\${customers-service-id://customers-service}")
  private val hostname: String? = null

  fun getOwner(ownerId: Int): Mono<OwnerDetails> {
    return webClient.get()
      .uri("$hostname/owners/{ownerId}", ownerId)
      .retrieve()
      .bodyToMono(OwnerDetails::class.java)
  }
}

@Component
class VisitsServiceClient(private val webClient: WebClient) {
  @Value("\${visits-service-id://visits-service}")
  private var hostname: String? = null

  fun getVisitsForPets(petIds: List<Int>): Mono<Visits> {
    return webClient
      .get()
      .uri("$hostname/pets/visits?petId={petId}", joinIds(petIds))
      .retrieve()
      .bodyToMono(Visits::class.java)
  }

  private fun joinIds(petIds: List<Int>): String {
    return petIds.stream().map { obj: Int -> obj.toString() }
      .collect(Collectors.joining(","))
  }

  fun setHostname(hostname: String?) {
    this.hostname = hostname
  }
}

data class OwnerDetails(
  val id: Int,
  val firstName: String,
  val lastName: String,
  val address: String,
  val city: String,
  val telephone: String,
  val pets: MutableList<PetDetails> = mutableListOf()
  ) {

  @get:JsonIgnore
  val petIds: List<Int>
    get() = pets.stream().map { it.id }.collect(Collectors.toList())
}

data class PetDetails(
  val id: Int,
  val name: String,
  val birthDate: String,
  val type: PetType,
  val visits: MutableList<VisitDetails> = mutableListOf()
)

data class PetType(val name: String)

data class Visits(val items: MutableList<VisitDetails> = mutableListOf())

data class VisitDetails(
  val id: Int,
  val petId: Int,
  val date: String = LocalDate.now().toString(),
  val description: String
)

@RestController
@RequestMapping("/api/gateway")
class PetClinicController(
    val customersServiceClient: CustomersServiceClient,
    val visitsServiceClient: VisitsServiceClient
 ) {
  private val log = LoggerFactory.getLogger(this.javaClass.name)

  @GetMapping("owners/{ownerId}")
  fun getOwnerDetails(@PathVariable ownerId: Int): Mono<OwnerDetails> {
    return customersServiceClient.getOwner(ownerId)
      .flatMap { owner ->
        visitsServiceClient.getVisitsForPets(owner.petIds)
          .transform {
            it.onErrorResume({ throwable ->
              val responseStatus = (throwable as WebClientResponseException).statusCode
              val timedOut = (responseStatus === HttpStatus.GATEWAY_TIMEOUT)
              if (timedOut) {
                log.info("Response from visits service is a 504, returning an empty visits response as fallback..")
              }
              timedOut
            }, { _ -> emptyVisitsForPets() })
          }
          .map(addVisitsToOwner(owner))
      }
  }

  private fun addVisitsToOwner(owner: OwnerDetails): Function<Visits, OwnerDetails> {
    return Function<Visits, OwnerDetails> { visits: Visits ->
      owner.pets
        .forEach { pet ->
          pet.visits
            .addAll(
              visits.items.stream()
                .filter { v -> v.petId == pet.id }.toList()
            )
        }
      owner
    }
  }

  private fun emptyVisitsForPets(): Mono<Visits> = Mono.just(Visits())

}