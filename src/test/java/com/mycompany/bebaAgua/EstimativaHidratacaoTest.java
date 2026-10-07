package com.mycompany.bebaAgua;
import org.junit.jupiter.api.Test;import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;import static org.junit.jupiter.api.Assertions.*;
class EstimativaHidratacaoTest {
 @TempDir Path pasta;
 @Test void calculaFaixaComVirgulaOuPonto(){
  assertEquals("2,10 a 2,45 L/dia",EstimativaHidratacao.faixa(EstimativaHidratacao.peso("70")));
  assertEquals(EstimativaHidratacao.peso("70,5"),EstimativaHidratacao.peso("70.5"));
  for(String s:new String[]{"", "NaN", "Infinity", "-70", "0", "301", "1,234.5", "70kg"})assertThrows(IllegalArgumentException.class,()->EstimativaHidratacao.peso(s));
 }
 @Test void persisteEApagaSemAlterarPreferenciasOuHistorico()throws Exception{
  var dados=new DadosAgua(pasta);dados.salvar(new DadosAgua.Opcoes(true,true,true));
  assertEquals("",EstimativaHidratacao.carregar(dados));EstimativaHidratacao.salvar(dados,"70,5");
  assertEquals("70,5",EstimativaHidratacao.carregar(new DadosAgua(pasta)));
  assertThrows(IllegalArgumentException.class,()->EstimativaHidratacao.salvar(dados,"abc"));
  assertEquals("70,5",EstimativaHidratacao.carregar(dados));EstimativaHidratacao.apagar(dados);
  assertEquals("",EstimativaHidratacao.carregar(dados));assertTrue(dados.carregar().escuro());
 }
 @Test void pesoCorrompidoNaoViraEstimativa()throws Exception{
  Files.writeString(pasta.resolve("peso-kg.txt"),"NaN");
  assertThrows(java.io.IOException.class,()->EstimativaHidratacao.carregar(new DadosAgua(pasta)));
  assertEquals("NaN",Files.readString(pasta.resolve("peso-kg.txt")));
 }
}
