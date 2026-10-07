/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.bebaAgua;

import java.awt.Dimension;
import javax.swing.JFrame; // Trás a função de Janela, com título, bordas e controles de minimizar e fechar.
import javax.swing.JPanel; // Agrupa os componentes da interface. Você coloca os campos e botões no painel, e adiciona esse painel à janela (JFrame).
import javax.swing.JLabel; // Exibe textos.
import javax.swing.JButton; // Cria botões.
import javax.swing.JSpinner; // Cria o campo com setas de seleção de valores. Também permite digitar valores.
import javax.swing.SpinnerNumberModel; //Define as regras numéricas do JSpinner: valor inicial, mínimo, máximo e incremento das setas.
import java.awt.FlowLayout; // Organiza os componentes em sequência, da esquerda para a direita, passando para outra linha quando falta espaço.
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.ParseException;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.util.prefs.Preferences;
import javax.swing.Timer;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import javax.swing.JCheckBox;




public class JanelaPrincipal extends JFrame {
    
 
    
    LembreteService lembreteService =
       new LembreteService(() -> mostrarAlerta());
    private final GarrafaAnimada garrafa = new GarrafaAnimada(lembreteService);
    
    
    
    // Construtor para a configuração da Janela (titulo, tamanhoJanela e etc).
    public JanelaPrincipal(){
        this(false);
    }

    public JanelaPrincipal(boolean inicioAutomatico){
        this(inicioAutomatico, new ConfiguracaoIntervalo());
    }

    JanelaPrincipal(boolean inicioAutomatico, ConfiguracaoIntervalo configuracao){
        this(inicioAutomatico, configuracao, new DadosAgua());
    }

