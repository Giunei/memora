package com.memora.languages.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserLanguageRepository extends JpaRepository<UserLanguage, UUID> {

    /**
     * Fetches the language in the same query. A user studies a handful of
     * languages, so the whole list is returned unpaginated, but without the join
     * fetch this would be one extra query per row.
     */
    @Query("""
            select ul from UserLanguage ul
            join fetch ul.language
            where ul.user.id = :userId
            order by ul.primaryFocus desc, ul.startedAt asc
            """)
    List<UserLanguage> findAllForUser(@Param("userId") UUID userId);

    @Query("""
            select ul from UserLanguage ul
            join fetch ul.language
            where ul.id = :id and ul.user.id = :userId
            """)
    Optional<UserLanguage> findForUser(@Param("id") UUID id, @Param("userId") UUID userId);

    boolean existsByUserIdAndLanguageId(UUID userId, UUID languageId);

    /**
     * Executed immediately and followed by a session clear, so the subsequent
     * insert or update of the new primary cannot race the partial unique index.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update UserLanguage ul set ul.primaryFocus = false where ul.user.id = :userId and ul.primaryFocus = true")
    int clearPrimaryFocus(@Param("userId") UUID userId);

    long countByUserId(UUID userId);
}
