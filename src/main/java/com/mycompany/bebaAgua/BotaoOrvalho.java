package com.mycompany.bebaAgua;
import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;

/** Botões com transições curtas, foco visível e o mesmo ciclo de vida dos efeitos. */
final class BotaoOrvalho {
    static void aplicar(AbstractButton b,boolean destaque){aplicar(b,destaque,false);}
    static void link(AbstractButton b){aplicar(b,false,true);}
    private static void aplicar(AbstractButton b,boolean destaque,boolean link){
        b.setFont(new Font("Segoe UI",Font.BOLD,12));b.setForeground(new Color(destaque?0xFFFFFF:0x12304A));b.setBackground(new Color(destaque?0x176BDA:0xFFFFFF));
        b.setOpaque(false);b.setContentAreaFilled(false);b.setBorderPainted(false);b.setRolloverEnabled(true);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(link?7:12,link?4:20,link?7:12,link?4:20));
        Transicao t=(Transicao)b.getClientProperty("orvalho.transicaoBotao");
        if(t==null){t=new Transicao(b);b.putClientProperty("orvalho.transicaoBotao",t);}final Transicao transicao=t;
        b.setUI(new BasicButtonUI(){
            @Override public void paint(Graphics graphics,JComponent c){
                Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                boolean escuro=Boolean.TRUE.equals(c.getClientProperty("orvalho.escuro"));
                if(!link){
                    Color base=TemaOrvalho.cor(b.getBackground()),ativo=TemaOrvalho.cor(new Color(destaque?0x125DBE:escuro?0x29475F:0xE8F3FF));
                    Color fundo=b.isEnabled()?misturar(base,ativo,transicao.valor):TemaOrvalho.cor(new Color(escuro?0x293B4B:0xF2F2F2));
                    g.setColor(fundo);g.fillRoundRect(1,1,c.getWidth()-3,c.getHeight()-3,38,38);
                    g.setColor(TemaOrvalho.cor(new Color(destaque?0x176BDA:escuro?0x547089:0xCCCCCC)));g.drawRoundRect(1,1,c.getWidth()-3,c.getHeight()-3,38,38);
                }
                if(b.hasFocus()){g.setColor(TemaOrvalho.cor(new Color(escuro?0x82B5F5:0x0876F9)));g.setStroke(new BasicStroke(1.5f));g.drawRoundRect(4,4,c.getWidth()-9,c.getHeight()-9,link?12:32,link?12:32);}
                g.dispose();super.paint(graphics,c);
            }
            @Override protected void paintButtonPressed(Graphics g,AbstractButton button){}
            @Override protected void paintText(Graphics g,JComponent c,Rectangle r,String texto){
                TipografiaOrvalho.suavizar(g);boolean escuro=Boolean.TRUE.equals(c.getClientProperty("orvalho.escuro"));
                g.setColor(b.isEnabled()?b.getForeground():new Color(escuro?0xA6BBD0:0x777777));
                javax.swing.plaf.basic.BasicGraphicsUtils.drawStringUnderlineCharAt(g,texto,b.getDisplayedMnemonicIndex(),r.x,r.y+g.getFontMetrics().getAscent());
            }
        });
    }
    private static Color misturar(Color a,Color b,double f){return new Color((int)(a.getRed()+(b.getRed()-a.getRed())*f),(int)(a.getGreen()+(b.getGreen()-a.getGreen())*f),(int)(a.getBlue()+(b.getBlue()-a.getBlue())*f));}
    private static final class Transicao {
        double valor,inicio,alvo,tempo=1;final AnimacoesOrvalho.Atividade animacao;
        Transicao(AbstractButton b){
            animacao=AnimacoesOrvalho.vincular(b,()->tempo<1,dt->{tempo=Math.min(1,tempo+dt/.18);valor=inicio+(alvo-inicio)*tempo;b.repaint();},()->{tempo=1;valor=alvo;});
            b.getModel().addChangeListener(e->{double novo=b.isEnabled()?(b.getModel().isPressed()?1:b.getModel().isRollover()||b.isSelected()?.65:0):0;
                if(novo!=alvo){inicio=valor;alvo=novo;tempo=0;if(!AnimacoesOrvalho.permitidas()){tempo=1;valor=alvo;}animacao.solicitar();b.repaint();}
            });
        }
    }
}
