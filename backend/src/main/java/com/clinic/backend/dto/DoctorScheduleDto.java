package com.clinic.backend.dto;

import com.clinic.backend.entity.DoctorSchedule;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record DoctorScheduleDto(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {

    public static DoctorScheduleDto from(DoctorSchedule schedule) {
        return new DoctorScheduleDto(schedule.getDayOfWeek(), schedule.getStartTime(), schedule.getEndTime());
    }
}
