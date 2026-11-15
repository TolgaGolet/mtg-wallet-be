package com.mtg.mtgwalletbe.enums.converter;

import com.mtg.mtgwalletbe.enums.Status;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class StatusConverter extends CodedEnumConverter<Status> {
    public StatusConverter() {
        super(Status.class);
    }
}
