package com.example.demo.presentation.controller;

import com.example.demo.application.dto.UsuarioRequest;
import com.example.demo.application.dto.UsuarioResponse;
import com.example.demo.application.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    @PostMapping
    public ResponseEntity<UsuarioResponse> save(@RequestBody @Valid UsuarioRequest usuario) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.save(usuario));
    }
}
