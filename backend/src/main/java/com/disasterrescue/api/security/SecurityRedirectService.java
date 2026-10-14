package com.disasterrescue.api.security;

import com.disasterrescue.api.model.RoleCode;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;

@Service
public class SecurityRedirectService {

    private final Map<RoleCode, String> roleRedirectMap = new EnumMap<>(RoleCode.class);

    public SecurityRedirectService() {
        // Defined in one single place for easy route adjustments
        roleRedirectMap.put(RoleCode.ADMIN, "/admin");
        roleRedirectMap.put(RoleCode.OPERATOR, "/dieu-phoi");
        roleRedirectMap.put(RoleCode.RESCUER, "/nhiem-vu");
        roleRedirectMap.put(RoleCode.CITIZEN, "/gui-yeu-cau");
    }

    public String getRedirectPathForRole(RoleCode roleCode) {
        if (roleCode == null) {
            return "/";
        }
        return roleRedirectMap.getOrDefault(roleCode, "/");
    }

    /**
     * Sanitizes and validates internal redirect paths:
     * Must start with a single '/' and not with '//' to prevent open redirect vulnerabilities.
     */
    public boolean isValidInternalRedirect(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        return path.startsWith("/") && !path.startsWith("//") && !path.contains("://") && !path.contains("\\");
    }
}
