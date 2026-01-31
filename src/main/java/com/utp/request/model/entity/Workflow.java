package com.utp.request.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("workflow")
public class Workflow {

  @Id
  @Column("id_workflow")
  private Integer idWorkflow;

  @Column("id_request")
  private Integer idRequest;

  @Column("id_status")
  private Integer idStatus;

  @Column("date_create")
  private LocalDateTime dateCreate;

  @Column("date_update")
  private LocalDateTime dateUpdate;
}
