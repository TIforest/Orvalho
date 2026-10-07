package com.mycompany.bebaAgua;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

final class PainelPeso extends JPanel {
    private final JTextField peso = new JTextField(8);
    private final JLabel resultado = new JLabel("— L/dia");
    private final JLabel status = new JLabel(" ");
    private final boolean escuro;

    PainelPeso(DadosAgua dados, boolean escuro) {
        this.escuro = escuro;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(new Color(0xF4F8FC));
        setBorder(BorderFactory.createEmptyBorder(24,24,24,24));
        add(rotulo("Sua hidratação",24,true));
        espaco(8);
        add(rotulo("Uma referência diária a partir do seu peso.",13,false));
        espaco(24);

        JPanel cartao = new JPanel(new BorderLayout(0,16)) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(TemaOrvalho.cor(new Color(PainelPeso.this.escuro ? 0x172B3D : 0xFFFFFF)));
                g.fillRoundRect(0,0,getWidth()-1,getHeight()-1,24,24);
                g.setColor(TemaOrvalho.cor(new Color(PainelPeso.this.escuro ? 0x36556B : 0xD5E4EF)));
                g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,24,24);g.dispose();
            }
        };
        cartao.setOpaque(false);cartao.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        JPanel entrada = new JPanel(new BorderLayout(16,0));entrada.setOpaque(false);
        entrada.add(rotulo("Peso corporal",13,true),BorderLayout.WEST);
        JPanel campo = new JPanel(new BorderLayout(8,0));campo.setOpaque(false);
        peso.setName("pesoCorporal");peso.getAccessibleContext().setAccessibleName("Peso corporal em quilogramas");
        peso.setFont(new Font("Segoe UI",Font.PLAIN,18));peso.setHorizontalAlignment(JTextField.RIGHT);
        peso.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0x7B9CB5)),BorderFactory.createEmptyBorder(8,12,8,12)));
        peso.setPreferredSize(new Dimension(108,42));
        campo.add(peso,BorderLayout.CENTER);campo.add(rotulo("kg",13,false),BorderLayout.EAST);entrada.add(campo,BorderLayout.EAST);
        cartao.add(entrada,BorderLayout.NORTH);
        JPanel estimativa = new JPanel();estimativa.setOpaque(false);estimativa.setLayout(new BoxLayout(estimativa,BoxLayout.Y_AXIS));
        estimativa.add(rotulo("ESTIMATIVA DIÁRIA DE LÍQUIDOS",11,true));estimativa.add(Box.createVerticalStrut(8));
        resultado.setName("estimativaLitros");resultado.setFont(new Font("Segoe UI",Font.BOLD,28));resultado.setForeground(new Color(0x176BDA));estimativa.add(resultado);
        estimativa.add(Box.createVerticalStrut(8));estimativa.add(rotulo("30–35 ml/kg · Para adultos saudáveis",12,false));
        cartao.add(estimativa,BorderLayout.CENTER);cartao.setMaximumSize(new Dimension(Integer.MAX_VALUE,202));add(cartao);
        espaco(16);
        add(rotulo("<html>É uma estimativa, não uma meta obrigatória.<br>Respeite suas necessidades e orientações de saúde.</html>",12,false));
        espaco(16);
        add(rotulo("Seu peso fica só neste computador.",11,false));
        espaco(24);
        JPanel acoes = new JPanel(new GridLayout(1,2,12,0));acoes.setOpaque(false);
        JButton salvar = new JButton("Salvar peso"), apagar = new JButton("Apagar peso");
        BotaoOrvalho.aplicar(salvar,true);BotaoOrvalho.aplicar(apagar,false);acoes.add(salvar);acoes.add(apagar);
        acoes.setMaximumSize(new Dimension(Integer.MAX_VALUE,42));acoes.setPreferredSize(new Dimension(400,42));add(acoes);
        espaco(8);
        status.setFont(new Font("Segoe UI",Font.PLAIN,11));status.setPreferredSize(new Dimension(400,32));status.setMaximumSize(new Dimension(Integer.MAX_VALUE,32));add(status);
        espaco(8);
        JButton fontes = new JButton("Sobre o cálculo e cuidados");BotaoOrvalho.link(fontes);
        fontes.setBorder(BorderFactory.createEmptyBorder(4,0,4,0));fontes.addActionListener(e->sobre());add(fontes);
        add(Box.createVerticalGlue());

        Runnable calcular = () -> {
            status.setText(" ");status.setToolTipText(null);
            try { resultado.setText(EstimativaHidratacao.faixa(EstimativaHidratacao.peso(peso.getText())));salvar.setEnabled(true); }
            catch (IllegalArgumentException e) {
                resultado.setText("— L/dia");salvar.setEnabled(false);
                status.setText(peso.getText().isBlank()?"Informe seu peso para calcular.":"<html>"+e.getMessage()+"</html>");
            }
        };
        peso.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){calcular.run();}
            public void removeUpdate(DocumentEvent e){calcular.run();}
            public void changedUpdate(DocumentEvent e){calcular.run();}
        });
        calcular.run();try {peso.setText(EstimativaHidratacao.carregar(dados));}catch(Exception e){status.setText("Confira o peso salvo e informe um novo valor.");status.setToolTipText(e.getMessage());}
        salvar.addActionListener(e->{try{EstimativaHidratacao.salvar(dados,peso.getText());status.setText("Peso salvo neste computador.");}catch(Exception ex){status.setText("Não foi possível salvar o peso.");}});
        apagar.addActionListener(e->{try{EstimativaHidratacao.apagar(dados);peso.setText("");status.setText("Peso removido deste computador.");}catch(Exception ex){status.setText("Não foi possível apagar o peso.");}});
        for(Component c:getComponents())if(c instanceof JComponent jc)jc.setAlignmentX(LEFT_ALIGNMENT);
        TemaOrvalho.aplicar(this,escuro);
    }
    private void sobre() {
        JTextArea info = new JTextArea("COMO É CALCULADO\nReferência aproximada de 30–35 ml/kg por dia para adultos saudáveis. Não é uma quantidade mínima nem uma meta obrigatória. Água de outras bebidas e dos alimentos também contribui para a hidratação. O boneco usa 35 ml/kg como referência visual de preenchimento.\n\nCUIDADOS\nClima, atividade física, idade e saúde mudam suas necessidades. Em caso de doença renal ou cardíaca, restrição de líquidos, gestação, amamentação ou fragilidade, peça orientação profissional em vez de usar este cálculo. Não beba em excesso para atingir o valor indicado.\n\nPRIVACIDADE\nSeu peso fica apenas neste computador e não aparece na competição.\n\nFONTES\nRoyal Cornwall Hospitals (NHS):\nhttps://doclibrary-rcht.cornwall.nhs.uk/GET/d10358773\n\nMayo Clinic:\nhttps://www.mayoclinic.org/healthy-lifestyle/nutrition-and-healthy-eating/in-depth/water/art-20044256");
        info.setEditable(false);info.setLineWrap(true);info.setWrapStyleWord(true);info.setFont(new Font("Segoe UI",Font.PLAIN,13));info.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));info.setCaretPosition(0);
        JDialog d=new JDialog(SwingUtilities.getWindowAncestor(this),"Sobre o cálculo e cuidados",Dialog.ModalityType.APPLICATION_MODAL);
        JScrollPane scroll = new JScrollPane(info);scroll.setBorder(BorderFactory.createEmptyBorder());TemaOrvalho.aplicar(scroll,escuro);
        d.setContentPane(scroll);d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);dimensionar(d,540,540);d.setLocationRelativeTo(this);d.setVisible(true);
    }
    private static JLabel rotulo(String texto,int tamanho,boolean negrito){JLabel l=new JLabel(texto);l.setFont(new Font("Segoe UI",negrito?Font.BOLD:Font.PLAIN,tamanho));return l;}
    private void espaco(int altura){add(Box.createVerticalStrut(altura));}
    static void abrir(Window dono,DadosAgua dados,boolean escuro){
        JDialog d=new JDialog(dono,"Orvalho — Estimativa de hidratação",Dialog.ModalityType.APPLICATION_MODAL);
        var painel=new PainelPeso(dados,escuro);JScrollPane scroll=new JScrollPane(painel);scroll.setBorder(BorderFactory.createEmptyBorder());scroll.getVerticalScrollBar().setUnitIncrement(16);
        d.setContentPane(scroll);d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);dimensionar(d,500,574);d.setLocationRelativeTo(dono);d.setVisible(true);
    }
    static void dimensionar(JDialog d,int largura,int altura){Rectangle tela=d.getGraphicsConfiguration().getBounds();Insets m=Toolkit.getDefaultToolkit().getScreenInsets(d.getGraphicsConfiguration());d.setSize(Math.min(largura,tela.width-m.left-m.right-24),Math.min(altura,tela.height-m.top-m.bottom-24));}
}
