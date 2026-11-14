package com.mtg.mtgwalletbe.entity.auditing;

import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.entity.Payee;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuditableTest {

    /** Stands in for the lazy proxy Hibernate generates for an Account (a subclass that is a HibernateProxy). */
    static class AccountProxy extends Account implements HibernateProxy {
        private final LazyInitializer initializer;

        AccountProxy(long id, LazyInitializer initializer) {
            this.initializer = initializer;
            setId(id);
        }

        @Override
        public Object writeReplace() {
            return this;
        }

        @Override
        public LazyInitializer getHibernateLazyInitializer() {
            return initializer;
        }
    }

    private static LazyInitializer uninitializedAccount() {
        LazyInitializer initializer = mock(LazyInitializer.class);
        doReturn(Account.class).when(initializer).getPersistentClass();
        return initializer;
    }

    // Regression: equals/hashCode used Hibernate.getClass, which initializes the proxy (a SELECT, or
    // LazyInitializationException once detached) just to compare or hash a reference
    @Test
    void proxyEqualsTheEntity_withoutInitializingIt() {
        LazyInitializer initializer = uninitializedAccount();
        AccountProxy proxy = new AccountProxy(7L, initializer);
        Account account = new Account();
        account.setId(7L);

        assertThat(proxy).isEqualTo(account);
        assertThat(account).isEqualTo(proxy);
        assertThat(Set.of(account)).contains(proxy);
        verify(initializer, never()).initialize();
        verify(initializer, never()).getImplementation();
    }

    @Test
    void differentEntityClassesWithTheSameId_areNotEqual() {
        Payee payee = new Payee();
        payee.setId(7L);

        assertThat(new AccountProxy(7L, uninitializedAccount())).isNotEqualTo(payee);
    }
}
