package com.example.demo.application.dto;

import java.time.Instant;

public record UsuarioResponse(
        Long id,
        String nome,
        Instant criadoEm
) {
}
