/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.bebaAgua;

import javax.swing.JFrame; // Trás a função de Janela, com título, bordas e controles de minimizar e fechar.
import javax.swing.JPanel; // Agrupa os componentes da interface. Você coloca os campos e botões no painel, e adiciona esse painel à janela (JFrame).
import javax.swing.JLabel; // Exibe textos.
import javax.swing.JButton; // Cria botões.
import javax.swing.JSpinner; // Cria o campo com setas de seleção de valores. Também permite digitar valores.
import javax.swing.SpinnerNumberModel; //Define as regras numéricas do JSpinner: valor inicial, mínimo, máximo e incremento das setas.
import java.awt.FlowLayout; // Organiza os componentes em sequência, da esquerda para a direita, passando para outra linha quando falta espaço.
import javax.swing.BoxLayout;

public class JanelaPrincipal extends JFrame {
    
    // Construtor para a configuração da Janela (titulo, tamanhoJanela e etc).
    public JanelaPrincipal(){
    setTitle("Beba Água"); 
    setSize(410, 205);  
    setLocationRelativeTo(null); //O valor "null" posiciona a janela no centro da tela.
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Encerra o programa ao fechar o app.
    
    JPanel panel = new JPanel();
    
    JLabel label1 = new JLabel("Lembrete de água");
    JLabel label2 = new JLabel("Intervalo em minutos: ");
    
     
    JButton btnInciar = new JButton("Inciar");
    panel.add(btnInciar);
    
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    
    panel.add(label1);
    panel.add(label2);
    
    this.add(panel);
    
    
    
    
    
    }
}
