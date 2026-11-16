package com.clinic.backend.dto;

import java.time.LocalDateTime;

public record TimeSlotDto(LocalDateTime startTime, LocalDateTime endTime) {}
