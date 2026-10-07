package com.mycompany.bebaAgua;

import java.util.concurrent.atomic.AtomicLong;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GarrafaAnimadaTest {
 @Test void encheCongelaEsperaConfirmacaoEEsvaziaSuavemente() throws Exception {
  SwingUtilities.invokeAndWait(() -> {
   AnimacoesOrvalho.sistemaParaTeste(true);AnimacoesOrvalho.configurar(true);
   var tempo=new AtomicLong();
   var servico=new LembreteService(()->{},tempo::get);
   var garrafa=new GarrafaAnimada(servico,tempo::get);
   try {
    assertEquals(0.0,garrafa.nivelAgua());
    servico.start(1);tempo.set(30_000_000_000L);
    assertEquals(0.5,garrafa.nivelAgua(),0.000001);
    servico.pausar();tempo.addAndGet(100_000_000_000L);
    assertEquals(0.5,garrafa.nivelAgua(),0.000001);
    servico.start(1);tempo.addAndGet(30_000_000_000L);
    servico.restaurarAlertaPendente();
    assertEquals(1.0,garrafa.nivelAgua());
    tempo.addAndGet(60_000_000_000L);assertEquals(1.0,garrafa.nivelAgua());
    assertTrue(servico.confirmarAlerta());garrafa.esvaziar();
    assertEquals(1.0,garrafa.nivelAgua());
    tempo.addAndGet(325_000_000L);
    assertTrue(garrafa.nivelAgua()>0.49 && garrafa.nivelAgua()<0.52);
    tempo.addAndGet(325_000_000L);
    assertEquals(0.65/60,garrafa.nivelAgua(),0.000001);
   } finally {servico.pausar();AnimacoesOrvalho.sistemaParaTeste(null);}
  });
 }
}
