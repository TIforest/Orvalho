package com.mycompany.bebaAgua;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;import java.time.*;import static org.junit.jupiter.api.Assertions.*;
class CompeticaoTest {
 @TempDir Path pasta;
 @Test void volumesECancelamentosPersistemSemIncluirPausasAntigas()throws Exception{
  var d=new DadosAgua(pasta);var hora=LocalDateTime.now();
  d.registrar(hora);d.registrar(hora.plusSeconds(1),150,false);d.registrar(hora.plusSeconds(2),300,true);
  assertEquals(1,d.eventosCompeticao().size());var evento=d.eventosCompeticao().getFirst();
  assertEquals(300,evento.ml());assertFalse(evento.cancelada());
  d.desfazer(hora.toLocalDate(),evento.id());
  assertEquals(2,d.historico(hora.toLocalDate()).size());
  assertTrue(new DadosAgua(pasta).eventosCompeticao().getFirst().cancelada());
  d.registrar(hora.plusSeconds(3),200,true);assertEquals(2,d.eventosCompeticao().size());
 }
 @Test void limitesEPreferenciaDeQuantidade()throws Exception{
  var d=new DadosAgua(pasta);var c=new CompeticaoService(d);
  assertFalse(c.participa());assertEquals(200,c.ultimoVolume());c.volume(350);
  assertEquals(350,new CompeticaoService(d).ultimoVolume());
  assertThrows(IllegalArgumentException.class,()->d.registrar(LocalDateTime.now(),0,true));
  assertThrows(IllegalArgumentException.class,()->d.registrar(LocalDateTime.now(),2001,true));
  assertEquals("José & Ana",CompeticaoService.parseForm("name=Jos%C3%A9+%26+Ana").get("name"));
 }
 @Test void popupEntregaQuantidadeAoConfirmar()throws Exception{
  javax.swing.SwingUtilities.invokeAndWait(()->{
   var registrado=new java.util.concurrent.atomic.AtomicInteger();
   var p=new PopupLembrete(()->{},()->{},ml->{registrado.set(ml);return true;},new DadosAgua.Opcoes(false,false,true),true,350,true);
   try{NovosControlesTest.achar(p,javax.swing.JButton.class,"Já bebi").doClick(0);assertEquals(350,registrado.get());}
   finally{p.dispose();}
  });
 }
 @Test void filaOfflineReenviaMesmoIdECancelamentoCorrigeServidor()throws Exception{
  var d=new DadosAgua(pasta);
  var online=new java.util.concurrent.atomic.AtomicBoolean(true);
  var ids=new java.util.ArrayList<String>();var estados=new java.util.ArrayList<String>();
  CompeticaoService.Transporte api=(metodo,rota,campos,token)->{
   if(rota.endsWith("register"))return "id=abc&name=Ana&sector=TI";
   assertFalse(token.isBlank());ids.add(campos.get("id"));estados.add(campos.get("canceled"));
   if(!online.get())throw new java.io.IOException("Sem conexão");return "ok";
  };
  var c=new CompeticaoService(d,"https://teste.invalid",api);c.participar("Ana","TI","a".repeat(64));
  assertTrue(c.participa());var hora=LocalDateTime.now();d.registrar(hora,250,true);
  online.set(false);assertThrows(java.io.IOException.class,c::sincronizar);
  online.set(true);c=new CompeticaoService(d,"https://teste.invalid",api);assertTrue(c.participa());c.sincronizar();c.sincronizar();
  assertEquals(2,ids.size());assertEquals(ids.get(0),ids.get(1));
  d.desfazer(hora.toLocalDate(),ids.get(0));c.sincronizar();assertEquals("1",estados.getLast());
 }
 @Test void cadastroInterrompidoReutilizaCredencialSemAtivarAntesDaConfirmacao()throws Exception{
  var d=new DadosAgua(pasta);var tokens=new java.util.ArrayList<String>();
  CompeticaoService.Transporte falha=(m,r,f,t)->{tokens.add(f.get("token"));throw new java.io.IOException("Resposta perdida");};
  var c=new CompeticaoService(d,"https://teste.invalid",falha);
  assertThrows(java.io.IOException.class,()->c.participar("Ana","TI","a".repeat(64)));assertFalse(c.participa());
  var outro=new CompeticaoService(d,"https://teste.invalid",(m,r,f,t)->{tokens.add(f.get("token"));return "id=abc&name=Ana&sector=TI";});
  outro.participar("Ana","TI","a".repeat(64));assertEquals(tokens.getFirst(),tokens.getLast());assertTrue(outro.participa());
 }
 @Test void senhaPreservaMaiusculasENaoFicaNoDisco()throws Exception{
  var d=new DadosAgua(pasta);String senha="Senha Compartilhada!";
  var c=new CompeticaoService(d,"https://teste.invalid",(m,r,f,t)->{
   assertEquals(senha,f.get("password"));assertFalse(f.containsKey("invite"));
   return "id=abc&name=Ana&sector=TI";
  });
  c.participar("Ana","TI",senha);
  assertFalse(Files.readString(pasta.resolve("competicao.properties")).contains(senha));
  assertTrue(new CompeticaoService(d).participa());
 }
 @Test void listaEstruturadaPreservaEmpatesNomesEValores()throws Exception{
  var c=new CompeticaoService(new DadosAgua(pasta),"https://teste.invalid",(m,r,f,t)->
      "day=2026-09-30\n1\tAna%20Silva\tProdu%C3%A7%C3%A3o\t350\n1\tBruno\tTI\t350");
  var ranking=c.consultarRanking();assertEquals(LocalDate.of(2026,9,30),ranking.dia());
  assertEquals(2,ranking.participantes().size());assertEquals(1,ranking.participantes().get(1).posicao());
  assertEquals("Ana Silva",ranking.participantes().getFirst().nome());
  assertEquals("Produção",ranking.participantes().getFirst().setor());assertEquals(350,ranking.participantes().getFirst().ml());
  assertTrue(c.ranking().contains("0,35 L"));
 }
 @Test void rankingMalformadoNaoViraListaValida()throws Exception{
  for(String payload:java.util.List.of("day=ontem", "day=2026-09-30\n1\tAna\tTI\t-1")){
   var c=new CompeticaoService(new DadosAgua(pasta),"https://teste.invalid",(m,r,f,t)->payload);
   assertThrows(java.io.IOException.class,c::consultarRanking);
  }
 }
 @Test void rankingPersonalizadoNaoConfundeHomonimos()throws Exception{
  var c=new CompeticaoService(new DadosAgua(pasta),"https://teste.invalid",(m,r,f,t)->{
   assertEquals("/v1/ranking?format=2",r);
   return "day=2026-09-30\n1\tAna\tTI\t500\t0\n2\tAna\tTI\t250\t1";
  });
  var ranking=c.consultarRanking();
  assertFalse(ranking.participantes().getFirst().proprio());
  assertTrue(ranking.participantes().getLast().proprio());
  assertEquals(250,ranking.participantes().getLast().ml());
 }
}
