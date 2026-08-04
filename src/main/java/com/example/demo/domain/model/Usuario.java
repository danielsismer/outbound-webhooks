package com.example.demo.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name="users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = Generation.IDENTITY)
    private Long id;

    @Column(name="name", nullable=false)
    private String name;

    @Column(name="email", nullable=false, unique=true)
    private String email;

    @Column(nullable=false, updatable=false, name="created_at")
    private Instant created_at;

    @Column(nullable=false, insertable=false, name="updated_at")
    private Instant updated_at;

    @PrePersist
    void onSave(){
        if(created_at == null){
            this.created_at = Instant.now();
        }
    }

    @PreUpdate
    void onUpdate(){
        if(updated_at == null){
            this.updated_at = Instant.now();
        }
    }

    public Usuario (String name, String email){
        this.name = name;
        this.email = email;
    }



}