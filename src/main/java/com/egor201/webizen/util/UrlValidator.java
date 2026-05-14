package com.egor201.webizen.util;

import java.net.InetAddress;
import java.net.URI;
import java.util.List;

public class UrlValidator {

    private static final List<String> BLOCKED_HOSTNAMES = List.of(
        "localhost", "0.0.0.0"
    );

    public static ValidationResult validate(String url) {
        if (url == null || url.isBlank()) return new ValidationResult(false, "URL is empty");

        URI uri;
        try {
            uri = new URI(url);
        } catch (Exception e) {
            return new ValidationResult(false, "Malformed URL: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equals("http") && !scheme.equals("https") && !scheme.equals("ws") && !scheme.equals("wss"))) {
            return new ValidationResult(false, "Blocked URL scheme: " + scheme);
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return new ValidationResult(false, "No host in URL");
        }

        for (String blocked : BLOCKED_HOSTNAMES) {
            if (host.equalsIgnoreCase(blocked)) {
                return new ValidationResult(false, "Blocked host: " + host);
            }
        }

        try {
            InetAddress addr = InetAddress.getByName(host);
            if (addr.isLoopbackAddress()) {
                return new ValidationResult(false, "Blocked loopback address: " + host);
            }
            if (addr.isLinkLocalAddress()) {
                return new ValidationResult(false, "Blocked link-local address: " + host);
            }
            if (addr.isSiteLocalAddress()) {
                return new ValidationResult(false, "Blocked private/site-local address: " + host);
            }
            if (addr.isAnyLocalAddress()) {
                return new ValidationResult(false, "Blocked any-local address: " + host);
            }
            byte[] bytes = addr.getAddress();
            if (bytes.length == 4 && (bytes[0] & 0xFF) == 169 && (bytes[1] & 0xFF) == 254) {
                return new ValidationResult(false, "Blocked metadata address: " + host);
            }
        } catch (Exception e) {
            // DNS lookup failed — allow (may be a valid public host)
        }

        return new ValidationResult(true, null);
    }

    public record ValidationResult(boolean valid, String reason) {}
}
