package com.mycompany.bebaAgua;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import static org.junit.jupiter.api.Assertions.*;
class AdiamentoTest {
 @Test void tempoEscolhidoSobreviveAtualizacaoERetornaAoIntervaloHabitual() throws Exception {
  SwingUtilities.invokeAndWait(()->{
   var s=new LembreteService(()->{},()->0L);
   var restaurado=new LembreteService(()->{},()->0L);
   try {
    s.start(10);s.restaurarAlertaPendente();
    assertThrows(IllegalArgumentException.class,()->s.adiarAlerta(0));
    assertThrows(IllegalArgumentException.class,()->s.adiarAlerta(241));
    assertTrue(s.aguardandoConfirmacao());
    assertTrue(s.adiarAlerta(45));
    assertEquals(2700,s.getSegundosRestantes());
    assertEquals(10,s.intervalo());
    assertFalse(s.adiarAlerta(5));
    restaurado.restaurar(10,s.getSegundosRestantes(),true);
    assertEquals(2700,restaurado.getSegundosRestantes());
    restaurado.restaurarAlertaPendente();restaurado.confirmarAlerta();
    assertEquals(600,restaurado.getSegundosRestantes());
   } finally {s.pausar();restaurado.pausar();}
  });
 }
 @Test void alertaEntregaMinutosDigitadosSemRegistrarAgua() throws Exception {
  SwingUtilities.invokeAndWait(()->{
   var minutos=new java.util.concurrent.atomic.AtomicInteger();
   var p=new PopupLembrete(()->fail("Não deve confirmar"),minutos::set,
     ml->{fail("Não deve registrar água");return false;},
     new DadosAgua.Opcoes(false,false,false),true,200,true);
   try {
    var campo=NovosControlesTest.achar(p,JSpinner.class,"minutosAdiamento");
    ((JSpinner.DefaultEditor)campo.getEditor()).getTextField().setText("45");
    NovosControlesTest.achar(p,JButton.class,"Adiar").doClick(0);
    assertEquals(45,minutos.get());
    assertFalse(p.isDisplayable());
   } finally {p.dispose();}
  });
 }
 @Test void adiaCincoMinutosEPreservaPausaEAtualizacao() throws Exception {
  SwingUtilities.invokeAndWait(()->{
   var s=new LembreteService(()->{});var r=new LembreteService(()->{});
   try {
    s.start(1);s.restaurarAlertaPendente();s.pausar();
    assertTrue(s.adiarAlerta());assertFalse(s.ativo());assertFalse(s.aguardandoConfirmacao());
    assertEquals(300,s.getSegundosRestantes());assertFalse(s.adiarAlerta());
    r.restaurar(1,s.getSegundosRestantes(),true);
    assertTrue(r.getSegundosRestantes()>295);
    r.restaurarAlertaPendente();assertTrue(r.confirmarAlerta());
    assertEquals(60,r.getSegundosRestantes());
   }finally{s.pausar();r.pausar();}
  });
 }
}
