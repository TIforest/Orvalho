package com.mycompany.bebaAgua;

import java.io.IOException;
import java.time.LocalDate;

/** Progresso pessoal local: somente volumes confirmados, nunca o tempo do lembrete. */
record ProgressoHidratacao(long ml, double referenciaMl) {
    static ProgressoHidratacao carregar(DadosAgua dados, LocalDate dia) throws IOException {
        long ml = dados.historico(dia).stream().mapToLong(DadosAgua.Pausa::ml).sum();
        String peso = EstimativaHidratacao.carregar(dados);
        double referencia = peso.isBlank() ? 0 : EstimativaHidratacao.peso(peso)
                .multiply(new java.math.BigDecimal("35")).doubleValue();
        return new ProgressoHidratacao(ml, referencia);
    }
    double fracao() { return referenciaMl > 0 ? Math.min(1, Math.max(0, ml / referenciaMl)) : 0; }
}
