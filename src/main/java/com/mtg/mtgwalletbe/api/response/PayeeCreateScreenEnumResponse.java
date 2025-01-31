package com.mtg.mtgwalletbe.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayeeCreateScreenEnumResponse {
    private Page<CategoryResponse> categoryList;
}
