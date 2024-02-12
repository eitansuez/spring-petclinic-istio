package org.springframework.samples.petclinic.visits

import io.micrometer.core.instrument.MeterRegistry
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@WebMvcTest(controllers = [VisitsController::class])
class VisitsControllerTest {
  @Autowired private lateinit var mvc: MockMvc
  @MockBean private lateinit var visitRepository : VisitRepository
  @MockBean private lateinit var meterRegistry: MeterRegistry

  @Test
  fun shouldFetchVisits() {
    BDDMockito.given(visitRepository.findByPetIdIn(mutableListOf(111, 222)))
      .willReturn(
        listOf(
          Visit(id = 1, petId = 111),
          Visit(id = 2, petId = 222),
          Visit(id = 3, petId = 222)
        )
      )

    mvc.perform(MockMvcRequestBuilders.get("/pets/visits?petId=111,222"))
      .andExpect(MockMvcResultMatchers.status().isOk())
      .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].id").value(1))
      .andExpect(MockMvcResultMatchers.jsonPath("$.items[1].id").value(2))
      .andExpect(MockMvcResultMatchers.jsonPath("$.items[2].id").value(3))
      .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].petId").value(111))
      .andExpect(MockMvcResultMatchers.jsonPath("$.items[1].petId").value(222))
      .andExpect(MockMvcResultMatchers.jsonPath("$.items[2].petId").value(222))
  }
}