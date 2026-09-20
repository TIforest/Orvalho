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



public class JanelaPrincipal extends JFrame {
    private String status = "lembretes pausados";
    
    public String getStatus(){
        return status;
    }
    
    public void setStatus(String status){
        this.status = status;
    }
    
    LembreteService lembreteService = new LembreteService();
    
    // Construtor para a configuração da Janela (titulo, tamanhoJanela e etc).
    public JanelaPrincipal(){
    setTitle("Beba Água");
    setSize(420, 300);
    setLocationRelativeTo(null); //O valor "null" posiciona a janela no centro da tela.
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Encerra o programa ao fechar o app.
    
    JPanel panel = new JPanel();
    JPanel panel2  = new JPanel();
    JPanel panel3  = new JPanel();
    JPanel panel4 = new JPanel();
    JPanel panel5 = new JPanel();
    
    JLabel label1 = new JLabel("Lembrete de água");
    JLabel label2 = new JLabel("Intervalo em minutos: ");
    JLabel label3 = new JLabel("Status: " + getStatus());
    
    JButton btnIniciar = new JButton("Inciar");
    JButton btnPausar = new JButton("Pausar");
    JButton btnTestarAlerta = new JButton("Testar alerta");
    
    JSpinner campoIntervalo = new JSpinner();
    // setar as regras do JSpinner
    SpinnerNumberModel configSpinner = new SpinnerNumberModel(30, 1, 240, 1);
    campoIntervalo.setModel(configSpinner);
    
    // Essa linha obtém o editor do JSpinner, trata-o como um editor numérico e guarda sua referência na variável editor para você acessar o campo de texto interno.
    JSpinner.NumberEditor editor = (JSpinner.NumberEditor) campoIntervalo.getEditor();
    // quando perder o foco, mantém o numero digitado
    editor.getTextField().setFocusLostBehavior(JFormattedTextField.PERSIST);
    
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Layout VERTICAL
    label1.setAlignmentX(0.5f);
    campoIntervalo.setPreferredSize(new Dimension(80, 30));
    campoIntervalo.setMaximumSize(new Dimension(80,30));
    campoIntervalo.setMinimumSize(new Dimension(80,30));
    panel3.setLayout(new BoxLayout(panel3, BoxLayout.X_AXIS));
    panel4.setLayout(new BoxLayout(panel4, BoxLayout.Y_AXIS));
    panel4.setAlignmentX(0.5f); // seta o panel4 no centro
    panel5.setLayout(new BoxLayout(panel5, BoxLayout.X_AXIS));
    panel5.setAlignmentX(0.5f);
    
    panel.add(label1); // add o titulo do frame
    panel.add(Box.createVerticalStrut(12));
    panel2.add(label2); // texto da config 
    panel2.add(campoIntervalo); // campo num. da config
    panel.add(panel5);
    
    this.add(panel);
    panel.add(panel2);
    panel2.add(Box.createVerticalStrut(60));
    panel.add(panel3);
    panel.add(panel4);
    panel.add(panel5);
    
    // panel3 e panel4 agrupa os botoes no frame
    panel3.add(btnIniciar);
    panel3.add(btnPausar);
    panel4.add(Box.createVerticalStrut(16));
    panel4.add(btnTestarAlerta);
    btnTestarAlerta.setAlignmentX(0.5f);
    panel4.add(Box.createVerticalStrut(16));
    
    panel5.add(label3);
    
    
    // função botao INICIAR
    btnIniciar.addActionListener(event ->{
    
    try{
        campoIntervalo.commitEdit();
        int minutos = (Integer)(campoIntervalo.getValue());
        lembreteService.start(minutos);
    } catch(java.text.ParseException e){
        e.getErrorOffset();
        JOptionPane.showMessageDialog(this, "Insira valores de 1 a 240!");
        // volta para o ultimo valor valido
        editor.getTextField().setValue(campoIntervalo.getValue());
        
        return;
    }
        
    label3.setText("Status: timer de " + campoIntervalo.getValue() + " minutos ativado");
    });
    
   btnTestarAlerta.addActionListener(event -> {
      btnTestarAlerta.setEnabled(false);
      PopupLembrete telaPopup = new PopupLembrete();
      // chama um ouvinte de eventos para a janela telaPopup, ligando o botão "testar alerta" novamente depois da janela ser fechada.
      telaPopup.addWindowListener(new WindowAdapter(){
          @Override
          public void windowClosed(WindowEvent e){
              btnTestarAlerta.setEnabled(true);
          }
          });
      
      telaPopup.setVisible(true);
   }); 
  }
    
}
