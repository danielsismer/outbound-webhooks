package com.example.demo.application.service;

import com.example.demo.application.dto.UsuarioRequest;
import com.example.demo.application.dto.UsuarioResponse;
import com.example.demo.application.mapper.UsuarioMapper;
import com.example.demo.domain.event.UsuarioCriadoEvent;
import com.example.demo.domain.model.Usuario;
import com.example.demo.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repository;
    private final UsuarioMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Persiste o usuario e publica {@link UsuarioCriadoEvent}. A publicacao acontece dentro da
     * transacao, mas os consumidores registrados com {@code AFTER_COMMIT} so sao acionados depois
     * do commit — se a transacao falhar, nenhum webhook e disparado.
     */
    @Transactional
    public UsuarioResponse save(UsuarioRequest request) {
        Usuario usuario = repository.save(mapper.toEntity(request));
        eventPublisher.publishEvent(UsuarioCriadoEvent.de(usuario));
        return mapper.toResponse(usuario);
    }
}
