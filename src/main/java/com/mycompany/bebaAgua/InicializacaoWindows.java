package com.mycompany.bebaAgua;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/** Configura somente o registro Orvalho da conta atual do Windows. */
public final class InicializacaoWindows {
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
        String resultado = executar("""
            $key = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey($subkey)
            try {
                if ($null -eq $key) { 'AUSENTE' }
                else {
                    $value = $key.GetValue('Orvalho', $null)
                    if ($null -eq $value) { 'AUSENTE' }
                    else { [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes([string]$value)) }
                }
            } finally { if ($null -ne $key) { $key.Dispose() } }
            """, null);
        if (resultado.equals("AUSENTE")) return Estado.DESATIVADA;
        try {
            return avaliar(new String(Base64.getDecoder().decode(resultado), StandardCharsets.UTF_8), atual());
        } catch (IllegalArgumentException e) { throw new IOException("Resposta inesperada do Windows.", e); }
    }
    public static void configurar(boolean ativar) throws IOException {
        if (ativar) {
            Path exe = atual();
            if (exe == null || !Files.isRegularFile(exe))
                throw new IOException("Abra o Orvalho.exe para ativar a inicialização com o Windows.");
            executar("""
                $key = [Microsoft.Win32.Registry]::CurrentUser.CreateSubKey($subkey)
                try { $key.SetValue('Orvalho', $env:ORVALHO_STARTUP_COMMAND,
                    [Microsoft.Win32.RegistryValueKind]::String) } finally { $key.Dispose() }
                """, comandoPara(exe));
        } else {
            executar("""
                $key = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey($subkey, $true)
                if ($null -ne $key) {
                    try { $key.DeleteValue('Orvalho', $false) } finally { $key.Dispose() }
                }
                """, null);
        }
        Estado estado = consultar();
        if (ativar ? estado != Estado.ATIVA : estado != Estado.DESATIVADA)
            throw new IOException("Não foi possível confirmar a alteração no Windows.");
    }
    private static String executar(String operacao, String comando) throws IOException {
        String script = """
            $ErrorActionPreference = 'Stop'
            [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
            $subkey = 'Software\\Microsoft\\Windows\\CurrentVersion\\Run'
            try {
            """ + operacao + """
            } catch { [Console]::Error.WriteLine($_.Exception.Message); exit 2 }
            """;
        String windows = System.getenv("SystemRoot");
        if (windows == null) throw new IOException("Esta opção requer Windows.");
        Path log = Files.createTempFile("orvalho-inicio-", ".txt");
        try {
            ProcessBuilder pb = new ProcessBuilder(Path.of(windows, "System32", "WindowsPowerShell",
                "v1.0", "powershell.exe").toString(), "-NoLogo", "-NoProfile", "-NonInteractive",
                "-WindowStyle", "Hidden", "-EncodedCommand",
                Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE)));
            // Caminhos são dados, nunca trechos de código PowerShell.
            if (comando != null) pb.environment().put("ORVALHO_STARTUP_COMMAND", comando);
            Process p = pb.redirectErrorStream(true).redirectOutput(log.toFile()).start();
            try {
                if (!p.waitFor(10, TimeUnit.SECONDS)) {
                    p.destroyForcibly().waitFor();
                    throw new IOException("O Windows demorou para responder. Tente novamente.");
                }
            } catch (InterruptedException e) {
                p.destroyForcibly();
                Thread.currentThread().interrupt();
                throw new IOException("Consulta ao Windows interrompida.", e);
            }
            String saida = Files.readString(log, StandardCharsets.UTF_8).strip();
            if (p.exitValue() != 0) throw new IOException("Falha ao acessar o Windows: " + saida);
            return saida;
        } finally { Files.deleteIfExists(log); }
    }
}