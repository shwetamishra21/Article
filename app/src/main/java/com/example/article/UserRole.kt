package com.example.article

enum class UserRole {
    MEMBER,
    SERVICE_PROVIDER,
    ADMIN;

    companion object {
        fun from(value: String): UserRole =
            when (value.lowercase()) {
                "admin" -> ADMIN
                "service_provider" -> SERVICE_PROVIDER
                else -> MEMBER
            }
    }
}

