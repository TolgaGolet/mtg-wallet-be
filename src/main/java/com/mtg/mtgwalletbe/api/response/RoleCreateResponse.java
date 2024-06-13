package com.mtg.mtgwalletbe.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleCreateResponse {
    private Long id;
    private String name;
}
