/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.bebaAgua;

import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author bruno
 */
public class PopupLembrete extends JDialog {
    
    private boolean concluido;
    public PopupLembrete(){
        this(() -> {});
    }

    PopupLembrete(Runnable aoConfirmar){
        this(aoConfirmar, () -> {}, () -> true, new DadosAgua.Opcoes(false,false,true), false);
    }

    PopupLembrete(Runnable aoConfirmar, Runnable aoAdiar, java.util.function.BooleanSupplier aoBeber,
                  DadosAgua.Opcoes opcoes, boolean real) {
        this(aoConfirmar,aoAdiar,ml -> aoBeber.getAsBoolean(),opcoes,real,200,false);
    }
    PopupLembrete(Runnable aoConfirmar,Runnable aoAdiar,java.util.function.IntPredicate aoBeber,
                  DadosAgua.Opcoes opcoes,boolean real,int ultimoVolume,boolean quantidade) {
        this(aoConfirmar, minutos -> aoAdiar.run(), aoBeber, opcoes, real, ultimoVolume, quantidade);
    }
    PopupLembrete(Runnable aoConfirmar,java.util.function.IntConsumer aoAdiar,java.util.function.IntPredicate aoBeber,
                  DadosAgua.Opcoes opcoes,boolean real,int ultimoVolume,boolean quantidade) {
    java.net.URL recursoIcone = PopupLembrete.class.getResource("/imagens/agua.png"); 
    ImageIcon icone = new ImageIcon(recursoIcone);
    Image imagem = icone.getImage();
    setIconImage(imagem);
    
    
        // Ranking, histórico e configurações são modais. O alerta deve continuar
        // recebendo cliques mesmo quando alguma dessas telas estiver aberta.
        setModalExclusionType(java.awt.Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        setAlwaysOnTop(true);
        setFocusableWindowState(true);
        setAutoRequestFocus(false);
        
        setTitle("Lembrete");
        setSize(210, 150);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        
        JButton btnOk = new JButton("Já bebi");
        btnOk.setIcon(new javax.swing.Icon() {
            public int getIconWidth(){return 16;}
            public int getIconHeight(){return 18;}
            public void paintIcon(java.awt.Component c,java.awt.Graphics g,int x,int y){
                var d=(java.awt.Graphics2D)g.create();d.translate(x,y);
                d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                var gota=new java.awt.geom.Path2D.Double();gota.moveTo(8,1);
                gota.curveTo(6,5,2,8,2,11);gota.curveTo(2,19,14,19,14,11);
                gota.curveTo(14,8,10,5,8,1);gota.closePath();d.setColor(TemaOrvalho.cor(c.getForeground()));d.fill(gota);d.dispose();
            }
        });
        btnOk.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        JPanel panel = new JPanel();
        JPanel panel2 = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
        JLabel label = new JLabel("Hora de beber água!");
        
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Layout VERTICAL
        label.setAlignmentX(0.5f);
        panel.add(Box.createVerticalStrut(24));
       
        this.add(panel);
        panel.add(label);
        JButton selo=TemaOrvalho.selo();selo.setAlignmentX(0.5f);panel.add(selo);
        panel.add(Box.createVerticalStrut(12));
        JLabel mensagem = new JLabel("<html><div style='width:240px;text-align:center'>"
                + MensagensLembrete.proxima() + "</div></html>") {
            @Override protected void paintComponent(java.awt.Graphics graphics) {
                var g=(java.awt.Graphics2D)graphics.create();
                g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                boolean escuro=Boolean.TRUE.equals(getClientProperty("orvalho.escuro"));
                g.setColor(TemaOrvalho.cor(new java.awt.Color(escuro?0x203D52:0xE1EFFB)));
                g.fillRoundRect(0,0,getWidth(),getHeight(),18,18);
                g.setColor(TemaOrvalho.cor(new java.awt.Color(escuro?0x78AFD3:0x77A9D0)));
                g.fillRoundRect(0,10,3,Math.max(8,getHeight()-20),3,3);
                g.dispose();super.paintComponent(graphics);
            }
        };
        mensagem.setName("mensagemHumorada");
        mensagem.setAlignmentX(0.5f);
        mensagem.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        mensagem.setBorder(javax.swing.BorderFactory.createEmptyBorder(10,14,10,14));
        mensagem.setForeground(new java.awt.Color(0x52748E));
        mensagem.setVisible(opcoes.humor());
        panel.add(mensagem);
        panel.add(Box.createVerticalStrut(12));
        PainelTemperatura clima=new PainelTemperatura();panel.add(clima);
        addWindowListener(new WindowAdapter(){
            @Override public void windowOpened(java.awt.event.WindowEvent e){clima.iniciar();}
            @Override public void windowClosed(java.awt.event.WindowEvent e){clima.parar();}
        });
        javax.swing.JSpinner volume = new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(ultimoVolume,10,2000,50));
        volume.setPreferredSize(new java.awt.Dimension(85,32));
        panel.add(Box.createVerticalStrut(20));
        if(quantidade){
            JPanel linha=new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER,8,0));linha.setOpaque(false);
            linha.add(new JLabel("Bebi"));linha.add(volume);linha.add(new JLabel("ml"));
            linha.setAlignmentX(0.5f);
            linha.setMaximumSize(linha.getPreferredSize());
            panel.add(linha);
            panel.add(Box.createVerticalStrut(8));
            volume.setToolTipText("Informe quanto bebeu nesta pausa. A última quantidade fica salva.");
        }
        panel.add(panel2);
        panel2.add(btnOk);
        
        // Aparência do alerta, mantendo o mesmo comportamento.
        panel.setBackground(new java.awt.Color(0xF0F7FF));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 24, 20, 24));
        panel2.setOpaque(false);
        panel2.setAlignmentX(0.5f);
        label.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 24));
        label.setForeground(new java.awt.Color(0x12304A));
        btnOk.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 15));
        btnOk.setForeground(java.awt.Color.WHITE);
        btnOk.setBackground(new java.awt.Color(0x176BDA));
        btnOk.setContentAreaFilled(false);
        btnOk.setOpaque(false);
        BotaoOrvalho.aplicar(btnOk,true);
        btnOk.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 15));
        btnOk.setBorder(javax.swing.BorderFactory.createEmptyBorder(13, 29, 13, 29));
        btnOk.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        panel2.setMaximumSize(panel2.getPreferredSize());
        setMinimumSize(new java.awt.Dimension(380, 260));
        pack();
        setLocationRelativeTo(null);

        {
            JPanel acoes = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 8, 0));
            acoes.setOpaque(false);
            javax.swing.JSpinner espera = new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(5,1,240,1));
            espera.setName("minutosAdiamento");
            espera.setPreferredSize(new java.awt.Dimension(72,32));
            espera.setToolTipText("Escolha de 1 a 240 minutos para o próximo lembrete. O intervalo habitual será mantido.");
            espera.getAccessibleContext().setAccessibleName("Minutos até o próximo lembrete");
            acoes.add(new JLabel("Daqui a"));
            acoes.add(espera);
            acoes.add(new JLabel("min"));
            JButton adiar = new JButton("Adiar");
            BotaoOrvalho.aplicar(adiar,false);
            adiar.addActionListener(e -> {
                if (concluido) return;
                try { espera.commitEdit(); }
                catch (java.text.ParseException erro) {
                    DialogosOrvalho.showMessageDialog(this,"Informe de 1 a 240 minutos.");
                    return;
                }
                int minutos = ((Number)espera.getValue()).intValue();
                concluido = true; dispose(); aoAdiar.accept(minutos);
            });
            acoes.add(adiar);
            acoes.setAlignmentX(0.5f);
            acoes.setMaximumSize(acoes.getPreferredSize());
            panel.add(Box.createVerticalStrut(10));
            panel.add(acoes);
            if (!real) {
                JLabel teste = new JLabel("Teste: não registra pausas nem adia lembretes.");
                teste.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 11));
                teste.setAlignmentX(0.5f); panel.add(Box.createVerticalStrut(10)); panel.add(teste);
            }
        }
        TemaOrvalho.aplicar(panel, opcoes.escuro());
        pack(); setLocationRelativeTo(null);
        if (opcoes.som()) addWindowListener(new WindowAdapter(){
            @Override public void windowOpened(java.awt.event.WindowEvent e){SomAlerta.tocarGota();}
        });
        btnOk.setFocusable(false);
        btnOk.addActionListener(event -> {
        if (concluido) return;
        try{volume.commitEdit();}catch(java.text.ParseException erro){DialogosOrvalho.showMessageDialog(this,"Informe de 10 a 2000 ml.");return;}
        if (!aoBeber.test(((Number)volume.getValue()).intValue())) return;
        concluido = true;
        btnOk.setEnabled(false);
        this.dispose();
        aoConfirmar.run();
    });
        
        
    }
}
