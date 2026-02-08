package com.example.article

/**
 * User roles in the Article app
 */
enum class UserRole {
    /**
     * Regular community member
     * - Can create posts
     * - Can create service requests
     * - Can chat with other members and service providers
     */
    MEMBER,

    /**
     * Service provider
     * - Can only view feed (read-only)
     * - Cannot create posts or announcements
     * - Can accept and complete service requests
     * - Can chat with members who request services
     */
    SERVICE_PROVIDER,

    /**
     * Community administrator
     * - Can create posts and announcements
     * - Can manage service requests as a member
     * - Can moderate content
     * - Full access to all features
     */
    ADMIN;

    companion object {
        /**
         * Convert string role from Firestore to UserRole enum
         */
        fun from(roleString: String): UserRole {
            return when (roleString.lowercase()) {
                "service_provider", "serviceprovider", "provider" -> SERVICE_PROVIDER
                "admin", "administrator" -> ADMIN
                else -> MEMBER
            }
        }
    }
}