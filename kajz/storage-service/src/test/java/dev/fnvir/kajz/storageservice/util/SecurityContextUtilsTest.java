package dev.fnvir.kajz.storageservice.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for {@link SecurityContextUtils}.
 */
public class SecurityContextUtilsTest {

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(String principal, String... roles) {
        for (int i = 0; i < roles.length; i++)
            roles[i] = roles[i].startsWith("ROLE_") ? roles[i] : "ROLE_" + roles[i];
        var auth = new TestingAuthenticationToken(principal, null, roles);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("getAuthentication should return null when not authenticated")
    void getAuthentication_shouldReturnNullWhenNotAuthenticated() {
        assertNull(SecurityContextUtils.getAuthentication());
    }

    @Test
    @DisplayName("getAuthentication should return authentication when set")
    void getAuthentication_shouldReturnAuthWhenSet() {
        setAuthentication(testUserId.toString(), "USER");
        
        assertNotNull(SecurityContextUtils.getAuthentication());
        assertEquals(testUserId.toString(), SecurityContextUtils.getAuthentication().getName());
    }

    @Test
    @DisplayName("getCurrentUser should return empty when not authenticated")
    void getCurrentUser_shouldReturnEmptyWhenNotAuthenticated() {
        Optional<UUID> user = SecurityContextUtils.getCurrentUser();
        
        assertTrue(user.isEmpty());
    }

    @Test
    @DisplayName("getCurrentUser should return UUID when authenticated with valid UUID")
    void getCurrentUser_shouldReturnUuidWhenAuthenticated() {
        setAuthentication(testUserId.toString(), "USER");
        
        Optional<UUID> user = SecurityContextUtils.getCurrentUser();
        
        assertTrue(user.isPresent());
        assertEquals(testUserId, user.get());
    }

    @Test
    @DisplayName("getCurrentUser should return empty when principal is not a valid UUID")
    void getCurrentUser_shouldReturnEmptyForInvalidUuid() {
        setAuthentication("not-a-uuid", "USER");
        
        Optional<UUID> user = SecurityContextUtils.getCurrentUser();
        
        assertTrue(user.isEmpty());
    }

    @Test
    @DisplayName("isAuthenticated should return false when not authenticated")
    void isAuthenticated_shouldReturnFalseWhenNotAuthenticated() {
        assertFalse(SecurityContextUtils.isAuthenticated());
    }

    @Test
    @DisplayName("isAuthenticated should return true when authenticated with valid UUID")
    void isAuthenticated_shouldReturnTrueWhenAuthenticated() {
        setAuthentication(testUserId.toString(), "USER");
        
        assertTrue(SecurityContextUtils.isAuthenticated());
    }

    @Test
    @DisplayName("isAuthenticated should return false when principal is not a valid UUID")
    void isAuthenticated_shouldReturnFalseForInvalidUuid() {
        setAuthentication("invalid-user", "USER");
        
        assertFalse(SecurityContextUtils.isAuthenticated());
    }

    @Test
    @DisplayName("getAuthorities should return empty list when not authenticated")
    void getAuthorities_shouldReturnEmptyWhenNotAuthenticated() {
        Collection<? extends GrantedAuthority> authorities = SecurityContextUtils.getAuthorities();
        
        assertNotNull(authorities);
        assertTrue(authorities.isEmpty());
    }

    @Test
    @DisplayName("getAuthorities should return authorities when authenticated")
    void getAuthorities_shouldReturnAuthoritiesWhenAuthenticated() {
        setAuthentication(testUserId.toString(), "USER", "ADMIN");
        
        Collection<? extends GrantedAuthority> authorities = SecurityContextUtils.getAuthorities();
        
        assertEquals(2, authorities.size());
    }

    @Test
    @DisplayName("hasAnyRole should return false when not authenticated")
    void hasAnyRole_shouldReturnFalseWhenNotAuthenticated() {
        assertFalse(SecurityContextUtils.hasAnyRole("USER"));
        assertFalse(SecurityContextUtils.hasAnyRole("ADMIN", "USER"));
    }

    @Test
    @DisplayName("hasAnyRole should return true when user has matching role")
    void hasAnyRole_shouldReturnTrueWhenHasRole() {
        setAuthentication(testUserId.toString(), "USER");
        
        assertTrue(SecurityContextUtils.hasAnyRole("USER"));
        assertTrue(SecurityContextUtils.hasAnyRole("ADMIN", "USER"));
    }

    @Test
    @DisplayName("hasAnyRole should return false when user lacks role")
    void hasAnyRole_shouldReturnFalseWhenLacksRole() {
        setAuthentication(testUserId.toString(), "USER");
        
        assertFalse(SecurityContextUtils.hasAnyRole("ADMIN"));
        assertFalse(SecurityContextUtils.hasAnyRole("ADMIN", "SUPERUSER"));
    }

    @Test
    @DisplayName("hasAnyRole should be case-insensitive")
    void hasAnyRole_shouldBeCaseInsensitive() {
        setAuthentication(testUserId.toString(), "USER");
        
        assertTrue(SecurityContextUtils.hasAnyRole("user"));
        assertTrue(SecurityContextUtils.hasAnyRole("User"));
        assertTrue(SecurityContextUtils.hasAnyRole("USER"));
    }

    @Test
    @DisplayName("hasAnyRole should work with or without ROLE_ prefix")
    void hasAnyRole_shouldWorkWithOrWithoutPrefix() {
        setAuthentication(testUserId.toString(), "USER");
        
        assertTrue(SecurityContextUtils.hasAnyRole("USER"));
        assertTrue(SecurityContextUtils.hasAnyRole("ROLE_USER"));
    }

    @Test
    @DisplayName("hasAnyRole with List should work correctly")
    void hasAnyRole_withListShouldWork() {
        setAuthentication(testUserId.toString(), "USER", "MODERATOR");
        
        assertTrue(SecurityContextUtils.hasAnyRole(List.of("USER")));
        assertTrue(SecurityContextUtils.hasAnyRole(List.of("ADMIN", "MODERATOR")));
        assertFalse(SecurityContextUtils.hasAnyRole(List.of("ADMIN", "SUPERUSER")));
    }

    @Test
    @DisplayName("matchesUserIdOrHasAnyRole should return false when not authenticated")
    void matchesUserIdOrHasAnyRole_shouldReturnFalseWhenNotAuthenticated() {
        assertFalse(SecurityContextUtils.matchesUserIdOrHasAnyRole(testUserId, "ADMIN"));
    }

    @Test
    @DisplayName("matchesUserIdOrHasAnyRole should return true when userId matches")
    void matchesUserIdOrHasAnyRole_shouldReturnTrueWhenUserIdMatches() {
        setAuthentication(testUserId.toString(), "USER");
        
        assertTrue(SecurityContextUtils.matchesUserIdOrHasAnyRole(testUserId, "ADMIN"));
    }

    @Test
    @DisplayName("matchesUserIdOrHasAnyRole should return true when user has role")
    void matchesUserIdOrHasAnyRole_shouldReturnTrueWhenHasRole() {
        setAuthentication(testUserId.toString(), "ADMIN");
        UUID differentUserId = UUID.randomUUID();
        
        assertTrue(SecurityContextUtils.matchesUserIdOrHasAnyRole(differentUserId, "ADMIN"));
    }

    @Test
    @DisplayName("matchesUserIdOrHasAnyRole should return false when neither matches")
    void matchesUserIdOrHasAnyRole_shouldReturnFalseWhenNeitherMatches() {
        setAuthentication(testUserId.toString(), "USER");
        UUID differentUserId = UUID.randomUUID();
        
        assertFalse(SecurityContextUtils.matchesUserIdOrHasAnyRole(differentUserId, "ADMIN"));
    }

}
