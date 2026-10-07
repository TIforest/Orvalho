package com.mycompany.bebaAgua;
import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.Timer;
import static org.junit.jupiter.api.Assertions.*;

class LembreteServiceTest {
 @Test void nivelDaGarrafaAcompanhaFracoesPausaETrocaDeIntervalo() throws Exception {
  SwingUtilities.invokeAndWait(() -> {
   var tempo=new AtomicLong();
   var s=new LembreteService(() -> {},tempo::get);
   try {
    assertEquals(1.0,s.proporcaoRestante());
    s.start(1);tempo.addAndGet(30_500_000_000L);
    assertEquals(29.5/60,s.proporcaoRestante(),0.000001);
    s.pausar();tempo.addAndGet(90_000_000_000L);
    assertEquals(29.5/60,s.proporcaoRestante(),0.000001);
    s.start(1);assertEquals(29.5/60,s.proporcaoRestante(),0.000001);
    tempo.addAndGet(30_000_000_000L);assertEquals(0.0,s.proporcaoRestante());
    s.pausar();s.start(2);assertEquals(1.0,s.proporcaoRestante());
   } finally {s.pausar();}
  });
 }
 @Test void validaLimitesEPreservaContagemAoRejeitar() throws Exception {
  SwingUtilities.invokeAndWait(() -> {
   var tempo=new AtomicLong();
   var s=new LembreteService(() -> {},tempo::get);
   try {
    assertEquals(0,s.getSegundosRestantes());
    assertThrows(IllegalArgumentException.class,()->s.start(0));
    assertThrows(IllegalArgumentException.class,()->s.start(241));
    s.start(240); assertEquals(14400,s.getSegundosRestantes());
    assertThrows(IllegalArgumentException.class,()->s.start(-1));
    assertEquals(14400,s.getSegundosRestantes());
   } finally {s.pausar();}
  });
 }
 @Test void pausaCongelaERetomaSaldo() throws Exception {
  SwingUtilities.invokeAndWait(() -> {
   var tempo=new AtomicLong();
   var s=new LembreteService(() -> {},tempo::get);
   try {
    s.start(1);tempo.addAndGet(20_000_000_000L);
    assertEquals(40,s.getSegundosRestantes());
    s.pausar();tempo.addAndGet(90_000_000_000L);
    s.pausar();assertEquals(40,s.getSegundosRestantes());
    s.start(1);assertEquals(40,s.getSegundosRestantes());
    tempo.addAndGet(5_000_000_000L);assertEquals(35,s.getSegundosRestantes());
   } finally {s.pausar();}
  });
 }
 @Test void trocarIntervaloReinicia() throws Exception {
  SwingUtilities.invokeAndWait(() -> {
   var tempo=new AtomicLong();
   var s=new LembreteService(() -> {},tempo::get);
   try {
    s.start(1);tempo.addAndGet(20_000_000_000L);s.pausar();
    s.start(2);assertEquals(120,s.getSegundosRestantes());
   } finally {s.pausar();}
  });
 }
 @Test void aposRetomadaProximoCicloUsaIntervaloCompleto() throws Exception {
  var campo=LembreteService.class.getDeclaredField("temporizador");campo.setAccessible(true);
  SwingUtilities.invokeAndWait(() -> {
   var tempo=new AtomicLong();var alertas=new AtomicInteger();
   var s=new LembreteService(alertas::incrementAndGet,tempo::get);
   try {
    s.start(1);tempo.addAndGet(20_000_000_000L);s.pausar();s.start(1);
    Timer timer;
    try {timer=(Timer)campo.get(s);}catch(Exception e){throw new RuntimeException(e);}
    assertEquals(40000,timer.getInitialDelay());assertEquals(60000,timer.getDelay());
    tempo.addAndGet(40_000_000_000L);
    assertEquals(0.0,s.proporcaoRestante());
    timer.getActionListeners()[0].actionPerformed(new java.awt.event.ActionEvent(timer,0,"tick"));
    assertEquals(1,alertas.get());assertEquals(0,s.getSegundosRestantes());
    assertTrue(s.aguardandoConfirmacao());assertTrue(s.ativo());assertFalse(timer.isRunning());
    tempo.addAndGet(120_000_000_000L);
    timer.getActionListeners()[0].actionPerformed(new java.awt.event.ActionEvent(timer,0,"tick"));
    assertEquals(1,alertas.get());assertEquals(0,s.getSegundosRestantes());
    assertTrue(s.confirmarAlerta());
    assertFalse(s.confirmarAlerta());
    assertEquals(60,s.getSegundosRestantes());
    assertEquals(1.0,s.proporcaoRestante());
   } finally {s.pausar();}
  });
 }
 @Test void confirmarDurantePausaNaoReativaLembretes() throws Exception {
  SwingUtilities.invokeAndWait(() -> {
   var s=new LembreteService(()->{});
   try {
    s.start(15);s.restaurarAlertaPendente();s.pausar();
    assertTrue(s.confirmarAlerta());assertFalse(s.ativo());
    assertEquals(900,s.getSegundosRestantes());
    s.start(15);assertTrue(s.ativo());assertFalse(s.aguardandoConfirmacao());
   } finally {s.pausar();}
  });
 }
 @Test void retomarPendenteEsperaConfirmacaoEMudarIntervaloCancelaPendencia() throws Exception {
  SwingUtilities.invokeAndWait(() -> {
   var s=new LembreteService(()->{});
   try {
    s.start(15);s.restaurarAlertaPendente();s.pausar();s.start(15);
    assertTrue(s.ativo());assertTrue(s.aguardandoConfirmacao());assertEquals(0,s.getSegundosRestantes());
    s.pausar();s.start(20);
    assertFalse(s.confirmarAlerta());assertEquals(1200,s.getSegundosRestantes());
   } finally {s.pausar();}
  });
 }
}
