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
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "lesson")
public class LessonEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClassEntity schoolClass;

    @Column(name = "lesson_date", nullable = false)
    private LocalDate date;

    @Column(name = "period_from", nullable = false)
    private int periodFrom;

    @Column(name = "period_to", nullable = false)
    private int periodTo;

    @Column(name = "lane", nullable = false)
    private int lane;

    @Column(name = "lane_count", nullable = false)
    private int laneCount;

    @Column(name = "teacher")
    private String teacher;

    @Column(name = "room")
    private String room;

    @Column(name = "subject")
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LessonStatus status;

    @Column(name = "status_raw")
    private String statusRaw;

    @Column(name = "hint")
    private String hint;

    @Column(name = "note")
    private String note;

    @Column(name = "digikabu_id")
    private String digikabuId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
