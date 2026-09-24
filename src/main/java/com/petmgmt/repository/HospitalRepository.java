package com.petmgmt.repository;

import com.petmgmt.model.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    @Query(value = "SELECT *, (6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:lng)) + sin(radians(:lat)) * sin(radians(latitude)))) AS distance FROM hospitals HAVING distance < :radius ORDER BY distance", nativeQuery = true)
    List<Hospital> findNearby(@Param("lat") double lat, @Param("lng") double lng, @Param("radius") double radius);
}
