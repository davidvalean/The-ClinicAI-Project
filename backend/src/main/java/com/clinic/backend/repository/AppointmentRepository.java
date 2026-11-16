package com.clinic.backend.repository;

import com.clinic.backend.entity.Appointment;
import com.clinic.backend.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Appointments of a doctor with the given status that overlap the interval [from, to).
    @Query("""
            SELECT a FROM Appointment a
            WHERE a.doctor.id = :doctorId AND a.status = :status
              AND a.startTime < :to AND a.endTime > :from
            """)
    List<Appointment> findOverlapping(@Param("doctorId") Long doctorId,
                                      @Param("status") AppointmentStatus status,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    // Same condition as above, but the database only answers yes/no.
    @Query("""
            SELECT COUNT(a) > 0 FROM Appointment a
            WHERE a.doctor.id = :doctorId AND a.status = :status
              AND a.startTime < :to AND a.endTime > :from
            """)
    boolean existsOverlapping(@Param("doctorId") Long doctorId,
                              @Param("status") AppointmentStatus status,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to);

    // Does the patient already have an appointment with this status at this doctor in [dayStart, dayEnd)?
    @Query("""
            SELECT COUNT(a) > 0 FROM Appointment a
            WHERE a.patient.id = :patientId AND a.doctor.id = :doctorId AND a.status = :status
              AND a.startTime >= :dayStart AND a.startTime < :dayEnd
            """)
    boolean existsForPatientOnDay(@Param("patientId") Long patientId,
                                  @Param("doctorId") Long doctorId,
                                  @Param("status") AppointmentStatus status,
                                  @Param("dayStart") LocalDateTime dayStart,
                                  @Param("dayEnd") LocalDateTime dayEnd);

    // @EntityGraph loads the doctor, its user and its specialty in the same query.
    @EntityGraph(attributePaths = {"doctor", "doctor.user", "doctor.specialty"})
    List<Appointment> findByPatientId(Long patientId);

    @EntityGraph(attributePaths = {"doctor", "doctor.user", "doctor.specialty"})
    List<Appointment> findByPatientIdAndStatus(Long patientId, AppointmentStatus status);

    @EntityGraph(attributePaths = {"doctor", "doctor.user", "doctor.specialty"})
    Optional<Appointment> findWithDetailsById(Long id);
}
