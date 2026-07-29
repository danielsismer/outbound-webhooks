package com.example.demo.infrastructure.webhook;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Assina o corpo do webhook com HMAC-SHA256 para que o consumidor consiga provar
 * que o payload veio desta aplicacao e nao foi alterado no caminho.
 */
@Component
public class WebhookSigner {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String PREFIX = "sha256=";

    private final SecureRandom random = new SecureRandom();

    /** Gera um segredo aleatorio para uma nova inscricao. */
    public String generateSecret() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /** Retorna a assinatura no formato {@code sha256=<hex>}. */
    public String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return PREFIX + HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao assinar payload do webhook", e);
        }
    }

    /**
     * Valida a assinatura recebida. A comparacao usa {@link MessageDigest#isEqual} para
     * nao vazar informacao por tempo de execucao.
     */
    public boolean isValid(String payload, String secret, String signature) {
        if (signature == null || secret == null) {
            return false;
        }
        byte[] esperada = sign(payload, secret).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(esperada, signature.getBytes(StandardCharsets.UTF_8));
    }
}
