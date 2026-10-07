package com.mycompany.bebaAgua;
import javax.swing.*;import org.junit.jupiter.api.Test;import java.awt.*;
import static org.junit.jupiter.api.Assertions.*;
class DialogosOrvalhoTest {
 @Test void confirmaCancelaEHerdaTemaDoDono()throws Exception{
  SwingUtilities.invokeAndWait(()->{
   for(boolean escuro:new boolean[]{false,true}){
    JFrame dono=new JFrame();((JComponent)dono.getContentPane()).putClientProperty("orvalho.escuro",escuro);
    int[] resultado={JOptionPane.CLOSED_OPTION};JDialog d=DialogosOrvalho.criar(dono,"Seu nome sairá do ranking.","Sair da competição",JOptionPane.QUESTION_MESSAGE,true,resultado);
    try{
     assertEquals(escuro,DialogosOrvalho.escuro(d));
     assertEquals("Cancelar",d.getRootPane().getDefaultButton().getText());
     d.getRootPane().getDefaultButton().doClick(0);assertEquals(JOptionPane.NO_OPTION,resultado[0]);
    }finally{d.dispose();}
    resultado[0]=JOptionPane.CLOSED_OPTION;d=DialogosOrvalho.criar(dono,"Confirma?","Orvalho",JOptionPane.QUESTION_MESSAGE,true,resultado);
    try{botao(d,"Confirmar").doClick(0);assertEquals(JOptionPane.YES_OPTION,resultado[0]);}finally{d.dispose();dono.dispose();}
   }
  });
 }
 @Test void erroLongoNaoAumentaJanelaAlemDaTelaETextoNaoViraHtml()throws Exception{
  SwingUtilities.invokeAndWait(()->{
   JPanel dono=new JPanel();dono.putClientProperty("orvalho.escuro",true);
   int[] resultado={JOptionPane.CLOSED_OPTION};JDialog d=DialogosOrvalho.criar(dono,"<html>Mensagem de erro\n".repeat(100),"Orvalho",JOptionPane.ERROR_MESSAGE,false,resultado);
   try{
    assertTrue(d.getHeight()<d.getGraphicsConfiguration().getBounds().height);
    assertEquals("OK",d.getRootPane().getDefaultButton().getText());
    d.getRootPane().getDefaultButton().doClick(0);assertEquals(JOptionPane.YES_OPTION,resultado[0]);
   }finally{d.dispose();}
  });
 }
 private JButton botao(Container c,String texto){for(Component f:c.getComponents()){if(f instanceof JButton b&&texto.equals(b.getText()))return b;if(f instanceof Container p){var b=botao(p,texto);if(b!=null)return b;}}return null;}
}
