package com.memora.languages;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.memora.support.IntegrationTest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class LanguageApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("the seeded catalogue is paginated and searchable")
    void listsCatalogue() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(get("/api/languages").param("size", "5").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));

        mockMvc.perform(get("/api/languages")
                        .param("search", "japan")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].code").value("ja"));
    }

    @Test
    void rejectsAPageSizeAboveTheCap() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(get("/api/languages").param("size", "500").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void requiresAuthenticationToBrowseTheCatalogue() throws Exception {
        mockMvc.perform(get("/api/languages")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("the first enrolment becomes the active focus and shows up on the dashboard")
    void enrolsInALanguage() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(post("/api/me/languages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "languageCode", "en",
                                "currentLevel", "A2",
                                "targetLevel", "B2",
                                "primaryFocus", false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.language.code").value("en"))
                .andExpect(jsonPath("$.language.name").value("English"))
                .andExpect(jsonPath("$.currentLevel").value("A2"))
                .andExpect(jsonPath("$.primaryFocus").value(true))
                .andExpect(jsonPath("$.lastStudiedAt").doesNotExist());

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress.level").value(1))
                .andExpect(jsonPath("$.progress.totalXp").value(0))
                .andExpect(jsonPath("$.languages.length()").value(1))
                .andExpect(jsonPath("$.languages[0].language.code").value("en"));
    }

    @Test
    @DisplayName("only one language can hold the focus at a time")
    void movingFocusClearsThePrevious() throws Exception {
        String token = registerAndGetToken();
        enrol(token, "en", false);
        String spanishId = enrol(token, "es", true);

        mockMvc.perform(get("/api/me/languages").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // Ordered focus-first by the repository query.
                .andExpect(jsonPath("$[0].id").value(spanishId))
                .andExpect(jsonPath("$[0].primaryFocus").value(true))
                .andExpect(jsonPath("$[1].primaryFocus").value(false));
    }

    @Test
    void updatesLevelsOfAnEnrolment() throws Exception {
        String token = registerAndGetToken();
        String enrolmentId = enrol(token, "de", true);

        mockMvc.perform(put("/api/me/languages/" + enrolmentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentLevel", "B1", "targetLevel", "C1", "primaryFocus", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentLevel").value("B1"))
                .andExpect(jsonPath("$.targetLevel").value("C1"));
    }

    @Test
    @DisplayName("removing the focused language promotes the longest-running one")
    void promotesAnotherLanguageWhenTheFocusIsRemoved() throws Exception {
        String token = registerAndGetToken();
        enrol(token, "en", false);
        String spanishId = enrol(token, "es", true);

        mockMvc.perform(delete("/api/me/languages/" + spanishId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/me/languages").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].language.code").value("en"))
                .andExpect(jsonPath("$[0].primaryFocus").value(true));
    }

    @Test
    void rejectsATargetLevelBelowTheCurrentOne() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(post("/api/me/languages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "languageCode", "en",
                                "currentLevel", "C1",
                                "targetLevel", "A2",
                                "primaryFocus", false))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void rejectsAnUnknownLanguageCode() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(post("/api/me/languages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "languageCode", "xx",
                                "currentLevel", "A1",
                                "targetLevel", "B1",
                                "primaryFocus", false))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void rejectsEnrollingTwiceInTheSameLanguage() throws Exception {
        String token = registerAndGetToken();
        enrol(token, "fr", true);

        mockMvc.perform(post("/api/me/languages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "languageCode", "fr",
                                "currentLevel", "A1",
                                "targetLevel", "B1",
                                "primaryFocus", false))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("one account cannot touch another account's enrolment")
    void scopesEnrolmentsToTheirOwner() throws Exception {
        String ownerToken = registerAndGetToken();
        String enrolmentId = enrol(ownerToken, "it", true);
        String otherToken = registerAndGetToken();

        mockMvc.perform(get("/api/me/languages/" + enrolmentId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/me/languages/" + enrolmentId).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("AI reports itself unavailable when it is not configured, without leaking a key")
    void reportsAiStatus() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(get("/api/ai/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.provider").value("gemini"))
                .andExpect(jsonPath("$.apiKey").doesNotExist());
    }

    @Test
    void changesTheDailyGoal() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(put("/api/me/progress/daily-goal")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("dailyGoalMinutes", 45))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyGoalMinutes").value(45));

        mockMvc.perform(put("/api/me/progress/daily-goal")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("dailyGoalMinutes", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private String enrol(String token, String languageCode, boolean primaryFocus) throws Exception {
        String response = mockMvc.perform(post("/api/me/languages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "languageCode", languageCode,
                                "currentLevel", "A1",
                                "targetLevel", "B1",
                                "primaryFocus", primaryFocus))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return (String) readMap(response).get("id");
    }

    private String registerAndGetToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "learner-" + UUID.randomUUID() + "@example.com",
                                "password", "memora2026",
                                "displayName", "Learner"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return (String) readMap(response).get("accessToken");
    }

    private Map<String, Object> readMap(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }
}
