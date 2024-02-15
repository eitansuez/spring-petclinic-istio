package org.springframework.samples.petclinic.api

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PetClinicControllerTest(@Autowired val client: WebTestClient) {
  @MockkBean private lateinit var customersServiceClient: CustomersServiceClient
  @MockkBean private lateinit var visitsServiceClient: VisitsServiceClient

  @Test
  fun getOwnerDetails_withAvailableVisitsService() {
    val owner = OwnerDetails(id=1, firstName="John", lastName="Doe",
      address="123 Some St", city="Austin", telephone="1234445555")
    val cat = PetDetails(id=20, name="Garfield", birthDate="03-02-1999",
      type=PetType("cat"))
    owner.pets.add(cat)

    every {
      customersServiceClient.getOwner(1)
    } returns Mono.just(owner)

    val visits = Visits()
    visits.items.add(VisitDetails(id = 300, description = "First visit", petId = cat.id))
    every {
      visitsServiceClient.getVisitsForPets(listOf(cat.id))
    } returns Mono.just(visits)

    client.get()
      .uri("/api/gateway/owners/1")
      .exchange()
      .expectStatus().isOk() //.expectBody(String.class)
      //.consumeWith(response ->
      //    Assertions.assertThat(response.getResponseBody()).isEqualTo("Garfield"));
      .expectBody()
      .jsonPath("$.pets[0].name").isEqualTo("Garfield")
      .jsonPath("$.pets[0].visits[0].description").isEqualTo("First visit")
  }

  /**
   * Test fallback
   */
  @Test
  fun getOwnerDetails_withServiceError() {
    val owner = OwnerDetails(id=1, firstName="John", lastName="Doe",
      address="123 Some St", city="Austin", telephone="123445555")
    val cat = PetDetails(id=20, name="Garfield", birthDate="01-01-2020",
      type=PetType("cat"))
    owner.pets.add(cat)

    every {
      customersServiceClient.getOwner(1)
    } returns Mono.just(owner)

    every {
      visitsServiceClient.getVisitsForPets(listOf(cat.id))
    } returns Mono.error(
      WebClientResponseException(HttpStatus.GATEWAY_TIMEOUT.value(), "Simulated Gateway Timeout", null, null, null)
    )

    client.get()
      .uri("/api/gateway/owners/1")
      .exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.pets[0].name").isEqualTo("Garfield")
      .jsonPath("$.pets[0].visits").isEmpty()
  }

}