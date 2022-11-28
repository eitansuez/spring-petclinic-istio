package org.springframework.samples.petclinic.api.boundary.web;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.api.application.CustomersServiceClient;
import org.springframework.samples.petclinic.api.application.VisitsServiceClient;
import org.springframework.samples.petclinic.api.dto.OwnerDetails;
import org.springframework.samples.petclinic.api.dto.PetDetails;
import org.springframework.samples.petclinic.api.dto.VisitDetails;
import org.springframework.samples.petclinic.api.dto.Visits;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Collections;

@WebFluxTest(controllers = PetClinicController.class)
@Import({InsecurityConfiguration.class})
class PetClinicControllerTest {

	@MockBean
	private CustomersServiceClient customersServiceClient;

	@MockBean
	private VisitsServiceClient visitsServiceClient;

	@Autowired
	private WebTestClient client;

	@Test
	void getOwnerDetails_withAvailableVisitsService() {
		OwnerDetails owner = new OwnerDetails();
		PetDetails cat = new PetDetails();
		cat.setId(20);
		cat.setName("Garfield");
		owner.getPets().add(cat);
		Mockito
				.when(customersServiceClient.getOwner(1))
				.thenReturn(Mono.just(owner));

		Visits visits = new Visits();
		VisitDetails visit = new VisitDetails();
		visit.setId(300);
		visit.setDescription("First visit");
		visit.setPetId(cat.getId());
		visits.getItems().add(visit);
		Mockito
				.when(visitsServiceClient.getVisitsForPets(Collections.singletonList(cat.getId())))
				.thenReturn(Mono.just(visits));

		client.get()
				.uri("/api/gateway/owners/1")
				.exchange()
				.expectStatus().isOk()
				//.expectBody(String.class)
				//.consumeWith(response ->
				//    Assertions.assertThat(response.getResponseBody()).isEqualTo("Garfield"));
				.expectBody()
				.jsonPath("$.pets[0].name").isEqualTo("Garfield")
				.jsonPath("$.pets[0].visits[0].description").isEqualTo("First visit");
	}

	/**
	 * Test fallback
	 */
	@Test
	void getOwnerDetails_withServiceError() {
		OwnerDetails owner = new OwnerDetails();
		PetDetails cat = new PetDetails();
		cat.setId(20);
		cat.setName("Garfield");
		owner.getPets().add(cat);

		Mockito
				.when(customersServiceClient.getOwner(1))
				.thenReturn(Mono.just(owner));

		Mockito
				.when(visitsServiceClient.getVisitsForPets(Collections.singletonList(cat.getId())))
				.thenReturn(
						Mono.error(
								new WebClientResponseException(HttpStatus.GATEWAY_TIMEOUT.value(), "Simulated Gateway Timeout", null, null, null)
						)
				);

		client.get()
				.uri("/api/gateway/owners/1")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.pets[0].name").isEqualTo("Garfield")
				.jsonPath("$.pets[0].visits").isEmpty();
	}
}
