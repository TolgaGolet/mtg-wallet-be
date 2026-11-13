package com.mtg.mtgwalletbe.specification;

import com.mtg.mtgwalletbe.api.request.TransactionSearchRequest;
import com.mtg.mtgwalletbe.entity.Transaction;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TransactionSpecificationTest {

    // The day filter is a plain [start of day, next day) range (index-friendly), not DATE(column) = DATE(:x)
    @Test
    @SuppressWarnings("unchecked")
    void dateFilter_usesDayRange() {
        Root<Transaction> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> dateTimePath = mock(Path.class);
        when(root.get("dateTime")).thenReturn(dateTimePath);

        TransactionSearchRequest request = new TransactionSearchRequest();
        request.setDateTime(LocalDateTime.of(2026, 11, 13, 15, 45));

        TransactionSpecification.search(request).toPredicate(root, query, cb);

        verify(cb).greaterThanOrEqualTo((Path) dateTimePath, LocalDateTime.of(2026, 11, 13, 0, 0));
        verify(cb).lessThan((Path) dateTimePath, LocalDateTime.of(2026, 11, 14, 0, 0));
        verify(cb, never()).function(anyString(), any(), any());
    }
}
