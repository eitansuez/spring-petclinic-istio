package org.springframework.samples.petclinic.customers

import com.fasterxml.jackson.annotation.JsonIgnore
import io.micrometer.core.annotation.Timed
import io.micrometer.core.aop.TimedAspect
import io.micrometer.core.instrument.MeterRegistry
import jakarta.persistence.*
import jakarta.validation.Valid
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.repository.query.Param
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@SpringBootApplication
class CustomersServiceApplication {
  @Bean
  fun timedAspect(registry: MeterRegistry): TimedAspect = TimedAspect(registry)
}

fun main(args: Array<String>) {
  runApplication<CustomersServiceApplication>(*args)
}

interface OwnerRepository : JpaRepository<Owner, Int>

interface PetRepository : JpaRepository<Pet, Int> {
  @Query("SELECT ptype FROM PetType ptype ORDER BY ptype.name")
  fun findPetTypes(): List<PetType>

  @Query("FROM PetType ptype WHERE ptype.id = :typeId")
  fun findPetTypeById(@Param("typeId") typeId: Int): PetType?
}

@Entity
@Table(name="types")
class PetType(
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Int? = null,
  @Column(name="name") var name: String
)

@Entity
@Table(name="pets")
class Pet(
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Int? = null,
  @Column(name="name") var name: String,
  @Column(name="birth_date")
  @DateTimeFormat(pattern="yyyy-MM-dd")
  var birthDate: LocalDate = LocalDate.now(),
  @ManyToOne
  @JoinColumn(name="type_id")
  var type: PetType? = null,
  @ManyToOne
  @JoinColumn(name="owner_id")
  @JsonIgnore
  var owner: Owner? = null
)

@Entity
@Table(name="owners")
class Owner(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,
    @Column(name = "first_name") @NotEmpty
    var firstName: String,
    @Column(name = "last_name") @NotEmpty
    var lastName: String,
    @Column(name = "address") @NotEmpty
    var address: String = "",
    @Column(name = "city") @NotEmpty
    var city: String = "",
    @Column(name = "telephone") @NotEmpty
    @Digits(fraction=0, integer=10)
    var telephone: String = "",
    @OneToMany(cascade=[CascadeType.ALL], fetch=FetchType.EAGER, mappedBy = "owner")
    var pets: MutableSet<Pet> = mutableSetOf()
) {
  fun getPets(): List<Pet> =
    pets.sortedWith(compareBy({ it.name }))

  fun addPet(pet: Pet) {
    pets.add(pet)
    pet.owner = this
  }
}


@RestController
@Timed("petclinic.pet")
class PetsController(
  val petRepository: PetRepository,
  val ownerRepository: OwnerRepository
) {
  private val log = LoggerFactory.getLogger(this.javaClass.name)

  @GetMapping("/petTypes")
  fun getPetTypes(): List<PetType> = petRepository.findPetTypes()

  @PostMapping("/owners/{ownerId}/pets")
  @ResponseStatus(HttpStatus.CREATED)
  fun processCreationForm(
    @RequestBody petRequest: PetRequest,
    @PathVariable("ownerId") ownerId: Int
  ): Pet {
    val owner = ownerRepository.findByIdOrNull(ownerId)
      ?: throw ResourceNotFoundException("Owner $ownerId not found")
    val petType = petRepository.findPetTypeById(petRequest.typeId)
    val pet = Pet(
      name = petRequest.name,
      birthDate = petRequest.birthDate,
      type = petType
    )
    owner.addPet(pet)
    log.info("Saving pet $pet")
    return petRepository.save(pet)
  }

  @PutMapping("/owners/*/pets/{petId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun processUpdateForm(@RequestBody petRequest: PetRequest) {
    val petId: Int = petRequest.id
    val pet = findPetById(petId)

    pet.name = petRequest.name
    pet.birthDate = petRequest.birthDate
    val petType = petRepository.findPetTypeById(petRequest.typeId)
    pet.type = petType
    log.info("Updating pet $pet")
    petRepository.save(pet)
  }

  @GetMapping("owners/*/pets/{petId}")
  fun findPet(@PathVariable("petId") petId: Int): PetDetails {
    return PetDetails(findPetById(petId))
  }

  private fun findPetById(petId: Int): Pet {
    val pet = petRepository.findByIdOrNull(petId) ?:
      throw ResourceNotFoundException("Pet $petId not found")
    return pet
  }
}

@ResponseStatus(HttpStatus.NOT_FOUND)
class ResourceNotFoundException(message: String) : RuntimeException(message)

data class PetRequest(
  val id: Int,
  @DateTimeFormat(pattern = "yyyy-MM-dd") val birthDate: LocalDate,
  @Size(min = 1) val name: String,
  val typeId: Int
)

data class PetDetails(
  val id: Long,
  val name: String,
  val owner: String,
  @DateTimeFormat(pattern = "yyyy-MM-dd") val birthDate: LocalDate,
  val type: PetType
) {
  constructor(pet: Pet) : this(pet.id!!.toLong(),
    pet.name,
    "${pet.owner?.firstName} ${pet.owner?.lastName}",
    pet.birthDate,
    pet.type!!
  )
}

@RequestMapping("/owners")
@RestController
@Timed("petclinic.owner")
class OwnersController(val ownerRepository: OwnerRepository) {
  private val log = LoggerFactory.getLogger(this.javaClass.name)

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun createOwner(@Valid @RequestBody owner: Owner): Owner =
    ownerRepository.save(owner)

  @GetMapping("/{ownerId}")
  fun findOwner(@PathVariable("ownerId") ownerId: Int): Owner? =
    ownerRepository.findByIdOrNull(ownerId)

  @GetMapping
  fun findAll() : List<Owner> = ownerRepository.findAll()

  @PutMapping("/{ownerId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun updateOwner(@PathVariable("ownerId") ownerId: Int,
                  @Valid @RequestBody ownerRequest: Owner) {
    val owner = ownerRepository.findByIdOrNull(ownerId)
      ?: throw ResourceNotFoundException("Owner $ownerId not found")
    owner.firstName = ownerRequest.firstName
    owner.lastName = ownerRequest.lastName
    owner.city = ownerRequest.city
    owner.address = ownerRequest.address
    owner.telephone = ownerRequest.telephone
    log.info("Saving owner $owner")
    ownerRepository.save(owner)
  }

}