package com.mycompany.bebaAgua;
import java.awt.*;
import java.util.*;
import javax.swing.*;

final class TemaOrvalho {
    private static final Map<Component,Color[]> originais=new WeakHashMap<>();
    private static final Map<JComponent,javax.swing.border.Border> bordas=new WeakHashMap<>();
    private static final Map<JComboBox<?>,ListCellRenderer<?>> renderizadores=new WeakHashMap<>();
    private static boolean rosa;
    static void configurar(DadosAgua.Opcoes o){rosa=o.outubroRosa();AnimacoesOrvalho.configurar(o.efeitos());}
    static void outubroRosa(boolean ativo){rosa=ativo;}
    static boolean outubroRosa(){return rosa;}
    static void rolagem(JScrollPane scroll){
        scroll.getViewport().setBackground(new Color(0xF4F8FC));scroll.setBackground(new Color(0xF4F8FC));
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(10,0));
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI(){
            @Override protected void paintTrack(Graphics g,JComponent c,Rectangle r){g.setColor(cor(new Color(Boolean.TRUE.equals(c.getClientProperty("orvalho.escuro"))?0x102131:0xF4F8FC)));g.fillRect(r.x,r.y,r.width,r.height);}
            @Override protected void paintThumb(Graphics g,JComponent c,Rectangle r){Graphics2D d=(Graphics2D)g.create();d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);d.setColor(cor(new Color(Boolean.TRUE.equals(c.getClientProperty("orvalho.escuro"))?0x547089:0xD5E4EF)));d.fillRoundRect(r.x+2,r.y,r.width-4,r.height,6,6);d.dispose();}
            @Override protected JButton createDecreaseButton(int o){return vazio();}
            @Override protected JButton createIncreaseButton(int o){return vazio();}
            private JButton vazio(){JButton b=new JButton();b.setPreferredSize(new Dimension(0,0));return b;}
        });
    }
    /** Paleta compartilhada pela interface e pelas ilustrações de hidratação. */
    static Color cor(Color base){
        if(!rosa||base==null)return base;
        int rgb=base.getRGB()&0xffffff;
        int nova=switch(rgb){
            case 0xF4F8FC,0xF0F7FF -> 0xFFF7FA;
            case 0xE8F3FF,0xE7F2FF,0xE1EFFB -> 0xFCE4EE;
            case 0x12304A,0x10324D,0x102131,0x172B3D -> 0x42102D;
            case 0x333333,0x000000 -> 0x42102D;
            case 0x526B86,0x52748E,0x526C82,0x547089 -> 0x806073;
            case 0x176BDA,0x0876F9,0x2785F2 -> 0xC73C75;
            case 0x65C9F3,0x69D5F5,0x58CAE9 -> 0xF5A9CC;
            case 0xF4FAFF,0xE6F3FC -> 0xFFF3F8;
            case 0x244358 -> 0xFCE4EE;
            case 0x83B9D9,0x8DBBD6 -> 0xE69AB9;
            case 0x125DBE -> 0xAB1E59;
            case 0xCCCCCC,0xD7E4F1,0xD7E4EF,0xD5E4EF,0x7B9CB5 -> 0xE2B8C9;
            case 0x82B5F5,0x77A9D0,0x78AFD3 -> 0xCA5685;
            case 0xF2F2F2 -> 0xF7EEF2;
            default -> -1;
        };
        if(nova<0&&base.getBlue()>base.getRed()+12){
            nova=base.getRed()>175?0xF6DFE9:base.getRed()>100?0xCE7296:base.getBlue()<130?0x755367:0xC73C75;
        }
        return nova<0?base:new Color((base.getAlpha()<<24)|nova,true);
    }
    static JButton selo(){
        JButton l=new JButton("Outubro Rosa",new Icon(){
            public int getIconWidth(){return 23;} public int getIconHeight(){return 28;}
            public void paintIcon(Component c,Graphics graphics,int x,int y){
                Graphics2D g=(Graphics2D)graphics.create();g.translate(x,y);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setStroke(new BasicStroke(5,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND));
                var p=new java.awt.geom.Path2D.Double();p.moveTo(4,25);p.curveTo(10,17,18,9,17,5);p.curveTo(16,0,7,0,6,5);p.curveTo(5,10,13,19,20,25);
                g.setColor(new Color(0xD83C7F));g.draw(p);g.dispose();
            }
        });
        BotaoOrvalho.link(l);l.setBorder(BorderFactory.createEmptyBorder(2,4,2,4));
        l.setName("abrirOutubroRosa");l.getAccessibleContext().setAccessibleName("Saiba mais sobre Outubro Rosa");
        l.addActionListener(e->PainelOutubroRosa.abrir(l));
        l.putClientProperty("orvalho.seloRosa",true);l.setFont(new Font("Segoe UI",Font.BOLD,11));l.setForeground(new Color(0x9D2458));
        l.setToolTipText("Clique para conhecer a campanha e os cuidados com a saúde das mamas");l.setVisible(rosa);return l;
    }
    static void aplicar(Component c,boolean escuro) {
        TipografiaOrvalho.aplicar(c);
        if(c instanceof JComponent jc){
            jc.putClientProperty("orvalho.escuro",escuro&&!rosa);
            if(Boolean.TRUE.equals(jc.getClientProperty("orvalho.seloRosa")))jc.setVisible(rosa);
            if(!bordas.containsKey(jc))bordas.put(jc,jc.getBorder());
            var original=bordas.get(jc);jc.setBorder(rosa?bordaRosa(original):original);
        }
        Color[] cores=originais.computeIfAbsent(c,k->new Color[]{k.getBackground(),k.getForeground()});
        if(rosa){
            c.setBackground(cor(cores[0]));c.setForeground(cor(cores[1]));
            if(c instanceof javax.swing.text.JTextComponent||c instanceof JComboBox<?>)c.setBackground(Color.WHITE);
            if(c instanceof JComponent jc){
                if(Boolean.TRUE.equals(jc.getClientProperty("orvalho.iniciar"))){c.setBackground(new Color(0xC73C75));c.setForeground(Color.WHITE);}
            }
        } else if(escuro) {
            Color fundo=cores[0];
            c.setBackground(fundo!=null && fundo.getBlue()>fundo.getRed()+25 && fundo.getRed()<60
                    ? new Color(0x176BDA) : new Color(fundo != null && fundo.getRGB() == new Color(0xF4F8FC).getRGB() ? 0x102131 : (c instanceof JPanel ? 0x172B3D : 0x20394E)));
            Color frente=cores[1];
            c.setForeground(new Color(frente != null && frente.getBlue() > frente.getRed() + 90 ? 0x82B5F5 : 0xE7F0F8));
        } else {c.setBackground(cores[0]);c.setForeground(cores[1]);}
        if(c instanceof javax.swing.text.JTextComponent t){t.setCaretColor(c.getForeground());t.setDisabledTextColor(new Color(escuro?0xA6BBD0:0x888888));}
        if(c instanceof JComboBox<?> combo)selecao(combo);
        if(c instanceof Container cont)for(Component filho:cont.getComponents())aplicar(filho,escuro);
        c.repaint();
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private static void selecao(JComboBox combo){
        ListCellRenderer original=renderizadores.computeIfAbsent(combo,k->k.getRenderer());
        if(!rosa){combo.setRenderer(original);return;}
        combo.setRenderer((lista,valor,indice,selecionado,foco)->{
            Component item=original.getListCellRendererComponent(lista,valor,indice,selecionado,foco);
            item.setBackground(new Color(selecionado?0xFCE4EE:0xFFFFFF));item.setForeground(new Color(0x42102D));
            return item;
        });
    }
    private static javax.swing.border.Border bordaRosa(javax.swing.border.Border b){
        if(b instanceof javax.swing.border.LineBorder l)return BorderFactory.createLineBorder(cor(l.getLineColor()),l.getThickness(),l.getRoundedCorners());
        if(b instanceof javax.swing.border.CompoundBorder p)return BorderFactory.createCompoundBorder(bordaRosa(p.getOutsideBorder()),bordaRosa(p.getInsideBorder()));
        return b;
    }
}
