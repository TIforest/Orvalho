package com.mycompany.bebaAgua;

import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import javax.swing.*;

/** Seletor de data com o mesmo tema, botões e espaçamento do aplicativo. */
final class PainelDataRanking extends JPanel {
    private final JTextField campo = new JTextField(12);
    private final JLabel erro = new JLabel(" ");
    private final JButton confirmar = new JButton("Ver ranking");
    PainelDataRanking(LocalDate inicial,boolean escuro,Consumer<LocalDate> selecionar,Runnable cancelar){
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));setBackground(new Color(0xF4F8FC));
        setBorder(BorderFactory.createEmptyBorder(24,24,20,24));
        add(rotulo("Escolher dia",22,true));add(Box.createVerticalStrut(8));
        add(rotulo("Consulte os registros de uma data anterior.",12,false));add(Box.createVerticalStrut(24));
        JLabel nome=rotulo("Data do ranking",13,true);nome.setLabelFor(campo);add(nome);add(Box.createVerticalStrut(8));
        campo.setName("dataRanking");campo.setText(inicial.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        campo.setFont(new Font("Segoe UI",Font.PLAIN,18));campo.setToolTipText("Dia, mês e ano. Exemplo: 02/10/2026.");
        campo.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(escuro?0x547089:0xBCD1E3)),BorderFactory.createEmptyBorder(10,12,10,12)));
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));add(campo);add(Box.createVerticalStrut(8));
        add(rotulo("dd/mm/aaaa · Hoje ou um dia anterior",11,false));add(Box.createVerticalStrut(12));
        erro.setFont(new Font("Segoe UI",Font.PLAIN,11));erro.setPreferredSize(new Dimension(360,32));erro.setMaximumSize(new Dimension(Integer.MAX_VALUE,32));add(erro);add(Box.createVerticalStrut(12));
        JPanel acoes=new JPanel(new GridLayout(1,2,12,0));acoes.setOpaque(false);
        JButton hoje=new JButton("Hoje");BotaoOrvalho.aplicar(confirmar,true);BotaoOrvalho.aplicar(hoje,false);acoes.add(confirmar);acoes.add(hoje);add(acoes);add(Box.createVerticalStrut(8));
        JButton voltar=new JButton("Cancelar");BotaoOrvalho.link(voltar);
        JPanel rodape=new JPanel(new FlowLayout(FlowLayout.CENTER,0,0));rodape.setOpaque(false);rodape.add(voltar);add(rodape);
        confirmar.addActionListener(e->{
            try{selecionar.accept(PainelRanking.validarDia(campo.getText(),LocalDate.now(ZoneId.of("America/Sao_Paulo"))));}
            catch(IllegalArgumentException ex){erro.setText("<html>Informe uma data válida entre 01/01/2026 e hoje.</html>");campo.requestFocusInWindow();campo.selectAll();}
        });
        hoje.addActionListener(e->selecionar.accept(LocalDate.now(ZoneId.of("America/Sao_Paulo"))));voltar.addActionListener(e->cancelar.run());campo.addActionListener(e->confirmar.doClick());
        for(Component c:getComponents())if(c instanceof JComponent jc)jc.setAlignmentX(LEFT_ALIGNMENT);
        TemaOrvalho.aplicar(this,escuro);erro.setForeground(new Color(escuro?0xFFB2A8:0xAA3024));
    }
    private static JLabel rotulo(String texto,int tamanho,boolean negrito){JLabel l=new JLabel(texto);l.setFont(new Font("Segoe UI",negrito?Font.BOLD:Font.PLAIN,tamanho));return l;}
    static LocalDate escolher(Component dono,LocalDate inicial,boolean escuro){
        LocalDate[] escolhido={null};JDialog d=new JDialog(SwingUtilities.getWindowAncestor(dono),"Orvalho — Data do ranking",Dialog.ModalityType.APPLICATION_MODAL);
        PainelDataRanking painel=new PainelDataRanking(inicial,escuro,dia->{escolhido[0]=dia;d.dispose();},d::dispose);
        d.setContentPane(painel);d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);d.getRootPane().setDefaultButton(painel.confirmar);
        d.getRootPane().registerKeyboardAction(e->d.dispose(),KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE,0),JComponent.WHEN_IN_FOCUSED_WINDOW);
        d.pack();d.setSize(Math.max(420,d.getWidth()),d.getHeight());d.setResizable(false);d.setLocationRelativeTo(dono);
        SwingUtilities.invokeLater(()->{painel.campo.requestFocusInWindow();painel.campo.selectAll();});d.setVisible(true);return escolhido[0];
    }
}
