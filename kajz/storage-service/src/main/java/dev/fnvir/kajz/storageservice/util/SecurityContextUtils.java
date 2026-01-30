package dev.fnvir.kajz.storageservice.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SecurityContextUtils {
    
    
    private static final String ROLE_PREFIX = "ROLE_";

    private SecurityContextUtils() {}
    
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static Optional<UUID> getCurrentUser() {
        var auth = getAuthentication();
        if (auth == null)
            return Optional.empty();
        try {
            return Optional.ofNullable(UUID.fromString(auth.getName()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
    
    public static boolean isAuthenticated() {
        return getCurrentUser().isPresent();
    }

    public static Collection<? extends GrantedAuthority> getAuthorities() {
        var auth = getAuthentication();
        if (auth == null)
            return List.of();
        return auth.getAuthorities();
    }

    public static boolean hasAnyRole(String... roles) {
        return hasAnyRole(List.of(roles));
    }

    public static boolean hasAnyRole(List<String> roles) {
        List<String> authorities = getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();
        for (var role : roles) {
            role = role.toUpperCase();
            if (!role.startsWith(ROLE_PREFIX))
                role = ROLE_PREFIX + role;
            if (authorities.contains(role)) {   // list.contains is ok since size is small
                return true;                    // (creating a set will add more overhead than small O(N) search)
            }
        }
        return false;
    }

    public static boolean matchesUserIdOrHasAnyRole(UUID userId, String... roles) {
        var auth = getAuthentication();
        if (auth == null || auth.getName() == null)
            return false;

        UUID authUserId = null;

        try {
            authUserId = UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            log.error("Unable to map authentication name to UUID: {}", e.getMessage());
        }

        if (authUserId != null && authUserId.equals(userId)) {
            return true;
        }

        List<String> roleList = Arrays.stream(roles).map(role -> {
            if (!role.startsWith(ROLE_PREFIX)) {
                role = ROLE_PREFIX + role;
            }
            return role;
        }).toList();

        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(roleList::contains);

    }

}
