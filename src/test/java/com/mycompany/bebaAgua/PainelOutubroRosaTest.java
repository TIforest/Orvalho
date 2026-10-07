package com.mycompany.bebaAgua;
import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PainelOutubroRosaTest {
 @Test void seloAbreUmaJanelaEPermiteConfirmarLembrete()throws Exception{
  SwingUtilities.invokeAndWait(()->{
   TemaOrvalho.outubroRosa(true);AtomicInteger confirmou=new AtomicInteger();
   PopupLembrete alerta=new PopupLembrete(confirmou::incrementAndGet,()->{},ml->true,new DadosAgua.Opcoes(false,false,true,true),true,200,true);
   try{
    alerta.setVisible(true);JButton selo=NovosControlesTest.achar(alerta,JButton.class,"abrirOutubroRosa");assertNotNull(selo);selo.doClick(0);selo.doClick(0);
    var telas=Arrays.stream(alerta.getOwnedWindows()).filter(w->w.isShowing()&&"informacaoOutubroRosa".equals(w.getName())).toList();
    assertEquals(1,telas.size());assertFalse(((JDialog)telas.getFirst()).isModal());
    JButton fechar=NovosControlesTest.achar((Container)telas.getFirst(),JButton.class,"fecharOutubroRosa");assertNotNull(fechar);fechar.doClick(0);assertFalse(telas.getFirst().isDisplayable());
    selo.doClick(0);NovosControlesTest.achar(alerta,JButton.class,"Já bebi").doClick(0);assertEquals(1,confirmou.get());
    assertTrue(Arrays.stream(alerta.getOwnedWindows()).noneMatch(Window::isShowing));
   }finally{alerta.dispose();TemaOrvalho.outubroRosa(false);}
  });
 }
}
