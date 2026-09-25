package com.mycompany.bebaAgua;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Executado numa cópia temporária do aplicativo, fora da pasta substituída pelo MSI. */
final class InstaladorAtualizacao {
    private InstaladorAtualizacao() {}

    static Path preparar(Path exe, Path pasta) throws IOException {
        Path origem = exe.toAbsolutePath().normalize().getParent();
        Path destino = pasta.resolve("helper").toAbsolutePath().normalize();
        if (destino.startsWith(origem)) throw new IOException("Auxiliar deve ficar fora da instalação");
        try (var arquivos = Files.walk(origem)) {
            for (Path arquivo : arquivos.toList()) {
                if (Files.isSymbolicLink(arquivo)) throw new IOException("Link inesperado na instalação");
                Path alvo = destino.resolve(origem.relativize(arquivo));
                if (Files.isDirectory(arquivo)) Files.createDirectories(alvo);
                else Files.copy(arquivo, alvo, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return destino.resolve(exe.getFileName());
    }

    static void executar() {
        Map<String,String> env = System.getenv();
        Path exe = Path.of(env.get("ORVALHO_APP_EXE"));
        Path installer = Path.of(env.get("ORVALHO_UPDATE_EXE"));
        Path log = installer.getParent().resolve("instalacao.log");
        boolean processosEncerrados = false;
        try {
            for (String nome : List.of("ORVALHO_OLD_PID", "ORVALHO_PARENT_PID")) {
                long pid = Long.parseLong(env.getOrDefault(nome, "0"));
                if (pid <= 0) continue;
                Optional<ProcessHandle> processo = ProcessHandle.of(pid);
                if (processo.isPresent() && processo.get().isAlive()
                        && processo.get().info().command().map(c -> Path.of(c).equals(exe)).orElse(false)) {
                    processo.get().onExit().get(60, TimeUnit.SECONDS);
                }
            }
            processosEncerrados = true;
            Atualizacao.conferirArquivo(installer, new Atualizacao.Versao("0.0.0", installer.toUri(),
                    env.get("ORVALHO_UPDATE_HASH"), Long.parseLong(env.get("ORVALHO_UPDATE_SIZE"))));
            Process p = new ProcessBuilder(installer.toString(), "/qn", "/norestart",
                    "INSTALLDIR=" + exe.getParent(), "/L*v", installer.getParent().resolve("msi.log").toString())
                    .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile())).start();
            // Não encerra o Windows Installer no meio de uma transação.
            int status = p.waitFor();
            if (status != 0 && status != 3010) throw new IOException("Instalador retornou " + status);
            Files.writeString(log, "Atualização concluída.\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            Files.deleteIfExists(installer);
        } catch (Exception e) {
            try { Files.writeString(log, "Falha: " + e + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND); }
            catch (IOException ignored) { }
        } finally {
            if (processosEncerrados && Files.isRegularFile(exe)) {
                try {
                    List<String> args = new ArrayList<>(); args.add(exe.toString());
                    String estado = env.getOrDefault("ORVALHO_RESTART_ARGS", "").trim();
                    if (!estado.isEmpty()) args.addAll(Arrays.asList(estado.split("\\s+")));
                    new ProcessBuilder(args).start();
                } catch (IOException ignored) { }
            }
        }
    }
}
