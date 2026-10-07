package com.mycompany.bebaAgua;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import static org.junit.jupiter.api.Assertions.*;
class AdiamentoTest {
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
