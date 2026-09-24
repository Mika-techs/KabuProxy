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
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "absence")
public class AbsenceEntity
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private DigikabuAccountEntity account;

    @Column(name = "absence_date", nullable = false)
    private LocalDate date;

    @Column(name = "from_text")
    private String fromText;

    @Column(name = "to_text")
    private String toText;

    @Column(name = "remark")
    private String remark;

    @Column(name = "kind")
    private String kind;

    @Column(name = "excused")
    private String excused;
}
