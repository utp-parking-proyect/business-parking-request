package com.utp.request.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestDto {
    private String idApplicant;
    private String vehicleType;
    private String numberPlate;
    private LocalDateTime dateRequest;
    private Integer idStatus;
    private Integer approved;
    private boolean isNew;
}
