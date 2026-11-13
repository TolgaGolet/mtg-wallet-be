package com.mtg.mtgwalletbe.specification;

import com.mtg.mtgwalletbe.api.request.PayeeSearchRequest;
import com.mtg.mtgwalletbe.entity.Payee;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class PayeeSpecificationTest {

    // Regression for CQ-07: the category filter must use the JPA path category.id and the categoryId value
    @Test
    @SuppressWarnings("unchecked")
    void search_byCategoryId_comparesCategoryIdPath() {
        Root<Payee> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> categoryPath = mock(Path.class);
        Path<Object> categoryIdPath = mock(Path.class);
        when(root.get("category")).thenReturn(categoryPath);
        when(categoryPath.get("id")).thenReturn(categoryIdPath);

        PayeeSearchRequest request = new PayeeSearchRequest();
        request.setId(99L);
        request.setCategoryId(7L);

        PayeeSpecification.search(request, null).toPredicate(root, query, cb);

        verify(cb).equal(categoryIdPath, 7L);
        verify(root, never()).get("category_id");
    }
}
