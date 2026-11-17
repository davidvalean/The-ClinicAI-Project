package com.clinic.backend.controller;

import com.clinic.backend.dto.MonthlyStatsDto;
import com.clinic.backend.dto.SpecialtyStatsDto;
import com.clinic.backend.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// @PreAuthorize on the class applies to every endpoint in it.
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @GetMapping("/specialties")
    public List<SpecialtyStatsDto> bySpecialty(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return adminStatsService.countBySpecialty(from, to);
    }

    // Without ?year= the current year is used.
    @GetMapping("/monthly")
    public List<MonthlyStatsDto> monthly(@RequestParam(required = false) Integer year) {
        return adminStatsService.countByMonth(year);
    }
}
