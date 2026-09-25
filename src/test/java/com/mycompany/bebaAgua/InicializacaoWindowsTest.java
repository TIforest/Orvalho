package com.mycompany.bebaAgua;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class InicializacaoWindowsTest {
 @TempDir Path pasta;
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