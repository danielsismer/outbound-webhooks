package com.example.demo.application.service;

import com.example.demo.application.dto.UsuarioRequest;
import com.example.demo.application.dto.UsuarioResponse;
import com.example.demo.application.mapper.UsuarioMapper;
import com.example.demo.domain.event.UsuarioCriadoEvent;
import com.example.demo.domain.model.Usuario;
import com.example.demo.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void devePublicarEventoAoSalvarUsuario() {
        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario salvo = invocation.getArgument(0);
            salvo.setId(42L);
            salvo.setCriadoEm(Instant.parse("2026-07-29T12:00:00Z"));
            return salvo;
        });

        UsuarioService service = new UsuarioService(repository, new UsuarioMapper(), eventPublisher);

        UsuarioResponse response = service.save(new UsuarioRequest("Ana"));

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.nome()).isEqualTo("Ana");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());

        assertThat(captor.getValue()).isInstanceOf(UsuarioCriadoEvent.class);
        UsuarioCriadoEvent evento = (UsuarioCriadoEvent) captor.getValue();
        assertThat(evento.eventType()).isEqualTo("usuario.criado");
        assertThat(evento.eventId()).isNotBlank();
        assertThat(evento.data().id()).isEqualTo(42L);
        assertThat(evento.data().nome()).isEqualTo("Ana");
    }
}
