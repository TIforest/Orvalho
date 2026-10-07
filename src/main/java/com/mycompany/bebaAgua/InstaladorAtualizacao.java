package com.mycompany.bebaAgua;

import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** O auxiliar confirma que está pronto antes de o aplicativo fechar. */
final class InstaladorAtualizacao {
    private InstaladorAtualizacao() {}

    static List<Path> arquivosInstalacao(Path origem) throws IOException {
        List<Path> arquivos = new ArrayList<>();
        // Copia somente o aplicativo, mesmo se o usuário escolher uma pasta compartilhada.
        for (String nome : List.of("Orvalho.exe", "app", "runtime")) {
            Path item = origem.resolve(nome);
            if (!Files.exists(item, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Instalação incompleta: " + nome);
            try (var arvore = Files.walk(item)) {
                for (Path arquivo : arvore.toList()) {
                    if (Files.isSymbolicLink(arquivo)) throw new IOException("Link inesperado na instalação");
                    arquivos.add(arquivo);
                }
            }
        }
        return arquivos;
    }

    static void verificarEspaco(Path exe, Path pasta, long download) throws IOException {
        long imagem = 0;
        for (Path arquivo : arquivosInstalacao(exe.toAbsolutePath().getParent()))
            if (Files.isRegularFile(arquivo)) imagem = Math.addExact(imagem, Files.size(arquivo));
        Map<FileStore, Long> volumes = new HashMap<>();
        volumes.merge(Files.getFileStore(pasta), imagem + download, Long::sum);
        volumes.merge(Files.getFileStore(exe), imagem, Long::sum);
        volumes.merge(Files.getFileStore(Path.of(System.getProperty("java.io.tmpdir"))), imagem + download * 2, Long::sum);
        for (var volume : volumes.entrySet()) exigirEspaco(volume.getKey().getUsableSpace(), volume.getValue() + 100L * 1024 * 1024);
    }

    static void exigirEspaco(long disponivel, long necessario) throws IOException {
        if (disponivel < necessario)
            throw new IOException("Espaço insuficiente para atualizar. São necessários aproximadamente "
                    + (necessario / 1024 / 1024 + 1) + " MB livres no volume de instalação ou arquivos temporários.");
    }

    static void confirmarVersao(Path exe, String esperada) throws IOException {
        String opcao = "java-options=-Djpackage.app-version=" + esperada;
        if (Files.readAllLines(exe.getParent().resolve("app/Orvalho.cfg")).stream().noneMatch(opcao::equals))
            throw new IOException("O instalador terminou, mas a versão esperada não foi confirmada. Verifique msi.log e se há reinicialização pendente.");
    }

    static Path preparar(Path exe, Path pasta) throws IOException {
        Path origem = exe.toAbsolutePath().normalize().getParent();
        Path raiz = pasta.toAbsolutePath().normalize();
        if (raiz.startsWith(origem)) throw new IOException("Auxiliar deve ficar fora da instalação");
        List<Path> arquivos = arquivosInstalacao(origem);
        Files.createDirectories(raiz);
        Path destino = Files.createTempDirectory(raiz, "helper-");
        Files.writeString(destino.resolve(".orvalho-helper"), "1");
        try {
            for (Path arquivo : arquivos) {
                Path alvo = destino.resolve(origem.relativize(arquivo));
                if (Files.isDirectory(arquivo)) Files.createDirectories(alvo);
                else Files.copy(arquivo, alvo);
            }
            return destino.resolve(exe.getFileName());
        } catch (IOException erro) {
            Files.writeString(destino.resolve("concluido"), "copia incompleta");
            throw erro;
        }
    }

    static void aguardarPronto(Path sessao, BooleanSupplier vivo, long prazoMs) throws Exception {
        long fim = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(prazoMs);
        while (System.nanoTime() < fim) {
            if (!vivo.getAsBoolean()) throw new IOException("O auxiliar de atualização foi encerrado. Confira o antivírus e os logs.");
            if (Files.isRegularFile(sessao.resolve("pronto"))) return;
            Thread.sleep(100);
        }
        throw new IOException("O auxiliar não confirmou que está pronto. O Orvalho continuará aberto.");
    }

    static void limparConcluidos(Path pasta) throws IOException {
        Path raiz = pasta.toAbsolutePath().normalize();
        if (!Files.isDirectory(raiz)) return;
        try (var diretorios = Files.list(raiz)) {
            for (Path sessao : diretorios.toList()) {
                if (!sessao.getFileName().toString().startsWith("helper-") || Files.isSymbolicLink(sessao)
                        || !Files.isDirectory(sessao) || !Files.isRegularFile(sessao.resolve(".orvalho-helper"))
                        || !Files.isRegularFile(sessao.resolve("concluido"))) continue;
                // Somente sessões concluídas, pertencentes ao Orvalho e sem auxiliar em execução.
                try (FileChannel canal = FileChannel.open(sessao.resolve("ocupado"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                    try (FileLock trava = canal.tryLock()) {
                        if (trava == null) continue;
                        try (var arquivos = Files.walk(sessao)) {
                            for (Path arquivo : arquivos.sorted(Comparator.reverseOrder()).toList()) {
                                Path alvo = arquivo.toAbsolutePath().normalize();
                                if (!alvo.startsWith(raiz) || alvo.equals(raiz)) throw new IOException("Limpeza fora da pasta de atualização");
                                if (!alvo.equals(sessao) && !Set.of("ocupado", ".orvalho-helper", "concluido")
                                        .contains(sessao.relativize(alvo).toString())) Files.deleteIfExists(alvo);
                            }
                        }
                    }
                } catch (OverlappingFileLockException | IOException e) { continue; }
                Files.deleteIfExists(sessao.resolve("ocupado"));
                Files.deleteIfExists(sessao.resolve("concluido"));
                Files.deleteIfExists(sessao.resolve(".orvalho-helper"));
                Files.deleteIfExists(sessao);
            }
        }
    }

    static void executar() {
        Map<String,String> env = System.getenv();
        Path pasta = DiagnosticoAtualizacao.pasta().resolve("atualizacoes");
        Path log = pasta.resolve("instalacao.log");
        Path sessao = null, installer = null, exe = null;
        boolean processosEncerrados = false;
        try {
            exe = Path.of(env.get("ORVALHO_APP_EXE")).toAbsolutePath().normalize();
            installer = Path.of(env.get("ORVALHO_UPDATE_EXE")).toAbsolutePath().normalize();
            sessao = Path.of(env.get("ORVALHO_UPDATE_SESSION")).toAbsolutePath().normalize();
            if (!sessao.getParent().equals(pasta.toAbsolutePath().normalize())
                    || !sessao.getFileName().toString().startsWith("helper-")
                    || !Files.isRegularFile(sessao.resolve(".orvalho-helper"))) throw new IOException("Sessão inválida");
            try (FileChannel canal = FileChannel.open(sessao.resolve("ocupado"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock trava = canal.lock()) {
                Atualizacao.conferirArquivo(installer, new Atualizacao.Versao("0.0.0", installer.toUri(),
                        env.get("ORVALHO_UPDATE_HASH"), Long.parseLong(env.get("ORVALHO_UPDATE_SIZE"))));
                if (!Files.isRegularFile(exe) || !Files.isWritable(exe.getParent()))
                    throw new IOException("A conta atual não pode atualizar esta instalação.");
                Files.writeString(sessao.resolve("pronto"), "ok", StandardOpenOption.CREATE_NEW);
                long prazo = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
                while (!Files.exists(sessao.resolve("continuar"))) {
                    if (Files.exists(sessao.resolve("cancelar")) || System.nanoTime() > prazo)
                        throw new IOException("Atualização cancelada antes de fechar o aplicativo.");
                    Thread.sleep(100);
                }
                if (Files.exists(sessao.resolve("cancelar"))) throw new IOException("Atualização cancelada.");
                for (String nome : List.of("ORVALHO_OLD_PID", "ORVALHO_PARENT_PID")) {
                    long pid = Long.parseLong(env.getOrDefault(nome, "0"));
                    if (pid <= 0) continue;
                    Optional<ProcessHandle> processo = ProcessHandle.of(pid);
                    if (processo.isPresent() && processo.get().isAlive()) {
                        String comando = processo.get().info().command().orElseThrow(() -> new IOException("Não foi possível verificar o processo anterior."));
                        if (Path.of(comando).equals(exe)) processo.get().onExit().get(60, TimeUnit.SECONDS);
                    }
                }
                processosEncerrados = true;
                // Confere novamente depois de aguardar o fechamento.
                Atualizacao.conferirArquivo(installer, new Atualizacao.Versao("0.0.0", installer.toUri(),
                        env.get("ORVALHO_UPDATE_HASH"), Long.parseLong(env.get("ORVALHO_UPDATE_SIZE"))));
                Path msi = pasta.resolve("msi.log");
                if (Files.exists(msi)) Files.move(msi, pasta.resolve("msi.log.1"), StandardCopyOption.REPLACE_EXISTING);
                Process p = new ProcessBuilder(installer.toString(), "/qn", "/norestart",
                        "INSTALLDIR=" + exe.getParent(), "/L*v", msi.toString())
                        .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile())).start();
                // Não mata o Windows Installer no meio da transação.
                int status = p.waitFor();
                if (status != 0 && status != 3010) throw new IOException("Instalador retornou " + status);
                confirmarVersao(exe, env.get("ORVALHO_UPDATE_VERSION"));
                String resultado = "Atualização para " + env.get("ORVALHO_UPDATE_VERSION") + " concluída."
                        + (status == 3010 ? " O Windows solicitou reinicialização." : "");
                DiagnosticoAtualizacao.registrar(log, resultado);
                Files.writeString(pasta.resolve("resultado.txt"), resultado);
            }
        } catch (Exception e) {
            try {
                DiagnosticoAtualizacao.registrar(log, "Falha: " + e);
                Files.writeString(pasta.resolve("resultado.txt"), "Falha na última instalação: " + DiagnosticoAtualizacao.explicar(e));
            } catch (IOException ignored) { }
        } finally {
            try {
                if (sessao != null && sessao.getParent().equals(pasta.toAbsolutePath().normalize())
                        && Files.isRegularFile(sessao.resolve(".orvalho-helper"))) Files.writeString(sessao.resolve("concluido"), "ok");
                if (installer != null && installer.getParent().equals(pasta.toAbsolutePath().normalize())
                        && installer.getFileName().toString().matches("Orvalho-[0-9.]+-.*\\.exe")) Files.deleteIfExists(installer);
            } catch (IOException erro) { DiagnosticoAtualizacao.registrar("Limpeza: " + erro); }
            if (processosEncerrados && exe != null && Files.isRegularFile(exe)) {
                try {
                    List<String> args = new ArrayList<>(); args.add(exe.toString());
                    String estado = env.getOrDefault("ORVALHO_RESTART_ARGS", "").trim();
                    if (!estado.isEmpty()) args.addAll(Arrays.asList(estado.split("\\s+")));
                    new ProcessBuilder(args).start();
                } catch (IOException erro) { DiagnosticoAtualizacao.registrar("Falha ao reabrir: " + erro); }
            }
        }
    }
}
