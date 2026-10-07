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
    private static final ScheduledExecutorService CONSULTAS = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "orvalho-atualizacoes"); t.setDaemon(true); return t;
    });
    private static final java.util.concurrent.atomic.AtomicBoolean ocupada = new java.util.concurrent.atomic.AtomicBoolean();
    private static volatile String situacao = "Aguardando consulta.";
    private static volatile String ultimaConsulta = "Ainda não realizada nesta execução.";
    private static boolean iniciado;
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

    public static synchronized void iniciar(JanelaPrincipal janela) {
        if (iniciado) return;
        iniciado = true;
        String exe = System.getProperty("jpackage.app-path");
        if (exe == null) { situacao = "Execute a versão instalada para receber atualizações."; return; }
        CONSULTAS.execute(() -> {
            try {
                Path historico = DiagnosticoAtualizacao.pasta().resolve("consulta.properties");
                if (Files.isRegularFile(historico)) {
                    Properties anterior = new Properties();
                    try (InputStream in = Files.newInputStream(historico)) { anterior.load(in); }
                    ultimaConsulta = anterior.getProperty("data", ultimaConsulta);
                    situacao = "Último resultado: " + anterior.getProperty("situacao", "Consulta pendente.");
                }
                Path resultado = DiagnosticoAtualizacao.pasta().resolve("atualizacoes/resultado.txt");
                if (Files.isRegularFile(resultado) && (!Files.exists(historico)
                        || Files.getLastModifiedTime(resultado).compareTo(Files.getLastModifiedTime(historico)) > 0))
                    situacao = Files.readString(resultado);
                InstaladorAtualizacao.limparConcluidos(DiagnosticoAtualizacao.pasta().resolve("atualizacoes"));
            } catch (IOException erro) { DiagnosticoAtualizacao.registrar("Limpeza: " + erro); }
        });
        CONSULTAS.schedule(() -> {
            try { InstaladorAtualizacao.limparConcluidos(DiagnosticoAtualizacao.pasta().resolve("atualizacoes")); }
            catch (IOException erro) { DiagnosticoAtualizacao.registrar("Limpeza: " + erro); }
        }, 20, TimeUnit.SECONDS);
        CONSULTAS.scheduleWithFixedDelay(() -> solicitarConsulta(janela, false), 30, 30, TimeUnit.SECONDS);
    }

    private static void solicitarConsulta(JanelaPrincipal janela, boolean manual) {
        String exe = System.getProperty("jpackage.app-path");
        if (exe == null || !ocupada.compareAndSet(false, true)) return;
        CONSULTAS.execute(() -> {
            ultimaConsulta = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            estado("Consultando atualizações...");
            try { consultar(janela, Path.of(exe), manual); }
            catch (Exception erro) {
                estado("Falha: " + DiagnosticoAtualizacao.explicar(erro));
                DiagnosticoAtualizacao.registrar(erro.toString());
            } finally { ocupada.set(false); }
        });
    }

    private static void estado(String mensagem) {
        situacao = mensagem;
        DiagnosticoAtualizacao.registrar(mensagem);
        try {
            Path arquivo = DiagnosticoAtualizacao.pasta().resolve("consulta.properties");
            Properties dados = new Properties(); dados.setProperty("data", ultimaConsulta); dados.setProperty("situacao", mensagem);
            try (OutputStream out = Files.newOutputStream(arquivo)) { dados.store(out, "Ultima consulta do Orvalho"); }
        } catch (IOException erro) { DiagnosticoAtualizacao.registrar("Falha ao salvar diagnóstico: " + erro); }
    }

    static void mostrarSituacao(JanelaPrincipal janela) {
        boolean escuro=Boolean.TRUE.equals(((javax.swing.JComponent)janela.getContentPane()).getClientProperty("orvalho.escuro"));
        javax.swing.JDialog janelaInfo=new javax.swing.JDialog(janela,"Sobre e atualizações",false);janelaInfo.setName("sobreAtualizacoes");
        javax.swing.JPanel dialogo=new javax.swing.JPanel(new java.awt.BorderLayout(12,18));
        dialogo.setBorder(javax.swing.BorderFactory.createEmptyBorder(24,24,24,24));
        dialogo.setBackground(new java.awt.Color(0xF4F8FC));
        javax.swing.JTextArea texto = new javax.swing.JTextArea(7, 44);
        texto.setEditable(false); texto.setLineWrap(true); texto.setWrapStyleWord(true);
        texto.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        Runnable atualizar = () -> texto.setText("Orvalho " + versaoAtual() + "\n\nÚltima consulta: " + ultimaConsulta
                + "\n\n" + situacao);
        javax.swing.JButton verificar = new javax.swing.JButton("Verificar atualização agora");
        verificar.addActionListener(e -> solicitarConsulta(janela, true));
        javax.swing.JButton logs = new javax.swing.JButton("Abrir logs");
        BotaoOrvalho.aplicar(verificar,true);BotaoOrvalho.aplicar(logs,false);
        logs.addActionListener(e -> {
            try {
                Files.createDirectories(DiagnosticoAtualizacao.pasta());
                java.awt.Desktop.getDesktop().open(DiagnosticoAtualizacao.pasta().toFile());
            } catch (Exception erro) { DialogosOrvalho.showMessageDialog(dialogo, erro.getMessage()); }
        });
        javax.swing.JPanel botoes = new javax.swing.JPanel(); botoes.add(verificar); botoes.add(logs);
        dialogo.add(new javax.swing.JScrollPane(texto)); dialogo.add(botoes, java.awt.BorderLayout.SOUTH);
        javax.swing.Timer refresh = new javax.swing.Timer(500, e -> {
            atualizar.run(); verificar.setEnabled(!ocupada.get() && System.getProperty("jpackage.app-path") != null);
        });
        dialogo.addHierarchyListener(e->{if((e.getChangeFlags()&java.awt.event.HierarchyEvent.SHOWING_CHANGED)!=0){if(dialogo.isShowing())refresh.start();else refresh.stop();}});
        atualizar.run(); verificar.setEnabled(System.getProperty("jpackage.app-path") != null);
        TemaOrvalho.aplicar(dialogo,escuro);janelaInfo.setContentPane(dialogo);janelaInfo.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);janelaInfo.pack();janelaInfo.setLocationRelativeTo(janela);janelaInfo.setVisible(true);
    }

    private static void consultar(JanelaPrincipal janela, Path exe, boolean manual) throws Exception {
        Properties config = new Properties();
        try (InputStream in = Atualizacao.class.getResourceAsStream("/atualizacao.properties")) {
            if (in == null) return;
            config.load(in);
        }
        String canal=CanalAtualizacao.ler(Path.of(System.getProperty("user.home"),".orvalho"));
        URI origem=CanalAtualizacao.manifesto(config,canal);
        PublicKey chave = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(
                Base64.getDecoder().decode(config.getProperty("publicKey"))));
        {
            byte[] envelope;
            try (InputStream in = abrir(origem, false)) {
                envelope = in.readNBytes(16_385);
            }
            Versao nova = verificar(envelope, chave, origem);
            if (comparar(nova.numero(), config.getProperty("version")) <= 0) { estado("Seu aplicativo está atualizado."); return; }
            Path pasta = Path.of(System.getProperty("user.home"), ".orvalho", "atualizacoes");
            Files.createDirectories(pasta);
            Path tentativa = pasta.resolve("tentativa-" + nova.numero() + ".txt");
            if (!manual && Files.exists(tentativa) && System.currentTimeMillis() - Files.getLastModifiedTime(tentativa).toMillis()
                    < Duration.ofHours(6).toMillis()) {
                estado("Uma instalação foi tentada recentemente. Nova tentativa automática em até 6 horas; você pode verificar agora."); return;
            }
            if(!podeInstalar(janela)){estado("Atualização disponível. A instalação aguarda o fechamento das janelas e a confirmação do lembrete.");return;}
            InstaladorAtualizacao.limparConcluidos(pasta);
            InstaladorAtualizacao.verificarEspaco(exe, pasta, nova.tamanho());
            estado("Baixando a versão " + nova.numero() + "...");
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
                if(!podeInstalar(janela)){estado("Instalação adiada enquanto há uma janela em uso. Nova consulta em 30 segundos.");return;}
                Path auxiliar = InstaladorAtualizacao.preparar(exe, pasta);
                // O helper só instala depois que esta JVM e o launcher terminarem.
                ProcessBuilder helper = new ProcessBuilder(auxiliar.toString(), "--instalar-atualizacao");
                Map<String, String> env = helper.environment();
                env.put("ORVALHO_UPDATE_EXE", download.toString());
                env.put("ORVALHO_UPDATE_HASH", nova.sha256());
                env.put("ORVALHO_UPDATE_SIZE", Long.toString(nova.tamanho()));
                env.put("ORVALHO_UPDATE_VERSION", nova.numero());
                env.put("ORVALHO_APP_EXE", exe.toString());
                env.put("ORVALHO_OLD_PID", Long.toString(ProcessHandle.current().pid()));
                env.put("ORVALHO_PARENT_PID", Long.toString(ProcessHandle.current().parent().map(ProcessHandle::pid).orElse(0L)));
                SwingUtilities.invokeAndWait(() -> env.put("ORVALHO_RESTART_ARGS", janela.argumentosAposAtualizacao()));
                Path sessao = auxiliar.getParent();
                env.put("ORVALHO_UPDATE_SESSION", sessao.toString());
                estado("Preparando a instalação da versão " + nova.numero() + "...");
                Process processo = null;
                try {
                    processo = helper.redirectErrorStream(true).redirectOutput(sessao.resolve("helper.log").toFile()).start();
                    InstaladorAtualizacao.aguardarPronto(sessao, processo::isAlive, 30_000);
                    SwingUtilities.invokeAndWait(() -> {
                        try {
                            if(janela.interacaoPendente())throw new IOException("Instalação adiada: há uma janela em uso.");
                            Files.writeString(tentativa, nova.numero());
                            Files.writeString(sessao.resolve("continuar"), "ok", StandardOpenOption.CREATE_NEW);
                            // A fila da interface não recebe novas ações antes da saída.
                            System.exit(0);
                        } catch(IOException erro) { throw new java.io.UncheckedIOException(erro); }
                    });
                } catch (Exception erro) {
                    Files.writeString(sessao.resolve("cancelar"), "cancelado");
                    if (processo == null || !processo.isAlive()) Files.writeString(sessao.resolve("concluido"), "falha ao iniciar");
                    // Sem autorização, o helper nunca executa o instalador.
                    throw erro;
                }
                pronto = true;

            } finally { if (!pronto) Files.deleteIfExists(download); }
        }
    }

    private static boolean podeInstalar(JanelaPrincipal janela)throws Exception {
        boolean[] livre={false};
        SwingUtilities.invokeAndWait(()->livre[0]=!janela.interacaoPendente());
        return livre[0];
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
            conexao.setSSLSocketFactory(RedeAtualizacao.fabrica());
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
                if (status == 407) throw new IOException("O proxy exige autenticação. Peça ao TI para liberar a consulta e o download do Orvalho.");
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
