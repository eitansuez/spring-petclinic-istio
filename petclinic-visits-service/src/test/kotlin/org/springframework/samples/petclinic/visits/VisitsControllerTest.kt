package org.springframework.samples.petclinic.visits

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@AutoConfigureObservability
@WebMvcTest(controllers = [VisitsController::class])
class VisitsControllerTest(@Autowired val mvc: MockMvc) {
  @MockkBean private lateinit var visitRepository : VisitRepository

  @Test
  fun shouldFetchVisits() {
    every {
      visitRepository.findByPetIdIn(mutableListOf(111, 222))
    } returns listOf(
      Visit(id = 1, petId = 111),
      Visit(id = 2, petId = 222),
      Visit(id = 3, petId = 222)
    )

    mvc.get("/pets/visits?petId=111,222") {

    }.andExpect {
      status { isOk() }
      jsonPath("$.items[0].id") { value(1) }
      jsonPath("$.items[1].id") { value(2) }
      jsonPath("$.items[2].id") { value(3) }
      jsonPath("$.items[0].petId") { value(111) }
      jsonPath("$.items[1].petId") { value(222) }
      jsonPath("$.items[1].petId") { value(222) }
    }
  }
}