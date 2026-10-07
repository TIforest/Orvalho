package com.mycompany.bebaAgua;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
class NovosControlesTest {
 static <T extends Component> T achar(Container c,Class<T> tipo,String texto){
  for(var x:c.getComponents()) {
   if(tipo.isInstance(x)&& ((x instanceof JButton b&&b.getText().startsWith(texto)) || texto.equals(x.getName())))return tipo.cast(x);
   if(x instanceof Container p){T r=achar(p,tipo,texto);if(r!=null)return r;}
  }return null;
 }
 @Test void adiarNaoRegistraEHumorPodeSerOcultado() throws Exception {
  SwingUtilities.invokeAndWait(()->{
   var registrados=new AtomicInteger();var adiados=new AtomicInteger();var confirmados=new AtomicInteger();
   var p=new PopupLembrete(confirmados::incrementAndGet,adiados::incrementAndGet,()->{registrados.incrementAndGet();return true;},new DadosAgua.Opcoes(true,false,false),true);
   try{
    assertFalse(achar(p,JLabel.class,"mensagemHumorada").isVisible());
    achar(p,JButton.class,"Daqui a 5").doClick(0);
    assertEquals(1,adiados.get());assertEquals(0,registrados.get());assertEquals(0,confirmados.get());
   }finally{p.dispose();}
  });
 }
 @Test void falhaAoRegistrarMantemAlertaAberto() throws Exception {
  SwingUtilities.invokeAndWait(()->{
   var confirmados=new AtomicInteger();
   var p=new PopupLembrete(confirmados::incrementAndGet,()->{},()->false,new DadosAgua.Opcoes(false,false,true),true);
   try {achar(p,JButton.class,"Já bebi").doClick(0);assertTrue(p.isDisplayable());assertEquals(0,confirmados.get());}
   finally{p.dispose();}
  });
 }
 @Test void registraUmaVezEConfirmaApenasAposGravar() throws Exception {
  SwingUtilities.invokeAndWait(()->{
   var registros=new AtomicInteger();var confirmacoes=new AtomicInteger();
   var p=new PopupLembrete(confirmacoes::incrementAndGet,()->{},()->{registros.incrementAndGet();return true;},new DadosAgua.Opcoes(false,false,true),true);
   try {
    var b=achar(p,JButton.class,"Já bebi");b.doClick(0);b.doClick(0);
    assertEquals(1,registros.get());assertEquals(1,confirmacoes.get());assertFalse(p.isDisplayable());
   }finally{p.dispose();}
  });
 }
 @Test void temaPodeVoltarAoClaroSemPerderCoresOriginais() throws Exception {
  SwingUtilities.invokeAndWait(()->{
   TemaOrvalho.outubroRosa(false);
   JPanel p=new JPanel();p.setBackground(new Color(0xF4F8FC));
   JLabel l=new JLabel("Texto");l.setForeground(new Color(0x12304A));p.add(l);
   TemaOrvalho.aplicar(p,true);assertNotEquals(new Color(0xF4F8FC),p.getBackground());
   TemaOrvalho.aplicar(p,false);assertEquals(new Color(0xF4F8FC),p.getBackground());assertEquals(new Color(0x12304A),l.getForeground());
  });
 }}
