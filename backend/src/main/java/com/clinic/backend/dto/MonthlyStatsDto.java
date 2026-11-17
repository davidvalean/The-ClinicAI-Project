package com.clinic.backend.dto;

// Number of appointments in one month (1 = January ... 12 = December), in total and split by status.
// Built directly by the JPQL query in AppointmentRepository.countByMonth.
public record MonthlyStatsDto(
        Integer month,
        Long total,
        Long scheduled,
        Long completed,
        Long cancelled
) {

    // Used for the months in which the query found no appointments.
    public static MonthlyStatsDto empty(int month) {
        return new MonthlyStatsDto(month, 0L, 0L, 0L, 0L);
    }
}
