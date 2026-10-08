package com.mycompany.bebaAgua;

import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ModalidadeLembreteTest {
    @Test void confirmarComHistoricoModalAberto() throws Exception { verificar(false); }
    @Test void adiarComRankingEDialogoFilhoAbertos() throws Exception { verificar(true); }

    private void verificar(boolean adiar) throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        JFrame[] dono=new JFrame[1];JDialog[] modal=new JDialog[1],filho=new JDialog[1];
        PopupLembrete[] popup=new PopupLembrete[1];Point[] clique=new Point[1];
        CountDownLatch aberto=new CountDownLatch(1),concluido=new CountDownLatch(1);
        AtomicInteger confirmacoes=new AtomicInteger(),adiamentos=new AtomicInteger(),volume=new AtomicInteger();
        Robot robot=new Robot();Point mouse=MouseInfo.getPointerInfo().getLocation();
        try {
            SwingUtilities.invokeAndWait(()->{
                dono[0]=new JFrame("Teste de modalidade");dono[0].setSize(200,100);dono[0].setVisible(true);
                modal[0]=new JDialog(dono[0],"Ranking / histórico",true);modal[0].setSize(250,150);
                JDialog alvo=modal[0];
                if(adiar){filho[0]=new JDialog(modal[0],"Data do ranking",Dialog.ModalityType.APPLICATION_MODAL);filho[0].setSize(200,100);alvo=filho[0];}
                alvo.addWindowListener(new WindowAdapter(){@Override public void windowOpened(WindowEvent e){aberto.countDown();}});
                SwingUtilities.invokeLater(()->modal[0].setVisible(true));
                if(adiar)SwingUtilities.invokeLater(()->filho[0].setVisible(true));
            });
            assertTrue(aberto.await(5,TimeUnit.SECONDS),"A tela modal precisa estar aberta");
            SwingUtilities.invokeAndWait(()->{
                popup[0]=new PopupLembrete(()->{confirmacoes.incrementAndGet();concluido.countDown();},
                    ()->{adiamentos.incrementAndGet();concluido.countDown();},ml->{volume.set(ml);return true;},
                    new DadosAgua.Opcoes(false,false,false),false,250,adiar);
                // O teste de modalidade não depende de consultas meteorológicas.
                for(WindowListener listener:popup[0].getWindowListeners())popup[0].removeWindowListener(listener);
                popup[0].setLocationRelativeTo(null);popup[0].setVisible(true);
                JButton botao=encontrar(popup[0],adiar?"Adiar":"Já bebi");assertNotNull(botao);
                clique[0]=botao.getLocationOnScreen();clique[0].translate(botao.getWidth()/2,botao.getHeight()/2);
            });
            robot.waitForIdle();robot.mouseMove(clique[0].x,clique[0].y);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            assertTrue(concluido.await(5,TimeUnit.SECONDS),"Clique real deve chegar ao alerta apesar das telas modais");
            SwingUtilities.invokeAndWait(()->{
                assertTrue(modal[0].isShowing(),"A tela original deve continuar aberta");
                if(adiar)assertTrue(filho[0].isShowing());
                assertFalse(popup[0].isDisplayable());
                assertEquals(adiar?0:1,confirmacoes.get());assertEquals(adiar?1:0,adiamentos.get());
                assertEquals(adiar?0:250,volume.get());
            });
        } finally {
            SwingUtilities.invokeAndWait(()->{if(popup[0]!=null)popup[0].dispose();if(filho[0]!=null)filho[0].dispose();if(modal[0]!=null)modal[0].dispose();if(dono[0]!=null)dono[0].dispose();});
            robot.mouseMove(mouse.x,mouse.y);
        }
    }
    private JButton encontrar(Container pai,String texto){
        for(Component c:pai.getComponents()){
            if(c instanceof JButton b&&texto.equals(b.getText()))return b;
            if(c instanceof Container filho){JButton b=encontrar(filho,texto);if(b!=null)return b;}
        }return null;
    }
}
