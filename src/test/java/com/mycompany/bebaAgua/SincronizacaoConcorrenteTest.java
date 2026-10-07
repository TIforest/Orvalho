package com.mycompany.bebaAgua;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
class SincronizacaoConcorrenteTest {
 @TempDir Path pasta;
 @Test void sincronizacoesNaoDisputamConfirmacaoECancelamento()throws Exception {
  var entrou=new CountDownLatch(1);var liberar=new CountDownLatch(1);var chamadas=new AtomicInteger();
  var dados=new DadosAgua(pasta);
  var c=new CompeticaoService(dados,"https://teste.invalid",(m,r,f,t)->{
   if(r.endsWith("register"))return "id=abc&name=Ana&sector=TI";
   chamadas.incrementAndGet();entrou.countDown();
   try{if(!liberar.await(5,TimeUnit.SECONDS))throw new java.io.IOException("Timeout do teste");}
   catch(InterruptedException e){Thread.currentThread().interrupt();throw new java.io.IOException(e);}
   return "ok";
  });
  c.participar("Ana","TI","senha");dados.registrar(LocalDateTime.now(),250,true);
  try(var executor=Executors.newSingleThreadExecutor()) {
   var primeiro=executor.submit(()->{c.sincronizar();return true;});
   try {
    assertTrue(entrou.await(5,TimeUnit.SECONDS));
    assertThrows(java.io.IOException.class,c::sincronizar);
    assertTrue(c.participa());
    var evento=dados.eventosCompeticao().getFirst();dados.desfazer(LocalDateTime.now().toLocalDate(),evento.id());
   }finally{liberar.countDown();}
   assertTrue(primeiro.get(5,TimeUnit.SECONDS));
   c.sincronizar();c.sincronizar();assertEquals(2,chamadas.get());
  }
 }
}
