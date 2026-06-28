package com.notificationengine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Simulates a national DND (Do Not Disturb) registry database.
 */
@Entity
@Table(name = "dnd_registry")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DndRegistry {

    /** The phone number, e.g. +919876543210. Used as primary key for fast lookups. */
    @Id
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    /** If true, all promotional categories are blocked. */
    @Column(name = "fully_blocked")
    private boolean fullyBlocked;

    /** 
     * Comma-separated list of allowed promotional categories if not fully blocked.
     * E.g. "BANKING,EDUCATION"
     */
    @Column(name = "allowed_categories")
    private String allowedCategories;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
