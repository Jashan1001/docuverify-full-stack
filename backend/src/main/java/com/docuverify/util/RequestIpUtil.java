package com.docuverify.util;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestIpUtil {

    private RequestIpUtil() {
    }

    public static String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}