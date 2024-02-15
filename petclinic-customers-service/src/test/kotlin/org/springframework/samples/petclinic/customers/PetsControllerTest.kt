package org.springframework.samples.petclinic.customers

import com.ninjasquad.springmockk.MockkBean
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.every
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.get

@ExtendWith(SpringExtension::class)
@WebMvcTest(controllers = [PetsController::class])
class PetsControllerTest(@Autowired val mvc: MockMvc) {
  @MockkBean
  private lateinit var petRepository : PetRepository
  @MockkBean
  private lateinit var meterRegistry: MeterRegistry
  @MockkBean
  private lateinit var ownerRepository : OwnerRepository

  @Test
  fun shouldGetAPetInJsonFormat() {
    val pet = setupPet()
    every {
      petRepository.findByIdOrNull(2)
    } returns pet

    mvc.get("/owners/2/pets/2") {
      accept = APPLICATION_JSON
    }.andExpect {
      status { isOk() }
      content { contentType(APPLICATION_JSON) }
      jsonPath("$.id") { value(2) }
      jsonPath("$.name") { value("Basil") }
      jsonPath("$.type.id") { value(6) }
    }
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