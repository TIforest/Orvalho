package com.mycompany.bebaAgua;
import java.io.IOException;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Configura somente o valor Orvalho da conta atual, usando o utilitário nativo do Windows. */
public final class InicializacaoWindows {
    private static final String CHAVE = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    public enum Estado { DESATIVADA, ATIVA, CAMINHO_INVALIDO }
    private InicializacaoWindows() {}
    public static String comandoPara(Path exe) {
        return "\"" + exe.toAbsolutePath().normalize() + "\" --iniciar-com-windows";
    }
    static Estado avaliar(String comando, Path atual) {
        if (comando == null) return Estado.DESATIVADA;
        var m = java.util.regex.Pattern.compile("^\"([^\"]+)\"\\s+--iniciar-com-windows\\s*$").matcher(comando);
        if (!m.matches()) return Estado.CAMINHO_INVALIDO;
        try {
            Path registrado = Path.of(m.group(1));
            return Files.isRegularFile(registrado) && (atual == null || Files.isSameFile(registrado, atual))
                    ? Estado.ATIVA : Estado.CAMINHO_INVALIDO;
        } catch (IOException | InvalidPathException e) { return Estado.CAMINHO_INVALIDO; }
    }
    private static Path atual() {
        String exe = System.getProperty("jpackage.app-path");
        return exe == null ? null : Path.of(exe).toAbsolutePath().normalize();
    }
    public static Estado consultar() throws IOException {
        // A chave Run pode não existir em um perfil recém-criado.
        String chaves = executar("query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion");
        boolean existe = chaves.lines().anyMatch(l -> l.strip().equalsIgnoreCase(
                "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"));
        if (!existe) return Estado.DESATIVADA;
        Path arquivo = Files.createTempFile("orvalho-registro-", ".reg");
        try {
            executar("export", CHAVE, arquivo.toString(), "/y");
            // reg export usa UTF-16, preservando nomes de usuário com acentos.
            return avaliar(lerComando(Files.readString(arquivo, StandardCharsets.UTF_16)), atual());
        } finally { Files.deleteIfExists(arquivo); }
    }
    static String lerComando(String exportacao) throws IOException {
        for (String linha : exportacao.lines().toList()) {
            if (!linha.regionMatches(true, 0, "\"Orvalho\"=", 0, 10)) continue;
            String valor = linha.substring(10);
            if (!valor.startsWith("\"") || !valor.endsWith("\"")) return "registro inválido";
            StringBuilder texto = new StringBuilder();
            for (int i = 1; i < valor.length() - 1; i++) {
                char c = valor.charAt(i);
                if (c == '\\') {
                    if (++i >= valor.length() - 1) throw new IOException("Registro de inicialização inválido.");
                    c = valor.charAt(i);
                }
                texto.append(c);
            }
            return texto.toString();
        }
        return null;
    }
    public static void configurar(boolean ativar) throws IOException {
        if (ativar) {
            Path exe = atual();
            if (exe == null || !Files.isRegularFile(exe))
                throw new IOException("Abra o Orvalho.exe instalado para ativar a inicialização com o Windows.");
            gravarComando(CHAVE, comandoPara(exe));
        } else if (consultar() != Estado.DESATIVADA) {
            executar("delete", CHAVE, "/v", "Orvalho", "/f");
        }
        Estado estado = consultar();
        if (ativar ? estado != Estado.ATIVA : estado != Estado.DESATIVADA)
            throw new IOException("Não foi possível confirmar a alteração no Windows.");
    }
    static void gravarComando(String chave, String comando) throws IOException {
        if (!chave.startsWith("HKCU\\Software\\") || chave.contains("\n") || chave.contains("\r")
                || chave.contains("]") || comando.contains("\n") || comando.contains("\r"))
            throw new IOException("Comando de inicialização inválido.");
        Path arquivo = Files.createTempFile("orvalho-inicio-", ".reg");
        try {
            String valor = comando.replace("\\", "\\\\").replace("\"", "\\\"");
            String conteudo = "Windows Registry Editor Version 5.00\r\n\r\n[HKEY_CURRENT_USER\\"
                    + chave.substring(5) + "]\r\n\"Orvalho\"=\"" + valor + "\"\r\n";
            Files.writeString(arquivo, "\uFEFF" + conteudo, StandardCharsets.UTF_16LE);
            executar("import", arquivo.toString());
        } finally { Files.deleteIfExists(arquivo); }
    }
    private static String executar(String... argumentos) throws IOException {
        String windows = System.getenv("SystemRoot");
        if (windows == null) throw new IOException("Esta opção requer Windows.");
        List<String> comando = new ArrayList<>();
        comando.add(Path.of(windows, "System32", "reg.exe").toString());
        comando.addAll(Arrays.asList(argumentos));
        Path log = Files.createTempFile("orvalho-inicio-", ".txt");
        try {
            Process p = new ProcessBuilder(comando).redirectErrorStream(true).redirectOutput(log.toFile()).start();
            try {
                if (!p.waitFor(10, TimeUnit.SECONDS)) {
                    p.destroyForcibly();
                    throw new IOException("O Windows demorou para responder. Tente novamente.");
                }
            } catch (InterruptedException e) {
                p.destroyForcibly(); Thread.currentThread().interrupt();
                throw new IOException("Consulta ao Windows interrompida.", e);
            }
            if (p.exitValue() != 0)
                throw new IOException("O Windows bloqueou o acesso à inicialização. Verifique as permissões e as políticas com o TI.");
            return new String(Files.readAllBytes(log), Charset.forName(System.getProperty("native.encoding", "windows-1252")));
        } finally { Files.deleteIfExists(log); }
    }
}
