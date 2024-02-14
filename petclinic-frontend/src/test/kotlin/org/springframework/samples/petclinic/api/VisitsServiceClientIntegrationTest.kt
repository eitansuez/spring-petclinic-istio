package org.springframework.samples.petclinic.api

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import reactor.core.publisher.Mono

class VisitsServiceClientIntegrationTest {

  private lateinit var visitsServiceClient: VisitsServiceClient
  private lateinit var server: MockWebServer

  @BeforeEach
  fun setUp() {
    server = MockWebServer()
    visitsServiceClient = VisitsServiceClient(WebClient.builder().build())
    visitsServiceClient.setHostname(server.url("/").toString())
  }

  @AfterEach
  fun shutdown() {
    server.shutdown()
  }

  @Test
  fun getVisitsForPets_withAvailableVisitsService() {
    val response = MockResponse()
    response
      .setHeader("Content-Type", "application/json")
      .setBody("{\"items\":[{\"id\":5,\"date\":\"2018-11-15\",\"description\":\"test visit\",\"petId\":1}]}")
    server.enqueue(response)

    val mono: Mono<Visits> =
      visitsServiceClient.getVisitsForPets(listOf(1))

    val visits = mono.block()!!
    assertEquals(1, visits.items.size)
    assertNotNull(visits.items[0])
    assertEquals(1, visits.items[0].petId)
    assertEquals("test visit", visits.items[0].description)
  }


}