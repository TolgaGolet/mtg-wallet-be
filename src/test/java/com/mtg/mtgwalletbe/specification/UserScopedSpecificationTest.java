package com.mtg.mtgwalletbe.specification;

import com.mtg.mtgwalletbe.api.request.AccountSearchRequest;
import com.mtg.mtgwalletbe.api.request.CategorySearchRequest;
import com.mtg.mtgwalletbe.api.request.PayeeSearchRequest;
import com.mtg.mtgwalletbe.api.request.TransactionSearchRequest;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// F7: per-user isolation of search lives in the specifications, so every one must always filter by the user id it
// is given (the request no longer carries a userId the client could set)
@SuppressWarnings({"unchecked", "rawtypes"})
class UserScopedSpecificationTest {

    private Root root;
    private CriteriaBuilder cb;
    private Path userIdPath;

    @BeforeEach
    void setUp() {
        root = mock(Root.class);
        cb = mock(CriteriaBuilder.class);
        Path anyPath = mock(Path.class);
        Path userPath = mock(Path.class);
        userIdPath = mock(Path.class);
        when(root.get(any(String.class))).thenReturn(anyPath);
        when(anyPath.get(any(String.class))).thenReturn(anyPath);
        when(root.get("user")).thenReturn(userPath);
        when(userPath.get("id")).thenReturn(userIdPath);
    }

    private void assertScopedTo(Specification<?> specification, long userId) {
        specification.toPredicate(root, mock(CriteriaQuery.class), cb);
        verify(cb).equal(userIdPath, userId);
    }

    @Test
    void account_emptyFilter_isStillScopedToTheUser() {
        assertScopedTo(AccountSpecification.search(new AccountSearchRequest(), null, 7L), 7L);
    }

    @Test
    void category_emptyFilter_isStillScopedToTheUser() {
        assertScopedTo(CategorySpecification.search(new CategorySearchRequest(), null, 7L), 7L);
    }

    @Test
    void payee_emptyFilter_isStillScopedToTheUser() {
        assertScopedTo(PayeeSpecification.search(new PayeeSearchRequest(), null, 7L), 7L);
    }

    @Test
    void transaction_emptyFilter_isStillScopedToTheUser() {
        assertScopedTo(TransactionSpecification.search(new TransactionSearchRequest(), 7L), 7L);
    }

    @Test
    void category_childrenOnly_filtersTopLevelCategories() {
        CategorySearchRequest request = CategorySearchRequest.builder().childrenOnly(true).parentCategoryId(3L).build();
        Path parentPath = mock(Path.class);
        when(root.get("parentCategory")).thenReturn(parentPath);

        CategorySpecification.search(request, null, 7L).toPredicate(root, mock(CriteriaQuery.class), cb);

        verify(cb).isNull(parentPath);
        verify(cb, never()).equal(any(Path.class), eq(3L));
    }

    @Test
    void missingUserId_failsInsteadOfReturningEveryonesRows() {
        Specification<?> specification = AccountSpecification.search(new AccountSearchRequest(), null, null);

        assertThatThrownBy(() -> specification.toPredicate(root, mock(CriteriaQuery.class), cb))
                .isInstanceOf(NullPointerException.class);
    }
}
