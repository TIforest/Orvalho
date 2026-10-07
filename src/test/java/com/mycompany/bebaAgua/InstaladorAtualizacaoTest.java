package com.mycompany.bebaAgua;

import java.nio.file.*;
import java.nio.channels.*;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class InstaladorAtualizacaoTest {
    @TempDir Path pasta;
    @Test void naoAutorizaAuxiliarMortoOuSemConfirmacao() throws Exception {
        assertThrows(IOException.class, () -> InstaladorAtualizacao.aguardarPronto(pasta, () -> false, 100));
        assertThrows(IOException.class, () -> InstaladorAtualizacao.aguardarPronto(pasta, () -> true, 100));
        Files.writeString(pasta.resolve("pronto"), "ok");
        assertThrows(IOException.class, () -> InstaladorAtualizacao.aguardarPronto(pasta, () -> false, 100));
        InstaladorAtualizacao.aguardarPronto(pasta, () -> true, 100);
        assertFalse(Files.exists(pasta.resolve("continuar")));
    }
    @Test void verificaEspacoAntesDeInstalar() throws Exception {
        assertThrows(IOException.class, () -> InstaladorAtualizacao.exigirEspaco(9, 10));
        InstaladorAtualizacao.exigirEspaco(10, 10);
    }
    @Test void naoDeclaraSucessoSeInstaladorNaoTrocouAVersao() throws Exception {
        Files.createDirectory(pasta.resolve("app"));
        Files.writeString(pasta.resolve("app/Orvalho.cfg"),"java-options=-Djpackage.app-version=1.1.2\n");
        assertThrows(IOException.class,()->InstaladorAtualizacao.confirmarVersao(pasta.resolve("Orvalho.exe"),"1.1.4"));
        Files.writeString(pasta.resolve("app/Orvalho.cfg"),"java-options=-Djpackage.app-version=1.1.4\n");
        InstaladorAtualizacao.confirmarVersao(pasta.resolve("Orvalho.exe"),"1.1.4");
    }
    @Test void limpaSomenteSessaoConcluidaEInativa() throws Exception {
        Path sessao=Files.createDirectory(pasta.resolve("helper-teste"));
        Files.writeString(sessao.resolve(".orvalho-helper"), "1");
        Files.writeString(sessao.resolve("Orvalho.exe"), "teste");
        Path alheio=Files.createDirectory(pasta.resolve("helper-nao-pertence"));
        Files.writeString(alheio.resolve("concluido"), "ok");
        InstaladorAtualizacao.limparConcluidos(pasta);
        assertTrue(Files.exists(sessao));
        Files.writeString(sessao.resolve("concluido"), "ok");
        try (var canal=FileChannel.open(sessao.resolve("ocupado"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
             var trava=canal.lock()) {
            InstaladorAtualizacao.limparConcluidos(pasta);
            assertTrue(Files.exists(sessao.resolve("Orvalho.exe")));
        }
        InstaladorAtualizacao.limparConcluidos(pasta);
        assertFalse(Files.exists(sessao));
        assertTrue(Files.exists(alheio));
    }
    @Test void copiaSomenteArquivosDoAplicativo() throws Exception {
        Path origem=Files.createDirectory(pasta.resolve("app"));
        Files.createDirectories(origem.resolve("runtime/bin"));
        Files.createDirectory(origem.resolve("app"));
        Files.writeString(origem.resolve("Orvalho.exe"),"exe");
        Files.writeString(origem.resolve("documento-pessoal.txt"),"não copiar");
        Path exe=InstaladorAtualizacao.preparar(origem.resolve("Orvalho.exe"),pasta.resolve("updates"));
        assertFalse(Files.exists(exe.getParent().resolve("documento-pessoal.txt")));
    }
    @Test void logsMantemHistoricoComLimite() throws Exception {
        Path log=pasta.resolve("atualizacao.log");
        DiagnosticoAtualizacao.registrar(log,"primeiro");
        DiagnosticoAtualizacao.registrar(log,"segundo");
        assertTrue(Files.readString(log).contains("primeiro"));
        assertTrue(Files.readString(log).contains("segundo"));
        Files.writeString(log,"x".repeat(1_048_576));
        DiagnosticoAtualizacao.registrar(log,"terceiro");
        assertTrue(Files.exists(pasta.resolve("atualizacao.log.1")));
        assertTrue(Files.size(log)<1024);
    }
}
