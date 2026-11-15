package com.mtg.mtgwalletbe.entity.auditing;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class Auditable {
    @CreatedBy
    @Column(updatable = false, nullable = false)
    protected String createdBy;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    protected LocalDateTime createdDate;

    @LastModifiedBy
    @Column
    protected String lastModifiedBy;

    @LastModifiedDate
    @Column
    protected LocalDateTime lastModifiedDate;

    public abstract Long getId();

    /**
     * Identity is the database id: two instances are equal only when both are persisted rows of the same entity
     * class (proxies included) with the same id. A new entity equals only itself, and the hash code never changes
     * when the id is assigned, so entities are safe in HashSets before and after persisting.
     */
    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Auditable other) || persistentClass(this) != persistentClass(other)) {
            return false;
        }
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public final int hashCode() {
        return persistentClass(this).hashCode();
    }

    /**
     * The entity class behind a possibly lazy proxy, without initializing it: Hibernate.getClass would load the
     * row (or throw LazyInitializationException once detached) just to compare or hash a reference.
     */
    private static Class<?> persistentClass(Object entity) {
        return entity instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : entity.getClass();
    }
}
