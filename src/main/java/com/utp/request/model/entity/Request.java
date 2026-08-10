package com.utp.request.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("requests")
public class Request {

  @Id
  @Column("id_request")
  private Integer idRequest;

  @Column("id_vehicle")
  private Integer idVehicle;

  @Column("id_cycle")
  private Integer idCycle;

  @Column("id_status")
  private Integer idStatus;

  @Column("id_acceptor")
  private Integer idAcceptor;

  @Column("date_request")
  private LocalDateTime dateRequest;

  @Column("date_response")
  private LocalDateTime dateResponse;
}
