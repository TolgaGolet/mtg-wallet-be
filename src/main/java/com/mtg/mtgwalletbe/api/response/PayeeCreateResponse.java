package com.mtg.mtgwalletbe.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayeeCreateResponse {
    private Long payeeId;
    private String name;
    private Long categoryId;
    private String username;
}
