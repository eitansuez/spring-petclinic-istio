package org.springframework.samples.petclinic.api

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PetClinicControllerTest {
  @MockBean
  private lateinit var customersServiceClient: CustomersServiceClient

  @MockBean
  private lateinit var visitsServiceClient: VisitsServiceClient

  @Autowired
  private lateinit var client: WebTestClient

  @Test
  fun getOwnerDetails_withAvailableVisitsService() {
    val owner = OwnerDetails(id=1, firstName="John", lastName="Doe",
      address="123 Some St", city="Austin", telephone="1234445555")
    val cat = PetDetails(id=20, name="Garfield", birthDate="03-02-1999",
      type=PetType("cat"))
    owner.pets.add(cat)
    Mockito
      .`when`(customersServiceClient.getOwner(1))
      .thenReturn(Mono.just(owner))

    val visits = Visits()
    val visit = VisitDetails(id=300, description="First visit",
      petId=cat.id)
    visits.items.add(visit)
    Mockito
      .`when`(visitsServiceClient.getVisitsForPets(listOf(cat.id)))
      .thenReturn(Mono.just(visits))

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

    Mockito
      .`when`(customersServiceClient.getOwner(1))
      .thenReturn(Mono.just(owner))

    Mockito
      .`when`(visitsServiceClient.getVisitsForPets(listOf(cat.id)))
      .thenReturn(
        Mono.error(
          WebClientResponseException(HttpStatus.GATEWAY_TIMEOUT.value(), "Simulated Gateway Timeout", null, null, null)
        )
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