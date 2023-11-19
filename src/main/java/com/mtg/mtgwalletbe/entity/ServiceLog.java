package com.mtg.mtgwalletbe.entity;

import com.mtg.mtgwalletbe.entity.auditing.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class ServiceLog extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(unique = true, updatable = false)
    private Long id;
    private String serviceName;
    @Size(max = 1000)
    private String request;
    @Size(max = 1000)
    private String response;
    private Long startTime;
    private Long endTime;
    private Long executionTime;
}
