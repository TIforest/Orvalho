package com.mycompany.bebaAgua;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;

/** Fonte e suavização comuns, sem forçar a escala de tela do Windows. */
final class TipografiaOrvalho {
    private TipografiaOrvalho() { }
    static void iniciar() {
        UIManager.put(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        var defaults=UIManager.getDefaults();
        for(Object chave:java.util.Collections.list(defaults.keys())) {
            Object valor=defaults.get(chave);
            if(valor instanceof Font fonte)UIManager.put(chave,new FontUIResource("Segoe UI",fonte.getStyle(),Math.max(12,fonte.getSize())));
        }
    }
    static void aplicar(Component c) {
        if(c instanceof JComponent jc)jc.putClientProperty(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font fonte=c.getFont();
        if(fonte!=null&&!"Segoe UI".equals(fonte.getName()))c.setFont(new Font("Segoe UI",fonte.getStyle(),fonte.getSize()));
    }
    static void suavizar(Graphics g) {
        if(g instanceof Graphics2D d)d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
}
