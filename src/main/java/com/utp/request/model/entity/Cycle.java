package com.utp.request.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("cycles")
public class Cycle {
    @Id
    @Column("id_cycle")
    private Integer idCycle;

    @Column("name_cycle")
    private String nameCycle;
}
