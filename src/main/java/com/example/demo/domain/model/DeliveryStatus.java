package com.example.demo.domain.model;

public enum DeliveryStatus {
    /** Entregue: o destino respondeu 2xx. */
    SUCCESS,
    /** Todas as tentativas falharam. */
    FAILED
}
