package com.logcollector.config.security;

import org.springframework.context.annotation.Configuration;

/**
 * RBAC role-based method security configuration.
 *
 * Defines the three roles: ADMIN, OPERATOR, VIEWER
 * Role enforcement is handled through @PreAuthorize annotations on methods.
 */
@Configuration
public class RoleConfig {

    /**
     * Role definitions:
     *
     * ADMIN: Full access
     * - Configure servers
     * - Manage alert rules
     * - View audit logs
     * - Trigger manual archival
     *
     * OPERATOR: Operational access
     * - View transactions and logs
     * - Acknowledge alerts
     * - View server configurations (read-only)
     *
     * VIEWER: Read-only access
     * - View logs and transactions
     * - View alerts (cannot acknowledge)
     */

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_OPERATOR = "OPERATOR";
    public static final String ROLE_VIEWER = "VIEWER";
}
