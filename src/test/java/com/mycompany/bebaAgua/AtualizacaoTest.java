package com.mycompany.bebaAgua;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AtualizacaoTest {
    @TempDir Path pasta;
    final URI origem = URI.create("https://orvalho.example/updates/latest.properties");
    byte[] manifesto(KeyPair keys, String version, String url, String hash, long size) throws Exception {
        byte[] payload = ("version=" + version + "\nurl=" + url + "\nsha256=" + hash + "\nsize=" + size + "\n")
                .getBytes(StandardCharsets.UTF_8);
        Signature s = Signature.getInstance("Ed25519"); s.initSign(keys.getPrivate()); s.update(payload);
        return ("payload=" + Base64.getEncoder().encodeToString(payload) + "\nsignature="
                + Base64.getEncoder().encodeToString(s.sign()) + "\n").getBytes(StandardCharsets.UTF_8);
    }
    @Test void comparaVersoesNumericamente() {
        assertTrue(Atualizacao.comparar("1.10.0", "1.9.9") > 0);
        assertTrue(Atualizacao.comparar("1.0.0", "1.1.0") < 0);
        assertEquals(0, Atualizacao.comparar("1.1.0", "1.1.0"));
        assertThrows(IllegalArgumentException.class, () -> Atualizacao.comparar("1.0;exit", "1.0.0"));
    }
    @Test void aceitaSomenteAssinaturaDaChaveDoAplicativo() throws Exception {
        KeyPair a = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        KeyPair b = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        byte[] data = manifesto(a, "1.1.0", "https://orvalho.example/releases/Orvalho-1.1.0.exe", "a".repeat(64), 100);
        assertEquals("1.1.0", Atualizacao.verificar(data, a.getPublic(), origem).numero());
        assertThrows(GeneralSecurityException.class, () -> Atualizacao.verificar(data, b.getPublic(), origem));
        String alterado = new String(data, StandardCharsets.UTF_8).replace("payload=", "payload=AAAA");
        assertThrows(Exception.class, () -> Atualizacao.verificar(alterado.getBytes(StandardCharsets.UTF_8), a.getPublic(), origem));
    }
    @Test void rejeitaOutrosHostsHttpETamanhoAbusivo() throws Exception {
        KeyPair keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        for (String url : List.of("http://orvalho.example/a.exe", "https://outro.example/a.exe", "https://user@orvalho.example/a.exe")) {
            byte[] data = manifesto(keys, "1.1.0", url, "a".repeat(64), 100);
            assertThrows(Exception.class, () -> Atualizacao.verificar(data, keys.getPublic(), origem));
        }
        byte[] huge = manifesto(keys, "1.1.0", "https://orvalho.example/a.exe", "a".repeat(64), Long.MAX_VALUE);
        assertThrows(Exception.class, () -> Atualizacao.verificar(huge, keys.getPublic(), origem));
    }
    @Test void rejeitaArquivoIncompletoOuAdulterado() throws Exception {
        Path file = pasta.resolve("teste.exe"); Files.writeString(file, "original");
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
        var v = new Atualizacao.Versao("1.1.0", URI.create("https://orvalho.example/a.exe"), hash, Files.size(file));
        Atualizacao.conferirArquivo(file, v);
        Files.writeString(file, "alterado");
        assertThrows(Exception.class, () -> Atualizacao.conferirArquivo(file, v));
        Files.writeString(file, "x");
        assertThrows(Exception.class, () -> Atualizacao.conferirArquivo(file, v));
    }
    @Test void restauraContagemAtivaEPausada() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            LembreteService s = new LembreteService(() -> {});
            try {
                s.restaurar(52, 123, false);
                assertFalse(s.ativo()); assertEquals(123, s.getSegundosRestantes());
                s.start(52); assertTrue(s.ativo()); assertTrue(s.getSegundosRestantes() <= 123);
                s.restaurar(52, 98, true); assertTrue(s.ativo()); assertTrue(s.getSegundosRestantes() <= 98);
            } finally { s.pausar(); }
        });
    }
    @Test void preparaAuxiliarForaDaInstalacaoSemAlterarOriginais() throws Exception {
        Path origem = pasta.resolve("app");
        Files.createDirectories(origem.resolve("runtime/bin"));
        Files.createDirectories(origem.resolve("app"));
        Files.writeString(origem.resolve("Orvalho.exe"), "launcher");
        Files.writeString(origem.resolve("runtime/bin/jvm.dll"), "runtime");
        Path exe = InstaladorAtualizacao.preparar(origem.resolve("Orvalho.exe"), pasta.resolve("updates"));
        assertEquals("launcher", Files.readString(exe));
        assertEquals("runtime", Files.readString(exe.getParent().resolve("runtime/bin/jvm.dll")));
        assertTrue(Files.exists(origem.resolve("Orvalho.exe")));
        assertThrows(Exception.class, () -> InstaladorAtualizacao.preparar(origem.resolve("Orvalho.exe"), origem));
    }
}
