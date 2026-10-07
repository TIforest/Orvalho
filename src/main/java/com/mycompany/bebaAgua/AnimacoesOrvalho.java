package com.mycompany.bebaAgua;

import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.util.*;
import java.util.function.*;
import javax.swing.*;

/** Um único pulso Swing para efeitos visuais. Nunca altera o relógio do lembrete. */
final class AnimacoesOrvalho {
    private static final Map<Atividade,Boolean> atividades=new WeakHashMap<>();
    private static final javax.swing.Timer pulso=new javax.swing.Timer(40,e->passo());
    private static boolean preferencia=true;
    private static Boolean sistemaTeste;
    private static long anterior,consultaSistema;
    private static boolean sistema=true;
    static boolean permitidas(){return preferencia&&(sistemaTeste!=null?sistemaTeste:sistema);}
    static void configurar(boolean ativo){preferencia=ativo;consultarSistema();reavaliar();}
    static void consultarSistema(){sistema=MovimentoWindows.animacoesPermitidas();consultaSistema=System.nanoTime();}
    static void sistemaParaTeste(Boolean permitido){sistemaTeste=permitido;reavaliar();}
    static Atividade vincular(JComponent c,BooleanSupplier precisa,DoubleConsumer quadro,Runnable estatico){return new Atividade(c,precisa,quadro,estatico);}
    static final class Atividade {
        final JComponent componente;final BooleanSupplier precisa;final DoubleConsumer quadro;final Runnable estatico;boolean estavaAtiva;
        Atividade(JComponent c,BooleanSupplier precisa,DoubleConsumer quadro,Runnable estatico){
            componente=c;this.precisa=precisa;this.quadro=quadro;this.estatico=estatico;
            c.addHierarchyListener(e->{if((e.getChangeFlags()&(HierarchyEvent.SHOWING_CHANGED|HierarchyEvent.DISPLAYABILITY_CHANGED))!=0){
                if(c.isDisplayable())atividades.put(this,true);else atividades.remove(this);reavaliar();
            }});
            if(c.isDisplayable())atividades.put(this,true);
        }
        boolean ativa(){return permitidas()&&visivel(componente)&&precisa.getAsBoolean();}
        void solicitar(){if(componente.isDisplayable())atividades.put(this,true);reavaliar();}
    }
    static boolean visivel(Component c){
        if(!c.isShowing())return false;
        for(Window w=SwingUtilities.getWindowAncestor(c);w!=null;w=w.getOwner())if(w instanceof Frame f&&(f.getExtendedState()&Frame.ICONIFIED)!=0)return false;
        return true;
    }
    static void reavaliar(){
        boolean alguma=false;
        for(Atividade a:new ArrayList<>(atividades.keySet())){
            boolean ativa=a.ativa();
            if(ativa)alguma=true;else if(a.estavaAtiva||!permitidas()){a.estatico.run();a.componente.repaint();}
            a.estavaAtiva=ativa;
        }
        if(alguma&&!pulso.isRunning()){anterior=System.nanoTime();pulso.start();}
        else if(!alguma)pulso.stop();
    }
    private static void passo(){
        long agora=System.nanoTime();double dt=Math.min(.08,(agora-anterior)/1e9);anterior=agora;
        if(agora-consultaSistema>5_000_000_000L)consultarSistema();
        for(Atividade a:new ArrayList<>(atividades.keySet()))if(a.ativa())a.quadro.accept(dt);
        reavaliar();
    }
    static boolean rodando(){return pulso.isRunning();}
    static int quantidadeAtiva(){return (int)atividades.keySet().stream().filter(Atividade::ativa).count();}
}
