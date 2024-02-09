package org.springframework.samples.petclinic.vets

import jakarta.persistence.*
import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class VetsServiceApplication

fun main(args: Array<String>) {
  runApplication<VetsServiceApplication>(*args)
}

interface VetRepository : JpaRepository<Vet, Int>

@RequestMapping("/vets")
@RestController
class VetsController(val repository: VetRepository) {
  @GetMapping
  fun vets(): List<Vet> = repository.findAll()
}

@Entity
@Table(name = "vets")
class Vet(
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Int? = null,
  @Column(name = "first_name") @NotEmpty var firstName: String,
  @Column(name = "last_name") @NotEmpty var lastName: String,
  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(name = "vet_specialties",
    joinColumns = [JoinColumn(name = "vet_id")],
    inverseJoinColumns = [JoinColumn(name = "specialty_id")]
  )
  var specialties: MutableSet<Specialty> = mutableSetOf()
) {
  fun getSpecialties(): List<Specialty> = specialties.sortedWith(compareBy { it.name })

  fun getNrOfSpecialties(): Int = specialties.size

  fun addSpecialty(specialty: Specialty) = specialties.add(specialty)

}

@Entity
@Table(name = "specialties")
class Specialty(
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Int? = null,
  @Column(name="name") var name: String
)
