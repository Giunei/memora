package com.memora.shared.security;

import java.util.UUID;

/**
 * The principal attached to the security context for an authenticated request.
 *
 * <p>Deliberately minimal: it carries identity only. Authorities live on the
 * {@code Authentication} object where Spring Security expects them, so this type
 * stays free of any module dependency and can be injected into any controller
 * via {@code @AuthenticationPrincipal}.
 */
public record AuthenticatedUser(UUID id, String email) {}
