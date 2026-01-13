package dev.fnvir.kajz.apigateway.util;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;

public class IpUtils {
    
    private static final String[] IP_HEADERS = {
        "X-Forwarded-For",
        "X-Real-IP",
        "Proxy-Client-IP",       // Apache / WebLogic
        "WL-Proxy-Client-IP",    // WebLogic
        "CF-Connecting-IP",      // Cloudflare
        "True-Client-IP",
        "X-Client-IP",
        "Forwarded",
        "Forwarded-For",
        "HTTP_CLIENT_IP",        // Some older proxies
        "HTTP_X_FORWARDED_FOR"   // Legacy format
    };
    
    public static String getClientIp(ServerHttpRequest request) {
        for (String header : IP_HEADERS) {
            String ip = request.getHeaders().getFirst(header);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                String[] ips = ip.split(",");
                for (String clientIp : ips) {
                    clientIp = clientIp.trim();
                    if (StringUtils.hasText(clientIp) && !"unknown".equalsIgnoreCase(clientIp)) {
                        return clientIp;
                    }
                }
            }
        }
        var remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            String hostAddress = remoteAddress.getAddress().getHostAddress();
            if (hostAddress != null)
                return hostAddress.equals("::1") ? "127.0.0.1" : hostAddress;
        }
        return "unknown";
    }

}
