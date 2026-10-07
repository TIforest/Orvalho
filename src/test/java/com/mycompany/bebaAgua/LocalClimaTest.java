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
  for(int i=0;i<4;i++){LocalClima.salvar(dados,i);assertEquals(i,LocalClima.opcao(dados));}
  var sp=LocalClima.interpretar("city=S%C3%A3o+Paulo&lat=-23.55&lon=-46.63");
  assertEquals("São Paulo",sp.cidade());assertTrue(sp.url().endsWith("lat=-23.55&lon=-46.63"));
  assertTrue(new TemperaturaService.Clima(20,java.time.Instant.now(),sp.cidade()).texto().startsWith("São Paulo"));
 }
 @Test void rejeitaLocalizacaoAusenteOuInvalida(){
  for(String s:new String[]{"","city=X&lat=NaN&lon=0","city=X&lat=91&lon=0","city=X&lat=0&lon=181","city=%0A&lat=0&lon=0"})assertThrows(IOException.class,()->LocalClima.interpretar(s));
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
