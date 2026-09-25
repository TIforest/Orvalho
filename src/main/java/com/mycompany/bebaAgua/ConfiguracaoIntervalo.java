package com.mycompany.bebaAgua;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Um único arquivo de intervalo para a execução manual e a inicialização automática. */
final class ConfiguracaoIntervalo {
    private final Path arquivo;

    ConfiguracaoIntervalo() {
        this(Path.of(System.getProperty("user.home"), ".orvalho", "intervalo.txt"));
    }

    ConfiguracaoIntervalo(Path arquivo) { this.arquivo = arquivo; }

    int carregar(int valorAntigo) throws IOException {
        if (!Files.exists(arquivo)) {
            int inicial = valido(valorAntigo) ? valorAntigo : 30;
            salvar(inicial);
            return inicial;
        }
        try {
            int minutos = Integer.parseInt(Files.readString(arquivo, StandardCharsets.UTF_8).strip());
            return valido(minutos) ? minutos : 30;
        } catch (NumberFormatException e) {
            return 30;
        }
    }

    void salvar(int minutos) throws IOException {
        if (!valido(minutos)) throw new IllegalArgumentException("Informe de 1 a 240 minutos.");
        Path pasta = arquivo.toAbsolutePath().getParent();
        Files.createDirectories(pasta);
        Path temporario = Files.createTempFile(pasta, "intervalo-", ".tmp");
        try {
            Files.writeString(temporario, Integer.toString(minutos), StandardCharsets.UTF_8);
            try (FileChannel canal = FileChannel.open(temporario, StandardOpenOption.WRITE)) {
                canal.force(true);
            }
            try {
                Files.move(temporario, arquivo, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    private static boolean valido(int minutos) { return minutos >= 1 && minutos <= 240; }
}