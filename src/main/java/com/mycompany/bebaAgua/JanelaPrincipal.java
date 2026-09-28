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
    
    
    
    // Construtor para a configuração da Janela (titulo, tamanhoJanela e etc).
    public JanelaPrincipal(){
        this(false);
    }

    public JanelaPrincipal(boolean inicioAutomatico){
        this(inicioAutomatico, new ConfiguracaoIntervalo());
    }

    JanelaPrincipal(boolean inicioAutomatico, ConfiguracaoIntervalo configuracao){
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
    setSize(420, 300);
    setLocationRelativeTo(null); //O valor "null" posiciona a janela no centro da tela.
    setDefaultCloseOperation(bandejaDisponivel ? JFrame.HIDE_ON_CLOSE : JFrame.EXIT_ON_CLOSE);
    
    JPanel panel = new JPanel();
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
        JOptionPane.showMessageDialog(this, "Não foi possível ler o intervalo salvo: " + erro.getMessage(),
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
            javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    rolagem.setBorder(javax.swing.BorderFactory.createEmptyBorder());
    rolagem.getVerticalScrollBar().setUnitIncrement(16);
    this.add(rolagem);

    panel5.setLayout(new java.awt.BorderLayout(14, 0));
    panel5.setOpaque(false);
    panel5.setAlignmentX(0.5f);
    panel5.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
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
                d.setColor(c.getBackground());
                d.fillRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 24, 24);
                d.setColor(new java.awt.Color(0xD7E4F1));
                d.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 24, 24);
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
    painelContagem.add(label3);
    painelContagem.add(Box.createVerticalStrut(18));
    painelContagem.add(tituloContagem);
    painelContagem.add(labelContagem);
    painelContagem.add(Box.createVerticalStrut(8));
    painelContagem.add(descricaoContagem);
    panel.add(painelContagem);
    panel.add(Box.createVerticalStrut(16));

    panel4.setBackground(java.awt.Color.WHITE);
    panel4.setPreferredSize(new Dimension(396, 252));
    panel4.setMaximumSize(new Dimension(Integer.MAX_VALUE, 252));
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
        botao.setForeground(texto);
        botao.setBackground(botao == btnTestarAlerta
                ? new java.awt.Color(0xE8F3FF) : java.awt.Color.WHITE);
        botao.setContentAreaFilled(false);
        botao.setOpaque(false);
        botao.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(java.awt.Graphics g, javax.swing.JComponent c) {
                JButton b = (JButton) c;
                java.awt.Graphics2D d = (java.awt.Graphics2D) g.create();
                d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                d.setColor(b.isEnabled() ? b.getBackground() : new java.awt.Color(0xF2F2F2));
                d.fillRoundRect(1, 1, c.getWidth() - 3, c.getHeight() - 3, 42, 42);
                d.setColor(b == btnTestarAlerta ? new java.awt.Color(0x82B5F5)
                        : new java.awt.Color(0xCCCCCC));
                d.drawRoundRect(1, 1, c.getWidth() - 3, c.getHeight() - 3, 42, 42);
                if (b.hasFocus()) {
                    d.setColor(azul);
                    d.drawRoundRect(4, 4, c.getWidth() - 9, c.getHeight() - 9, 36, 36);
                }
                d.dispose();
                super.paint(g, c);
            }
        });
        botao.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 4, 10, 4));
        botao.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        panel3.add(botao);
    }
    panel4.add(panel3);
    panel4.add(Box.createVerticalStrut(12));
    check.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 11));
    check.setForeground(secundario);
    check.setOpaque(false);
    check.setAlignmentX(0.0f);
    check.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    check.setMargin(new java.awt.Insets(0, 0, 0, 0));
    panel4.add(check);
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
        JOptionPane.showMessageDialog(this, "Não foi possível salvar o intervalo: " + e.getMessage(),
                "Configuração do Orvalho", JOptionPane.ERROR_MESSAGE);
        return;
    } catch(java.text.ParseException e){
        JOptionPane.showMessageDialog(this, "Insira valores de 1 a 240!");
        // volta para o ultimo valor valido
        editor.getTextField().setValue(campoIntervalo.getValue());
        
        return;
        
    } catch(IllegalArgumentException i){
        JOptionPane.showMessageDialog(this, i.getMessage());
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
   
   menuItem1.addActionListener(event -> {
       setExtendedState(getExtendedState() & ~JFrame.ICONIFIED);
       setVisible(true);
       toFront();
       requestFocus();
   });
   
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
                  JOptionPane.showMessageDialog(JanelaPrincipal.this, causa.getMessage(),
                          "Inicialização com o Windows", JOptionPane.ERROR_MESSAGE);
              } finally { consultarInicio.run(); }
          }
      }.execute();
  });
  consultarInicio.run();
  // Reutiliza o mesmo evento de Iniciar para manter controles e contagem sincronizados.
  if (inicioAutomatico) btnIniciar.doClick(0);
  pack();
  setMinimumSize(new Dimension(Math.min(420, getWidth()), Math.min(400, getHeight())));
   
   
   
 }  
    private boolean bandejaDisponivel;
    private Runnable sincronizarContagem;

    String argumentosAposAtualizacao() {
        return "--atualizado" + (isVisible() ? "" : " --oculto")
                + (lembreteService.ativo() ? " --iniciar-com-windows" : "")
                + " --intervalo=" + lembreteService.intervalo()
                + " --restante=" + lembreteService.getSegundosRestantes();
    }

    void restaurarAposAtualizacao(int minutos, long segundos, boolean ativo) {
        if (minutos > 0) lembreteService.restaurar(minutos, segundos, ativo);
        sincronizarContagem.run();
    }

    public boolean temBandeja() {
        return bandejaDisponivel;
    }

    private final ConfiguracaoIntervalo configuracaoIntervalo;
    private Preferences preferencias = Preferences.userNodeForPackage(JanelaPrincipal.class);
    
    private PopupLembrete telaPopup;
    private JButton btnTestarAlerta;

    private void atualizarContagem(JLabel labelContagem){
        long segundos = lembreteService.getSegundosRestantes();
        labelContagem.setText(String.format("%02d:%02d",
                segundos / 60, segundos % 60));
    }
    
    private void mostrarAlerta(){
        if(telaPopup != null && telaPopup.isShowing()){
            return;
        }
        telaPopup = new PopupLembrete();
        btnTestarAlerta.setEnabled(false);
        telaPopup.addWindowListener(new WindowAdapter(){
          @Override
          public void windowClosed(WindowEvent e){
              btnTestarAlerta.setEnabled(true);
          }
          });
        
        telaPopup.setVisible(true);
        
    }
    
}
