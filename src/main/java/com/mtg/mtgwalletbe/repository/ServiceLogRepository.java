package com.mtg.mtgwalletbe.repository;

import com.mtg.mtgwalletbe.entity.ServiceLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceLogRepository extends JpaRepository<ServiceLog, Long> {
}
