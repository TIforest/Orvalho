package com.mycompany.bebaAgua;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;
class CanalAtualizacaoTest {
 @TempDir Path pasta;
 @Test void padraoEmpresaEPilotoExplicito()throws Exception {
  assertEquals("empresa",CanalAtualizacao.ler(pasta));
  Files.writeString(pasta.resolve("canal-atualizacao.txt"),"piloto\r\n");
  assertEquals("piloto",CanalAtualizacao.ler(pasta));
  var p=new Properties();p.setProperty("manifest","https://empresa.invalid/latest");p.setProperty("manifestPilot","https://piloto.invalid/latest");
  assertEquals("piloto.invalid",CanalAtualizacao.manifesto(p,"piloto").getHost());
  assertEquals("empresa.invalid",CanalAtualizacao.manifesto(p,"empresa").getHost());
 }
 @Test void erroNaoRetornaAoCanalEmpresaSilenciosamente()throws Exception {
  Files.writeString(pasta.resolve("canal-atualizacao.txt"),"pilot");
  assertThrows(IOException.class,()->CanalAtualizacao.ler(pasta));
  assertThrows(IOException.class,()->CanalAtualizacao.manifesto(new Properties(),"piloto"));
  var p=new Properties();p.setProperty("manifestPilot","http://teste.invalid/latest");
  assertThrows(IOException.class,()->CanalAtualizacao.manifesto(p,"piloto"));
 }
 @Test void janelasEmUsoAdiamAtualizacao()throws Exception {
  javax.swing.SwingUtilities.invokeAndWait(()->{
   var frame=new javax.swing.JFrame();var sobre=new javax.swing.JDialog(frame);var editar=new javax.swing.JDialog(sobre);
   try {
    assertFalse(JanelaPrincipal.temDialogoEmUso(frame));
    sobre.setName("sobreAtualizacoes");sobre.setSize(100,100);sobre.setVisible(true);
    assertFalse(JanelaPrincipal.temDialogoEmUso(frame));
    editar.setSize(100,100);editar.setVisible(true);
    assertTrue(JanelaPrincipal.temDialogoEmUso(frame));
    editar.dispose();assertFalse(JanelaPrincipal.temDialogoEmUso(frame));
   }finally{editar.dispose();sobre.dispose();frame.dispose();}
  });
 }
}