    JanelaPrincipal(boolean inicioAutomatico, ConfiguracaoIntervalo configuracao, DadosAgua dadosAgua){
    this.configuracaoIntervalo = configuracao;
    JCheckBox check = new JCheckBox("Iniciar com o Windows");
    //Icone do APP
    SystemTray bandeja = SystemTray.isSupported() ? SystemTray.getSystemTray() : null;
    java.net.URL recursoIcone = JanelaPrincipal.class.getResource("/imagens/agua.png"); 
    ImageIcon icone = new ImageIcon(recursoIcone);
    Image imagem = icone.getImage();
    TrayIcon iconeBandeja = bandeja != null ? new TrayIcon(imagem, "Orvalho") : null;
    
    MenuItem menuItem1 = new MenuItem("Abrir");
    MenuItem menuItem2 = new MenuItem("Fechar");
    
    if(SystemTray.isSupported()){    
    iconeBandeja.setImageAutoSize(true);
    PopupMenu popupMenu = new PopupMenu();
    
    popupMenu.add(menuItem1);
    MenuItem sobre = new MenuItem("Sobre e atualizações");
    sobre.addActionListener(event -> javax.swing.SwingUtilities.invokeLater(() -> Atualizacao.mostrarSituacao(this)));
    popupMenu.add(sobre);
    popupMenu.add(menuItem2);
    
    try{
    bandeja.add(iconeBandeja);
    bandejaDisponivel = true;
    iconeBandeja.setPopupMenu(popupMenu);
        
    }catch(java.awt.AWTException e){
        System.out.println(e.getMessage());
    }
    }else{
        System.out.println("Bandeja nao disponivel nesse ambiente");
    }
    
    setIconImage(imagem);
    
    
    setTitle("Orvalho");
    getRootPane().registerKeyboardAction(event -> Atualizacao.mostrarSituacao(this),
            javax.swing.KeyStroke.getKeyStroke("F1"), javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
    setSize(420, 300);
    setLocationRelativeTo(null); //O valor "null" posiciona a janela no centro da tela.
    setDefaultCloseOperation(bandejaDisponivel ? JFrame.HIDE_ON_CLOSE : JFrame.EXIT_ON_CLOSE);
    
    JPanel panel = new PainelRolavel();panel.setName("conteudoPrincipal");
    JPanel panel2  = new JPanel();
    JPanel panel3  = new JPanel();
    JPanel panel4 = new JPanel();
    JPanel panel5 = new JPanel();
    
    JLabel label1 = new JLabel("Uma pausa. Um gole. Continue.");
    JLabel label2 = new JLabel("Intervalo em minutos: ");
    JLabel label3 = new JLabel("Status: lembretes pausados");
    JLabel labelContagem = new JLabel("Próximo alerta: pausado");
    sincronizarContagem = () -> atualizarContagem(labelContagem);
    // Reserva espaço para o texto não mudar de tamanho durante a contagem.
    int larguraContagem = Math.max(labelContagem.getPreferredSize().width,
            labelContagem.getFontMetrics(labelContagem.getFont()).stringWidth("Próximo alerta: 240:00"));
    labelContagem.setPreferredSize(new Dimension(larguraContagem, labelContagem.getPreferredSize().height));
    labelContagem.setHorizontalAlignment(JLabel.CENTER);
    JPanel painelContagem = new JPanel();
    painelContagem.setOpaque(false);
    painelContagem.add(labelContagem);

    // Este timer apenas atualiza o texto; os alertas continuam a cargo do serviço.
    Timer atualizadorContagem = new Timer(1000, event -> atualizarContagem(labelContagem));
    addWindowListener(new WindowAdapter(){
        @Override
        public void windowClosed(WindowEvent event){
            atualizadorContagem.stop();
        }
    });
    
    JButton btnIniciar = new JButton("Iniciar");
    btnIniciar.setName("iniciarLembretes");
    JButton btnPausar = new JButton("Pausar");
    btnPausar.setEnabled(false);
    btnTestarAlerta = new JButton("Testar alerta");
    
    int minutosSalvos;
    try {
        minutosSalvos = configuracaoIntervalo.carregar(preferencias.getInt("intervaloMinutos", 30));
    } catch (java.io.IOException erro) {
        minutosSalvos = 30;
        DialogosOrvalho.showMessageDialog(this, "Não foi possível ler o intervalo salvo: " + erro.getMessage(),
                "Configuração do Orvalho", JOptionPane.ERROR_MESSAGE);
    }
    
    JSpinner campoIntervalo = new JSpinner();
    // setar as regras do JSpinner
    SpinnerNumberModel configSpinner = new SpinnerNumberModel(minutosSalvos, 1, 240, 1);
    campoIntervalo.setModel(configSpinner);
    
    // Essa linha obtém o editor do JSpinner, trata-o como um editor numérico e guarda sua referência na variável editor para você acessar o campo de texto interno.
    JSpinner.NumberEditor editor = (JSpinner.NumberEditor) campoIntervalo.getEditor();
    // quando perder o foco, mantém o numero digitado
    editor.getTextField().setFocusLostBehavior(JFormattedTextField.PERSIST);
    
    // Layout da referência: cabeçalho, cartão de contagem e cartão de controles.
    java.awt.Color texto = new java.awt.Color(0x12304A);
    java.awt.Color azul = new java.awt.Color(0x0876F9);
    java.awt.Color secundario = new java.awt.Color(0x526B86);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBackground(new java.awt.Color(0xF4F8FC));
    panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(28, 28, 18, 28));
    panel.setPreferredSize(new Dimension(480, 600));
    javax.swing.JScrollPane rolagem = new javax.swing.JScrollPane(panel,
            javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    rolagem.setBorder(javax.swing.BorderFactory.createEmptyBorder());
    rolagem.getVerticalScrollBar().setUnitIncrement(16);
    TemaOrvalho.rolagem(rolagem);
    this.add(rolagem);

    panel5.setLayout(new java.awt.BorderLayout(14, 0));
    panel5.setOpaque(false);
    panel5.setAlignmentX(0.5f);
    panel5.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
    JPanel textosCabecalho = new JPanel();
    textosCabecalho.setOpaque(false);
    textosCabecalho.setLayout(new BoxLayout(textosCabecalho, BoxLayout.Y_AXIS));
    label1.setText("Orvalho");
    label1.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 27));
    label1.setForeground(texto);
    JLabel slogan = new JLabel("Uma pequena pausa. Um bom hábito.");
    slogan.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
    slogan.setForeground(secundario);
    textosCabecalho.add(label1);
    textosCabecalho.add(Box.createVerticalStrut(4));
    textosCabecalho.add(slogan);
    panel5.add(textosCabecalho, java.awt.BorderLayout.CENTER);
    JPanel campanha=new JPanel();campanha.setOpaque(false);campanha.setLayout(new BoxLayout(campanha,BoxLayout.Y_AXIS));
    JButton selo=TemaOrvalho.selo();selo.setAlignmentX(1f);campanha.add(selo);
    panel5.add(campanha, java.awt.BorderLayout.EAST);
    panel.add(panel5);
    panel.add(Box.createVerticalStrut(20));

