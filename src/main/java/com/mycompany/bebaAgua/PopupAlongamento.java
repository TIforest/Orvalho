package com.mycompany.bebaAgua;

import java.awt.*;
import javax.swing.*;

/** Convite com identidade âmbar e contagem iniciada pelo usuário. */
final class PopupAlongamento extends JDialog {
    private final java.util.function.LongSupplier relogio;
    private final Timer timer;
    private final JLabel contagem=new JLabel("02:00",SwingConstants.CENTER);
    private final JLabel situacao=new JLabel("Clique em Iniciar quando estiver pronto.",SwingConstants.CENTER);
    private final JButton iniciar=new JButton("Iniciar");
    private boolean iniciado;
    private long prazo;
    PopupAlongamento(boolean escuro, boolean som, boolean teste) {
        this(escuro,som,teste,System::nanoTime);
    }
    PopupAlongamento(boolean escuro, boolean som, boolean teste,java.util.function.LongSupplier relogio) {
        // Sem vínculo com o frame: minimizar a janela principal não deve esconder o aviso.
        super((Window)null,"Orvalho — Alongamento",ModalityType.MODELESS);
        this.relogio=relogio;
        timer=new Timer(200,e->atualizarTempo());
        setName("alertaAlongamento");
        setIconImage(new ImageIcon(getClass().getResource("/imagens/agua.png")).getImage());
        setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
        setAlwaysOnTop(true);setAutoRequestFocus(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);setResizable(false);
        Color fundo=new Color(escuro?0x142536:0xFFF9EF),texto=new Color(escuro?0xFFF1D9:0x493019);
        Color destaque=new Color(escuro?0xFFBE63:0xA85209);
        JPanel raiz=new JPanel();raiz.setLayout(new BoxLayout(raiz,BoxLayout.Y_AXIS));raiz.setBackground(fundo);
        raiz.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(7,0,0,0,new Color(0xE8922B)),BorderFactory.createEmptyBorder(25,28,25,28)));
        JLabel icone=new JLabel(new Icon(){
            public int getIconWidth(){return 78;}public int getIconHeight(){return 78;}
            public void paintIcon(Component c,Graphics graphics,int x,int y){
                Graphics2D g=(Graphics2D)graphics.create();g.translate(x,y);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(escuro?0x493621:0xFFE9C6));g.fillRoundRect(0,0,78,78,26,26);
                g.setColor(destaque);g.setStroke(new BasicStroke(4.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                g.fillOval(33,14,12,12);g.drawLine(39,32,39,49);
                g.drawPolyline(new int[]{17,28,39,50,61},new int[]{22,34,32,24,14},5);
                g.drawLine(39,49,26,65);g.drawLine(39,49,53,65);g.dispose();
            }
        });adicionar(raiz,icone,14);
        adicionar(raiz,label("PAUSA EM MOVIMENTO",12,true,destaque),8);
        adicionar(raiz,label("Hora do alongamento!",25,true,texto),12);
        adicionar(raiz,label("Vamos fazer uma pausa de 2 minutos?",15,false,texto),8);
        JLabel mensagem=label("<html><div style='width:280px;text-align:center'>Faça os alongamentos no seu próprio<br>ambiente de trabalho.</div></html>",15,false,texto);
        adicionar(raiz,mensagem,18);
        contagem.setName("contagemAlongamento");contagem.setForeground(destaque);contagem.setFont(new Font("Segoe UI",Font.BOLD,36));
        adicionar(raiz,contagem,4);
        situacao.setName("situacaoAlongamento");situacao.setForeground(texto);situacao.setFont(new Font("Segoe UI",Font.PLAIN,12));
        adicionar(raiz,situacao,18);
        iniciar.setName("iniciarAlongamento");BotaoOrvalho.aplicar(iniciar,true);
        iniciar.setBackground(new Color(0xA85209));iniciar.setForeground(Color.WHITE);
        iniciar.addActionListener(e->{
            if(iniciado)return;
            iniciado=true;prazo=relogio.getAsLong()+120_000_000_000L;
            iniciar.setEnabled(false);iniciar.setText("Em andamento");situacao.setText("Seu tempo de alongamento começou.");
            timer.start();atualizarTempo();
        });
        JButton ok=new JButton("OK");ok.setName("confirmarAlongamento");BotaoOrvalho.aplicar(ok,false);
        ok.setBackground(fundo);ok.setForeground(destaque);ok.addActionListener(e->dispose());
        JPanel acoes=new JPanel(new FlowLayout(FlowLayout.CENTER,12,0));acoes.setOpaque(false);acoes.add(iniciar);acoes.add(ok);
        adicionar(raiz,acoes,0);getRootPane().setDefaultButton(ok);
        if(teste){raiz.add(Box.createVerticalStrut(12));adicionar(raiz,label("Prévia do aviso de alongamento",11,false,destaque),0);}
        setContentPane(raiz);pack();setLocationRelativeTo(null);
        if(som)addWindowListener(new java.awt.event.WindowAdapter(){
            @Override public void windowOpened(java.awt.event.WindowEvent e){SomAlerta.tocarAlongamento();}
        });
    }
    void atualizarTempo(){
        if(!iniciado)return;
        long segundos=Math.max(0,(prazo-relogio.getAsLong()+999_999_999L)/1_000_000_000L);
        contagem.setText(String.format("%02d:%02d",segundos/60,segundos%60));
        if(segundos==0){timer.stop();iniciar.setText("Concluído");situacao.setText("Alongamento concluído! Bom trabalho.");}
    }
    @Override public void dispose(){if(timer!=null)timer.stop();super.dispose();}
    private static JLabel label(String texto,int tamanho,boolean negrito,Color cor){
        JLabel l=new JLabel(texto,SwingConstants.CENTER);l.setFont(new Font("Segoe UI",negrito?Font.BOLD:Font.PLAIN,tamanho));l.setForeground(cor);return l;
    }
    private static void adicionar(JPanel p,JComponent c,int espaco){c.setAlignmentX(.5f);p.add(c);if(espaco>0)p.add(Box.createVerticalStrut(espaco));}
}
