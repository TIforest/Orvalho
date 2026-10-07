package com.mycompany.bebaAgua;

import java.awt.*;
import java.awt.geom.Path2D;
import javax.swing.JComponent;

/** Ilustração do tempo restante; não representa consumo de água. */
class GarrafaAnimada extends JComponent {
    private final LembreteService lembrete;
    private final AnimacoesOrvalho.Atividade animacao;
    private double fase;
    private final java.util.function.LongSupplier relogio;
    private boolean esvaziando;
    private long inicioEsvaziamento;

    GarrafaAnimada(LembreteService lembrete) {
        this(lembrete, System::nanoTime);
    }

    GarrafaAnimada(LembreteService lembrete, java.util.function.LongSupplier relogio) {
        this.lembrete = lembrete;
        this.relogio = relogio;
        setPreferredSize(new Dimension(76, 144));
        setMinimumSize(new Dimension(76, 144));
        setToolTipText("A garrafa enche até o lembrete e esvazia ao confirmar o lembrete. A pausa é registrada pelo botão Já bebi.");
        animacao=AnimacoesOrvalho.vincular(this,()->true,dt->{if(lembrete.ativo())fase+=dt*1.4;repaint();},()->{fase=0;esvaziando=false;});
    }

    void esvaziar() {
        if(!AnimacoesOrvalho.permitidas()){esvaziando=false;repaint();return;}
        inicioEsvaziamento = relogio.getAsLong();
        esvaziando = true;
        repaint();
    }

    double nivelAgua() {
        double nivel = 1.0 - lembrete.proporcaoRestante();
        if (!esvaziando) return nivel;
        double progresso = Math.max(0.0, (relogio.getAsLong() - inicioEsvaziamento) / 650_000_000.0);
        if (progresso >= 1.0) { esvaziando = false; return nivel; }
        // Transição de 650 ms da garrafa cheia ao nível do novo ciclo.
        double suave = progresso * progresso * (3 - 2 * progresso);
        return 1.0 + (nivel - 1.0) * suave;
    }


    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            boolean rosa = TemaOrvalho.outubroRosa();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double escala = Math.min(getWidth() / 76.0, getHeight() / 144.0);
            g.translate((getWidth() - 76 * escala) / 2, (getHeight() - 144 * escala) / 2);
            g.scale(escala, escala);
            Path2D corpo = new Path2D.Double();
            corpo.moveTo(27, 20);
            corpo.lineTo(27, 30);
            corpo.curveTo(27, 37, 14, 40, 14, 53);
            corpo.lineTo(14, 123);
            corpo.quadTo(14, 135, 26, 135);
            corpo.lineTo(50, 135);
            corpo.quadTo(62, 135, 62, 123);
            corpo.lineTo(62, 53);
            corpo.curveTo(62, 40, 49, 37, 49, 30);
            corpo.lineTo(49, 20);
            corpo.closePath();
            g.setColor(new Color(rosa ? 0xFFF3F8 : 0xF4FAFF));
            g.fill(corpo);

            Graphics2D agua = (Graphics2D) g.create();
            try {
                agua.clip(corpo);
                double nivel = nivelAgua();
                double y = 135 - 112 * nivel;
                double onda = 1.7 * Math.sin(Math.PI * nivel);
                Path2D superficie = new Path2D.Double();
                superficie.moveTo(10, y);
                for (int x = 10; x <= 66; x++)
                    superficie.lineTo(x, y + onda * Math.sin(x / 10.0 + fase));
                superficie.lineTo(66, 138);
                superficie.lineTo(10, 138);
                superficie.closePath();
                agua.setPaint(new GradientPaint(0, (float) y, new Color(rosa ? 0xF5A9CC : 0x65C9F3),
                        0, 135, new Color(rosa ? 0xC73C75 : 0x0876F9)));
                agua.fill(superficie);
                agua.setColor(new Color(255, 255, 255, 125));
                agua.fillRoundRect(20, 48, 4, 71, 4, 4);
            } finally { agua.dispose(); }

            g.setStroke(new BasicStroke(2));
            g.setColor(new Color(rosa ? 0xE69AB9 : 0x82B5F5));
            g.draw(corpo);
            g.setColor(new Color(rosa ? 0xC73C75 : 0x0876F9));
            g.fillRoundRect(24, 10, 28, 13, 5, 5);
            g.setColor(new Color(255, 255, 255, 90));
            g.fillRoundRect(28, 12, 19, 3, 3, 3);
        } finally { g.dispose(); }
    }
}
