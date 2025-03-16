package com.utp.request.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestDto {
    private Integer idApplicant;
    private String vehicleType;
    private String numberPlate;
    private Integer idCycle;
    private LocalDateTime dateRequest;
    private Integer idStatus;
    private Integer approved;
    private boolean isNew;
}
