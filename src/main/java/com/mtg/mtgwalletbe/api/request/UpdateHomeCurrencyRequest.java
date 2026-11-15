package com.mtg.mtgwalletbe.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // Jackson 3 needs it to deserialize request bodies
@AllArgsConstructor
public class UpdateHomeCurrencyRequest {
    @NotNull // the value itself is checked by Currency.of (unknown -> 400 INVALID_REQUEST)
    private String currencyValue;
    /**
     * Optional ordering, both or neither: the sending tab's id (one per page load) and its pick counter. An older pick
     * from the same tab that commits late (e.g. after a client timeout) is then ignored instead of overwriting a newer one.
     */
    @Size(max = 36)
    private String clientId;
    @PositiveOrZero
    private Long sequence;
}
