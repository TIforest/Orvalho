package com.mycompany.bebaAgua;

import java.awt.*;
import java.time.*;
import javax.swing.*;

/** Um ponto por dia atingido, sem premiar volumes excedentes. */
final class PainelConquistas extends JPanel {
    private final CompeticaoService servico;
    private final boolean escuro;
    private final JComboBox<YearMonth> meses = new JComboBox<>();
    private final JCheckBox compartilhar = new JCheckBox("Compartilhar minhas conquistas");
    private final JToggleButton hoje = new JToggleButton("Quem conseguiu hoje"), mensal = new JToggleButton("Histórico mensal");
    private final JButton atualizar = new JButton("Atualizar");
    private final JLabel resumo = new JLabel("Consultando conquistas…");
    private final JLabel detalhe = new JLabel("Cada dia conta uma vez.");
    private final JLabel periodo = new JLabel("Conquistas do dia");
    private final JTextArea estado = new JTextArea(" ",1,40);
    private final DefaultListModel<CompeticaoService.Conquista> modelo = new DefaultListModel<>();
    private final JList<CompeticaoService.Conquista> lista = new JList<>(modelo);
    private boolean ocupado, ajustandoMeses;
    PainelConquistas(CompeticaoService servico,boolean escuro){
        this.servico=servico;this.escuro=escuro;
        setLayout(new BorderLayout(0,16));setBackground(TemaOrvalho.cor(new Color(0xF4F8FC)));setBorder(BorderFactory.createEmptyBorder(24,24,20,24));
        JPanel topo=vertical();topo.add(label("Conquistas",26,true));topo.add(Box.createVerticalStrut(6));
        topo.add(label("Um dia de cada vez. Cada dia conta uma vez.",12,false));topo.add(Box.createVerticalStrut(16));
        JPanel abas=new JPanel(new GridLayout(1,2,12,0));abas.setOpaque(false);abas.setAlignmentX(LEFT_ALIGNMENT);
        ButtonGroup grupo=new ButtonGroup();grupo.add(hoje);grupo.add(mensal);mensal.setSelected(true);
        // Os controles de seleção usam a mesma pintura dos botões do Orvalho.
        for(JToggleButton b:new JToggleButton[]{hoje,mensal}){
            b.setFont(new Font("Segoe UI",Font.BOLD,12));b.setOpaque(false);b.setContentAreaFilled(false);b.setBorderPainted(false);b.setBorder(BorderFactory.createEmptyBorder(10,12,10,12));
            b.setUI(new javax.swing.plaf.basic.BasicToggleButtonUI(){
                @Override public void paint(Graphics g,JComponent c){Graphics2D d=(Graphics2D)g.create();d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    d.setColor(TemaOrvalho.cor(new Color(b.isSelected()?0x176BDA:(escuro?0x203D52:0xE7F2FF))));d.fillRoundRect(0,0,c.getWidth()-1,c.getHeight()-1,24,24);
                    if(b.hasFocus()){d.setColor(TemaOrvalho.cor(new Color(0x82B5F5)));d.drawRoundRect(3,3,c.getWidth()-7,c.getHeight()-7,20,20);}d.dispose();super.paint(g,c);}
                @Override protected void paintButtonPressed(Graphics g,AbstractButton b){}
                @Override protected void paintText(Graphics g,AbstractButton b,Rectangle r,String s){g.setColor(TemaOrvalho.cor(b.isSelected()?Color.WHITE:new Color(escuro?0xE7F0F8:0x12304A)));TipografiaOrvalho.suavizar(g);g.drawString(s,r.x,r.y+g.getFontMetrics().getAscent());}
            });
        }
        abas.add(hoje);abas.add(mensal);topo.add(abas);topo.add(Box.createVerticalStrut(12));
        atualizarMeses();

        meses.setRenderer(new DefaultListCellRenderer(){@Override public Component getListCellRendererComponent(JList<?> l,Object v,int i,boolean s,boolean f){super.getListCellRendererComponent(l,v,i,s,f);if(v instanceof YearMonth m)setText(MesesConquistas.nome(m));return this;}});
        meses.setFont(new Font("Segoe UI",Font.PLAIN,13));
        meses.setBorder(BorderFactory.createEmptyBorder(4,12,4,8));
        meses.setUI(new javax.swing.plaf.basic.BasicComboBoxUI(){
            @Override protected JButton createArrowButton(){JButton b=new JButton(icone(false));BotaoOrvalho.link(b);b.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));return b;}
        });
        JPanel filtro=new JPanel(new BorderLayout(12,0));filtro.setOpaque(false);filtro.setAlignmentX(LEFT_ALIGNMENT);
        JPanel escolha=cartao(false);escolha.setLayout(new BorderLayout());escolha.setBorder(BorderFactory.createEmptyBorder(2,4,2,4));escolha.add(meses);filtro.add(escolha,BorderLayout.CENTER);
        periodo.setFont(new Font("Segoe UI",Font.BOLD,13));periodo.setBorder(BorderFactory.createEmptyBorder(0,12,0,0));
        escolha.add(periodo,BorderLayout.WEST);periodo.setVisible(false);
        BotaoOrvalho.aplicar(atualizar,false);filtro.add(atualizar,BorderLayout.EAST);topo.add(filtro);topo.add(Box.createVerticalStrut(12));
        JPanel destaque=cartao(true);destaque.setLayout(new BorderLayout(12,0));destaque.setBorder(BorderFactory.createEmptyBorder(14,16,14,16));destaque.setAlignmentX(LEFT_ALIGNMENT);
        JLabel simbolo=new JLabel(icone(true));destaque.add(simbolo,BorderLayout.WEST);JPanel textos=vertical();
        resumo.setFont(new Font("Segoe UI",Font.BOLD,19));resumo.setAlignmentX(LEFT_ALIGNMENT);textos.add(resumo);textos.add(Box.createVerticalStrut(4));
        detalhe.setFont(new Font("Segoe UI",Font.PLAIN,12));detalhe.setAlignmentX(LEFT_ALIGNMENT);textos.add(detalhe);destaque.add(textos);topo.add(destaque);add(topo,BorderLayout.NORTH);
        lista.setFixedCellHeight(86);lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setCellRenderer((l,p,i,s,f)->linha(p,s));lista.getAccessibleContext().setAccessibleName("Participantes que atingiram a referência diária");
        lista.addListSelectionListener(e->{if(!e.getValueIsAdjusting()&&lista.getSelectedValue()!=null){var p=lista.getSelectedValue();estado.setText(hoje.isSelected()?"Meta diária atingida hoje.":"Dias com meta atingida no mês: "+p.dias().size());estado.setToolTipText(estado.getText());}});
        JScrollPane scroll=new JScrollPane(lista);scroll.setBorder(BorderFactory.createEmptyBorder());scroll.getVerticalScrollBar().setUnitIncrement(43);add(scroll,BorderLayout.CENTER);
        JPanel rodape=vertical();estado.setEditable(false);estado.setOpaque(false);estado.setLineWrap(true);estado.setWrapStyleWord(true);estado.setAlignmentX(LEFT_ALIGNMENT);estado.setFont(new Font("Segoe UI",Font.PLAIN,11));rodape.add(estado);rodape.add(Box.createVerticalStrut(12));
        JPanel privacidade=cartao(false);privacidade.setLayout(new BorderLayout(0,6));privacidade.setBorder(BorderFactory.createEmptyBorder(10,12,10,12));privacidade.setAlignmentX(LEFT_ALIGNMENT);
        compartilhar.setAlignmentX(LEFT_ALIGNMENT);compartilhar.setSelected(servico.compartilhaConquistas());compartilhar.setOpaque(false);compartilhar.setFont(new Font("Segoe UI",Font.PLAIN,12));privacidade.add(compartilhar,BorderLayout.NORTH);
        privacidade.add(label("Participação opcional. Você pode ocultar suas conquistas.",11,false));rodape.add(privacidade);rodape.add(Box.createVerticalStrut(10));
        rodape.add(label("<html>Beba água com consciência. A referência não é uma obrigação.<br>Não beba em excesso. Horário de Brasília.</html>",11,false));add(rodape,BorderLayout.SOUTH);
        atualizar.addActionListener(e->carregar());meses.addActionListener(e->{if(!ocupado&&!ajustandoMeses)carregar();});
        meses.addPopupMenuListener(new javax.swing.event.PopupMenuListener(){
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e){atualizarMeses();}
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e){}
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e){}
        });
        hoje.addActionListener(e->carregar());mensal.addActionListener(e->carregar());
        compartilhar.addActionListener(e->alterarCompartilhamento());TemaOrvalho.aplicar(this,escuro);
        lista.setBackground(TemaOrvalho.cor(new Color(escuro?0x102131:0xF4F8FC)));scroll.getViewport().setBackground(lista.getBackground());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(10,0));
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI(){
            @Override protected JButton createDecreaseButton(int o){return vazio();}
            @Override protected JButton createIncreaseButton(int o){return vazio();}
            private JButton vazio(){JButton b=new JButton();b.setPreferredSize(new Dimension(0,0));return b;}
            @Override protected void paintTrack(Graphics g,JComponent c,Rectangle r){g.setColor(TemaOrvalho.cor(lista.getBackground()));g.fillRect(r.x,r.y,r.width,r.height);}
            @Override protected void paintThumb(Graphics g,JComponent c,Rectangle r){Graphics2D d=(Graphics2D)g.create();d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);d.setColor(TemaOrvalho.cor(new Color(escuro?0x31546B:0xC6D9EB)));d.fillRoundRect(r.x+2,r.y,r.width-4,r.height,8,8);d.dispose();}
        });
        meses.setOpaque(false);
        meses.setBackground(TemaOrvalho.cor(new Color(escuro?0x172B3D:0xFFFFFF)));
    }
    private Icon icone(boolean conquista){return new Icon(){
        public int getIconWidth(){return conquista?32:14;}public int getIconHeight(){return conquista?32:14;}
        public void paintIcon(Component c,Graphics g,int x,int y){Graphics2D d=(Graphics2D)g.create();d.translate(x,y);d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);d.setColor(TemaOrvalho.cor(new Color(escuro?0x82B5F5:0x176BDA)));d.setStroke(new BasicStroke(2.2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));if(conquista){d.drawOval(2,2,27,27);d.drawPolyline(new int[]{9,14,23},new int[]{16,21,11},3);}else{d.drawPolyline(new int[]{3,7,11},new int[]{5,9,5},3);}d.dispose();}
    };}
    private JPanel cartao(boolean destaque){
        return new JPanel(){
            {setOpaque(false);}
            @Override protected void paintComponent(Graphics g){
                Graphics2D d=(Graphics2D)g.create();d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                d.setColor(TemaOrvalho.cor(new Color(escuro?(destaque?0x193A54:0x172B3D):(destaque?0xE7F2FF:0xFFFFFF))));d.fillRoundRect(0,0,getWidth()-1,getHeight()-1,22,22);
                d.setColor(TemaOrvalho.cor(new Color(escuro?0x31546B:0xD7E4F1)));d.drawRoundRect(0,0,getWidth()-1,getHeight()-1,22,22);d.dispose();super.paintComponent(g);
            }
        };
    }
    private JPanel linha(CompeticaoService.Conquista p,boolean selecionado){
        JPanel externo=new JPanel(new BorderLayout());externo.setOpaque(false);externo.setBorder(BorderFactory.createEmptyBorder(0,0,8,2));
        JPanel linha=cartao(selecionado||p.proprio());linha.setLayout(new BorderLayout(14,0));linha.setBorder(BorderFactory.createEmptyBorder(12,16,12,16));externo.add(linha);
        JLabel lugar=label(hoje.isSelected()?"":p.posicao()+"º",20,true);if(hoje.isSelected())lugar.setIcon(icone(true));lugar.setPreferredSize(new Dimension(40,40));lugar.setHorizontalAlignment(SwingConstants.CENTER);lugar.setForeground(TemaOrvalho.cor(new Color(escuro?0x82B5F5:0x176BDA)));linha.add(lugar,BorderLayout.WEST);
        JPanel pessoa=vertical();JLabel nome=label(p.nome()+(p.proprio()?" · Você":""),13,true);nome.putClientProperty("html.disable",true);nome.setToolTipText(p.nome());pessoa.add(nome);
        pessoa.add(Box.createVerticalStrut(6));JLabel setor=label(p.setor(),11,false);setor.putClientProperty("html.disable",true);pessoa.add(setor);linha.add(pessoa,BorderLayout.CENTER);
        JLabel dias=label(hoje.isSelected()?"Atingida hoje":p.dias().size()+(p.dias().size()==1?" dia":" dias"),hoje.isSelected()?12:18,true);linha.add(dias,BorderLayout.EAST);
        for(JLabel l:new JLabel[]{nome,setor,dias})l.setForeground(TemaOrvalho.cor(new Color(escuro?0xE7F0F8:0x12304A)));
        setor.setForeground(TemaOrvalho.cor(new Color(escuro?0xA6BBD0:0x526C82)));return externo;
    }
    private void alterarCompartilhamento(){
        boolean ativar=compartilhar.isSelected();
        if(ativar){
            String aviso="Ao ativar, os participantes verão seu nome, setor e os dias em que\nvocê atingiu a referência de água. O servidor guardará a referência\nem ml para conferir o resultado; ela não aparece aos colegas.\n\nA referência usa o peso configurado (35 ml/kg) e fica fixa no dia.\nConta a partir do dia da ativação, sem preencher meses anteriores.\nVocê pode ocultar suas conquistas desmarcando esta opção.\n\nNão beba em excesso para conquistar um dia. Deseja compartilhar?";
            if(DialogosOrvalho.showConfirmDialog(this,aviso,"Compartilhar conquistas",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION){compartilhar.setSelected(false);return;}
        }
        bloquear(true);estado.setText("Salvando preferência…");
        new SwingWorker<Void,Void>(){
            protected Void doInBackground()throws Exception{servico.compartilharConquistas(ativar);return null;}
            protected void done(){if(!isDisplayable())return;try{get();}catch(Exception e){DialogosOrvalho.showMessageDialog(PainelConquistas.this,"Não foi possível salvar. Tente novamente.");}compartilhar.setSelected(servico.compartilhaConquistas());bloquear(false);carregar();}
        }.execute();
    }
    private void bloquear(boolean valor){ocupado=valor;atualizar.setEnabled(!valor);meses.setEnabled(!valor&&!hoje.isSelected());hoje.setEnabled(!valor);mensal.setEnabled(!valor);compartilhar.setEnabled(!valor);}
    private void atualizarMeses(){
        var disponiveis=MesesConquistas.disponiveis(java.time.Clock.systemUTC());
        if(meses.getItemCount()==disponiveis.size()&&(disponiveis.isEmpty()||disponiveis.getFirst().equals(meses.getItemAt(0))))return;
        Object selecionado=meses.getSelectedItem();ajustandoMeses=true;
        try{meses.removeAllItems();disponiveis.forEach(meses::addItem);if(selecionado!=null&&disponiveis.contains(selecionado))meses.setSelectedItem(selecionado);}
        finally{ajustandoMeses=false;}
    }
    void carregar(){
        if(ocupado)return;
        meses.setVisible(!hoje.isSelected());
        periodo.setVisible(hoje.isSelected());
        revalidate();repaint();
        atualizarMeses();
        if(meses.getItemCount()==0){modelo.clear();resumo.setText("Histórico disponível a partir de outubro de 2026.");return;}
        YearMonth mes=hoje.isSelected()?YearMonth.now(ZoneId.of("America/Sao_Paulo")):(YearMonth)meses.getSelectedItem();
        bloquear(true);modelo.clear();resumo.setText("Consultando conquistas…");estado.setText(" ");
        new SwingWorker<CompeticaoService.Conquistas,Void>(){
            protected CompeticaoService.Conquistas doInBackground()throws Exception{return servico.consultarConquistas(mes);}
            protected void done(){if(!isDisplayable())return;try{mostrar(get());}catch(Exception e){resumo.setText("Não foi possível carregar as conquistas.");estado.setText("Tente atualizar novamente. Seus registros continuam salvos.");}finally{bloquear(false);}}
        }.execute();
    }
    void mostrar(CompeticaoService.Conquistas dados){
        modelo.clear();dados.pessoas().stream().filter(p->!hoje.isSelected()||p.dias().contains(dados.hoje())).forEach(modelo::addElement);
        resumo.setText(modelo.isEmpty()?"Cada dia é uma nova oportunidade":modelo.size()+(modelo.size()==1?" participante":" participantes"));
        detalhe.setText(hoje.isSelected()?"Pessoas que atingiram a referência hoje.":"Dias com referência atingida no mês selecionado.");
        estado.setText(dados.aviso().isBlank()?(hoje.isSelected()?"Conquistas de hoje, no horário de Brasília.":"Selecione uma pessoa para ver o total de dias no mês."):dados.aviso());estado.setToolTipText(estado.getText());
    }
    private static JPanel vertical(){JPanel p=new JPanel();p.setOpaque(false);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));return p;}
    private static JLabel label(String s,int tamanho,boolean negrito){JLabel l=new JLabel(s);l.setFont(new Font("Segoe UI",negrito?Font.BOLD:Font.PLAIN,tamanho));l.setAlignmentX(LEFT_ALIGNMENT);return l;}
    static void abrir(Window dono,CompeticaoService servico,boolean escuro){
        JDialog d=new JDialog(dono,"Orvalho — Conquistas",Dialog.ModalityType.APPLICATION_MODAL);
        var painel=new PainelConquistas(servico,escuro);d.setContentPane(painel);d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        PainelPeso.dimensionar(d,620,720);d.setLocationRelativeTo(dono);d.addNotify();painel.carregar();d.setVisible(true);
    }
}
