package com.mycompany.bebaAgua;

import java.awt.*;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import javax.swing.*;

/** Histórico local, com correções que preservam a fila de sincronização. */
final class TelaHistorico extends JPanel {
    private static final DateTimeFormatter DATA=DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DadosAgua dados;
    private final boolean escuro;
    private final BooleanSupplier participa;
    private final Runnable alterou;
    private final JComboBox<LocalDate> dias=new JComboBox<>();
    private final DefaultListModel<DadosAgua.Pausa> modelo=new DefaultListModel<>();
    private final JList<DadosAgua.Pausa> lista=new JList<>(modelo);
    private final JLabel total=new JLabel("—"), pausas=new JLabel("—"), status=new JLabel("Carregando…"), aviso=new JLabel(" ");
    private final JButton editar=new JButton("Corrigir quantidade"), desfazer=new JButton("Desfazer registro"), atualizar=new JButton("Atualizar");
    private boolean carregando, ajustandoDatas;
    private final Color fundo, superficie, texto, suave, borda;

    TelaHistorico(DadosAgua dados,boolean escuro,BooleanSupplier participa,Runnable alterou) {
        this.dados=dados;this.escuro=escuro;this.participa=participa;this.alterou=alterou;
        fundo=TemaOrvalho.cor(new Color(escuro?0x102131:0xF4F8FC));superficie=TemaOrvalho.cor(new Color(escuro?0x172B3D:0xFFFFFF));
        texto=TemaOrvalho.cor(new Color(escuro?0xE7F0F8:0x12304A));suave=TemaOrvalho.cor(new Color(escuro?0xA6BBD0:0x52748E));borda=TemaOrvalho.cor(new Color(escuro?0x315169:0xD7E4EF));
        setLayout(new BorderLayout(0,14));setBackground(fundo);setBorder(BorderFactory.createEmptyBorder(20,22,18,22));
        JPanel topo=vertical();topo.add(label("Histórico pessoal",25,true));topo.add(Box.createVerticalStrut(5));
        topo.add(label("Suas pausas e quantidades registradas neste computador.",12,false));topo.add(Box.createVerticalStrut(16));
        dias.setName("diaHistorico");dias.setFont(new Font("Segoe UI",Font.PLAIN,13));
        dias.getAccessibleContext().setAccessibleName("Dia do histórico");
        dias.setRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> l,Object valor,int i,boolean selecionado,boolean foco){
                super.getListCellRendererComponent(l,valor,i,selecionado,foco);
                if(valor instanceof LocalDate dia)setText((dia.equals(LocalDate.now())?"Hoje · ":"")+dia.format(DATA));return this;
            }
        });
        TemaOrvalho.aplicar(dias,escuro);
        JPanel navegar=transparente(new BorderLayout(10,0));navegar.add(dias,BorderLayout.CENTER);estilo(atualizar,false);navegar.add(atualizar,BorderLayout.EAST);topo.add(navegar);
        topo.add(Box.createVerticalStrut(14));
        JPanel resumo=new JPanel(new GridLayout(1,2,12,0));resumo.setOpaque(false);resumo.setAlignmentX(LEFT_ALIGNMENT);
        resumo.add(metrica("Quantidade registrada",total));resumo.add(metrica("Pausas confirmadas",pausas));topo.add(resumo);
        topo.add(Box.createVerticalStrut(10));aviso.setFont(new Font("Segoe UI",Font.PLAIN,11));aviso.setForeground(suave);aviso.setAlignmentX(LEFT_ALIGNMENT);topo.add(aviso);add(topo,BorderLayout.NORTH);
        lista.setName("registrosHistorico");lista.setBackground(fundo);lista.setForeground(texto);lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);lista.setFixedCellHeight(60);
        lista.getAccessibleContext().setAccessibleName("Registros do dia selecionado");
        lista.setCellRenderer((l,p,i,selecionado,foco)->linha(p,selecionado));
        JScrollPane scroll=new JScrollPane(lista);scroll.setBorder(BorderFactory.createLineBorder(borda));scroll.getViewport().setBackground(fundo);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);scroll.getVerticalScrollBar().setUnitIncrement(30);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(10,0));
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI(){
            @Override protected void configureScrollBarColors(){thumbColor=borda;trackColor=fundo;}
            @Override protected JButton createDecreaseButton(int orientation){return semSeta();}
            @Override protected JButton createIncreaseButton(int orientation){return semSeta();}
            private JButton semSeta(){JButton b=new JButton();b.setPreferredSize(new Dimension(0,0));return b;}
        });add(scroll,BorderLayout.CENTER);
        JPanel rodape=vertical();status.setName("statusHistorico");status.setFont(new Font("Segoe UI",Font.PLAIN,12));status.setForeground(suave);status.setAlignmentX(LEFT_ALIGNMENT);rodape.add(status);rodape.add(Box.createVerticalStrut(10));
        JPanel acoes=transparente(new FlowLayout(FlowLayout.LEFT,8,0));estilo(editar,true);estilo(desfazer,false);acoes.add(editar);acoes.add(desfazer);rodape.add(acoes);
        rodape.add(Box.createVerticalStrut(10));
        rodape.add(label("Datas e horários deste computador. Volumes autodeclarados.",11,false));
        rodape.add(label("Alterações da competição são enviadas quando houver conexão.",11,false));add(rodape,BorderLayout.SOUTH);
        dias.addActionListener(e->{if(!ajustandoDatas&&!carregando)carregar();});
        atualizar.addActionListener(e->carregar());lista.addListSelectionListener(e->selecionou());
        editar.addActionListener(e->corrigir());desfazer.addActionListener(e->desfazer());ocupado(true);
    }
    static void abrir(JFrame dono,DadosAgua dados,boolean escuro,BooleanSupplier participa,Runnable alterou) {
        JDialog d=new JDialog(dono,"Orvalho — Histórico pessoal",true);
        var p=new TelaHistorico(dados,escuro,participa,alterou);d.setContentPane(p);d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        PainelPeso.dimensionar(d,550,640);d.setLocationRelativeTo(dono);d.addNotify();p.carregar();d.setVisible(true);
    }
    void carregar() {
        LocalDate escolhido=(LocalDate)dias.getSelectedItem();final LocalDate dia=escolhido==null?LocalDate.now():escolhido;
        ocupado(true);status.setText("Carregando registros…");
        new SwingWorker<Consulta,Void>(){
            protected Consulta doInBackground()throws Exception{return new Consulta(dados.diasHistorico(),dados.historico(dia));}
            protected void done(){
                try{var resultado=get();mostrar(dia,resultado.dias(),resultado.pausas());}
                catch(Exception e){modelo.clear();total.setText("—");pausas.setText("—");status.setText("Não foi possível ler o histórico. Os arquivos foram preservados.");status.setToolTipText(causa(e));}
                finally{ocupado(false);}
            }
        }.execute();
    }
    private record Consulta(List<LocalDate> dias,List<DadosAgua.Pausa> pausas){}
    void mostrar(LocalDate dia,List<LocalDate> datas,List<DadosAgua.Pausa> registros) {
        ajustandoDatas=true;dias.removeAllItems();datas.forEach(dias::addItem);dias.setSelectedItem(dia);ajustandoDatas=false;
        modelo.clear();registros.stream().sorted(java.util.Comparator.comparing(DadosAgua.Pausa::hora).reversed()).forEach(modelo::addElement);
        total.setText(String.format(Locale.forLanguageTag("pt-BR"),"%.2f L",registros.stream().mapToLong(DadosAgua.Pausa::ml).sum()/1000.0));pausas.setText(Integer.toString(registros.size()));
        long semVolume=registros.stream().filter(p->p.ml()==0).count();
        aviso.setText(semVolume>0?semVolume+" pausa(s) sem quantidade informada; não entram no total.":" ");
        status.setText(registros.isEmpty()?"Nenhum registro neste dia.":"Selecione um registro para ver as opções.");status.setToolTipText(null);ocupado(false);
    }
    private void ocupado(boolean valor){carregando=valor;dias.setEnabled(!valor);lista.setEnabled(!valor);atualizar.setEnabled(!valor);selecionou();}
    private void selecionou(){
        var p=lista.getSelectedValue();boolean permitido=p!=null&&!carregando&&(!p.competicao()||participa.getAsBoolean());
        boolean antigo=p!=null&&p.competicao()&&p.instante().isBefore(Instant.now().minus(Duration.ofDays(7)));
        editar.setEnabled(permitido&&!antigo);desfazer.setEnabled(permitido);
        String motivo=p!=null&&p.competicao()&&!participa.getAsBoolean()?"Entre novamente na competição para alterar este registro.":antigo?"A competição permite corrigir quantidades registradas nos últimos sete dias.":"Corrigir preserva o horário original e atualiza o total.";
        editar.setToolTipText(motivo);desfazer.setToolTipText(p!=null&&p.competicao()&&!participa.getAsBoolean()?motivo:"Remove esta confirmação do histórico e, quando aplicável, da competição.");
    }
    private void corrigir(){
        var p=lista.getSelectedValue();if(p==null||!editar.isEnabled())return;
        JDialog editor=new JDialog(SwingUtilities.getWindowAncestor(this),"Corrigir quantidade",Dialog.ModalityType.APPLICATION_MODAL);
        JPanel form=new JPanel(new BorderLayout(10,14));form.setBorder(BorderFactory.createEmptyBorder(18,18,18,18));
        form.add(new JLabel("Quantidade às "+p.hora().format(DateTimeFormatter.ofPattern("HH:mm:ss"))+" (ml)"),BorderLayout.NORTH);
        JSpinner volume=new JSpinner(new SpinnerNumberModel(p.ml()>0?p.ml():200,10,2000,50));volume.setPreferredSize(new Dimension(180,32));form.add(volume,BorderLayout.CENTER);
        JPanel botoes=new JPanel(new FlowLayout(FlowLayout.RIGHT));JButton salvar=new JButton("Salvar correção"),cancelar=new JButton("Cancelar");estilo(salvar,true);estilo(cancelar,false);botoes.add(cancelar);botoes.add(salvar);form.add(botoes,BorderLayout.SOUTH);
        TemaOrvalho.aplicar(form,escuro);cancelar.addActionListener(e->editor.dispose());
        salvar.addActionListener(e->{try{
            volume.commitEdit();int ml=((Number)volume.getValue()).intValue();
            if(p.competicao()&&!participa.getAsBoolean())throw new IllegalStateException("Entre novamente na competição.");
            boolean mudou=dados.corrigir(p.hora().toLocalDate(),p.id(),ml);editor.dispose();carregar();if(mudou)alterou.run();
        }catch(Exception ex){DialogosOrvalho.showMessageDialog(form,"Não foi possível corrigir: "+causa(ex),"Orvalho",JOptionPane.ERROR_MESSAGE);}});
        editor.setContentPane(form);editor.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);editor.pack();editor.setLocationRelativeTo(this);editor.setVisible(true);
    }
    private void desfazer(){
        var p=lista.getSelectedValue();if(p==null||!desfazer.isEnabled())return;
        if(DialogosOrvalho.showConfirmDialog(this,"Desfazer o registro de "+p.hora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))+"?\n"+(p.competicao()?"A alteração também será enviada à competição.":"O total pessoal será recalculado."),"Desfazer registro",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try{if(p.competicao()&&!participa.getAsBoolean())throw new IllegalStateException("Entre novamente na competição.");
            if(dados.desfazer(p.hora().toLocalDate(),p.id()))alterou.run();carregar();
        }catch(Exception ex){DialogosOrvalho.showMessageDialog(this,"Não foi possível desfazer: "+causa(ex),"Orvalho",JOptionPane.ERROR_MESSAGE);}
    }
    private JPanel linha(DadosAgua.Pausa p,boolean selecionado){
        JPanel linha=new JPanel(new BorderLayout(8,4));linha.setBackground(selecionado?TemaOrvalho.cor(new Color(escuro?0x244966:0xE1EFFF)):superficie);
        linha.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,borda),BorderFactory.createEmptyBorder(9,14,9,14)));
        JPanel descricao=vertical();descricao.add(label(p.hora().format(DateTimeFormatter.ofPattern("HH:mm:ss")),14,true));
        descricao.add(label(p.competicao()?"Vinculado à competição":"Registro pessoal",11,false));linha.add(descricao,BorderLayout.CENTER);
        linha.add(label(p.ml()>0?p.ml()+" ml":"Sem quantidade",14,true),BorderLayout.EAST);return linha;
    }
    private JPanel metrica(String titulo,JLabel valor){
        JPanel p=new JPanel(new BorderLayout(0,5)){
            @Override protected void paintComponent(Graphics g){
                Graphics2D d=(Graphics2D)g.create();d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                d.setColor(superficie);d.fillRoundRect(1,1,getWidth()-3,getHeight()-3,16,16);
                d.setColor(borda);d.drawRoundRect(1,1,getWidth()-3,getHeight()-3,16,16);d.dispose();super.paintComponent(g);
            }
        };p.setOpaque(false);p.setBorder(BorderFactory.createEmptyBorder(12,14,12,14));
        p.add(label(titulo,12,false),BorderLayout.NORTH);valor.setFont(new Font("Segoe UI",Font.BOLD,23));valor.setForeground(texto);p.add(valor);return p;
    }
    private JLabel label(String s,int tamanho,boolean forte){JLabel l=new JLabel(s);l.setFont(new Font("Segoe UI",forte?Font.BOLD:Font.PLAIN,tamanho));l.setForeground(forte?texto:suave);l.setAlignmentX(LEFT_ALIGNMENT);return l;}
    private JPanel vertical(){JPanel p=transparente(null);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));return p;}
    private JPanel transparente(LayoutManager layout){JPanel p=new JPanel(layout);p.setOpaque(false);p.setAlignmentX(LEFT_ALIGNMENT);return p;}
    private void estilo(JButton b,boolean destaque){BotaoOrvalho.aplicar(b,destaque);TemaOrvalho.aplicar(b,escuro);}
    private static String causa(Exception e){Throwable c=e.getCause()==null?e:e.getCause();return c.getMessage()==null?"Verifique a quantidade informada.":c.getMessage();}
}
