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
    
    public PopupLembrete(){
    java.net.URL recursoIcone = PopupLembrete.class.getResource("/imagens/agua.png"); 
    ImageIcon icone = new ImageIcon(recursoIcone);
    Image imagem = icone.getImage();
    setIconImage(imagem);
    
    
        setAlwaysOnTop(true);
        setFocusableWindowState(false);
        
        setTitle("Lembrete");
        setSize(210, 150);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        
        JButton btnOk = new JButton("Ok, obrigado!");
        JPanel panel = new JPanel();
        JPanel panel2 = new JPanel();
        JLabel label = new JLabel("Hora de beber água!");
        
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Layout VERTICAL
        label.setAlignmentX(0.5f);
        panel.add(Box.createVerticalStrut(24));
       
        this.add(panel);
        panel.add(label);
        panel.add(panel2);
        panel2.add(btnOk);
        
        // Aparência do alerta, mantendo o mesmo comportamento.
        panel.setBackground(new java.awt.Color(0xF0F7FF));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 24, 20, 24));
        panel2.setOpaque(false);
        panel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 0, 0, 0));
        label.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 24));
        label.setForeground(new java.awt.Color(0x12304A));
        btnOk.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 15));
        btnOk.setForeground(java.awt.Color.WHITE);
        btnOk.setBackground(new java.awt.Color(0x176BDA));
        btnOk.setContentAreaFilled(false);
        btnOk.setOpaque(false);
        btnOk.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(java.awt.Graphics g, javax.swing.JComponent c) {
                JButton botao = (JButton) c;
                java.awt.Graphics2D desenho = (java.awt.Graphics2D) g.create();
                desenho.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                desenho.setColor(botao.getBackground());
                desenho.fillRoundRect(1, 1, c.getWidth() - 2, c.getHeight() - 2, 20, 20);
                if (botao.hasFocus()) {
                    desenho.setColor(botao.getForeground());
                    desenho.drawRoundRect(4, 4, c.getWidth() - 9, c.getHeight() - 9, 16, 16);
                }
                desenho.dispose();
                super.paint(g, c);
            }
        });
        btnOk.setBorder(javax.swing.BorderFactory.createEmptyBorder(13, 29, 13, 29));
        btnOk.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        setSize(380, 240);
        setLocationRelativeTo(null);

        btnOk.setFocusable(false);
        btnOk.addActionListener(event -> {
        this.dispose();
    });
        
        
    }
}
