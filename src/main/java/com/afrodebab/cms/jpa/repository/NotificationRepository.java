package com.afrodebab.cms.jpa.repository;

import com.afrodebab.cms.jpa.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findAllByEmployeeIdOrderByCreatedAtDesc(Long employeeId, Pageable pageable);

    Page<Notification> findAllByManagerIdOrderByCreatedAtDesc(Long managerId, Pageable pageable);

    long countByEmployeeIdAndReadAtIsNull(Long employeeId);

    long countByManagerIdAndReadAtIsNull(Long managerId);

    long countByBroadcastIdAndReadAtIsNotNull(Long broadcastId);

    Optional<Notification> findByIdAndEmployeeId(Long id, Long employeeId);

    Optional<Notification> findByIdAndManagerId(Long id, Long managerId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.employeeId = :employeeId AND n.readAt IS NULL")
    int markAllReadForEmployee(@Param("employeeId") Long employeeId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.managerId = :managerId AND n.readAt IS NULL")
    int markAllReadForManager(@Param("managerId") Long managerId, @Param("now") Instant now);
}
