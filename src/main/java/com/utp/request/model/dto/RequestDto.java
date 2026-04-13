package com.utp.request.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestDto {

  private Integer idApplicant;
  private Integer vehicleType;
  private String numberPlate;
  private Integer idCycle;
  private LocalDateTime dateRequest;
  private Integer idStatus;
  private Integer approved;
  private Boolean isNew;
}