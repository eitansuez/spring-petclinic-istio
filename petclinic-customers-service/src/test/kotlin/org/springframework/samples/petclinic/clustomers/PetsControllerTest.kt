package org.springframework.samples.petclinic.customers

import io.micrometer.core.instrument.MeterRegistry
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import java.util.*
import org.mockito.BDDMockito.given
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [PetsController::class])
class PetsControllerTest {
  @Autowired
  private lateinit var mvc: MockMvc
  @MockBean
  private lateinit var petRepository : PetRepository
  @MockBean
  private lateinit var meterRegistry: MeterRegistry
  @MockBean
  private lateinit var ownerRepository : OwnerRepository

  @Test
  fun shouldGetAPetInJsonFormat() {
    val pet = setupPet()
    given(petRepository.findById(2)).willReturn(Optional.of(pet))
    mvc.perform(get("/owners/2/pets/2")
      .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().contentType("application/json"))
      .andExpect(jsonPath("$.id").value(2))
      .andExpect(jsonPath("$.name").value("Basil"))
      .andExpect(jsonPath("$.type.id").value(6))
  }

  private fun setupPet(): Pet {
    val owner = Owner(
      firstName="George",
      lastName="Bush"
    )
    val pet = Pet(name = "Basil", id = 2, type = PetType(id = 6, name = "x"))
    owner.addPet(pet)
    return pet
  }
}