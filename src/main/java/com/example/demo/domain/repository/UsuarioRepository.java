package com.example.demo.domain.repository;

import com.example.demo.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
        @Query( value = 
            "SELECT id, name, email, created_at
                FROM users
                JOIN webhook w
                ON w.user_id = w.id
                LEFT JOIN subscription s
                ON s.id = w.topic_exchange
                JOIN delivery d
                ON d.id = s.delivered_by
                WHERE w.id = :id"
        , nativeQuery=true)
        public Usuario findUserByWebhook(@Param("id") UUID id)
    
\
}
