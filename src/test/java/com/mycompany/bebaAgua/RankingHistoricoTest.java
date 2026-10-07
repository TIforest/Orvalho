package com.mycompany.bebaAgua;
import org.junit.jupiter.api.Test;import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;import java.time.*;import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
class RankingHistoricoTest {
 @TempDir Path pasta;
 @Test void consultaDataEConfereResposta()throws Exception{
  var servico=new CompeticaoService(new DadosAgua(pasta),"https://teste.invalid",(m,r,c,t)->{
   assertEquals("/v1/ranking?format=2&day=2026-09-15",r);return "day=2026-09-15\n1\tAna\tTI\t300\t1";
  });
  var ranking=servico.consultarRanking(LocalDate.of(2026,9,15));assertEquals(300,ranking.participantes().getFirst().ml());assertEquals(LocalDate.of(2026,9,15),ranking.dia());
  var errado=new CompeticaoService(new DadosAgua(pasta),"https://teste.invalid",(m,r,c,t)->"day=2026-09-16");
  assertThrows(IOException.class,()->errado.consultarRanking(LocalDate.of(2026,9,15)));
 }
 @Test void rejeitaDataImpossivelEFutura(){
  var hoje=LocalDate.of(2026,10,2);assertEquals(LocalDate.of(2026,9,30),PainelRanking.validarDia("30/09/2026",hoje));
  for(String valor:new String[]{"31/09/2026","03/10/2026","29/02/2026","01/01/2025","abc"})assertThrows(IllegalArgumentException.class,()->PainelRanking.validarDia(valor,hoje));
 }
}
