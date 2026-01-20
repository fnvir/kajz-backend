package dev.fnvir.kajz.notificationservice.dto.res;

import java.time.Instant;
import java.util.List;

import lombok.Builder;

/**
 * Cursor based pagination response.
 * 
 * @param <T>        The type of the content.
 * @param content    The list of content.
 * @param nextCursor The next cursor position to fetch more results.
 */
@Builder
public record CursorPageResponse<T> (
        List<T> content,
        Instant nextCursor
) {}
