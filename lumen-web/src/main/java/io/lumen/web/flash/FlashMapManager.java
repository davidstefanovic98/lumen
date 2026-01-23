package io.lumen.web.flash;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;

public class FlashMapManager {
    private static final String FLASH_MAP_SESSION_KEY = "LUMEN_FLASH_MAP";

    @SuppressWarnings("unchecked")
    public static void save(HttpServletRequest request, Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty())
            return;

        HttpSession session = request.getSession();
        Map<String, Object> flashMap = (Map<String, Object>) session.getAttribute(FLASH_MAP_SESSION_KEY);

        if (flashMap == null) {
            flashMap = new HashMap<>();
        }
        flashMap.putAll(attributes);
        session.setAttribute(FLASH_MAP_SESSION_KEY, flashMap);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> consume(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null)
            return null;

        Map<String, Object> flashMap = (Map<String, Object>) session.getAttribute(FLASH_MAP_SESSION_KEY);
        if (flashMap != null) {
            session.removeAttribute(FLASH_MAP_SESSION_KEY);
        }
        return flashMap;
    }
}
