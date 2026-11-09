package com.memora.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.memora.support.IntegrationTest;
import com.memora.support.RefreshCookies;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class AuthenticationFlowIntegrationTest {

    private static final String PASSWORD = "memora2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("registering returns an access token and a hardened refresh cookie")
    void registersAndOpensSession() throws Exception {
        MockHttpServletResponse response = register(uniqueEmail(), "Ana Souza");

        assertThat(response.getStatus()).isEqualTo(201);
        Map<String, Object> body = readBody(response);
        assertThat(body.get("accessToken")).asString().isNotBlank();
        assertThat(body.get("tokenType")).isEqualTo("Bearer");
        assertThat(body.get("expiresIn")).isEqualTo(900);

        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) body.get("user");
        assertThat(user.get("displayName")).isEqualTo("Ana Souza");
        assertThat(user).doesNotContainKey("passwordHash");

        RefreshCookies.assertHardened(response);
    }

    @Test
    @DisplayName("the registration response never carries the refresh token in its body")
    void refreshTokenStaysOutOfTheResponseBody() throws Exception {
        MockHttpServletResponse response = register(uniqueEmail(), "Ana Souza");

        String refreshToken = RefreshCookies.requireValue(response);
        assertThat(response.getContentAsString()).doesNotContain(refreshToken);
        assertThat(readBody(response)).doesNotContainKey("refreshToken");
    }

    @Test
    @DisplayName("a new account can immediately read its own profile with the access token")
    void authenticatesSubsequentRequests() throws Exception {
        String email = uniqueEmail();
        String accessToken = accessToken(register(email, "Ana Souza"));

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    @DisplayName("a new account starts at level 1 with zero XP, not with invented numbers")
    void initialisesProgressOnRegistration() throws Exception {
        String accessToken = accessToken(register(uniqueEmail(), "Ana Souza"));

        mockMvc.perform(get("/api/me/progress").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalXp").value(0))
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.xpForNextLevel").value(100))
                .andExpect(jsonPath("$.currentStreakDays").value(0))
                .andExpect(jsonPath("$.dailyGoalMinutes").value(15))
                .andExpect(jsonPath("$.lastStudyDate").doesNotExist());
    }

    @Test
    void rejectsUnauthenticatedRequestsWithTheStandardErrorShape() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/users/me"));
    }

    @Test
    void rejectsAGarbageBearerToken() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void rejectsDuplicateRegistration() throws Exception {
        String email = uniqueEmail();
        register(email, "Ana Souza");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(email.toUpperCase(), "Someone Else")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("a weak password is rejected with field-level violations")
    void validatesRegistrationPayload() throws Exception {
        String payload = objectMapper.writeValueAsString(
                Map.of("email", "not-an-email", "password", "short", "displayName", "A"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
    }

    @Test
    @DisplayName("login is case-insensitive on the email")
    void logsInIgnoringEmailCase() throws Exception {
        String email = uniqueEmail();
        register(email, "Ana Souza");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email.toUpperCase(), "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    @DisplayName("a wrong password and an unknown email produce the same answer")
    void revealsNothingAboutAccountExistence() throws Exception {
        String email = uniqueEmail();
        register(email, "Ana Souza");

        String wrongPassword = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", "wrongpassword1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String unknownEmail = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", uniqueEmail(), "password", PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(readMessage(wrongPassword)).isEqualTo(readMessage(unknownEmail));
    }

    @Test
    @DisplayName("refreshing issues a new access token and replaces the cookie")
    void refreshesSession() throws Exception {
        MockHttpServletResponse registration = register(uniqueEmail(), "Ana Souza");
        String firstRefreshToken = RefreshCookies.requireValue(registration);

        MockHttpServletResponse refreshed = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(RefreshCookies.asRequestCookie(firstRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn()
                .getResponse();

        String secondRefreshToken = RefreshCookies.requireValue(refreshed);
        assertThat(secondRefreshToken)
                .as("rotation must hand out a different token")
                .isNotEqualTo(firstRefreshToken);

        // The rotated token works, proving the new cookie is usable.
        mockMvc.perform(post("/api/auth/refresh").cookie(RefreshCookies.asRequestCookie(secondRefreshToken)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("replaying a consumed refresh token is refused and kills the whole family")
    void detectsRefreshTokenReuse() throws Exception {
        MockHttpServletResponse registration = register(uniqueEmail(), "Ana Souza");
        String stolenToken = RefreshCookies.requireValue(registration);

        MockHttpServletResponse rotated = mockMvc.perform(
                        post("/api/auth/refresh").cookie(RefreshCookies.asRequestCookie(stolenToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        String legitimateToken = RefreshCookies.requireValue(rotated);

        // Replay of the already-consumed token.
        mockMvc.perform(post("/api/auth/refresh").cookie(RefreshCookies.asRequestCookie(stolenToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        // The successor is revoked too: the session cannot be trusted any more.
        mockMvc.perform(post("/api/auth/refresh").cookie(RefreshCookies.asRequestCookie(legitimateToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void rejectsRefreshWithoutACookie() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    @DisplayName("logging out revokes the token and clears the cookie")
    void logsOut() throws Exception {
        MockHttpServletResponse registration = register(uniqueEmail(), "Ana Souza");
        String refreshToken = RefreshCookies.requireValue(registration);

        MockHttpServletResponse logout = mockMvc.perform(
                        post("/api/auth/logout").cookie(RefreshCookies.asRequestCookie(refreshToken)))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse();

        assertThat(RefreshCookies.header(logout))
                .get()
                .asString()
                .as("the cookie must be expired immediately")
                .contains("Max-Age=0");

        mockMvc.perform(post("/api/auth/refresh").cookie(RefreshCookies.asRequestCookie(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutWithoutACookieIsStillSuccessful() throws Exception {
        mockMvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent());
    }

    private MockHttpServletResponse register(String email, String displayName) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(email, displayName)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse();
    }

    private String registerPayload(String email, String displayName) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", PASSWORD,
                "displayName", displayName,
                "locale", "pt-BR",
                "timeZone", "America/Sao_Paulo"));
    }

    private String accessToken(MockHttpServletResponse response) throws Exception {
        return (String) readBody(response).get("accessToken");
    }

    private Map<String, Object> readBody(MockHttpServletResponse response) throws Exception {
        return objectMapper.readValue(response.getContentAsString(), new TypeReference<Map<String, Object>>() {});
    }

    private String readMessage(String json) {
        return (String) objectMapper
                .readValue(json, new TypeReference<Map<String, Object>>() {})
                .get("message");
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
