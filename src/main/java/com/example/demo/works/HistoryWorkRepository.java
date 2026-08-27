package com.example.demo.works;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoryWorkRepository extends JpaRepository<HistoryWork, Long> {
    Page<HistoryWork> findByUserIdAndExpiresAtAfterOrderByCreatedAtDescIdDesc(
            Long userId, Instant now, Pageable pageable);
    Optional<HistoryWork> findByIdAndUserIdAndExpiresAtAfter(Long id, Long userId, Instant now);
}
