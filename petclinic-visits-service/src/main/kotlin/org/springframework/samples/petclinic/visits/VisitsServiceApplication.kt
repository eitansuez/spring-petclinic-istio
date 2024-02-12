package org.springframework.samples.petclinic.visits

import io.micrometer.core.annotation.Timed
import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry
import jakarta.persistence.*
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@SpringBootApplication
class VisitsServiceApplication {
  @Bean
  fun timedAspect(registry: MeterRegistry): TimedAspect = TimedAspect(registry)
}


fun main(args: Array<String>) {
  runApplication<VisitsServiceApplication>(*args)
}


@RestController
@Timed("petclinic.visit")
class VisitsController(
    val repository: VisitRepository,
    @Value("\${delay.millis:0}") val delayMillis: Int = 0
  )
{
  private val log = LoggerFactory.getLogger(this.javaClass.name)

  @PostMapping("owners/*/pets/{petId}/visits")
  @ResponseStatus(HttpStatus.CREATED)
  fun create(@RequestBody visit:@Valid Visit, @PathVariable("petId") petId: Int): Visit {
    visit.petId = petId
    log.info("Saving visit {}", visit)
    return repository.save(visit)
  }

  @GetMapping("owners/*/pets/{petId}/visits")
  fun visits(@PathVariable("petId") petId: Int): List<Visit> = repository.findByPetId(petId)

  @GetMapping("pets/visits")
  fun visitsMultiGet(@RequestParam("petId") petIds: List<Int>): Visits {
    try  {
      Thread.sleep(delayMillis.toLong())
    } catch (ignored: InterruptedException) {}
    return Visits(repository.findByPetIdIn(petIds))
  }
}

interface VisitRepository : JpaRepository<Visit, Int> {
  fun findByPetId(petId: Int): List<Visit>
  fun findByPetIdIn(petIds: List<Int>): List<Visit>
}

@Entity
@Table(name = "visits")
class Visit(
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Int? = null,
  @Column(name = "visit_date") @DateTimeFormat(pattern = "yyyy-MM-dd") var date: LocalDate = LocalDate.now(),
  @Column(name = "description") var description:@Size(max = 8192) String = "",
  @Column(name = "pet_id") var petId: Int? = null
)

class Visits(val items: List<Visit>)