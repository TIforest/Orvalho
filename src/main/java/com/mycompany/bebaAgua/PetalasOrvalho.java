package com.mycompany.bebaAgua;

import java.awt.*;
import java.awt.geom.Path2D;
import javax.swing.*;

/** Vidro decorativo transparente a eventos, limitado às margens do conteúdo. */
final class PetalasOrvalho extends JComponent {
    private final double[] fases=new double[12],duracoes=new double[12],tamanhos=new double[12];
    private final JScrollPane rolagem;
    private static final Path2D PETALA=new Path2D.Double();
    static{PETALA.moveTo(0,-.5);PETALA.curveTo(.5,-.2,.35,.35,0,.5);PETALA.curveTo(-.38,.23,-.25,-.22,0,-.5);PETALA.closePath();}
    PetalasOrvalho(){this(null);}
    PetalasOrvalho(JScrollPane rolagem){
        this.rolagem=rolagem;
        setOpaque(false);setFocusable(false);setName("decoracaoPetalas");
        for(int i=0;i<12;i++){fases[i]=(i*.173)%1;duracoes[i]=12+(i*7%13);tamanhos[i]=10+(i*5%17);}
        AnimacoesOrvalho.vincular(this,TemaOrvalho::outubroRosa,dt->{avancar(dt);repaint();},()->{});
    }
    @Override public boolean contains(int x,int y){return false;}
    int quantidade(){return getWidth()<500?8:12;}
    void avancar(double dt){for(int i=0;i<12;i++)fases[i]=(fases[i]+dt/duracoes[i])%1;}
    @Override protected void paintComponent(Graphics graphics){
        if(!TemaOrvalho.outubroRosa()||!AnimacoesOrvalho.permitidas())return;
        Graphics2D g=(Graphics2D)graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        // Só pinta nas margens: nenhum texto, contador, campo ou botão fica encoberto.
        int direita=rolagem!=null&&rolagem.getVerticalScrollBar().isVisible()?SwingUtilities.convertPoint(rolagem.getVerticalScrollBar(),0,0,this).x:getWidth();
        var margens=new java.awt.geom.Area(new Rectangle(0,0,22,getHeight()));
        margens.add(new java.awt.geom.Area(new Rectangle(Math.max(0,direita-22),0,22,getHeight())));g.clip(margens);
        for(int i=0;i<quantidade();i++){
            Graphics2D p=(Graphics2D)g.create();double f=fases[i];
            double x=i%2==0?11+5*Math.sin(f*6+i):direita-11+5*Math.sin(f*6+i);
            p.translate(x,-28+f*(getHeight()+56));p.rotate(Math.sin(f*6+i)*.5+i);p.scale(tamanhos[i],tamanhos[i]);
            p.setColor(new Color(199,60,117,38+i%6*12));p.fill(PETALA);p.dispose();
        }g.dispose();
    }
}
