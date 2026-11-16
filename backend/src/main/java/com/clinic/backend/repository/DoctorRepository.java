package com.clinic.backend.repository;

import com.clinic.backend.entity.Doctor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    // @EntityGraph loads the related rows in the same query, avoiding one extra query per doctor.
    @Override
    @EntityGraph(attributePaths = {"user", "specialty"})
    List<Doctor> findAll(Sort sort);

    @EntityGraph(attributePaths = {"user", "specialty"})
    List<Doctor> findBySpecialtyId(Long specialtyId, Sort sort);

    @EntityGraph(attributePaths = {"user", "specialty", "schedules"})
    Optional<Doctor> findWithDetailsById(Long id);
}
