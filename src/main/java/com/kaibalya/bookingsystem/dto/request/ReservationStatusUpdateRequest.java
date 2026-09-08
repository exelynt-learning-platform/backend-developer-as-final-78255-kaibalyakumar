package com.kaibalya.bookingsystem.dto.request;

import com.kaibalya.bookingsystem.entity.ReservationStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private ReservationStatus status;
}
