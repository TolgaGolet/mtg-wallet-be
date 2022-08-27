package com.mtg.mtgwalletbe.entity;

import com.mtg.mtgwalletbe.entity.auditing.Auditable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @NotNull
    @Size(min = 3, max = 15)
    private String name;
}
