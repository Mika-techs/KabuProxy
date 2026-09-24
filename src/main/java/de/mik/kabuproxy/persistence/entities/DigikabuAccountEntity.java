package de.mik.kabuproxy.persistence.entities;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "digikabu_account")
public class DigikabuAccountEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUserEntity user;

    @Column(name = "digikabu_username", nullable = false)
    private String digikabuUsername;

    /**
     * base64(iv || AES-GCM ciphertext), see CredentialCipher. Never log or expose.
     */
    @Column(name = "password_enc", nullable = false)
    private String passwordEnc;

    @Column(name = "display_name")
    private String displayName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private SchoolClassEntity schoolClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "crawl_status", nullable = false)
    private CrawlStatus crawlStatus;

    @Column(name = "fail_count", nullable = false)
    private int failCount;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "absence_full_days")
    private Integer absenceFullDays;

    @Column(name = "absence_full_days_unexcused")
    private Integer absenceFullDaysUnexcused;

    @Column(name = "absence_hours")
    private Integer absenceHours;

    @Column(name = "absence_hours_unexcused")
    private Integer absenceHoursUnexcused;

    @Column(name = "absences_updated_at")
    private Instant absencesUpdatedAt;
}
