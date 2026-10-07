package com.mycompany.bebaAgua;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class InicializacaoWindowsTest {
 @Test void leExportacaoUnicodeSemConfundirOutrasEntradas() throws Exception {
  String comando="\"C:\\Users\\João Silva\\Orvalho.exe\" --iniciar-com-windows";
  String escapado=comando.replace("\\","\\\\").replace("\"","\\\"");
  String reg="Windows Registry Editor Version 5.00\n\"Outro\"=\"valor\"\n\"Orvalho\"=\""+escapado+"\"\n";
  assertEquals(comando,InicializacaoWindows.lerComando(reg));
  assertNull(InicializacaoWindows.lerComando("\"Outro\"=\"valor\""));
 }
 @TempDir Path pasta;
 @Test void utilitarioNativoPreservaAspasEAcentos() throws Exception {
  org.junit.jupiter.api.Assumptions.assumeTrue(System.getProperty("os.name").startsWith("Windows"));
  var executar=InicializacaoWindows.class.getDeclaredMethod("executar",String[].class);
  executar.setAccessible(true);
  String chave="HKCU\\Software\\OrvalhoTeste-"+java.util.UUID.randomUUID();
  String comando=comandoTeste();
  Path exportacao=pasta.resolve("teste.reg");
  boolean criada=false;
  try {
   InicializacaoWindows.gravarComando(chave,comando); criada=true;
   executar.invoke(null,(Object)new String[]{"export",chave,exportacao.toString(),"/y"});
   assertEquals(comando,InicializacaoWindows.lerComando(Files.readString(exportacao,java.nio.charset.StandardCharsets.UTF_16)));
  } finally {
   if(criada)executar.invoke(null,(Object)new String[]{"delete",chave,"/f"});
  }
 }
 private String comandoTeste() { return "\"C:\\Usuários\\João Silva\\Orvalho.exe\" --iniciar-com-windows"; }
 @Test void registroAusente() {
  assertEquals(InicializacaoWindows.Estado.DESATIVADA,InicializacaoWindows.avaliar(null,null));
 }
 @Test void executavelValidoComEspacosEAcentos() throws Exception {
  Path exe=Files.createFile(pasta.resolve("Orvalho João.exe"));
  assertEquals(InicializacaoWindows.Estado.ATIVA,
    InicializacaoWindows.avaliar(InicializacaoWindows.comandoPara(exe),exe));
 }
 @Test void executavelMovidoOuRemovido() throws Exception {
  Path antigo=Files.createFile(pasta.resolve("antigo.exe")), atual=Files.createFile(pasta.resolve("atual.exe"));
  String registro=InicializacaoWindows.comandoPara(antigo);
  assertEquals(InicializacaoWindows.Estado.CAMINHO_INVALIDO,InicializacaoWindows.avaliar(registro,atual));
  Files.delete(antigo);
  assertEquals(InicializacaoWindows.Estado.CAMINHO_INVALIDO,InicializacaoWindows.avaliar(registro,null));
 }
 @Test void registroIncompletoOuMalformado() {
  for(String valor:new String[]{"", "Orvalho.exe", "\"C:\\app.exe\"", "\"C:\\app.exe\" --outro"}) {
   assertEquals(InicializacaoWindows.Estado.CAMINHO_INVALIDO,InicializacaoWindows.avaliar(valor,null));
  }
 }
}
