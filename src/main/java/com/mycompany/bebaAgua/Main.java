/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.bebaAgua;

import javax.swing.SwingUtilities;
/**
 *
 * @author bruno|
 */
public class Main {
    public static void main(String [] args){
        
    SwingUtilities.invokeLater(() -> {
    JanelaPrincipal frame = new JanelaPrincipal();
               
    frame.pack();
    frame.setVisible(true);
    });
  }
}
