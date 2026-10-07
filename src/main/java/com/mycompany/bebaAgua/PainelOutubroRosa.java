package com.mycompany.bebaAgua;

import java.awt.*;
import java.net.URI;
import javax.swing.*;
import javax.swing.text.DefaultCaret;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;

/** Conteúdo educativo local; a internet só é aberta ao escolher a fonte oficial. */
final class PainelOutubroRosa extends JPanel {
    static final String FONTE="https://www.gov.br/inca/pt-br/assuntos/cancer/tipos/mama/versao-para-populacao";
    PainelOutubroRosa(boolean escuro,Runnable fechar){
        setLayout(new BorderLayout(0,20));setBackground(new Color(0xF4F8FC));
        setBorder(BorderFactory.createEmptyBorder(24,24,20,24));
        JPanel topo=vertical();
        topo.add(titulo("Outubro Rosa",28));topo.add(Box.createVerticalStrut(8));
        topo.add(texto("Informação, acolhimento e cuidado o ano inteiro.",13));add(topo,BorderLayout.NORTH);
        PainelRolavel conteudo=new PainelRolavel();conteudo.setOpaque(false);conteudo.setLayout(new BoxLayout(conteudo,BoxLayout.Y_AXIS));
        conteudo.setBorder(BorderFactory.createEmptyBorder(0,0,0,8));
        conteudo.add(cartao(null,"Uma campanha pela saúde",
                texto("Outubro Rosa promove a conscientização sobre o câncer de mama, a redução de riscos e a detecção precoce. Também é um convite ao acolhimento de quem enfrenta a doença.",14)));
        conteudo.add(Box.createVerticalStrut(16));
        conteudo.add(cartao("01","Conheça suas mamas",
                texto("Observe mudanças no dia a dia, sem uma técnica obrigatória de autoexame.",14),
                lista("Sinais que merecem avaliação",
                        "Caroço na mama ou axila.",
                        "Pele retraída ou parecida com casca de laranja.",
                        "Mudança no mamilo ou saída espontânea de líquido."),
                orientacao("Ao notar uma alteração",
                        "Procure uma UBS ou profissional de saúde, mesmo sem dor. Não espere outubro ou o próximo exame. Uma alteração não significa necessariamente câncer.")));
        conteudo.add(Box.createVerticalStrut(16));
        conteudo.add(cartao("02","Cuidados que ajudam a reduzir o risco",
                lista(null,"Pratique atividade física, procure manter um peso saudável e evite bebidas alcoólicas.",
                        "Amamentar, quando possível, também é um fator de proteção."),
                texto("Esses cuidados reduzem riscos, mas não garantem prevenção.",14)));
        conteudo.add(Box.createVerticalStrut(16));
        conteudo.add(cartao("03","Converse sobre os exames",
                texto("A mamografia pode identificar alterações antes dos sintomas.",14),
                texto("Converse com a equipe de saúde sobre quando fazê-la, considerando sua idade e histórico familiar.",14),
                orientacao("Quando indicada",
                        "Observar ou palpar as mamas não substitui a mamografia quando indicada.")));
        JScrollPane scroll=new JScrollPane(conteudo);scroll.setBorder(BorderFactory.createEmptyBorder());
        TemaOrvalho.rolagem(scroll);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);scroll.getVerticalScrollBar().setUnitIncrement(24);add(scroll);
        JPanel rodape=vertical();rodape.add(texto("Informação educativa. Não substitui avaliação de saúde.",11));
        rodape.add(Box.createVerticalStrut(4));
        rodape.add(texto("Fonte: INCA · Consultada em 06/10/2026",11));rodape.add(Box.createVerticalStrut(12));
        JPanel acoes=new JPanel(new GridLayout(1,2,12,0));acoes.setOpaque(false);acoes.setAlignmentX(LEFT_ALIGNMENT);
        JButton fonte=new JButton("Saiba mais no INCA"),ok=new JButton("Entendi");
        BotaoOrvalho.aplicar(fonte,false);BotaoOrvalho.aplicar(ok,true);ok.setName("fecharOutubroRosa");
        fonte.setToolTipText("Abrir a página oficial do INCA no navegador");fonte.addActionListener(e->abrirFonte(fonte));
        ok.addActionListener(e->fechar.run());acoes.add(fonte);acoes.add(ok);rodape.add(acoes);add(rodape,BorderLayout.SOUTH);
        TemaOrvalho.aplicar(this,escuro);
    }
    private static JPanel vertical(){JPanel p=new JPanel();p.setOpaque(false);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));return p;}
    private static JLabel titulo(String s,int tamanho){JLabel l=new JLabel(s);l.setFont(new Font("Segoe UI",Font.BOLD,tamanho));l.setForeground(new Color(0x12304A));l.setAlignmentX(LEFT_ALIGNMENT);return l;}
    private static JTextPane texto(String s,int tamanho){
        JTextPane a=new JTextPane(){
            @Override public void setBounds(int x,int y,int largura,int altura){
                boolean mudouLargura=largura!=getWidth();super.setBounds(x,y,largura,altura);
                if(mudouLargura)SwingUtilities.invokeLater(()->{
                    // Recalcula as quebras de linha depois que os cartões ajustam a largura.
                    for(Container pai=getParent();pai!=null&&!(pai instanceof Window);pai=pai.getParent())pai.invalidate();
                    revalidate();
                });
            }
            @Override public Dimension getPreferredSize(){
                Container pai=getParent();Insets margem=pai==null?new Insets(0,0,0,0):pai.getInsets();
                int largura=pai==null||pai.getWidth()==0?480:Math.max(100,pai.getWidth()-margem.left-margem.right);
                Object recuo=getClientProperty("orvalho.recuoTexto");if(recuo instanceof Integer n)largura=Math.max(100,largura-n);
                var view=getUI().getRootView(this);view.setSize(largura,Float.MAX_VALUE);
                return new Dimension(largura,(int)Math.ceil(view.getPreferredSpan(View.Y_AXIS))+2);
            }
            @Override public Dimension getMinimumSize(){return new Dimension(0,getPreferredSize().height);}
            @Override public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);}
        };
        ((DefaultCaret)a.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);a.getCaret().setVisible(false);
        a.setText(s);a.setEditable(false);a.setFocusable(false);a.setOpaque(false);a.setBorder(BorderFactory.createEmptyBorder());a.setMargin(new Insets(0,0,0,0));
        a.setFont(new Font("Segoe UI",Font.PLAIN,tamanho));a.setForeground(new Color(0x52748E));a.setAlignmentX(LEFT_ALIGNMENT);
        SimpleAttributeSet paragrafo=new SimpleAttributeSet();StyleConstants.setLineSpacing(paragrafo,0.16f);
        a.getStyledDocument().setParagraphAttributes(0,a.getDocument().getLength(),paragrafo,false);return a;
    }
    private static JPanel superficie(Color fundo,int margem){
        JPanel p=new JPanel(){
            @Override public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);}
            @Override protected void paintComponent(Graphics g){
                Graphics2D d=(Graphics2D)g.create();d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                d.setColor(getBackground());d.fillRoundRect(0,0,getWidth()-1,getHeight()-1,20,20);
                boolean escuro=Boolean.TRUE.equals(getClientProperty("orvalho.escuro"));
                d.setColor(TemaOrvalho.cor(new Color(escuro?0x36556B:0xD5E4EF)));d.drawRoundRect(0,0,getWidth()-1,getHeight()-1,20,20);d.dispose();
            }
        };
        p.setOpaque(false);p.setBackground(fundo);p.setBorder(BorderFactory.createEmptyBorder(margem,margem,margem,margem));p.setAlignmentX(LEFT_ALIGNMENT);return p;
    }
    private static JPanel cartao(String numero,String cabecalho,JComponent... blocos){
        JPanel p=superficie(Color.WHITE,20);p.setLayout(new BorderLayout(0,16));
        JPanel topo=new JPanel(new BorderLayout(12,0));topo.setOpaque(false);
        JTextPane titulo=texto(cabecalho,15);titulo.setFont(new Font("Segoe UI",Font.BOLD,15));titulo.setForeground(new Color(0x12304A));
        if(numero!=null){
            JLabel marcador=new JLabel(numero,SwingConstants.CENTER){
                @Override protected void paintComponent(Graphics g){
                    Graphics2D d=(Graphics2D)g.create();d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    d.setColor(getBackground());d.fillRoundRect(0,0,getWidth(),getHeight(),12,12);d.dispose();super.paintComponent(g);
                }
            };
            marcador.setFont(new Font("Segoe UI",Font.BOLD,13));marcador.setForeground(new Color(0x176BDA));marcador.setBackground(new Color(0xE8F3FF));
            marcador.setPreferredSize(new Dimension(36,36));
            JPanel icone=new JPanel(new BorderLayout());icone.setOpaque(false);icone.add(marcador,BorderLayout.NORTH);topo.add(icone,BorderLayout.WEST);
        }
        JPanel tituloCentro=new JPanel(new GridBagLayout());tituloCentro.setOpaque(false);
        GridBagConstraints gc=new GridBagConstraints();gc.weightx=1;gc.fill=GridBagConstraints.HORIZONTAL;tituloCentro.add(titulo,gc);
        topo.add(tituloCentro);p.add(topo,BorderLayout.NORTH);
        JPanel corpo=vertical();for(int i=0;i<blocos.length;i++){if(i>0)corpo.add(Box.createVerticalStrut(12));corpo.add(blocos[i]);}
        p.add(corpo);return p;
    }
    private static JPanel lista(String cabecalho,String... itens){
        JPanel p=vertical();p.setAlignmentX(LEFT_ALIGNMENT);
        if(cabecalho!=null){p.add(titulo(cabecalho,13));p.add(Box.createVerticalStrut(8));}
        for(int i=0;i<itens.length;i++){
            if(i>0)p.add(Box.createVerticalStrut(8));
            JPanel linha=new JPanel(new BorderLayout(8,0));linha.setOpaque(false);linha.setAlignmentX(LEFT_ALIGNMENT);
            JLabel ponto=new JLabel("•");ponto.setForeground(new Color(0x176BDA));ponto.setFont(new Font("Segoe UI",Font.BOLD,14));ponto.setVerticalAlignment(SwingConstants.TOP);ponto.setPreferredSize(new Dimension(10,20));
            JTextPane item=texto(itens[i],14);item.putClientProperty("orvalho.recuoTexto",18);linha.add(ponto,BorderLayout.WEST);linha.add(item);p.add(linha);
        }return p;
    }
    private static JPanel orientacao(String cabecalho,String corpo){
        JPanel p=superficie(new Color(0xE8F3FF),14);p.setLayout(new BorderLayout(0,8));
        JLabel titulo=titulo(cabecalho,13);titulo.setForeground(new Color(0x176BDA));p.add(titulo,BorderLayout.NORTH);
        JTextPane mensagem=texto(corpo,13);mensagem.setForeground(new Color(0x12304A));p.add(mensagem);return p;
    }
    static void abrir(Component origem){
        Window dono=SwingUtilities.getWindowAncestor(origem);
        if(dono!=null)for(Window filha:dono.getOwnedWindows())if(filha instanceof JDialog d&&"informacaoOutubroRosa".equals(d.getName())&&d.isDisplayable()){d.setVisible(true);d.toFront();return;}
        JDialog d=new JDialog(dono,"Orvalho — Outubro Rosa",Dialog.ModalityType.MODELESS);d.setName("informacaoOutubroRosa");
        d.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        boolean escuro=origem instanceof JComponent c&&Boolean.TRUE.equals(c.getClientProperty("orvalho.escuro"));
        d.setContentPane(new PainelOutubroRosa(escuro,d::dispose));d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        d.getRootPane().registerKeyboardAction(e->d.dispose(),KeyStroke.getKeyStroke("ESCAPE"),JComponent.WHEN_IN_FOCUSED_WINDOW);
        PainelPeso.dimensionar(d,600,740);d.setMinimumSize(new Dimension(420,400));d.setLocationRelativeTo(dono);d.setVisible(true);
    }
    private static void abrirFonte(JButton botao){
        botao.setEnabled(false);
        new SwingWorker<Void,Void>(){
            protected Void doInBackground()throws Exception{
                if(!Desktop.isDesktopSupported()||!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))throw new IllegalStateException();
                Desktop.getDesktop().browse(URI.create(FONTE));return null;
            }
            protected void done(){botao.setEnabled(true);try{get();}catch(Exception e){DialogosOrvalho.showMessageDialog(botao,"Não foi possível abrir o navegador. Acesse gov.br/inca e procure por câncer de mama.");}}
        }.execute();
    }
}
