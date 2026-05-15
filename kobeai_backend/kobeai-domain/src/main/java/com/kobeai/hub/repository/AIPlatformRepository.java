package com.kobeai.hub.repository;

import com.kobeai.hub.model.AIPlatform;
import com.kobeai.hub.model.Platform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AIPlatformRepository extends JpaRepository<AIPlatform, Long> {
    List<AIPlatform> findByEnabledTrue();

    Optional<AIPlatform> findByType(Platform type);

    boolean existsByType(Platform type);
}