package com.demo.ticket.security;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NullMarked;

/** 取得不包含應用程式 context path 的請求路徑，供 Filter 共用。 */
@NullMarked
public final class RequestPathUtils {

    private RequestPathUtils() {
    }

    public static String path(HttpServletRequest request) {
        return request.getRequestURI()
                .substring(request.getContextPath().length());
    }
}
