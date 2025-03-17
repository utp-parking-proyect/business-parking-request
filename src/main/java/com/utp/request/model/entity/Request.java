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

    @Column("id_applicant")
    private Integer idApplicant;

    @Column("id_acceptor")
    private Integer idAcceptor;

    @Column("id_vehicle_type")
    private Integer idVehicleType;

    @Column("id_vehicle")
    private Integer idVehicle;

    @Column("id_status")
    private Integer idStatus;

    @Column("id_cycle")
    private Integer idCycle;

    @Column("number_plate")
    private String numberPlate;

    @Column("date_request")
    private LocalDateTime dateRequest;

    @Column("date_response")
    private LocalDateTime dateResponse;

    @Column("comment")
    private String comment;

    @Column("is_new")
    private Boolean isNew;

    @Column("approved")
    private Boolean approved;
}
