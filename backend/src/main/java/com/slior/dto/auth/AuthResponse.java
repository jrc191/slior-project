package com.slior.dto.auth;

import com.slior.model.enums.UserRole;

import java.util.UUID;

/**
 * DTO de salida para los endpoints de autenticación.
 * Contiene el JWT y datos básicos del usuario.
 * NUNCA incluye el password.
 */
public record AuthResponse(

        String token,

        String type,       // Siempre "Bearer"

        UUID userId,

        String nombre,

        String email,

        UserRole rol
) {}