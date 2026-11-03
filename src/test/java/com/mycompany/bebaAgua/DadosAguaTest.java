package com.mycompany.bebaAgua;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
class DadosAguaTest {
 @TempDir Path pasta;
 @Test void persistePreferenciasEPausasPorDia() throws Exception {
  var d=new DadosAgua(pasta);var hoje=LocalDate.of(2026,9,30);
  d.salvar(new DadosAgua.Opcoes(true,true,false));
  assertEquals(new DadosAgua.Opcoes(true,true,false),new DadosAgua(pasta).carregar());
  d.registrar(hoje.atTime(10,20));d.registrar(hoje.atTime(11,0));d.registrar(hoje.plusDays(1).atStartOfDay());
  assertEquals(2,new DadosAgua(pasta).historico(hoje).size());
  assertEquals(1,d.historico(hoje.plusDays(1)).size());
  assertTrue(d.historico(hoje.minusDays(1)).isEmpty());
 }
 @Test void naoSobrescreveHistoricoCorrompido() throws Exception {
  var dia=LocalDate.of(2026,9,30);var arquivo=pasta.resolve("pausas-"+dia+".tsv");
  Files.writeString(arquivo,"corrompido");
  assertThrows(java.io.IOException.class,()->new DadosAgua(pasta).registrar(dia.atStartOfDay()));
  assertEquals("corrompido",Files.readString(arquivo));
 }
 @Test void desfazSomenteRegistroEscolhidoMesmoComHorariosIguais() throws Exception {
  var d=new DadosAgua(pasta);var hora=LocalDateTime.of(2026,9,30,10,0);var dia=hora.toLocalDate();
  d.registrar(hora);d.registrar(hora);var lista=d.historico(dia);
  assertTrue(d.desfazer(dia,lista.getFirst().id()));assertFalse(d.desfazer(dia,lista.getFirst().id()));
  assertEquals(lista.getLast(),new DadosAgua(pasta).historico(dia).getFirst());
 }
 @Test void migraRegistrosAntigosSemReimportarPausaDesfeita() throws Exception {
  var dia=LocalDate.of(2026,9,30);var f=pasta.resolve("consumo-"+dia+".tsv");
  String antigo="2026-09-30T10:00\t200\n";Files.writeString(f,antigo);
  Files.writeString(pasta.resolve("preferencias.properties"),"escuro=true\nsom=false\nhumor=true\nvolume=350\n");
  var d=new DadosAgua(pasta);assertFalse(d.carregar().outubroRosa());assertFalse(d.carregar().escuro());
  var registro=d.historico(dia).getFirst();assertTrue(d.desfazer(dia,registro.id()));
  assertTrue(new DadosAgua(pasta).historico(dia).isEmpty());assertEquals(antigo,Files.readString(f));
 }
}
