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




public class JanelaPrincipal extends JFrame {
    
 
    
    LembreteService lembreteService =
       new LembreteService(() -> mostrarAlerta());
    
    
    
    // Construtor para a configuração da Janela (titulo, tamanhoJanela e etc).
    public JanelaPrincipal(){
    //Icone do APP
    SystemTray bandeja = SystemTray.getSystemTray();
    java.net.URL recursoIcone = JanelaPrincipal.class.getResource("/imagens/agua.png"); 
    ImageIcon icone = new ImageIcon(recursoIcone);
    Image imagem = icone.getImage();
    TrayIcon iconeBandeja = new TrayIcon(imagem, "Pausa azul");
    
    MenuItem menuItem1 = new MenuItem("Abrir");
    MenuItem menuItem2 = new MenuItem("Fechar");
    
    if(SystemTray.isSupported()){    
    iconeBandeja.setImageAutoSize(true);
    PopupMenu popupMenu = new PopupMenu();
    
    popupMenu.add(menuItem1);
    popupMenu.add(menuItem2);
    
    try{
    bandeja.add(iconeBandeja);
    iconeBandeja.setPopupMenu(popupMenu);
        
    }catch(java.awt.AWTException e){
        System.out.println(e.getMessage());
    }
    }else{
        System.out.println("Bandeja nao disponivel nesse ambiente");
    }
    
    setIconImage(imagem);
    
    
    setTitle("Pausa Azul");
    setSize(420, 300);
    setLocationRelativeTo(null); //O valor "null" posiciona a janela no centro da tela.
    setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE); // Encerra o programa ao fechar o app.
    
    JPanel panel = new JPanel();
    JPanel panel2  = new JPanel();
    JPanel panel3  = new JPanel();
    JPanel panel4 = new JPanel();
    JPanel panel5 = new JPanel();
    
    JLabel label1 = new JLabel("Uma pausa. Um gole. Continue.");
    JLabel label2 = new JLabel("Intervalo em minutos: ");
    JLabel label3 = new JLabel("Status: lembretes pausados");
    JLabel labelContagem = new JLabel("Próximo alerta: pausado");
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
    JButton btnPausar = new JButton("Pausar");
    btnPausar.setEnabled(false);
    btnTestarAlerta = new JButton("Testar alerta");
    
    int minutosSalvos = preferencias.getInt("intervaloMinutos", 30);
    
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
    panel.setPreferredSize(new Dimension(452, 544));
    this.add(panel);

    panel5.setLayout(new java.awt.BorderLayout(14, 0));
    panel5.setOpaque(false);
    panel5.setAlignmentX(0.5f);
    panel5.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
    JPanel textosCabecalho = new JPanel();
    textosCabecalho.setOpaque(false);
    textosCabecalho.setLayout(new BoxLayout(textosCabecalho, BoxLayout.Y_AXIS));
    label1.setText("Pausa azul");
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
        lembreteService.start(minutos);
        preferencias.putInt("intervaloMinutos", minutos);
        
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
       labelContagem.setText("--:--");
       label3.setText("Lembretes pausados");
       ajuda.setText("Escolha o intervalo e inicie os lembretes.");
   });
   
   menuItem1.addActionListener(event -> {
       this.setVisible(true);
   });
   
   menuItem2.addActionListener(event -> {
       lembreteService.pausar();
       bandeja.remove(iconeBandeja);
       System.exit(0);
   });
   
   
   
 }  
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
