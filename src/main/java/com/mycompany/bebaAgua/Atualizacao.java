package com.mycompany.bebaAgua;

import java.io.*;
import java.net.URI;
import javax.net.ssl.HttpsURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.SwingUtilities;

/** Atualizações publicadas são autenticadas antes de qualquer execução. */
public final class Atualizacao {
    static final long MAX_INSTALLER = 300L * 1024 * 1024;
    private static final ScheduledExecutorService PRAZOS = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "orvalho-prazos-http"); t.setDaemon(true); return t;
    });
    record Versao(String numero, URI download, String sha256, long tamanho) {}
    private Atualizacao() {}

    static String versaoAtual() {
        Properties config = new Properties();
        try (InputStream in = Atualizacao.class.getResourceAsStream("/atualizacao.properties")) {
            if (in != null) config.load(in);
            return config.getProperty("version", "desenvolvimento");
        } catch (IOException e) { return "desenvolvimento"; }
    }

    static int comparar(String a, String b) {
        if (!a.matches("[0-9]{1,5}\\.[0-9]{1,5}\\.[0-9]{1,5}")
                || !b.matches("[0-9]{1,5}\\.[0-9]{1,5}\\.[0-9]{1,5}"))
            throw new IllegalArgumentException("Versão inválida");
        String[] aa = a.split("\\."), bb = b.split("\\.");
        for (int i = 0; i < 3; i++) {
            int c = Integer.compare(Integer.parseInt(aa[i]), Integer.parseInt(bb[i]));
            if (c != 0) return c;
        }
        return 0;
    }

    static Versao verificar(byte[] envelope, PublicKey chave, URI origem) throws Exception {
        if (envelope.length > 16_384) throw new IOException("Manifesto muito grande");
        Properties p = new Properties(); p.load(new ByteArrayInputStream(envelope));
        byte[] payload = Base64.getDecoder().decode(p.getProperty("payload", ""));
        Signature assinatura = Signature.getInstance("Ed25519");
        assinatura.initVerify(chave); assinatura.update(payload);
        if (!assinatura.verify(Base64.getDecoder().decode(p.getProperty("signature", ""))))
            throw new GeneralSecurityException("Assinatura inválida");
        Properties dados = new Properties(); dados.load(new ByteArrayInputStream(payload));
        String numero = dados.getProperty("version", ""); comparar(numero, "0.0.0");
        URI url = URI.create(dados.getProperty("url", ""));
        boolean mesmoHost = Objects.equals(origem.getHost(), url.getHost()) && url.getPort() == origem.getPort();
        boolean github = "github.com".equals(url.getHost()) && url.getPort() == -1
                && url.getPath().startsWith("/BrunoTrech/Orvalho-Releases/releases/download/");
        if (!"https".equals(url.getScheme()) || !(mesmoHost || github) || url.getUserInfo() != null
                || url.getFragment() != null || !url.getPath().endsWith(".exe"))
            throw new IOException("Origem do instalador inválida");
        String hash = dados.getProperty("sha256", "");
        if (!hash.matches("[a-fA-F0-9]{64}")) throw new IOException("Hash inválido");
        long tamanho = Long.parseLong(dados.getProperty("size", "0"));
        if (tamanho < 1 || tamanho > MAX_INSTALLER) throw new IOException("Tamanho inválido");
        return new Versao(numero, url, hash, tamanho);
    }

    static void conferirArquivo(Path arquivo, Versao versao) throws Exception {
        if (Files.size(arquivo) != versao.tamanho()) throw new IOException("Download incompleto");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(arquivo)) {
            byte[] buffer = new byte[65536]; int n;
            while ((n = in.read(buffer)) != -1) digest.update(buffer, 0, n);
        }
        if (!HexFormat.of().formatHex(digest.digest()).equalsIgnoreCase(versao.sha256()))
            throw new GeneralSecurityException("Instalador diferente do publicado");
    }

    public static void iniciar(JanelaPrincipal janela) {
        String exe = System.getProperty("jpackage.app-path");
        if (exe == null) return; // O NetBeans não é uma instalação atualizável.
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "orvalho-atualizacoes"); t.setDaemon(true); return t;
        });
        executor.scheduleWithFixedDelay(() -> {
            try { consultar(janela, Path.of(exe)); }
            catch (Exception erro) {
                try {
                    Path log = Path.of(System.getProperty("user.home"), ".orvalho", "atualizacao.log");
                    Files.createDirectories(log.getParent());
                    Files.writeString(log, java.time.Instant.now() + " " + erro + System.lineSeparator());
                } catch (IOException ignored) { }
            }
        }, 30, 6 * 60 * 60, TimeUnit.SECONDS);
    }

    private static void consultar(JanelaPrincipal janela, Path exe) throws Exception {
        Properties config = new Properties();
        try (InputStream in = Atualizacao.class.getResourceAsStream("/atualizacao.properties")) {
            if (in == null) return;
            config.load(in);
        }
        String endereco = config.getProperty("manifest", "");
        if (endereco.isBlank()) return;
        URI origem = URI.create(endereco);
        if (!"https".equals(origem.getScheme())) throw new IOException("HTTPS obrigatório");
        PublicKey chave = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(
                Base64.getDecoder().decode(config.getProperty("publicKey"))));
        {
            byte[] envelope;
            try (InputStream in = abrir(origem, false)) {
                envelope = in.readNBytes(16_385);
            }
            Versao nova = verificar(envelope, chave, origem);
            if (comparar(nova.numero(), config.getProperty("version")) <= 0) return;
            Path pasta = Path.of(System.getProperty("user.home"), ".orvalho", "atualizacoes");
            Files.createDirectories(pasta);
            Path tentativa = pasta.resolve("tentativa-" + nova.numero() + ".txt");
            if (Files.exists(tentativa) && System.currentTimeMillis() - Files.getLastModifiedTime(tentativa).toMillis()
                    < Duration.ofHours(6).toMillis()) return;
            Path download = Files.createTempFile(pasta, "Orvalho-" + nova.numero() + "-", ".exe");
            boolean pronto = false;
            try {
                try (InputStream in = abrir(nova.download(), true); OutputStream out = Files.newOutputStream(download)) {
                    byte[] buffer = new byte[65536]; long total = 0; int n;
                    while ((n = in.read(buffer)) != -1) {
                        total += n;
                        if (total > nova.tamanho()) throw new IOException("Download excedeu o tamanho esperado");
                        out.write(buffer, 0, n);
                    }
                }
                conferirArquivo(download, nova);
                Path auxiliar = InstaladorAtualizacao.preparar(exe, pasta);
                // O helper só instala depois que esta JVM e o launcher terminarem.
                ProcessBuilder helper = new ProcessBuilder(auxiliar.toString(), "--instalar-atualizacao");
                Map<String, String> env = helper.environment();
                env.put("ORVALHO_UPDATE_EXE", download.toString());
                env.put("ORVALHO_UPDATE_HASH", nova.sha256());
                env.put("ORVALHO_UPDATE_SIZE", Long.toString(nova.tamanho()));
                env.put("ORVALHO_APP_EXE", exe.toString());
                env.put("ORVALHO_OLD_PID", Long.toString(ProcessHandle.current().pid()));
                env.put("ORVALHO_PARENT_PID", Long.toString(ProcessHandle.current().parent().map(ProcessHandle::pid).orElse(0L)));
                SwingUtilities.invokeAndWait(() -> env.put("ORVALHO_RESTART_ARGS", janela.argumentosAposAtualizacao()));
                Files.writeString(tentativa, nova.numero());
                helper.redirectErrorStream(true).redirectOutput(pasta.resolve("helper.log").toFile()).start();
                pronto = true;
                SwingUtilities.invokeLater(() -> System.exit(0));
            } finally { if (!pronto) Files.deleteIfExists(download); }
        }
    }

    private static InputStream comPrazo(InputStream in, int segundos) {
        ScheduledFuture<?> prazo = PRAZOS.schedule(() -> {
            try { in.close(); } catch (IOException ignored) { }
        }, segundos, TimeUnit.SECONDS);
        return new FilterInputStream(in) {
            @Override public void close() throws IOException { prazo.cancel(false); super.close(); }
        };
    }

    static InputStream abrir(URI url, boolean permitirGithub) throws Exception {
        for (int redirects = 0; redirects < 4; redirects++) {
            HttpsURLConnection conexao = (HttpsURLConnection) url.toURL().openConnection();
            conexao.setConnectTimeout(20_000); conexao.setReadTimeout(30_000);
            conexao.setInstanceFollowRedirects(false);
            conexao.setRequestProperty("Cache-Control", "no-cache");
            conexao.setRequestProperty("User-Agent", "Orvalho-Updater");
            try {
                int status = conexao.getResponseCode();
                if (status == 200) {
                    InputStream stream = conexao.getInputStream();
                    return comPrazo(new FilterInputStream(stream) {
                        @Override public void close() throws IOException {
                            try { super.close(); } finally { conexao.disconnect(); }
                        }
                    }, permitirGithub ? 600 : 30);
                }
                if (!permitirGithub || status / 100 != 3) throw new IOException("HTTP " + status);
                URI destino = url.resolve(Objects.requireNonNull(conexao.getHeaderField("Location")));
                if (!"https".equals(destino.getScheme()) || destino.getUserInfo() != null
                        || destino.getPort() != -1 || !"release-assets.githubusercontent.com".equals(destino.getHost()))
                    throw new IOException("Redirecionamento não autorizado");
                conexao.disconnect(); url = destino;
            } catch (Exception e) { conexao.disconnect(); throw e; }
        }
        throw new IOException("Excesso de redirecionamentos");
    }
}
