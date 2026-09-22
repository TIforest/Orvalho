/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.bebaAgua;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowListener;
import javax.swing.BoxLayout;
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
        setAlwaysOnTop(true);
        setFocusableWindowState(false);
        
        setTitle("Hora de beber água!!");
        setSize(210, 150);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        JButton btnOk = new JButton("Ok, obrigado!");
        JPanel panel = new JPanel();
        JPanel panel2 = new JPanel();
        JLabel label = new JLabel("Hora de beber água!");
        
        
        
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Layout VERTICAL
        label.setAlignmentX(0.5f);
        this.add(panel);
        panel.add(label);
        panel.add(panel2);
        panel2.add(btnOk);
        
        btnOk.addActionListener(event -> {
        this.dispose();
    });
        
        
    }
}
