package de.mik.kabuproxy.persistence.entities;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Comparator;

/**
 * Key of a lesson colour: a subject, optionally narrowed to one teacher. {@code teacher} is empty for the colour of the
 * whole subject (it is part of the primary key, so it can't be null).
 */
@Getter
@EqualsAndHashCode
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class LessonColorKey implements Serializable, Comparable<LessonColorKey>
{
    private static final long serialVersionUID = 1L;

    private static final Comparator<LessonColorKey> ORDER = Comparator
        .comparing(LessonColorKey::getSubject, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(LessonColorKey::getSubject)
        .thenComparing(LessonColorKey::getTeacher, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(LessonColorKey::getTeacher);

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "teacher", nullable = false)
    private String teacher;

    /**
     * Trims both parts; a null teacher means the whole subject.
     */
    public LessonColorKey(String subject, String teacher)
    {
        this.subject = subject == null ? "" : subject.trim();
        this.teacher = teacher == null ? "" : teacher.trim();
    }

    public static LessonColorKey of(String subject)
    {
        return new LessonColorKey(subject, null);
    }

    public boolean isWholeSubject()
    {
        return teacher.isEmpty();
    }

    @Override
    public int compareTo(LessonColorKey other)
    {
        return ORDER.compare(this, other);
    }
}
