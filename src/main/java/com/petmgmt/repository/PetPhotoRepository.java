package com.petmgmt.repository;

import com.petmgmt.model.PetPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PetPhotoRepository extends JpaRepository<PetPhoto, Long> {
    List<PetPhoto> findByPetIdOrderByCreatedAtDesc(Long petId);
}
