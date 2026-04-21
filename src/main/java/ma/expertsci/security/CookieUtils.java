package ma.expertsci.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public class CookieUtils {
    public static String extractRefreshTokenFromCookie(HttpServletRequest request) {

        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if ("Refresh_Token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
