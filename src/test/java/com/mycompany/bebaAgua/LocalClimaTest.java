package com.mycompany.bebaAgua;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
class LocalClimaTest {
 @TempDir Path pasta;
 @Test void preferenciasLocaisECoordenadas()throws Exception{
  var dados=new DadosAgua(pasta);assertEquals(0,LocalClima.opcao(dados));
  assertEquals(2,LocalClima.OPCOES.length);
  for(int i=0;i<2;i++){LocalClima.salvar(dados,i);assertEquals(i,LocalClima.opcao(dados));}
  var sp=LocalClima.consultar(dados);
  assertEquals("São Paulo",sp.cidade());assertTrue(sp.url().endsWith("lat=-23.55&lon=-46.63"));
  assertTrue(new TemperaturaService.Clima(20,java.time.Instant.now(),sp.cidade()).texto().startsWith("São Paulo"));
 }
 @Test void migraPreferenciasAnterioresSemTrocarCidades() throws Exception {
  var dados=new DadosAgua(pasta);
  assertEquals(LocalClima.TB,LocalClima.consultar(dados));
  for(String valor:new String[]{"0","1","3","-1","99","inválido",""}){
   java.nio.file.Files.writeString(pasta.resolve("cidade-clima.txt"),valor);
   assertEquals(LocalClima.TB,LocalClima.consultar(dados));
  }
  java.nio.file.Files.writeString(pasta.resolve("cidade-clima.txt"),"2");
  assertEquals(LocalClima.SP,LocalClima.consultar(dados));
  LocalClima.salvar(dados,0);assertEquals(LocalClima.TB,LocalClima.consultar(dados));
  assertThrows(IllegalArgumentException.class,()->LocalClima.salvar(dados,2));
 }
 @Test void trocaDeCidadeInvalidaCacheESemLocalNaoUsaTelemaco(){
  var lugar=new java.util.concurrent.atomic.AtomicReference<>(LocalClima.TB);
  var chamadas=new java.util.concurrent.atomic.AtomicInteger();
  var servico=new TemperaturaService((m,u)->{
   chamadas.incrementAndGet();assertEquals("",m);assertEquals(lugar.get().url(),u);
   return new TemperaturaService.Resposta(200,TemperaturaTest.json("2026-10-01T15:00:00Z","23"),"last",TemperaturaTest.AGORA.plusSeconds(7200));
  },new TemperaturaTest.Relogio(),()->{if(lugar.get()==null)throw new IOException();return lugar.get();});
  assertEquals("Telêmaco Borba",servico.consultar().orElseThrow().cidade());
  servico.consultar();assertEquals(1,chamadas.get());
  lugar.set(LocalClima.SP);assertEquals("São Paulo",servico.consultar().orElseThrow().cidade());assertEquals(2,chamadas.get());
  lugar.set(null);assertTrue(servico.consultar().isEmpty());
 }
}
