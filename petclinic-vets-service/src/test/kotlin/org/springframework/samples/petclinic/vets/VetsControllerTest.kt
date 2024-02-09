package org.springframework.samples.petclinic.vets

import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = [VetsController::class])
class VetsControllerTest {

  @Autowired private lateinit var mvc: MockMvc
  @MockBean private lateinit var vetRepository: VetRepository

  @Test
  fun shouldGetAListOfVets() {
    val vet = Vet(1, "John", "Doe")
    given(vetRepository.findAll()).willReturn(listOf(vet));
    mvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].id").value(1));
  }

}