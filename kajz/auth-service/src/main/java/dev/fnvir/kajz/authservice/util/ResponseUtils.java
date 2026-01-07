package dev.fnvir.kajz.authservice.util;

import java.util.Map;

import jakarta.ws.rs.core.Response;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

public final class ResponseUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Map<String, Object> jakartaResponseToMap(Response response) {
        String body = response.readEntity(String.class);
        String contentType = response.getHeaderString("Content-Type");

        try {
            if (contentType != null && contentType.contains("application/json")) {
                return objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {}

        return Map.of("message", body);
    }
}
