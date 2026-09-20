/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.bebaAgua;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowListener;
import javax.swing.JDialog;

/**
 *
 * @author bruno
 */
public class PopupLembrete extends JDialog {
    
    public PopupLembrete(){
        setTitle("Hora de beber água!!");
        setSize(210, 150);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
}
