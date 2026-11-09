package com.memora.languages.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LanguageRepository extends JpaRepository<Language, UUID> {

    Page<Language> findByActiveTrue(Pageable pageable);

    @Query("""
            select l from Language l
            where l.active = true
              and (lower(l.name) like lower(concat('%', :term, '%'))
                   or lower(l.nativeName) like lower(concat('%', :term, '%'))
                   or lower(l.code) = lower(:term))
            """)
    Page<Language> search(@Param("term") String term, Pageable pageable);

    Optional<Language> findByCodeIgnoreCaseAndActiveTrue(String code);
}
