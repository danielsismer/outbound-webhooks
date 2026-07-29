package com.example.demo.application.mapper;

import com.example.demo.application.dto.UsuarioRequest;
import com.example.demo.application.dto.UsuarioResponse;
import com.example.demo.domain.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public Usuario toEntity(UsuarioRequest request) {
        return new Usuario(request.nome());
    }

    public UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getCriadoEm());
    }
}
