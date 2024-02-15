package org.springframework.samples.petclinic.vets

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(controllers = [VetsController::class])
class VetsControllerTest(@Autowired val mvc: MockMvc) {

  @MockkBean
  private lateinit var vetRepository: VetRepository

  @Test
  fun shouldGetAListOfVets() {
    every {
      vetRepository.findAll()
    } returns listOf(Vet(1, "John", "Doe"))

    mvc.get("/vets") {
      accept = APPLICATION_JSON
    }.andExpect {
      status { isOk() }
      jsonPath("$[0].id") { value(1) }
    }
  }

}