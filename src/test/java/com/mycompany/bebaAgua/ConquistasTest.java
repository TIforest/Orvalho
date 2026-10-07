package com.mycompany.bebaAgua;
import org.junit.jupiter.api.Test;import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;import java.time.*;import java.util.*;import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
class ConquistasTest {
 @TempDir Path pasta;
 @Test void referenciaSoComAceiteEPesoFicaFixaEReenviaOffline()throws Exception{
  var d=new DadosAgua(pasta);EstimativaHidratacao.salvar(d,"70");
  var online=new java.util.concurrent.atomic.AtomicBoolean(true);var referencias=new ArrayList<Map<String,String>>();
  CompeticaoService.Transporte api=(m,r,c,t)->{
   if(r.endsWith("register"))return "id=abc&name=Ana&sector=TI";
   if(r.endsWith("reference")){referencias.add(c);if(!online.get())throw new IOException("offline");}
   return "ok";
  };
  var s=new CompeticaoService(d,"https://test.invalid",api);s.participar("Ana","TI","senha");s.sincronizar();assertTrue(referencias.isEmpty());
  s.compartilharConquistas(true);online.set(false);assertThrows(IOException.class,s::sincronizar);
  EstimativaHidratacao.salvar(d,"90");online.set(true);
  s=new CompeticaoService(d,"https://test.invalid",api);s.sincronizar();s.sincronizar();
  assertEquals(2,referencias.size());assertEquals("2450",referencias.get(0).get("target_ml"));assertEquals(referencias.get(0),referencias.get(1));
  assertFalse(referencias.get(0).containsKey("peso"));s.compartilharConquistas(false);assertFalse(s.compartilhaConquistas());
 }
 @Test void interpretaMesDatasEIdentidadeSemConfundirHomonimos()throws Exception{
  var d=new DadosAgua(pasta);var mes=YearMonth.of(2026,1);
  var s=new CompeticaoService(d,"https://test.invalid",(m,r,c,t)->"month=2026-01\nday=2026-02-01\n1\tAna\tTI\t2026-01-02,2026-01-03\t1\n1\tAna\tTI\t2026-01-04,2026-01-05\t0");
  var resultado=s.consultarConquistas(mes);assertEquals(2,resultado.pessoas().size());assertTrue(resultado.pessoas().getFirst().proprio());assertFalse(resultado.pessoas().getLast().proprio());assertEquals(2,resultado.pessoas().getFirst().dias().size());
  for(String dates:List.of("2026-02-01","2026-01-02,2026-01-02","nao-data")){
   var invalido=new CompeticaoService(d,"https://test.invalid",(m,r,c,t)->"month=2026-01\nday=2026-02-01\n1\tAna\tTI\t"+dates+"\t1");
   assertThrows(IOException.class,()->invalido.consultarConquistas(mes));
  }
 }
}
