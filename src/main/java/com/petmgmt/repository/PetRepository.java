package com.petmgmt.repository;

import com.petmgmt.model.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {
    Page<Pet> findByOwnerId(Long ownerId, Pageable pageable);
    List<Pet> findByOwnerId(Long ownerId);
    List<Pet> findByOwnerIdAndNameContainingOrBreedContaining(Long ownerId, String name, String breed);
    long countByOwnerId(Long ownerId);
    long countBySpecies(String species);
}
