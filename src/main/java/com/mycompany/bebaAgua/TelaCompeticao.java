package com.mycompany.bebaAgua;
import java.awt.*;
import javax.swing.*;

final class TelaCompeticao {
    static final String AVISO="Esta competição é apenas um incentivo para beber água. Não beba quantidades excessivas para subir no ranking. Seja consciente e respeite suas necessidades e orientações de saúde.";
    static void abrir(JFrame dono,CompeticaoService servico,boolean escuro,Runnable atualizar) {
        if(servico.participa()){abrirRanking(dono,servico,escuro,atualizar);return;}
        JDialog d=new JDialog(dono,"Competição da Forest",true);
        JPanel p=new JPanel();p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        JTextArea descricao=texto("Ranking diário por litros registrados, do maior para o menor. Participação voluntária e volumes autodeclarados.\n\n"+AVISO,5);
        p.add(descricao);p.add(Box.createVerticalStrut(12));
        JLabel status=new JLabel(" ");status.setFont(new Font("Segoe UI",Font.PLAIN,12));
        if(!servico.participa()){
            JTextField nome=new JTextField(26),setor=new JTextField(26);JPasswordField senha=new JPasswordField(26);
            p.add(new JLabel("Seu nome"));p.add(nome);
            p.add(new JLabel("Setor"));p.add(setor);
            p.add(new JLabel("Senha da competição"));p.add(senha);
            JCheckBox aceite=new JCheckBox("Quero compartilhar nome, setor e litros no ranking interno.");
            p.add(aceite);p.add(texto("Use a senha da competição fornecida pela Forest. Após entrar, este computador lembrará sua participação. Compartilhe a senha somente com funcionários da empresa.",3));
            JButton entrar=new JButton("Participar da competição");BotaoOrvalho.aplicar(entrar,true);p.add(entrar);
            if(!servico.disponivel()){status.setText("Aguardando ativação do serviço pelo TI.");entrar.setEnabled(false);}
            entrar.addActionListener(e->{
                if(!aceite.isSelected()||nome.getText().strip().length()<2||setor.getText().strip().length()<2){status.setText("Preencha nome, setor e marque o aceite.");return;}
                String codigo=new String(senha.getPassword());
                if(codigo.isBlank()||codigo.length()>128){status.setText("Informe a senha da competição.");return;}
                String n=nome.getText(),s=setor.getText();entrar.setEnabled(false);status.setText("Validando senha...");
                new SwingWorker<Void,Void>(){
                    protected Void doInBackground()throws Exception{servico.participar(n,s,codigo);return null;}
                    protected void done(){senha.setText("");try{get();atualizar.run();d.dispose();abrir(dono,servico,escuro,atualizar);}catch(Exception ex){status.setText("Não foi possível participar. Confira a senha e os dados.");DialogosOrvalho.showMessageDialog(d,causa(ex));entrar.setEnabled(true);}}
                }.execute();
            });
        }
        p.add(Box.createVerticalStrut(8));p.add(status);
        p.setBackground(new Color(0xF4F8FC));
        for(Component componente:p.getComponents()){
            if(!(componente instanceof JButton))componente.setForeground(new Color(0x10324D));
            if(componente instanceof JLabel || componente instanceof JTextField || componente instanceof JCheckBox)
                componente.setFont(new Font("Segoe UI",Font.PLAIN,13));
            if(componente instanceof JCheckBox c)c.setOpaque(false);
        }
        TemaOrvalho.aplicar(p,escuro);
        for(Component componente:p.getComponents()){
            if(componente instanceof JComponent c){
                c.setAlignmentX(Component.LEFT_ALIGNMENT);
                if(c instanceof JLabel || c instanceof JTextField || c instanceof JCheckBox)
                    c.setMaximumSize(new Dimension(Integer.MAX_VALUE,c.getPreferredSize().height));
            }
        }
        d.setContentPane(p);d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);d.pack();d.setLocationRelativeTo(dono);d.setVisible(true);
    }
    private static void abrirRanking(JFrame dono,CompeticaoService servico,boolean escuro,Runnable atualizar) {
        JDialog d=new JDialog(dono,"Orvalho — Competição da Forest",true);
        PainelRanking painel=new PainelRanking(escuro);
        int[] consulta={0};
        Runnable carregar=()->{
            int numero=++consulta[0];
            var diaConsulta=painel.diaSelecionado();
            painel.carregando();
            new SwingWorker<CompeticaoService.Ranking,Void>() {
                protected CompeticaoService.Ranking doInBackground()throws Exception{return servico.consultarRanking(diaConsulta);}
                protected void done(){
                    if(!painel.isDisplayable()||numero!=consulta[0])return;
                    try{painel.mostrar(get());}catch(Exception e){painel.falhou();}
                }
            }.execute();
        };
        painel.botaoData().addActionListener(e->{if(painel.escolherDia())carregar.run();});
        painel.botaoConquistas().addActionListener(e->PainelConquistas.abrir(d,servico,escuro));
        painel.botaoAtualizar().addActionListener(e->carregar.run());
        painel.botaoSair().addActionListener(e->{
            if(DialogosOrvalho.showConfirmDialog(d,"Seu nome sairá do ranking. Para voltar, use seu cadastro e a senha da competição. Continuar?","Sair da competição",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
            painel.botaoSair().setEnabled(false);painel.botaoAtualizar().setEnabled(false);
            new SwingWorker<Void,Void>() {
                protected Void doInBackground()throws Exception{servico.sair();return null;}
                protected void done(){if(!painel.isDisplayable())return;try{get();atualizar.run();d.dispose();}catch(Exception ex){DialogosOrvalho.showMessageDialog(d,causa(ex));painel.botaoSair().setEnabled(true);painel.botaoAtualizar().setEnabled(true);}}
            }.execute();
        });
        d.setContentPane(painel);d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        PainelPeso.dimensionar(d,560,780);d.setLocationRelativeTo(dono);d.addNotify();carregar.run();d.setVisible(true);
    }
    private static JTextArea texto(String s,int linhas){JTextArea a=new JTextArea(s,linhas,40);a.setEditable(false);a.setLineWrap(true);a.setWrapStyleWord(true);a.setOpaque(false);a.setFont(new Font("Segoe UI",Font.PLAIN,13));return a;}
    private static String causa(Exception e){Throwable c=e.getCause()==null?e:e.getCause();return c.getMessage();}
}
