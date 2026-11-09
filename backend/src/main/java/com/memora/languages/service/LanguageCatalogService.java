package com.memora.languages.service;

import com.memora.languages.domain.Language;
import com.memora.languages.domain.LanguageRepository;
import com.memora.shared.error.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class LanguageCatalogService {

    private final LanguageRepository languageRepository;

    public LanguageCatalogService(LanguageRepository languageRepository) {
        this.languageRepository = languageRepository;
    }

    public Page<Language> list(String searchTerm, Pageable pageable) {
        return StringUtils.hasText(searchTerm)
                ? languageRepository.search(searchTerm.trim(), pageable)
                : languageRepository.findByActiveTrue(pageable);
    }

    public Language requireActiveByCode(String code) {
        return languageRepository
                .findByCodeIgnoreCaseAndActiveTrue(code)
                .orElseThrow(() -> ResourceNotFoundException.of("Language", code));
    }
}
