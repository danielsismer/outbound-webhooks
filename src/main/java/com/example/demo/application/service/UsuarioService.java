package com.example.demo.application.service;

import com.example.demo.application.dto.UsuarioRequest;
import com.example.demo.application.dto.UsuarioResponse;
import com.example.demo.application.mapper.UsuarioMapper;
import com.example.demo.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repository;
    private final UsuarioMapper mapper;

    @Transactional
    public UsuarioResponse save(UsuarioRequest request) {
        return mapper.toResponse(repository.save(mapper.toEntity(request)));
    }
}
