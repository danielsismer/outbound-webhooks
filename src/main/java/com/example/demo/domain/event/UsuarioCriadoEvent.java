package com.example.demo.domain.event;

import com.example.demo.domain.model.Usuario;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento publicado depois que um usuario e persistido com sucesso.
 */
public record UsuarioCriadoEvent(
        String eventId,
        Instant occurredAt,
        UsuarioData data
) implements DomainEvent {

    public static final String TYPE = "usuario.criado";

    public record UsuarioData(Long id, String nome, Instant criadoEm) {
    }

    public static UsuarioCriadoEvent de(Usuario usuario) {
        return new UsuarioCriadoEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                new UsuarioData(usuario.getId(), usuario.getNome(), usuario.getCriadoEm())
        );
    }

    @Override
    public String eventType() {
        return TYPE;
    }
}
