package de.mik.kabuproxy.persistence.entities;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "crawl_debug")
public class CrawlDebugEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private DigikabuAccountEntity account;

    @Column(name = "url")
    private String url;

    @Column(name = "error")
    private String error;

    @Column(name = "html", columnDefinition = "MEDIUMTEXT")
    private String html;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
