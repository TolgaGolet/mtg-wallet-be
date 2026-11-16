package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.repository.*;
import com.mtg.mtgwalletbe.util.SearchText;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Fills the search columns (searchName, searchNotes) of rows written before those columns existed: ddl-auto adds
 * them as NULL, and entities only fill them on their next write. Runs once at startup, in batches, each in its own
 * transaction; a no-op once every row is filled. Bulk JPQL updates leave auditing columns and @Version untouched.
 */
@Slf4j
@Component
public class SearchTextBackfill {
    private record Target(String name, Function<Pageable, List<SearchTextSource>> findBatch,
                          BiConsumer<Long, String> update) {
    }

    private final List<Target> targets;
    private final TransactionTemplate transactionTemplate;
    private int batchSize = 500;

    public SearchTextBackfill(AccountRepository accounts, CategoryRepository categories, PayeeRepository payees,
                              TransactionRepository transactions, TransactionTemplate transactionTemplate) {
        this.targets = List.of(
                new Target("account", accounts::findWithoutSearchText, accounts::setSearchText),
                new Target("category", categories::findWithoutSearchText, categories::setSearchText),
                new Target("payee", payees::findWithoutSearchText, payees::setSearchText),
                new Target("transaction", transactions::findWithoutSearchText, transactions::setSearchText));
        this.transactionTemplate = transactionTemplate;
    }

    // Tests check the batch loop with a few rows
    void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void backfillOnStartup() {
        try {
            backfill();
        } catch (RuntimeException e) {
            // Search misses the unfilled rows until the next start; never block startup over it
            log.warn("Search text backfill failed", e);
        }
    }

    public void backfill() {
        for (Target target : targets) {
            int total = 0;
            int filled;
            do {
                filled = transactionTemplate.execute(status -> fillBatch(target));
                total += filled;
            } while (filled == batchSize);
            if (total > 0) {
                log.info("Filled search text for {} {} rows", total, target.name());
            }
        }
    }

    // Filled rows drop out of the query, so the first page is always the next batch
    private int fillBatch(Target target) {
        List<SearchTextSource> rows = target.findBatch().apply(Pageable.ofSize(batchSize));
        rows.forEach(row -> target.update().accept(row.getId(), SearchText.fold(row.getText())));
        return rows.size();
    }
}
