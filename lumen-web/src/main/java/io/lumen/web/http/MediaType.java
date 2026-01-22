package io.lumen.web.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record MediaType(String type, String subtype, Map<String, String> parameters) {

    public static final MediaType ALL = new MediaType("*", "*", Collections.emptyMap());
    public static final MediaType APPLICATION_JSON = new MediaType("application", "json", Collections.emptyMap());
    public static final MediaType TEXT_PLAIN = new MediaType("text", "plain", Collections.emptyMap());
    public static final MediaType APPLICATION_OCTET_STREAM = new MediaType("application", "octet-stream", Collections.emptyMap());

    public static MediaType parse(String mediaType) {
        if (mediaType == null || mediaType.isBlank()) return ALL;

        String[] parts = mediaType.split(";");
        String[] fullType = parts[0].trim().split("/");

        if (fullType.length != 2) return ALL;

        Map<String, String> params = new HashMap<>();
        for (int i = 1; i < parts.length; i++) {
            String[] param = parts[i].split("=");
            if (param.length == 2) {
                params.put(param[0].trim(), param[1].trim());
            }
        }
        return new MediaType(fullType[0].toLowerCase(), fullType[1].toLowerCase(), params);
    }

    public boolean isCompatibleWith(MediaType other) {
        if (other == null) return false;
        if (this.type.equals("*") || other.type.equals("*")) return true;
        if (!this.type.equalsIgnoreCase(other.type)) return false;
        if (this.subtype.equals("*") || other.subtype.equals("*")) return true;
        return this.subtype.equalsIgnoreCase(other.subtype);
    }

    /**
     * Returns the 'q' parameter value as a double. Defaults to 1.0.
     */
    public double getQuality() {
        String q = parameters.get("q");
        if (q == null) return 1.0;
        try {
            return Double.parseDouble(q);
        } catch (NumberFormatException e) {
            return 1.0;
        }
    }

    @Override
    public String toString() {
        return type + "/" + subtype;
    }
}