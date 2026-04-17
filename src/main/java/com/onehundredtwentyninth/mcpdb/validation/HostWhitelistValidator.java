package com.onehundredtwentyninth.mcpdb.validation;

import java.net.IDN;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class HostWhitelistValidator {

    public void validate(String host, List<String> whitelist) {
        var normalizedHost = normalize(host);
        var allowed = whitelist.stream()
                .map(this::normalize)
                .anyMatch(allowedHost -> allowedHost.equals(normalizedHost));
        if (!allowed) {
            throw new IllegalArgumentException("DB host '%s' не входит в whitelist".formatted(host));
        }
    }

    private String normalize(String host) {
        return IDN.toASCII(host.trim()).toLowerCase(Locale.ROOT);
    }
}
