package com.mycompany.bebaAgua;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Locale;

/** Cartão de consumo pessoal; a referência não é uma obrigação de consumo. */
final class PainelHidratacao extends JPanel {
    private final CorpoHidratacao corpo = new CorpoHidratacao();
    private final JLabel volume = new JLabel("— L");
    private final JLabel referencia = new JLabel(" ");
    private final JLabel percentual = new JLabel(" ");
    private double fracao,exibida,inicial,tempo=1;
    private final AnimacoesOrvalho.Atividade animacao;
    private final JPanel barra = new JPanel() {
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = grafico(graphics);
            g.setColor(TemaOrvalho.cor(new Color(escuro()?0x29485E:0xDDEBF6)));
            g.fillRoundRect(0,0,getWidth(),6,6,6);
            int largura=(int)Math.round(getWidth()*exibida);
            if(largura>0){g.setPaint(new GradientPaint(0,0,TemaOrvalho.cor(new Color(0x176BDA)),getWidth(),0,TemaOrvalho.cor(new Color(0x58CAE9))));g.fillRoundRect(0,0,largura,6,6,6);}
            g.dispose();
        }
    };
    PainelHidratacao(Runnable configurarPeso) {
        animacao=AnimacoesOrvalho.vincular(this,()->tempo<1,dt->{tempo=Math.min(1,tempo+dt/.55);double suave=tempo*tempo*(3-2*tempo);exibida=inicial+(fracao-inicial)*suave;barra.repaint();},()->{exibida=fracao;tempo=1;});
        setOpaque(false);setLayout(new BorderLayout(16,12));
        setBorder(BorderFactory.createEmptyBorder(16,20,28,20));
        setPreferredSize(new Dimension(396,204));setMaximumSize(new Dimension(Integer.MAX_VALUE,204));
        JPanel cabecalho = new JPanel(new BorderLayout());cabecalho.setOpaque(false);
        JLabel titulo = rotulo("Hidratação de hoje",13,true);cabecalho.add(titulo,BorderLayout.WEST);
        JButton peso = new JButton("Ajustar peso");BotaoOrvalho.link(peso);
        peso.setFont(new Font("Segoe UI",Font.PLAIN,11));peso.setBorder(BorderFactory.createEmptyBorder(0,4,0,0));
        peso.setToolTipText("Alterar o peso usado na estimativa diária.");peso.addActionListener(e->configurarPeso.run());
        cabecalho.add(peso,BorderLayout.EAST);add(cabecalho,BorderLayout.NORTH);

        JPanel texto = new JPanel();texto.setOpaque(false);texto.setLayout(new BoxLayout(texto,BoxLayout.Y_AXIS));
        volume.setFont(new Font("Segoe UI",Font.BOLD,28));volume.setForeground(new Color(0x176BDA));texto.add(volume);
        referencia.setFont(new Font("Segoe UI",Font.PLAIN,11));texto.add(referencia);texto.add(Box.createVerticalStrut(12));
        JPanel indicadores=new JPanel(new BorderLayout());indicadores.setOpaque(false);
        indicadores.add(rotulo("Registrado no dia",10,false),BorderLayout.WEST);
        percentual.setFont(new Font("Segoe UI",Font.BOLD,10));indicadores.add(percentual,BorderLayout.EAST);
        indicadores.setMaximumSize(new Dimension(Integer.MAX_VALUE,16));texto.add(indicadores);texto.add(Box.createVerticalStrut(6));
        barra.setOpaque(false);barra.setPreferredSize(new Dimension(220,6));barra.setMaximumSize(new Dimension(Integer.MAX_VALUE,6));texto.add(barra);
        texto.add(Box.createVerticalStrut(8));texto.add(rotulo("Referência, sem obrigação de consumo.",10,false));
        for(Component c:texto.getComponents())if(c instanceof JComponent jc)jc.setAlignmentX(LEFT_ALIGNMENT);
        add(texto,BorderLayout.CENTER);
        JPanel ilustracao=new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics graphics){
                Graphics2D g=grafico(graphics);
                g.setColor(TemaOrvalho.cor(new Color(escuro()?0x203D52:0xEAF5FC)));
                g.fillRoundRect(0,0,getWidth(),getHeight(),24,24);g.dispose();
            }
        };
        ilustracao.setOpaque(false);ilustracao.setBorder(BorderFactory.createEmptyBorder(6,8,6,8));
        corpo.setPreferredSize(new Dimension(56,94));ilustracao.add(corpo);add(ilustracao,BorderLayout.EAST);
        addComponentListener(new java.awt.event.ComponentAdapter(){@Override public void componentResized(java.awt.event.ComponentEvent e){ilustracao.setVisible(getWidth()>=360);revalidate();}});
    }
    private boolean escuro(){return Boolean.TRUE.equals(getClientProperty("orvalho.escuro"));}
    private static Graphics2D grafico(Graphics graphics){Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);return g;}
    private static JLabel rotulo(String texto,int tamanho,boolean negrito){JLabel l=new JLabel(texto);l.setFont(new Font("Segoe UI",negrito?Font.BOLD:Font.PLAIN,tamanho));return l;}
    @Override protected void paintComponent(Graphics graphics){
        super.paintComponent(graphics);Graphics2D g=grafico(graphics);
        g.setColor(TemaOrvalho.cor(new Color(escuro()?0x172B3D:0xFFFFFF)));g.fillRoundRect(0,0,getWidth()-1,getHeight()-13,24,24);
        g.setColor(TemaOrvalho.cor(new Color(escuro()?0x36556B:0xD5E4EF)));g.drawRoundRect(0,0,getWidth()-1,getHeight()-13,24,24);g.dispose();
    }
    void atualizar(DadosAgua dados) {
        try {
            var progresso = ProgressoHidratacao.carregar(dados,LocalDate.now());var pt=Locale.forLanguageTag("pt-BR");
            volume.setText(String.format(pt,"%.2f L",progresso.ml()/1000.0));
            referencia.setText(progresso.referenciaMl()>0?String.format(pt,"Referência diária: %.2f L",progresso.referenciaMl()/1000.0):"Ajuste seu peso para ver a referência.");
            double nova=progresso.fracao();
            if(nova!=fracao){inicial=exibida;fracao=nova;tempo=0;if(!isShowing()||!AnimacoesOrvalho.permitidas()){exibida=fracao;tempo=1;}animacao.solicitar();}
            percentual.setText(progresso.referenciaMl()>0?String.format(pt,"%.0f%%",Math.floor(fracao*100)):"—");
            String descricao=volume.getText()+" registrados. "+referencia.getText()+". Referência visual de 35 ml/kg, sem obrigação de consumo.";
            setToolTipText(descricao);barra.setToolTipText(descricao);corpo.atualizar(fracao,descricao);barra.repaint();
        } catch(IOException e) {
            volume.setText("Indisponível");referencia.setText("Confira o histórico e o peso salvo.");percentual.setText("—");fracao=0;exibida=0;tempo=1;barra.repaint();
            setToolTipText("Não foi possível ler os dados de hidratação.");barra.setToolTipText(getToolTipText());corpo.atualizar(0,getToolTipText());
        }
    }
}
