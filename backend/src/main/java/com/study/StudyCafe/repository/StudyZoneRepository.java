package com.study.StudyCafe.repository;

import com.study.StudyCafe.entity.StudyZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudyZoneRepository extends JpaRepository<StudyZone, Long> {
    Optional<StudyZone> findByZoneName(String zoneName);

}
