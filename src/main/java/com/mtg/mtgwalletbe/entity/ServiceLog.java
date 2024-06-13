package com.mtg.mtgwalletbe.entity;

import com.mtg.mtgwalletbe.entity.auditing.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.mtg.mtgwalletbe.aspect.LoggableAspect.MAX_CHARS_REQUEST;
import static com.mtg.mtgwalletbe.aspect.LoggableAspect.MAX_CHARS_RESPONSE;

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
    @Column(length = 500)
    private String serviceName;
    @Size(max = 1)
    @Column(length = 1)
    private String status;
    @Column(length = MAX_CHARS_REQUEST)
    private String request;
    @Column(length = MAX_CHARS_RESPONSE)
    private String response;
    private Long startTime;
    private Long endTime;
    private Long executionTime;
}
