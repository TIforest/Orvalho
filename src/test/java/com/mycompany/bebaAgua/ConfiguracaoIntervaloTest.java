package com.mycompany.bebaAgua;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class ConfiguracaoIntervaloTest {
 @TempDir Path pasta;
 @Test void ultimoIntervaloSubstituiValorAntigo() throws Exception {
  Path arquivo=pasta.resolve("intervalo.txt");
  new ConfiguracaoIntervalo(arquivo).salvar(1);
  assertEquals(1,new ConfiguracaoIntervalo(arquivo).carregar(240));
  new ConfiguracaoIntervalo(arquivo).salvar(17);
  assertEquals(17,new ConfiguracaoIntervalo(arquivo).carregar(240));
 }
 @Test void migraSomenteNaPrimeiraExecucao() throws Exception {
  var c=new ConfiguracaoIntervalo(pasta.resolve("intervalo.txt"));
  assertEquals(15,c.carregar(15));
  assertEquals(15,c.carregar(240));
 }
 @Test void invalidoNaoApagaUltimaEscolha() throws Exception {
  var c=new ConfiguracaoIntervalo(pasta.resolve("intervalo.txt"));c.salvar(7);
  assertThrows(IllegalArgumentException.class,()->c.salvar(241));
  assertEquals(7,c.carregar(240));
 }
 @Test void erroDeGravacaoNaoPassaSilenciosamente() throws Exception {
  Path bloqueio=Files.writeString(pasta.resolve("nao-e-pasta"),"x");
  var c=new ConfiguracaoIntervalo(bloqueio.resolve("intervalo.txt"));
  assertThrows(java.io.IOException.class,()->c.salvar(1));
 }
 @Test void arquivoCorrompidoUsaPadraoValido() throws Exception {
  Path arquivo=Files.writeString(pasta.resolve("intervalo.txt"),"invalido");
  assertEquals(30,new ConfiguracaoIntervalo(arquivo).carregar(240));
 }
 @Test void gravaELeEmProcessosSeparados() throws Exception {
  Path arquivo=pasta.resolve("intervalo de João.txt");
  assertEquals("gravado",executar("salvar",arquivo,"1"));
  assertEquals("1",executar("ler",arquivo,"240"));
  assertEquals("gravado",executar("salvar",arquivo,"23"));
  assertEquals("23",executar("ler",arquivo,"240"));
 }
 private String executar(String modo,Path arquivo,String valor)throws Exception{
  String cp=Path.of(ConfiguracaoIntervalo.class.getProtectionDomain().getCodeSource().getLocation().toURI())
      +java.io.File.pathSeparator+Path.of(PersistenciaProcesso.class.getProtectionDomain().getCodeSource().getLocation().toURI());
  Process p=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java.exe").toString(),
      "-cp",cp,PersistenciaProcesso.class.getName(),modo,arquivo.toString(),valor).redirectErrorStream(true).start();
  if(!p.waitFor(10,TimeUnit.SECONDS)){p.destroyForcibly();fail("Processo não terminou");}
  String out=new String(p.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).strip();
  assertEquals(0,p.exitValue(),out);
  return out;
 }
}
class PersistenciaProcesso {
 public static void main(String[] args)throws Exception{
  var c=new ConfiguracaoIntervalo(Path.of(args[1]));
  if(args[0].equals("salvar")){c.salvar(Integer.parseInt(args[2]));System.out.print("gravado");}
  else System.out.print(c.carregar(Integer.parseInt(args[2])));
 }
}