    // Os dois cartões compartilham apenas a mesma pintura de fundo.
    for (JPanel cartao : new JPanel[]{painelContagem, panel4}) {
        cartao.setOpaque(false);
        cartao.setLayout(new BoxLayout(cartao, BoxLayout.Y_AXIS));
        cartao.setAlignmentX(0.5f);
        cartao.setBorder(javax.swing.BorderFactory.createEmptyBorder(22, 24, 22, 24));
        cartao.setUI(new javax.swing.plaf.basic.BasicPanelUI() {
            @Override
            public void paint(java.awt.Graphics g, javax.swing.JComponent c) {
                java.awt.Graphics2D d = (java.awt.Graphics2D) g.create();
                d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                d.setColor(new java.awt.Color(66,16,45,12));d.fillRoundRect(0,3,c.getWidth()-1,c.getHeight()-4,24,24);
                d.setColor(TemaOrvalho.cor(c.getBackground()));
                d.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 4, 24, 24);
                d.setColor(TemaOrvalho.cor(new java.awt.Color(painelAgua != null && painelAgua.opcoes().escuro() ? 0x39536A : 0xD7E4F1)));
                d.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 4, 24, 24);
                d.dispose();
            }
        });
    }
    painelContagem.setBackground(new java.awt.Color(0xE8F3FF));
    painelContagem.setPreferredSize(new Dimension(396, 188));
    painelContagem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 188));
    label3.setText("Lembretes pausados");
    label3.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
    label3.setForeground(azul);
    label3.setMaximumSize(new Dimension(Integer.MAX_VALUE, label3.getPreferredSize().height + 2));
    JLabel tituloContagem = new JLabel("PRÓXIMO LEMBRETE EM");
    tituloContagem.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 11));
    tituloContagem.setForeground(secundario);
    labelContagem.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 48));
    labelContagem.setForeground(texto);
    labelContagem.setText("--:--");
    labelContagem.setPreferredSize(new Dimension(344, 62));
    labelContagem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
    labelContagem.setHorizontalAlignment(JLabel.LEFT);
    JLabel descricaoContagem = new JLabel("Uma pausa a cada " + minutosSalvos + " min.");
    descricaoContagem.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
    descricaoContagem.setForeground(secundario);
    painelContagem.remove(labelContagem);
    JPanel textosContagem = new JPanel();
    textosContagem.setOpaque(false);
    textosContagem.setLayout(new BoxLayout(textosContagem, BoxLayout.Y_AXIS));
    textosContagem.add(label3);
    textosContagem.add(Box.createVerticalStrut(18));
    textosContagem.add(tituloContagem);
    textosContagem.add(labelContagem);
    textosContagem.add(Box.createVerticalStrut(8));
    textosContagem.add(descricaoContagem);
    painelContagem.setLayout(new java.awt.BorderLayout(8, 0));
    painelContagem.add(textosContagem, java.awt.BorderLayout.CENTER);
    painelContagem.add(garrafa, java.awt.BorderLayout.EAST);
    panel.add(painelContagem);
    panel.add(Box.createVerticalStrut(16));

    panel4.setBackground(java.awt.Color.WHITE);
    panel4.setPreferredSize(new Dimension(396, 216));
    panel4.setMaximumSize(new Dimension(Integer.MAX_VALUE, 216));
    JLabel tituloControles = new JLabel("No seu ritmo");
    tituloControles.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 17));
    tituloControles.setForeground(texto);
    panel4.add(tituloControles);
    panel4.add(Box.createVerticalStrut(14));
    panel2.setOpaque(false);
    panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));
    panel2.setAlignmentX(0.0f);
    panel2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
    label2.setText("Lembrar a cada");
    label2.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
    label2.setForeground(texto);
    JLabel unidade = new JLabel("minutos");
    unidade.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
    unidade.setForeground(secundario);
    campoIntervalo.setPreferredSize(new Dimension(110, 46));
    campoIntervalo.setMinimumSize(new Dimension(110, 46));
    campoIntervalo.setMaximumSize(new Dimension(110, 46));
    campoIntervalo.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0xCCCCCC)));
    editor.getTextField().setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 20));
    editor.getTextField().setHorizontalAlignment(javax.swing.JTextField.CENTER);
    editor.getTextField().setBackground(new java.awt.Color(0xF2F2F2));
    editor.getTextField().setForeground(texto);
    editor.getTextField().setDisabledTextColor(new java.awt.Color(0x888888));
    panel2.add(label2);
    panel2.add(Box.createHorizontalStrut(8));
    panel2.add(campoIntervalo);
    panel2.add(Box.createHorizontalStrut(8));
    panel2.add(unidade);
    panel4.add(panel2);
    panel4.add(Box.createVerticalStrut(10));
    JLabel ajuda = new JLabel("Escolha o intervalo e inicie os lembretes.");
    ajuda.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
    ajuda.setForeground(secundario);
    panel4.add(ajuda);
    panel4.add(Box.createVerticalStrut(18));
    panel3.setOpaque(false);
    panel3.setAlignmentX(0.0f);
    panel3.setLayout(new java.awt.GridLayout(1, 3, 8, 0));
    panel3.setPreferredSize(new Dimension(348, 44));
    panel3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
    for (JButton botao : new JButton[]{btnIniciar, btnPausar, btnTestarAlerta}) {
        botao.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        botao.putClientProperty("orvalho.iniciar",botao==btnIniciar);
        botao.setForeground(texto);
        botao.setBackground(botao == btnTestarAlerta
                ? new java.awt.Color(0xE8F3FF) : java.awt.Color.WHITE);
        botao.setContentAreaFilled(false);
        botao.setOpaque(false);
        BotaoOrvalho.aplicar(botao,botao==btnIniciar);
        botao.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 4, 10, 4));
        botao.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        panel3.add(botao);
    }
    panel4.add(panel3);
    check.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 11));
    check.setForeground(secundario);
    check.setOpaque(false);
    check.setAlignmentX(0.0f);
    check.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    check.setMargin(new java.awt.Insets(0, 0, 0, 0));
    panel.add(panel4);

    // função botao INICIAR
    btnIniciar.addActionListener(event ->{
    String textoDigitado;
    try{
        textoDigitado = editor.getTextField().getText();
        if(!textoDigitado.matches("[0-9]+")){
            editor.getTextField().setValue(campoIntervalo.getValue());
            throw new IllegalArgumentException("Insira apenas números!");
        }
        campoIntervalo.commitEdit();
        int minutos = (Integer)(campoIntervalo.getValue());
        configuracaoIntervalo.salvar(minutos);
        lembreteService.start(minutos);
        
    } catch(java.io.IOException e){
        DialogosOrvalho.showMessageDialog(this, "Não foi possível salvar o intervalo: " + e.getMessage(),
                "Configuração do Orvalho", JOptionPane.ERROR_MESSAGE);
        return;
    } catch(java.text.ParseException e){
        DialogosOrvalho.showMessageDialog(this, "Insira valores de 1 a 240!");
        // volta para o ultimo valor valido
        editor.getTextField().setValue(campoIntervalo.getValue());
        
        return;
        
    } catch(IllegalArgumentException i){
        DialogosOrvalho.showMessageDialog(this, i.getMessage());
        editor.getTextField().setValue(campoIntervalo.getValue());
        
        return;
    }
        btnPausar.setEnabled(true);
        btnIniciar.setEnabled(false);
        campoIntervalo.setEnabled(false);
        atualizarContagem(labelContagem);
        atualizadorContagem.restart();
        
    label3.setText("Lembretes ativos");
    descricaoContagem.setText("Uma pausa a cada " + campoIntervalo.getValue() + " min.");
    ajuda.setText("Pause os lembretes para alterar o intervalo.");
    });
    
   btnTestarAlerta.addActionListener(event -> {
      btnTestarAlerta.setEnabled(false);
      mostrarAlerta();
   }); 
    
   btnPausar.addActionListener(event -> {
       campoIntervalo.setEnabled(true);
       btnPausar.setEnabled(false);
       btnIniciar.setEnabled(true);
       lembreteService.pausar();
       atualizadorContagem.stop();
       atualizarContagem(labelContagem);
       label3.setText("Lembretes pausados");
       ajuda.setText("Inicie para retomar; outro intervalo reinicia o tempo.");
   });
   
   Runnable abrirJanela = () -> {
       if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
           javax.swing.SwingUtilities.invokeLater(this::abrirDaBandeja);
       } else abrirDaBandeja();
   };
   menuItem1.addActionListener(event -> abrirJanela.run());
   if (iconeBandeja != null) {
       iconeBandeja.addActionListener(event -> abrirJanela.run());
       iconeBandeja.addMouseListener(new java.awt.event.MouseAdapter() {
           @Override public void mouseClicked(java.awt.event.MouseEvent event) {
               if (event.getButton() == java.awt.event.MouseEvent.BUTTON1 && event.getClickCount() == 2)
                   abrirJanela.run();
           }
       });
       addWindowListener(new WindowAdapter() {
           @Override public void windowClosed(WindowEvent event) { bandeja.remove(iconeBandeja); }
       });
   }
   
   menuItem2.addActionListener(event -> {
       lembreteService.pausar();
       if (bandeja != null && iconeBandeja != null) bandeja.remove(iconeBandeja);
       System.exit(0);
   });
   
  // Consulta e gravação acontecem em segundo plano, sem bloquear a janela.
  Runnable consultarInicio = () -> {
      check.setEnabled(false);
      check.setToolTipText("Consultando a inicialização do Windows...");
      new javax.swing.SwingWorker<InicializacaoWindows.Estado, Void>() {
          @Override protected InicializacaoWindows.Estado doInBackground() throws Exception {
              return InicializacaoWindows.consultar();
          }
          @Override protected void done() {
              try {
                  InicializacaoWindows.Estado estado = get();
                  check.setSelected(estado == InicializacaoWindows.Estado.ATIVA);
                  preferencias.putBoolean("iniciarComWindows", check.isSelected());
                  check.setText(estado == InicializacaoWindows.Estado.CAMINHO_INVALIDO
                          ? "Iniciar com o Windows (atualizar caminho)" : "Iniciar com o Windows");
                  check.setToolTipText(estado == InicializacaoWindows.Estado.CAMINHO_INVALIDO
                          ? "Marque para registrar este executável no lugar do caminho antigo."
                          : "Abrir na bandeja e iniciar os lembretes ao entrar no Windows.");
              } catch (Exception erro) {
                  check.setText("Iniciar com o Windows (verificar)");
                  check.setToolTipText("Falha na consulta. Clique para tentar configurar novamente.");
              } finally { check.setEnabled(true); }
          }
      }.execute();
  };
  check.addActionListener(event -> {
      boolean ativar = check.isSelected();
      check.setEnabled(false);
      new javax.swing.SwingWorker<Void, Void>() {
          @Override protected Void doInBackground() throws Exception {
              InicializacaoWindows.configurar(ativar);
              return null;
          }
          @Override protected void done() {
              try { get(); }
              catch (Exception erro) {
                  Throwable causa = erro.getCause() != null ? erro.getCause() : erro;
                  DialogosOrvalho.showMessageDialog(JanelaPrincipal.this, causa.getMessage(),
                          "Inicialização com o Windows", JOptionPane.ERROR_MESSAGE);
              } finally { consultarInicio.run(); }
          }
      }.execute();
  });
  consultarInicio.run();
  painelAgua = new PainelAgua(this, dadosAgua, () -> {
      TemaOrvalho.aplicar(getContentPane(), painelAgua.opcoes().escuro());
      AnimacoesOrvalho.reavaliar();garrafa.repaint();
      if (telaPopup != null) {TemaOrvalho.aplicar(telaPopup.getContentPane(), painelAgua.opcoes().escuro());telaPopup.pack();}
  }, check, consultarInicio);
  panel.add(painelAgua);
  panel.setPreferredSize(new Dimension(590, 880));
  PetalasOrvalho petalas=new PetalasOrvalho(rolagem);setGlassPane(petalas);petalas.setVisible(true);
  addWindowStateListener(e->AnimacoesOrvalho.reavaliar());
  addWindowListener(new java.awt.event.WindowAdapter(){@Override public void windowActivated(java.awt.event.WindowEvent e){AnimacoesOrvalho.consultarSistema();AnimacoesOrvalho.reavaliar();}});
  panel.addComponentListener(new java.awt.event.ComponentAdapter(){@Override public void componentResized(java.awt.event.ComponentEvent e){garrafa.setPreferredSize(new Dimension(panel.getWidth()<500?56:76,144));panel.revalidate();}});
  TemaOrvalho.aplicar(getContentPane(), painelAgua.opcoes().escuro());
  // Reutiliza o mesmo evento de Iniciar para manter controles e contagem sincronizados.
  if (inicioAutomatico) btnIniciar.doClick(0);
  pack();
  // Em telas menores, o conteúdo continua acessível pela rolagem existente.
  var tela = getGraphicsConfiguration().getBounds();
  var margens = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(getGraphicsConfiguration());
  setSize(Math.min(getWidth(), tela.width-margens.left-margens.right),
          Math.min(getHeight(), tela.height-margens.top-margens.bottom));
  setLocationRelativeTo(null);
  setMinimumSize(new Dimension(Math.min(460, getWidth()), Math.min(400, getHeight())));
   
   
   
 }  
    private PainelAgua painelAgua;
    private void abrirDaBandeja() {
        setExtendedState(getExtendedState() & ~JFrame.ICONIFIED);
        setVisible(true);
        toFront();
        requestFocus();
    }
    private boolean bandejaDisponivel;
    private Runnable sincronizarContagem;

    boolean interacaoPendente(){
        if(lembreteService.aguardandoConfirmacao())return true;
        return temDialogoEmUso(this);
    }
    static boolean temDialogoEmUso(java.awt.Window janela){
        for(var filha:janela.getOwnedWindows()){
            if(filha.isShowing()&&!"sobreAtualizacoes".equals(filha.getName()))return true;
            if(temDialogoEmUso(filha))return true;
        }
        return false;
    }
    String argumentosAposAtualizacao() {
        return "--atualizado" + (isVisible() ? "" : " --oculto")
                + (lembreteService.ativo() ? " --iniciar-com-windows" : "")
                + (lembreteService.aguardandoConfirmacao() ? " --aguardando-alerta" : "")
                + " --intervalo=" + lembreteService.intervalo()
                + " --restante=" + lembreteService.getSegundosRestantes();
    }

    void restaurarAposAtualizacao(int minutos, long segundos, boolean ativo) {
        if (minutos > 0) lembreteService.restaurar(minutos, segundos, ativo);
        sincronizarContagem.run();
    }

    void restaurarAlertaPendente() {
        lembreteService.restaurarAlertaPendente();
        sincronizarContagem.run();
        mostrarAlerta();
    }

    public boolean temBandeja() {
        return bandejaDisponivel;
    }

    private final ConfiguracaoIntervalo configuracaoIntervalo;
    private Preferences preferencias = Preferences.userNodeForPackage(JanelaPrincipal.class);
    
    private PopupLembrete telaPopup;
    private boolean popupReal;
    private JButton btnTestarAlerta;

    private void atualizarContagem(JLabel labelContagem){
        garrafa.repaint();
        long segundos = lembreteService.getSegundosRestantes();
        labelContagem.setText(String.format("%02d:%02d",
                segundos / 60, segundos % 60));
    }
    
    private void mostrarAlerta(){
        boolean real = lembreteService.aguardandoConfirmacao();
        if(telaPopup != null && telaPopup.isShowing()){
            if (!real || popupReal) return;
            telaPopup.dispose();
        }
        popupReal = real;
        DadosAgua.Opcoes opcoes = painelAgua.opcoes();
        telaPopup = new PopupLembrete(() -> {
            if (real && lembreteService.confirmarAlerta()) {
                garrafa.esvaziar();
                sincronizarContagem.run();
            }
        }, () -> {
            if (real) lembreteService.adiarAlerta();
            sincronizarContagem.run();
        }, ml -> !real || painelAgua.registrar(ml), opcoes, real, painelAgua.ultimoVolume(), true);
        btnTestarAlerta.setEnabled(false);
        telaPopup.addWindowListener(new WindowAdapter(){
          @Override
          public void windowClosed(WindowEvent e){
              if (telaPopup == e.getWindow() || telaPopup == null || !telaPopup.isShowing()) btnTestarAlerta.setEnabled(true);
          }
          });
        
        telaPopup.setVisible(true);
        
    }
    
}
