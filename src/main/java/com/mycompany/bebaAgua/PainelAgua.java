package com.mycompany.bebaAgua;

import javax.swing.*;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.io.IOException;

/** Controles de pausas e preferências, separados do temporizador de lembretes. */
final class PainelAgua extends JPanel {
    private final DadosAgua dados;
    private CompeticaoService competicao;
    private boolean sincronizando;
    private boolean ultimaParticipacao;
    private final JCheckBox iniciarWindows;
    private final Runnable consultarInicio;
    private final JButton participar=new JButton("Participar da competição");
    private final JLabel sincronizacao=new JLabel(" ");
    private DadosAgua.Opcoes opcoes=new DadosAgua.Opcoes(false,false,true);
    private final JButton resumo=new JButton();
    private final PainelHidratacao hidratacao;

    private final JFrame janela;
    private final Runnable mudouTema;
    PainelAgua(JFrame janela, DadosAgua dados, Runnable mudouTema, JCheckBox iniciarWindows, Runnable consultarInicio) {
        this.janela=janela;this.dados=dados;this.mudouTema=mudouTema;
        this.iniciarWindows=iniciarWindows;this.consultarInicio=consultarInicio;
        try {opcoes=dados.carregar();competicao=new CompeticaoService(dados);}catch(IOException e){erro(e);}
        TemaOrvalho.configurar(opcoes);
        setOpaque(false);setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(12,0,0,0));
        BotaoOrvalho.link(resumo);
        resumo.setFont(new Font("Segoe UI",Font.PLAIN,13));
        resumo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        resumo.addActionListener(e->historico());
        resumo.setAlignmentX(0.5f);add(resumo);add(Box.createVerticalStrut(8));
        hidratacao = new PainelHidratacao(() -> {
            PainelPeso.abrir(janela,dados,opcoes.escuro()); atualizar();
        });
        hidratacao.setAlignmentX(0.5f); add(hidratacao);
        JPanel botoes=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));botoes.setOpaque(false);
        JButton configurar=new JButton();
        BotaoOrvalho.aplicar(configurar,false);
        configurar.setName("abrirConfiguracoes");
        configurar.setToolTipText("Configurações");
        configurar.getAccessibleContext().setAccessibleName("Configurações");
        configurar.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        configurar.setPreferredSize(new Dimension(38,38));
        configurar.setIcon(new Icon() {
            public int getIconWidth(){return 20;}
            public int getIconHeight(){return 20;}
            public void paintIcon(Component c,Graphics g,int x,int y){
                Graphics2D d=(Graphics2D)g.create();
                d.translate(x+10,y+10);
                d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                var contorno=new java.awt.geom.Path2D.Double();
                for(int i=0;i<64;i++){
                    double angulo=i*Math.PI/32;
                    double raio=(i%8>=2 && i%8<=5)?9:7;
                    double px=Math.cos(angulo)*raio,py=Math.sin(angulo)*raio;
                    if(i==0)contorno.moveTo(px,py);else contorno.lineTo(px,py);
                }
                contorno.closePath();
                var engrenagem=new java.awt.geom.Area(contorno);
                engrenagem.subtract(new java.awt.geom.Area(new java.awt.geom.Ellipse2D.Double(-3.2,-3.2,6.4,6.4)));
                d.setColor(c.getForeground());d.fill(engrenagem);d.dispose();
            }
        });

        configurar.addActionListener(e->configurar());
        botoes.add(configurar);
        botoes.add(Box.createHorizontalStrut(12));
        BotaoOrvalho.aplicar(participar,false);botoes.add(participar);
        JButton historico=new JButton("Histórico");BotaoOrvalho.aplicar(historico,false);
        historico.setName("abrirHistorico");historico.setToolTipText("Ver dias anteriores e corrigir quantidades registradas.");
        historico.addActionListener(e->historico());botoes.add(Box.createHorizontalStrut(8));botoes.add(historico);
        participar.addActionListener(e->{if(competicao!=null)TelaCompeticao.abrir(janela,competicao,opcoes.escuro(),this::atualizar);});
        participar.setEnabled(competicao!=null);add(botoes);
        sincronizacao.setFont(new Font("Segoe UI",Font.PLAIN,11));sincronizacao.setAlignmentX(0.5f);add(sincronizacao);
        Timer sincronizador=new Timer(300000,e->sincronizar());sincronizador.start();
        janela.addWindowListener(new java.awt.event.WindowAdapter(){@Override public void windowClosed(java.awt.event.WindowEvent e){sincronizador.stop();}});
        sincronizar();
        atualizar();
        // Recalcula o dia ao reabrir a janela e durante a exibição, incluindo a meia-noite.
        Timer timer=new Timer(30000,e->atualizar());
        addHierarchyListener(e->{if((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED)!=0){
            if(isShowing()){atualizar();sincronizar();timer.start();}else timer.stop();
        }});
    }
    DadosAgua.Opcoes opcoes(){return opcoes;}
    int ultimoVolume(){return competicao==null?200:competicao.ultimoVolume();}
    boolean registrar(int ml){
        try{
            if(competicao!=null)competicao.volume(ml);
            dados.registrar(LocalDateTime.now(),ml,competicao!=null&&competicao.participa());
            atualizar();sincronizar();return true;}
        catch(IOException|IllegalArgumentException e){erro(e);return false;}
    }
    private void sincronizar(){
        if(competicao==null||!competicao.participa()||sincronizando)return;
        sincronizando=true;sincronizacao.setText("Sincronizando competição...");
        new SwingWorker<Void,Void>(){
            protected Void doInBackground()throws Exception{competicao.sincronizar();return null;}
            protected void done(){try{
                get();
                if(competicao.participa())sincronizacao.setText(competicao.situacao());
            }catch(Exception e){if(competicao.participa())sincronizacao.setText("Competição: registros pendentes de envio");}
            finally{if(!competicao.participa())sincronizacao.setText(" ");sincronizacao.setToolTipText(competicao.situacao());sincronizando=false;}}
        }.execute();
    }
    private void atualizar(){
        hidratacao.atualizar(dados);
        boolean entrou=competicao!=null&&competicao.participa();
        participar.setText(entrou?"Ver competição":"Participar da competição");
        if(!entrou)sincronizacao.setText(" ");
        else if(!ultimaParticipacao)sincronizar();
        ultimaParticipacao=entrou;

        try {
            var lista=dados.historico(LocalDate.now());

            resumo.setText("Hoje você fez " + lista.size() + (lista.size()==1 ? " pausa" : " pausas") + " para beber água.");
            resumo.setToolTipText("Clique para ver seu histórico, escolher um dia e corrigir registros.");
        }catch(IOException e){resumo.setText("Não foi possível ler as pausas de hoje");resumo.setToolTipText(e.getMessage());}
    }
    private void historico(){
        TelaHistorico.abrir(janela,dados,opcoes.escuro(),()->competicao!=null&&competicao.participa(),()->{atualizar();sincronizar();});
    }
    private void configurar(){
        JPanel form=new JPanel(new GridLayout(0,1,6,10));
        form.setOpaque(false);
        JComboBox<String> tema=new JComboBox<>(new String[]{"Tema original","Tema original escuro","Outubro Rosa"});
        tema.setName("temaAplicativo");tema.setSelectedIndex(opcoes.outubroRosa()?2:opcoes.escuro()?1:0);
        JCheckBox efeitos=new JCheckBox("Efeitos e animações",opcoes.efeitos());efeitos.setName("efeitosConfiguracoes");efeitos.setOpaque(false);efeitos.setToolTipText("Respeita também a preferência de animações do Windows.");
        JCheckBox som=new JCheckBox("Som do alerta (gota de água)",opcoes.som());
        JCheckBox humor=new JCheckBox("Mensagens humoradas",opcoes.humor());
        som.setOpaque(false);humor.setOpaque(false);
        humor.setName("mensagensHumoradas");
        humor.setToolTipText("<html>Exibe frases leves e bem-humoradas nos alertas<br>para incentivar você a beber água.<br>Desmarcar oculta essas frases e mantém os lembretes.</html>");
        iniciarWindows.setName("iniciarComWindows");iniciarWindows.setFont(new Font("Segoe UI",Font.PLAIN,13));
        form.add(new JLabel("Tema"));form.add(tema);form.add(efeitos);form.add(som);form.add(humor);form.add(iniciarWindows);
        JComboBox<String> cidade=new JComboBox<>(LocalClima.OPCOES);cidade.setSelectedIndex(LocalClima.opcao(dados));
        cidade.setToolTipText("<html>Escolha Telêmaco Borba ou São Paulo para a temperatura no alerta.<br>A cidade não aparece no ranking. A previsão é fornecida pelo MET Norway.</html>");
        form.add(new JLabel("Cidade para temperatura no alerta"));form.add(cidade);
        consultarInicio.run();
        JButton hidratacao=new JButton("Estimativa de hidratação por peso");BotaoOrvalho.aplicar(hidratacao,false);form.add(hidratacao);
        hidratacao.addActionListener(e->{PainelPeso.abrir(SwingUtilities.getWindowAncestor(form),dados,opcoes.escuro());atualizar();});
        JButton salvar=new JButton("Salvar");BotaoOrvalho.aplicar(salvar,true);form.add(salvar);
        JPanel raiz=new JPanel(new BorderLayout());raiz.setBackground(new Color(0xF4F8FC));raiz.setBorder(BorderFactory.createEmptyBorder(24,24,24,24));raiz.add(form,BorderLayout.NORTH);TemaOrvalho.aplicar(raiz,opcoes.escuro());
        JDialog dialogo=new JDialog(janela,"Configurações",true);dialogo.setContentPane(raiz);dialogo.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        salvar.addActionListener(e->{
            try {

                var novas=new DadosAgua.Opcoes(tema.getSelectedIndex()==1,som.isSelected(),humor.isSelected(),tema.getSelectedIndex()==2,efeitos.isSelected());
                LocalClima.salvar(dados,cidade.getSelectedIndex());dados.salvar(novas);opcoes=novas;TemaOrvalho.configurar(novas);atualizar();mudouTema.run();dialogo.dispose();
            }catch(Exception ex){DialogosOrvalho.showMessageDialog(janela,"Não foi possível salvar: "+ex.getMessage());}
        });
        dialogo.pack();dialogo.setLocationRelativeTo(janela);dialogo.setVisible(true);
    }
    private void erro(Exception e){DialogosOrvalho.showMessageDialog(janela,"Não foi possível concluir: "+e.getMessage(),"Orvalho",JOptionPane.ERROR_MESSAGE);}
}
