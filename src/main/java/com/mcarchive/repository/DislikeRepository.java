package com.mcarchive.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DislikeRepository extends JpaRepository<DislikeRecord, Long> {
    boolean existsByUserIdAndArchiveId(Long userId, Long archiveId);
    void deleteByUserIdAndArchiveId(Long userId, Long archiveId);
    List<DislikeRecord> findByUserId(Long userId);
}
