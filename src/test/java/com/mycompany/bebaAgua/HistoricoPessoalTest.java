package com.mycompany.bebaAgua;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class HistoricoPessoalTest {
 @TempDir Path pasta;
 @Test void diasIncluemHojeLegadosENovosSemDuplicar()throws Exception{
  var d=new DadosAgua(pasta);var ontem=LocalDate.now().minusDays(1);var antigo=ontem.minusDays(3);
  d.registrar(ontem.atTime(9,0),200,false);
  Files.writeString(pasta.resolve("consumo-"+ontem+".tsv"),ontem+"T08:00\t100\n");
  Files.writeString(pasta.resolve("consumo-"+antigo+".tsv"),antigo+"T08:00\t100\n");
  Files.writeString(pasta.resolve("pausas-2026-99-99.tsv"),"ignorar");
  assertEquals(List.of(LocalDate.now(),ontem,antigo),d.diasHistorico());
 }
 @Test void correcaoPreservaHorarioEContagemEDeixaCancelamentoParaSincronizar()throws Exception{
  var d=new DadosAgua(pasta);var hora=LocalDateTime.now().minusDays(1);d.registrar(hora,200,true);
  var original=d.historico(hora.toLocalDate()).getFirst();
  assertTrue(d.corrigir(hora.toLocalDate(),original.id(),350));
  var salvo=new DadosAgua(pasta);var atual=salvo.historico(hora.toLocalDate());assertEquals(1,atual.size());
  assertEquals(350,atual.getFirst().ml());assertEquals(original.instante(),atual.getFirst().instante());assertEquals(hora,atual.getFirst().hora());
  assertNotEquals(original.id(),atual.getFirst().id());
  var fila=salvo.eventosCompeticao();assertEquals(2,fila.size());assertTrue(fila.getFirst().cancelada());assertFalse(fila.getLast().cancelada());
  assertFalse(d.corrigir(hora.toLocalDate(),atual.getFirst().id(),350));
  assertThrows(IllegalArgumentException.class,()->d.corrigir(hora.toLocalDate(),original.id(),400));
 }
 @Test void correcaoNaoPromoveRegistroPessoalParaCompeticao()throws Exception{
  var d=new DadosAgua(pasta);var hora=LocalDateTime.now().minusDays(30);d.registrar(hora);
  d.corrigir(hora.toLocalDate(),d.historico(hora.toLocalDate()).getFirst().id(),250);
  assertEquals(250,d.historico(hora.toLocalDate()).getFirst().ml());assertTrue(d.eventosCompeticao().isEmpty());
 }
 @Test void prazoEVolumeInvalidosNaoAlteramArquivos()throws Exception{
  var d=new DadosAgua(pasta);var hora=LocalDateTime.now().minusDays(8);d.registrar(hora,200,true);
  var p=d.historico(hora.toLocalDate()).getFirst();var file=pasta.resolve("pausas-"+hora.toLocalDate()+".tsv");var antes=Files.readString(file);
  assertThrows(IllegalArgumentException.class,()->d.corrigir(hora.toLocalDate(),p.id(),300));
  for(int ml:new int[]{0,9,2001})assertThrows(IllegalArgumentException.class,()->d.corrigir(hora.toLocalDate(),p.id(),ml));
  assertEquals(antes,Files.readString(file));
 }
 @Test void correcaoOfflineReenviaCancelamentoESubstituicaoSemDuplicar()throws Exception{
  var d=new DadosAgua(pasta);var eventos=new LinkedHashMap<String,Integer>();var cancelados=new HashSet<String>();var online=new java.util.concurrent.atomic.AtomicBoolean(true);
  CompeticaoService.Transporte api=(m,r,f,t)->{
   if(r.endsWith("register"))return "id=teste&name=Teste&sector=TI";
   if(!online.get())throw new java.io.IOException("offline");
   if(f.get("canceled").equals("1"))cancelados.add(f.get("id"));
   eventos.put(f.get("id"),Integer.parseInt(f.get("ml")));return "ok";
  };
  var c=new CompeticaoService(d,"https://teste.invalid",api);c.participar("Teste","TI","senha de teste");
  var hora=LocalDateTime.now();d.registrar(hora,200,true);c.sincronizar();online.set(false);
  d.corrigir(hora.toLocalDate(),d.historico(hora.toLocalDate()).getFirst().id(),350);
  assertThrows(java.io.IOException.class,c::sincronizar);
  online.set(true);var reaberto=new CompeticaoService(new DadosAgua(pasta),"https://teste.invalid",api);reaberto.sincronizar();reaberto.sincronizar();
  assertEquals(2,eventos.size());assertEquals(350,eventos.entrySet().stream().filter(e->!cancelados.contains(e.getKey())).mapToInt(Map.Entry::getValue).sum());
 }
}
