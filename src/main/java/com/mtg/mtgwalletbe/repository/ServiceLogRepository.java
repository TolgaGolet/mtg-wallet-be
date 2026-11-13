package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.ServiceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ServiceLogRepository extends JpaRepository<ServiceLog, Long> {
    @Modifying
    @Query("delete from ServiceLog s where s.createdDate < :cutoff")
    int deleteByCreatedDateBefore(@Param("cutoff") LocalDateTime cutoff);
}
