package com.docuverify.util;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestIpUtil {

    private RequestIpUtil() {
    }

    public static String getClientIp(HttpServletRequest request) {
        // Trusting X-Forwarded-For directly is an IP spoofing vulnerability.
        // If behind a proxy, use Spring's server.forward-headers-strategy=framework
        return request.getRemoteAddr();
    }
}