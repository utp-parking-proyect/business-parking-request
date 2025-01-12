package com.utp.request.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("requests")
public class Request {

    @Id
    @Column("id_request")
    private Integer idRequest;

    @Column("fk_id_applicant")
    private Integer idApplicant;

    @Column("fk_id_acceptor")
    private Integer idAcceptor;

    @Column("fk_vehicle_type")
    private Integer vehicleType;

    @Column("fk_vehicle_id")
    private Integer vehicleId;

    @Column("fk_status_id")
    private Integer statusId;

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
