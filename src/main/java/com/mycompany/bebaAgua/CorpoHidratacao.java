package com.mycompany.bebaAgua;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

/** Silhueta vetorial preenchida pelo consumo diário, com transição e ondas suaves. */
final class CorpoHidratacao extends JPanel {
    private double destino, nivel, fase;
    private final AnimacoesOrvalho.Atividade animacao;
    CorpoHidratacao() {
        setOpaque(false);
        setPreferredSize(new Dimension(70, 106));
        animacao=AnimacoesOrvalho.vincular(this,()->true,dt->{nivel+=(destino-nivel)*Math.min(1,dt*7);if(Math.abs(destino-nivel)<.001)nivel=destino;fase+=dt*1.4;repaint();},()->{nivel=destino;fase=0;});
        getAccessibleContext().setAccessibleName("Consumo de água do dia");
    }
    void atualizar(double fracao, String descricao) {
        destino = Math.max(0, Math.min(1, fracao));
        if (!isShowing()||!AnimacoesOrvalho.permitidas()) nivel = destino;
        animacao.solicitar();
        setToolTipText(descricao);
        getAccessibleContext().setAccessibleDescription(descricao);
        repaint();
    }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double escala = Math.min(getWidth()/70.0, getHeight()/106.0);
            g.translate((getWidth()-70*escala)/2, (getHeight()-106*escala)/2);
            g.scale(escala, escala);
            Area corpo = new Area(new Ellipse2D.Double(25, 3, 20, 20));
            corpo.add(new Area(new RoundRectangle2D.Double(20, 28, 30, 41, 15, 15)));
            corpo.add(new Area(new RoundRectangle2D.Double(10, 30, 12, 39, 12, 12)));
            corpo.add(new Area(new RoundRectangle2D.Double(48, 30, 12, 39, 12, 12)));
            corpo.add(new Area(new RoundRectangle2D.Double(21, 58, 12, 45, 12, 12)));
            corpo.add(new Area(new RoundRectangle2D.Double(37, 58, 12, 45, 12, 12)));
            boolean escuro = Boolean.TRUE.equals(getClientProperty("orvalho.escuro"));
            g.setColor(TemaOrvalho.cor(new Color(escuro ? 0x244358 : 0xE6F3FC))); g.fill(corpo);
            Graphics2D agua = (Graphics2D) g.create();
            agua.clip(corpo);
            double y = 103 - 100*nivel;
            Path2D onda = new Path2D.Double(); onda.moveTo(0, y);
            double amplitude = 1.8 * Math.sin(Math.PI*nivel);
            for (int x=0; x<=70; x++) onda.lineTo(x, y + amplitude*Math.sin(x/9.0+fase));
            onda.lineTo(70, 108); onda.lineTo(0, 108); onda.closePath();
            agua.setPaint(new GradientPaint(0, (float)y, TemaOrvalho.cor(new Color(0x69D5F5)), 0, 106, TemaOrvalho.cor(new Color(0x176BDA))));
            agua.fill(onda); agua.dispose();
            g.setColor(TemaOrvalho.cor(new Color(escuro ? 0x83B9D9 : 0x8DBBD6)));
            g.setStroke(new BasicStroke(1.4f)); g.draw(corpo);
        } finally { g.dispose(); }
    }
}
