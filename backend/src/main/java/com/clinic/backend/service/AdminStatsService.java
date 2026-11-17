package com.clinic.backend.service;

import com.clinic.backend.dto.MonthlyStatsDto;
import com.clinic.backend.dto.SpecialtyStatsDto;
import com.clinic.backend.exception.InvalidRequestException;
import com.clinic.backend.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// Appointment statistics for the admin. The counting itself happens in the database.
@Service
@RequiredArgsConstructor
public class AdminStatsService {

    // Bounds used when 'from' or 'to' is missing, so the repository needs a single query
    // instead of one for every combination of present/missing filters.
    private static final LocalDateTime BEGINNING = LocalDate.of(1900, 1, 1).atStartOfDay();
    private static final LocalDateTime FAR_FUTURE = LocalDate.of(9999, 12, 31).atStartOfDay();

    private final AppointmentRepository appointmentRepository;
    private final Clock clock;

    // 'from' and 'to' are both optional and both included.
    @Transactional(readOnly = true)
    public List<SpecialtyStatsDto> countBySpecialty(LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new InvalidRequestException("'to' (" + to + ") must not be before 'from' (" + from + ")");
        }
        LocalDateTime start = from != null ? from.atStartOfDay() : BEGINNING;
        LocalDateTime end = to != null ? to.plusDays(1).atStartOfDay() : FAR_FUTURE;
        return appointmentRepository.countBySpecialty(start, end);
    }

    // Always returns 12 rows (January..December); months without appointments have 0.
    @Transactional(readOnly = true)
    public List<MonthlyStatsDto> countByMonth(Integer year) {
        int selectedYear = year != null ? year : LocalDate.now(clock).getYear();
        if (selectedYear < 1900 || selectedYear > 9998) {
            throw new InvalidRequestException("Year must be between 1900 and 9998");
        }

        LocalDateTime start = LocalDate.of(selectedYear, 1, 1).atStartOfDay();
        LocalDateTime end = start.plusYears(1);
        Map<Integer, MonthlyStatsDto> found = appointmentRepository.countByMonth(start, end).stream()
                .collect(Collectors.toMap(MonthlyStatsDto::month, Function.identity()));

        return IntStream.rangeClosed(1, 12)
                .mapToObj(month -> found.getOrDefault(month, MonthlyStatsDto.empty(month)))
                .toList();
    }
}
