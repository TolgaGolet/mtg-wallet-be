package com.mtg.mtgwalletbe.enums.converter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.enums.Currency;
import jakarta.persistence.Convert;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

// WalletUser is loaded on every request: a currency later removed from the enum must not make that load throw.
// (The column's CHECK constraint can't be relied on: ddl-auto=update never narrows it.)
class LenientCurrencyConverterTest {

    private final LenientCurrencyConverter converter = new LenientCurrencyConverter();

    @Test
    void unknownStoredValue_isReadAsNull() {
        assertThat(converter.convertToEntityAttribute("XXX")).isNull();
    }

    @Test
    void unknownStoredValue_isWarnedOnce_notOnEveryLoad() {
        Logger logger = (Logger) LoggerFactory.getLogger(LenientCurrencyConverter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        String stale = "OLD" + System.nanoTime(); // the warned set is static: a value no other test reads
        try {
            converter.convertToEntityAttribute(stale);
            new LenientCurrencyConverter().convertToEntityAttribute(stale);

            assertThat(appender.list).filteredOn(e -> e.getFormattedMessage().contains(stale)).hasSize(1);
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void knownValues_roundTrip() {
        assertThat(converter.convertToDatabaseColumn(Currency.EUR)).isEqualTo("EUR");
        assertThat(converter.convertToEntityAttribute("EUR")).isEqualTo(Currency.EUR);
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void walletUserHomeCurrency_usesIt() throws Exception {
        Convert convert = WalletUser.class.getDeclaredField("homeCurrency").getAnnotation(Convert.class);

        assertThat(convert.converter()).isEqualTo(LenientCurrencyConverter.class);
    }
}
