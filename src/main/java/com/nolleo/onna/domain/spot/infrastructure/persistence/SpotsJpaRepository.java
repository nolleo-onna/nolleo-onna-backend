package com.nolleo.onna.domain.spot.infrastructure.persistence;

import com.nolleo.onna.domain.spot.domain.entity.Spots;
import com.nolleo.onna.domain.spot.domain.repository.SpotsRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SpotsJpaRepository extends JpaRepository<Spots, String>, SpotsRepository {

    @Query("SELECT s FROM Spots s WHERE s.isActive = true")
    List<Spots> findAllActive();
}