package com.memora.languages.service;

import com.memora.languages.domain.CefrLevel;
import com.memora.languages.domain.Language;
import com.memora.languages.domain.UserLanguage;
import com.memora.languages.domain.UserLanguageRepository;
import com.memora.languages.web.dto.EnrolLanguageRequest;
import com.memora.languages.web.dto.UpdateUserLanguageRequest;
import com.memora.shared.error.BusinessRuleException;
import com.memora.shared.error.ResourceAlreadyExistsException;
import com.memora.shared.error.ResourceNotFoundException;
import com.memora.users.domain.User;
import com.memora.users.service.UserService;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserLanguageService {

    private static final int MAX_ENROLMENTS_PER_USER = 10;

    private final UserLanguageRepository userLanguageRepository;
    private final LanguageCatalogService languageCatalogService;
    private final UserService userService;

    public UserLanguageService(
            UserLanguageRepository userLanguageRepository,
            LanguageCatalogService languageCatalogService,
            UserService userService) {
        this.userLanguageRepository = userLanguageRepository;
        this.languageCatalogService = languageCatalogService;
        this.userService = userService;
    }

    public List<UserLanguage> listForUser(UUID userId) {
        return userLanguageRepository.findAllForUser(userId);
    }

    public UserLanguage requireForUser(UUID enrolmentId, UUID userId) {
        // Scoped by user id, so one account can never read another's enrolment
        // by guessing an identifier.
        return userLanguageRepository
                .findForUser(enrolmentId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Language enrolment", enrolmentId));
    }

    @Transactional
    public UserLanguage enrol(UUID userId, EnrolLanguageRequest request) {
        requireReachableTarget(request.currentLevel(), request.targetLevel());

        Language language = languageCatalogService.requireActiveByCode(request.languageCode());
        if (userLanguageRepository.existsByUserIdAndLanguageId(userId, language.getId())) {
            throw new ResourceAlreadyExistsException(
                    "You are already studying " + language.getName());
        }
        long existing = userLanguageRepository.countByUserId(userId);
        if (existing >= MAX_ENROLMENTS_PER_USER) {
            throw new BusinessRuleException(
                    "You can study at most %d languages at a time".formatted(MAX_ENROLMENTS_PER_USER));
        }

        // The first language a user adds becomes the focus automatically;
        // otherwise an account would have no active language until it picked one.
        boolean primaryFocus = request.primaryFocus() || existing == 0;
        if (primaryFocus) {
            userLanguageRepository.clearPrimaryFocus(userId);
        }

        User user = userService.require(userId);
        return userLanguageRepository.save(UserLanguage.enrol(
                user, language, request.currentLevel(), request.targetLevel(), primaryFocus));
    }

    @Transactional
    public UserLanguage update(UUID userId, UUID enrolmentId, UpdateUserLanguageRequest request) {
        requireReachableTarget(request.currentLevel(), request.targetLevel());

        if (request.primaryFocus()) {
            userLanguageRepository.clearPrimaryFocus(userId);
        }
        // Loaded after the bulk update because that call clears the session.
        UserLanguage enrolment = requireForUser(enrolmentId, userId);
        enrolment.changeLevels(request.currentLevel(), request.targetLevel());
        enrolment.setAsPrimaryFocus(request.primaryFocus());
        return enrolment;
    }

    @Transactional
    public void remove(UUID userId, UUID enrolmentId) {
        UserLanguage enrolment = requireForUser(enrolmentId, userId);
        boolean wasFocus = enrolment.isPrimaryFocus();
        userLanguageRepository.delete(enrolment);

        if (wasFocus) {
            // Leaving the account with no focus would break the "continue
            // studying" entry point, so the longest-running enrolment takes over.
            userLanguageRepository.flush();
            userLanguageRepository.findAllForUser(userId).stream()
                    .min(Comparator.comparing(UserLanguage::getStartedAt))
                    .ifPresent(next -> next.setAsPrimaryFocus(true));
        }
    }

    private void requireReachableTarget(CefrLevel current, CefrLevel target) {
        if (!target.isAtLeast(current)) {
            throw new BusinessRuleException(
                    "Target level %s cannot be lower than the current level %s".formatted(target, current));
        }
    }
}
