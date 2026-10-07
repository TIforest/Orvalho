package com.mycompany.bebaAgua;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;

final class DiagnosticoAtualizacao {
    static Path pasta() { return Path.of(System.getProperty("user.home"), ".orvalho"); }

    static synchronized void registrar(String mensagem) {
        try { registrar(pasta().resolve("atualizacao.log"), mensagem); }
        catch (IOException ignored) { }
    }

    static synchronized void registrar(Path log, String mensagem) throws IOException {
        Files.createDirectories(log.getParent());
        if (Files.exists(log) && Files.size(log) >= 1_048_576) {
            Files.move(log, log.resolveSibling(log.getFileName() + ".1"), StandardCopyOption.REPLACE_EXISTING);
        }
        Files.writeString(log, Instant.now() + " " + mensagem + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    static String explicar(Throwable erro) {
        for (Throwable causa = erro; causa != null; causa = causa.getCause()) {
            if (causa instanceof javax.net.ssl.SSLHandshakeException)
                return "Falha no certificado HTTPS. Peça ao TI para conferir a inspeção HTTPS e os certificados da empresa.";
            if (causa instanceof java.net.UnknownHostException || causa instanceof java.net.ConnectException)
                return "Sem acesso ao servidor. Confira internet, proxy, firewall e antivírus.";
            if (causa instanceof java.net.SocketTimeoutException)
                return "O servidor demorou para responder. Tente novamente.";
        }
        return erro.getMessage() == null ? erro.getClass().getSimpleName() : erro.getMessage();
    }
}
