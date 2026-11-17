package com.clinic.backend.repository;

import com.clinic.backend.dto.MonthlyStatsDto;
import com.clinic.backend.dto.SpecialtyStatsDto;
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

    // ---------- Doctor side ----------

    // Appointments of a doctor that start in [from, to), soonest first.
    // JOIN FETCH loads the patient in the same query, because the DTO shows the patient's name.
    @Query("""
            SELECT a FROM Appointment a JOIN FETCH a.patient
            WHERE a.doctor.id = :doctorId
              AND a.startTime >= :from AND a.startTime < :to
            ORDER BY a.startTime ASC
            """)
    List<Appointment> findForDoctorBetween(@Param("doctorId") Long doctorId,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);

    // Loads the appointment together with its patient and the doctor's login account,
    // which are needed to check ownership and to build the DTO.
    @EntityGraph(attributePaths = {"patient", "doctor", "doctor.user"})
    Optional<Appointment> findWithPatientById(Long id);

    // ---------- Admin statistics ----------
    // The counting is done by the database (GROUP BY), not by loading the appointments in Java.

    // One row per specialty, including specialties with no appointments (thanks to the LEFT JOINs).
    // The date condition sits in the ON clause, so it filters appointments without dropping specialties.
    @Query("""
            SELECT new com.clinic.backend.dto.SpecialtyStatsDto(
                s.id, s.name, COUNT(a),
                SUM(CASE WHEN a.status = com.clinic.backend.entity.AppointmentStatus.SCHEDULED THEN 1 ELSE 0 END),
                SUM(CASE WHEN a.status = com.clinic.backend.entity.AppointmentStatus.COMPLETED THEN 1 ELSE 0 END),
                SUM(CASE WHEN a.status = com.clinic.backend.entity.AppointmentStatus.CANCELLED THEN 1 ELSE 0 END))
            FROM Specialty s
            LEFT JOIN Doctor d ON d.specialty = s
            LEFT JOIN Appointment a ON a.doctor = d AND a.startTime >= :from AND a.startTime < :to
            GROUP BY s.id, s.name
            ORDER BY COUNT(a) DESC, s.name ASC
            """)
    List<SpecialtyStatsDto> countBySpecialty(@Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);

    // One row per month that has at least one appointment in [from, to).
    // Months without appointments are added with 0 by the service.
    @Query("""
            SELECT new com.clinic.backend.dto.MonthlyStatsDto(
                EXTRACT(MONTH FROM a.startTime), COUNT(a),
                SUM(CASE WHEN a.status = com.clinic.backend.entity.AppointmentStatus.SCHEDULED THEN 1 ELSE 0 END),
                SUM(CASE WHEN a.status = com.clinic.backend.entity.AppointmentStatus.COMPLETED THEN 1 ELSE 0 END),
                SUM(CASE WHEN a.status = com.clinic.backend.entity.AppointmentStatus.CANCELLED THEN 1 ELSE 0 END))
            FROM Appointment a
            WHERE a.startTime >= :from AND a.startTime < :to
            GROUP BY EXTRACT(MONTH FROM a.startTime)
            """)
    List<MonthlyStatsDto> countByMonth(@Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);
}
