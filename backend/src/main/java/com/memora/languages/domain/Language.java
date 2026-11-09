package com.memora.languages.domain;

import com.memora.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A language offered by the platform. Reference data, seeded by migration. */
@Entity
@Table(name = "languages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Language extends BaseEntity {

    /** ISO 639-1 code, used as the stable public identifier in URLs and payloads. */
    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "native_name", nullable = false, length = 80)
    private String nativeName;

    @Column(name = "flag_emoji", length = 16)
    private String flagEmoji;

    @Column(nullable = false)
    private boolean active;
}
