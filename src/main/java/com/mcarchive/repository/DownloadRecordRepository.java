package com.mcarchive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DownloadRecordRepository extends JpaRepository<DownloadRecord, Long> {
    boolean existsByUserIdAndArchiveId(Long userId, Long archiveId);
    List<DownloadRecord> findByUserIdOrderByDownloadedAtDesc(Long userId);
}
