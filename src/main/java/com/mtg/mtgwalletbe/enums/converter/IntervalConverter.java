package com.mtg.mtgwalletbe.enums.converter;

import com.mtg.mtgwalletbe.enums.Interval;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class IntervalConverter extends CodedEnumConverter<Interval> {
    public IntervalConverter() {
        super(Interval.class);
    }
}
