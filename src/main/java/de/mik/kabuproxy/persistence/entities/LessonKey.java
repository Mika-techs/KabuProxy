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
 * Key of a lesson colour or display name: a subject, optionally narrowed to one teacher. {@code teacher} is empty for
 * the whole subject (it is part of the primary key, so it can't be null).
 */
@Getter
@EqualsAndHashCode
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class LessonKey implements Serializable, Comparable<LessonKey>
{
    private static final long serialVersionUID = 1L;

    private static final Comparator<LessonKey> ORDER = Comparator
        .comparing(LessonKey::getSubject, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(LessonKey::getSubject)
        .thenComparing(LessonKey::getTeacher, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(LessonKey::getTeacher);

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "teacher", nullable = false)
    private String teacher;

    /**
     * Trims both parts; a null teacher means the whole subject.
     */
    public LessonKey(String subject, String teacher)
    {
        this.subject = subject == null ? "" : subject.trim();
        this.teacher = teacher == null ? "" : teacher.trim();
    }

    public static LessonKey of(String subject)
    {
        return new LessonKey(subject, null);
    }

    public boolean isWholeSubject()
    {
        return teacher.isEmpty();
    }

    @Override
    public int compareTo(LessonKey other)
    {
        return ORDER.compare(this, other);
    }
}
