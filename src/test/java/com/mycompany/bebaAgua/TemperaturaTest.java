package com.mycompany.bebaAgua;
import org.junit.jupiter.api.Test;import java.time.*;import java.io.*;import java.util.concurrent.atomic.AtomicInteger;import static org.junit.jupiter.api.Assertions.*;
class TemperaturaTest {
 static final Instant AGORA=Instant.parse("2026-10-01T15:10:00Z");
 static String json(String time,String temp){return "{\"properties\":{\"meta\":{\"units\":{\"air_temperature\":\"celsius\"}},\"timeseries\":[{\"time\":\""+time+"\",\"data\":{\"instant\":{\"details\":{\"air_temperature\":"+temp+"}}}}]}}";}
 static final class Relogio extends Clock {Instant agora=AGORA;public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId z){return this;}public Instant instant(){return agora;}}
 @Test void temperaturaValidaComHorarioDeBrasilia()throws Exception{
  var c=TemperaturaService.interpretar(json("2026-10-01T15:00:00Z","23.4"),AGORA);
  assertEquals("Telêmaco Borba · 23,4 °C",c.texto());assertTrue(c.fonte().endsWith("12:00"));
  assertEquals(-2,TemperaturaService.interpretar(json("2026-10-01T15:00:00Z","-2"),AGORA).celsius());
 }
 @Test void rejeitaDadosAntigosUnidadeErradaAusentesEImpossiveis(){
  for(String s:new String[]{json("2026-10-01T10:00:00Z","23"),json("2026-10-01T15:00:00Z","null"),json("2026-10-01T15:00:00Z","99"),json("2026-10-01T15:00:00Z","23").replace("celsius","fahrenheit"),"{}"})assertThrows(IOException.class,()->TemperaturaService.interpretar(s,AGORA));
 }
 @Test void naoExibeDiaAnteriorNaViradaDeBrasilia(){
  assertThrows(IOException.class,()->TemperaturaService.interpretar(json("2026-10-01T02:59:00Z","23"),Instant.parse("2026-10-01T03:01:00Z")));
 }
 @Test void cacheRespeitaExpiresERevalidaComLastModified(){
  var clock=new Relogio();var chamadas=new AtomicInteger();
  var service=new TemperaturaService(modificado->{int n=chamadas.incrementAndGet();if(n==1){assertEquals("",modificado);return new TemperaturaService.Resposta(200,json("2026-10-01T15:00:00Z","23"),"Wed, 01 Oct 2026 15:00:00 GMT",AGORA.plusSeconds(3600));}assertEquals("Wed, 01 Oct 2026 15:00:00 GMT",modificado);return new TemperaturaService.Resposta(304,"","",null);},clock);
  assertTrue(service.consultar().isPresent());service.consultar();clock.agora=AGORA.plusSeconds(1801);service.consultar();assertEquals(1,chamadas.get());
  clock.agora=AGORA.plusSeconds(3601);assertTrue(service.consultar().isEmpty());assertEquals(2,chamadas.get());
 }
 @Test void falhaDeRedeELimiteNaoImpedemLembreteNemRepetemConsultas(){
  var clock=new Relogio();var chamadas=new AtomicInteger();
  var service=new TemperaturaService(m->{chamadas.incrementAndGet();throw new IOException("offline");},clock);
  assertTrue(service.consultar().isEmpty());assertTrue(service.consultar().isEmpty());assertEquals(1,chamadas.get());
  var limitado=new TemperaturaService(m->{chamadas.incrementAndGet();return new TemperaturaService.Resposta(429,"","",null);},clock);
  limitado.consultar();clock.agora=AGORA.plusSeconds(1801);limitado.consultar();assertEquals(2,chamadas.get());
 }
}
