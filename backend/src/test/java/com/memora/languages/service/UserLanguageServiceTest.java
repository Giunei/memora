package com.memora.languages.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.memora.languages.domain.CefrLevel;
import com.memora.languages.domain.Language;
import com.memora.languages.domain.UserLanguage;
import com.memora.languages.domain.UserLanguageRepository;
import com.memora.languages.web.dto.EnrolLanguageRequest;
import com.memora.shared.error.BusinessRuleException;
import com.memora.shared.error.ResourceAlreadyExistsException;
import com.memora.users.domain.User;
import com.memora.users.service.UserService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserLanguageServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID LANGUAGE_ID = UUID.randomUUID();

    @Mock
    private UserLanguageRepository userLanguageRepository;

    @Mock
    private LanguageCatalogService languageCatalogService;

    @Mock
    private UserService userService;

    @Mock
    private Language language;

    @InjectMocks
    private UserLanguageService userLanguageService;

    @Test
    @DisplayName("a target level below the current level is rejected before any lookup")
    void rejectsUnreachableTarget() {
        assertThatThrownBy(() -> userLanguageService.enrol(
                        USER_ID, new EnrolLanguageRequest("en", CefrLevel.B2, CefrLevel.A1, false)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be lower");

        verify(languageCatalogService, never()).requireActiveByCode(anyString());
        verify(userLanguageRepository, never()).save(any(UserLanguage.class));
    }

    @Test
    @DisplayName("a target equal to the current level is allowed, for maintaining a level")
    void allowsTargetEqualToCurrent() {
        stubEnrolment(0L);

        UserLanguage enrolment = userLanguageService.enrol(
                USER_ID, new EnrolLanguageRequest("en", CefrLevel.B2, CefrLevel.B2, false));

        assertThat(enrolment.getCurrentLevel()).isEqualTo(CefrLevel.B2);
        assertThat(enrolment.getTargetLevel()).isEqualTo(CefrLevel.B2);
    }

    @Test
    void rejectsEnrollingTwiceInTheSameLanguage() {
        stubCatalogueLookup();
        when(userLanguageRepository.existsByUserIdAndLanguageId(USER_ID, LANGUAGE_ID)).thenReturn(true);

        assertThatThrownBy(() -> userLanguageService.enrol(
                        USER_ID, new EnrolLanguageRequest("en", CefrLevel.A1, CefrLevel.B1, false)))
                .isInstanceOf(ResourceAlreadyExistsException.class);

        verify(userLanguageRepository, never()).save(any(UserLanguage.class));
    }

    @Test
    @DisplayName("the first language becomes the active focus even when not requested")
    void firstEnrolmentBecomesPrimaryFocus() {
        stubEnrolment(0L);

        UserLanguage enrolment = userLanguageService.enrol(
                USER_ID, new EnrolLanguageRequest("en", CefrLevel.A1, CefrLevel.B1, false));

        assertThat(enrolment.isPrimaryFocus()).isTrue();
        verify(userLanguageRepository).clearPrimaryFocus(USER_ID);
    }

    @Test
    @DisplayName("a later language stays out of focus unless it asks for it")
    void laterEnrolmentKeepsExistingFocus() {
        stubEnrolment(2L);

        UserLanguage enrolment = userLanguageService.enrol(
                USER_ID, new EnrolLanguageRequest("en", CefrLevel.A1, CefrLevel.B1, false));

        assertThat(enrolment.isPrimaryFocus()).isFalse();
        verify(userLanguageRepository, never()).clearPrimaryFocus(USER_ID);
    }

    @Test
    @DisplayName("requesting focus clears the previous one first, to respect the single-focus constraint")
    void requestedFocusReplacesThePrevious() {
        stubEnrolment(2L);

        UserLanguage enrolment = userLanguageService.enrol(
                USER_ID, new EnrolLanguageRequest("en", CefrLevel.A1, CefrLevel.B1, true));

        assertThat(enrolment.isPrimaryFocus()).isTrue();
        verify(userLanguageRepository).clearPrimaryFocus(USER_ID);
    }

    @Test
    void rejectsEnrolmentBeyondTheCap() {
        stubCatalogueLookup();
        when(userLanguageRepository.existsByUserIdAndLanguageId(USER_ID, LANGUAGE_ID)).thenReturn(false);
        when(userLanguageRepository.countByUserId(USER_ID)).thenReturn(10L);

        assertThatThrownBy(() -> userLanguageService.enrol(
                        USER_ID, new EnrolLanguageRequest("en", CefrLevel.A1, CefrLevel.B1, false)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at most");
    }

    private void stubEnrolment(long existingEnrolments) {
        stubCatalogueLookup();
        when(userLanguageRepository.existsByUserIdAndLanguageId(USER_ID, LANGUAGE_ID)).thenReturn(false);
        when(userLanguageRepository.countByUserId(USER_ID)).thenReturn(existingEnrolments);
        when(userService.require(USER_ID)).thenReturn(mock(User.class));
        when(userLanguageRepository.save(any(UserLanguage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubCatalogueLookup() {
        when(language.getId()).thenReturn(LANGUAGE_ID);
        when(languageCatalogService.requireActiveByCode("en")).thenReturn(language);
    }
}
