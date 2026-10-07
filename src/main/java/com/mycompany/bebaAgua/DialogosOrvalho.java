package com.mycompany.bebaAgua;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Mensagens modais com o mesmo tema e controles do aplicativo. */
final class DialogosOrvalho {
    private DialogosOrvalho() { }
    static void showMessageDialog(Component dono, Object mensagem) {
        showMessageDialog(dono,mensagem,"Orvalho",JOptionPane.ERROR_MESSAGE);
    }
    static void showMessageDialog(Component dono,Object mensagem,String titulo,int tipo) {
        exibir(dono,mensagem,titulo,tipo,false);
    }
    static int showConfirmDialog(Component dono,Object mensagem,String titulo,int opcoes) {
        return exibir(dono,mensagem,titulo,JOptionPane.QUESTION_MESSAGE,true);
    }
    static boolean escuro(Component origem) {
        for(Component c=origem;c!=null;c=c instanceof Window w?w.getOwner():c.getParent()) {
            if(c instanceof JComponent jc && jc.getClientProperty("orvalho.escuro") instanceof Boolean tema)return tema;
            if(c instanceof RootPaneContainer raiz && raiz.getContentPane() instanceof JComponent jc
                    && jc.getClientProperty("orvalho.escuro") instanceof Boolean tema)return tema;
        }
        try{return new DadosAgua().carregar().escuro();}catch(Exception ignored){return false;}
    }
    static JDialog criar(Component origem,Object mensagem,String titulo,int tipo,boolean confirmar,int[] resultado) {
        Window dono=origem instanceof Window w?w:origem==null?null:SwingUtilities.getWindowAncestor(origem);
        JDialog dialogo=new JDialog(dono,titulo,Dialog.ModalityType.APPLICATION_MODAL);
        dialogo.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        if(dono!=null)dialogo.setIconImages(dono.getIconImages());
        boolean escuro=escuro(origem);
        JPanel painel=new JPanel(new BorderLayout(0,12));painel.setBackground(new Color(0xF4F8FC));
        painel.setBorder(BorderFactory.createEmptyBorder(16,16,14,16));
        JPanel cabecalho=new JPanel(new BorderLayout(10,0));cabecalho.setOpaque(false);
        JLabel simbolo=new JLabel(new Icon(){
            public int getIconWidth(){return 36;}public int getIconHeight(){return 36;}
            public void paintIcon(Component c,Graphics g,int x,int y){
                Graphics2D d=(Graphics2D)g.create();d.translate(x,y);d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                d.setColor(TemaOrvalho.cor(new Color(escuro?0x203D52:0xE7F2FF)));d.fillOval(0,0,35,35);
                d.setColor(TemaOrvalho.cor(new Color(escuro?0x82B5F5:0x176BDA)));d.setFont(new Font("Segoe UI",Font.BOLD,22));
                String s=confirmar?"?":tipo==JOptionPane.INFORMATION_MESSAGE?"i":"!";TipografiaOrvalho.suavizar(d);d.drawString(s,(36-d.getFontMetrics().stringWidth(s))/2,26);d.dispose();
            }
        });cabecalho.add(simbolo,BorderLayout.WEST);
        JLabel heading=new JLabel(titulo);heading.putClientProperty("html.disable",true);heading.setFont(new Font("Segoe UI",Font.BOLD,18));cabecalho.add(heading);painel.add(cabecalho,BorderLayout.NORTH);
        String conteudo=mensagem==null?"Não foi possível concluir a operação. Tente novamente.":mensagem.toString();
        JTextArea texto=new JTextArea(conteudo);texto.setName("mensagemOrvalho");texto.setEditable(false);texto.setLineWrap(true);texto.setWrapStyleWord(true);
        texto.setFont(new Font("Segoe UI",Font.PLAIN,14));texto.setOpaque(false);texto.setBorder(BorderFactory.createEmptyBorder(2,0,2,4));
        texto.getAccessibleContext().setAccessibleName("Mensagem");
        Rectangle area=dialogo.getGraphicsConfiguration().getBounds();Insets margens=Toolkit.getDefaultToolkit().getScreenInsets(dialogo.getGraphicsConfiguration());
        FontMetrics medidas=texto.getFontMetrics(texto.getFont());
        int linhaMaisLonga=conteudo.lines().mapToInt(medidas::stringWidth).max().orElse(0);
        int larguraDesejada=Math.max(confirmar?350:300,Math.max(linhaMaisLonga+44,cabecalho.getPreferredSize().width+32));
        int largura=Math.min(Math.min(420,larguraDesejada),Math.max(240,area.width-margens.left-margens.right-32));
        texto.setSize(largura-52,Short.MAX_VALUE);
        int altura=Math.min(texto.getPreferredSize().height+4,Math.max(60,Math.min(240,area.height-margens.top-margens.bottom-190)));
        JScrollPane scroll=new JScrollPane(texto);scroll.setBorder(BorderFactory.createEmptyBorder());scroll.setOpaque(false);scroll.getViewport().setOpaque(false);scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);scroll.setPreferredSize(new Dimension(largura-32,altura));painel.add(scroll);
        JPanel botoes=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));botoes.setOpaque(false);
        JButton cancelar=new JButton("Cancelar");
        if(confirmar){BotaoOrvalho.aplicar(cancelar,false);cancelar.setBorder(BorderFactory.createEmptyBorder(8,16,8,16));cancelar.addActionListener(e->{resultado[0]=JOptionPane.NO_OPTION;dialogo.dispose();});botoes.add(cancelar);}
        JButton ok=new JButton(confirmar?"Confirmar":"OK");BotaoOrvalho.aplicar(ok,true);
        ok.setBorder(BorderFactory.createEmptyBorder(8,16,8,16));
        ok.addActionListener(e->{resultado[0]=JOptionPane.YES_OPTION;dialogo.dispose();});botoes.add(ok);painel.add(botoes,BorderLayout.SOUTH);
        TemaOrvalho.aplicar(painel,escuro);dialogo.setContentPane(painel);dialogo.getRootPane().setDefaultButton(confirmar?cancelar:ok);
        dialogo.getRootPane().registerKeyboardAction(e->dialogo.dispose(),KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialogo.pack();dialogo.setMinimumSize(dialogo.getSize());dialogo.setLocationRelativeTo(origem);
        dialogo.addWindowListener(new WindowAdapter(){@Override public void windowOpened(WindowEvent e){(confirmar?cancelar:ok).requestFocusInWindow();}});
        return dialogo;
    }
    private static int exibir(Component dono,Object mensagem,String titulo,int tipo,boolean confirmar) {
        int[] resultado={JOptionPane.CLOSED_OPTION};
        Runnable abrir=()->{JDialog dialogo=criar(dono,mensagem,titulo,tipo,confirmar,resultado);try{dialogo.setVisible(true);}finally{dialogo.dispose();}};
        if(SwingUtilities.isEventDispatchThread())abrir.run();else try{SwingUtilities.invokeAndWait(abrir);}catch(Exception e){throw new IllegalStateException("Não foi possível exibir a mensagem.",e);}
        return resultado[0];
    }
}
