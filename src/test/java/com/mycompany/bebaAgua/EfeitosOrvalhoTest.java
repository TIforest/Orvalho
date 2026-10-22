package com.mycompany.bebaAgua;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class EfeitosOrvalhoTest {
 @TempDir Path pasta;
 @Test void removeVarianteVibranteEPreservaPreferenciasEntreReinicios()throws Exception{
  DadosAgua d=new DadosAgua(pasta);
  Files.writeString(pasta.resolve("preferencias.properties"),"escuro=false\nsom=true\nhumor=false\noutubroRosa=true\nrosaVibrante=true\nefeitos=false\ncampanhaOutubroRosa2026Aplicada=true\n");
  var o=new DadosAgua.Opcoes(false,true,false,true,false);assertEquals(o,d.carregar());d.salvar(o);
  assertEquals(o,new DadosAgua(pasta).carregar());
  assertFalse(Files.readString(pasta.resolve("preferencias.properties")).contains("rosaVibrante"));
  d.salvar(new DadosAgua.Opcoes(true,true,false,false,true));assertTrue(d.carregar().escuro());assertFalse(d.carregar().outubroRosa());
 }
 @Test void petalasNaoCapturamEventosETimerParaAoOcultarEDescartar()throws Exception{
  SwingUtilities.invokeAndWait(()->{
   JFrame f=new JFrame();try{
    TemaOrvalho.outubroRosa(true);AnimacoesOrvalho.sistemaParaTeste(true);AnimacoesOrvalho.configurar(true);
    PetalasOrvalho p=new PetalasOrvalho();f.setGlassPane(p);p.setVisible(true);f.setSize(600,600);f.setVisible(true);
    assertEquals(12,p.quantidade());assertFalse(p.contains(10,10));assertFalse(p.isFocusable());assertTrue(AnimacoesOrvalho.rodando());
    for(int i=0;i<10000;i++)p.avancar(.04);assertEquals(12,p.quantidade());
    f.setSize(460,500);f.validate();assertEquals(8,p.quantidade());
    AnimacoesOrvalho.sistemaParaTeste(false);assertFalse(AnimacoesOrvalho.rodando());
    AnimacoesOrvalho.sistemaParaTeste(true);assertTrue(AnimacoesOrvalho.rodando());
    f.setVisible(false);assertFalse(AnimacoesOrvalho.rodando());
    f.setVisible(true);assertTrue(AnimacoesOrvalho.rodando());
    TemaOrvalho.outubroRosa(false);AnimacoesOrvalho.reavaliar();assertFalse(AnimacoesOrvalho.rodando());
   }finally{f.dispose();AnimacoesOrvalho.sistemaParaTeste(null);TemaOrvalho.outubroRosa(false);}
   assertEquals(0,AnimacoesOrvalho.quantidadeAtiva());assertFalse(AnimacoesOrvalho.rodando());
  });
 }
 @Test void cliqueETecladoAtravessamCamadaEJanelaEstreitaMantemControles()throws Exception{
  var ref=new AtomicReference<JanelaPrincipal>();var ponto=new AtomicReference<Point>();
  DadosAgua d=new DadosAgua(pasta);d.salvar(new DadosAgua.Opcoes(false,false,true,true,true));
  Robot robot=new Robot();robot.setAutoDelay(50);
  try{
   SwingUtilities.invokeAndWait(()->{
    AnimacoesOrvalho.sistemaParaTeste(true);
    JanelaPrincipal f=new JanelaPrincipal(false,new ConfiguracaoIntervalo(pasta.resolve("intervalo.txt")),d);ref.set(f);f.setSize(460,650);f.setLocation(80,80);f.setAlwaysOnTop(true);f.setVisible(true);f.toFront();f.requestFocus();f.validate();
    assertNull(achar(f,JToggleButton.class,"controleEfeitos"));
    var conteudo=achar(f,JPanel.class,"conteudoPrincipal");var scroll=(JScrollPane)SwingUtilities.getAncestorOfClass(JScrollPane.class,conteudo);
    assertFalse(scroll.getHorizontalScrollBar().isVisible());assertEquals(scroll.getViewport().getExtentSize().width,conteudo.getWidth());
    for(String nome:new String[]{"Iniciar","Pausar","Testar alerta","abrirHistorico","abrirConfiguracoes"}){
     JButton b=achar(f,JButton.class,nome);assertNotNull(b);Rectangle r=SwingUtilities.convertRectangle(b.getParent(),b.getBounds(),conteudo);assertTrue(r.x>=0&&r.x+r.width<=conteudo.getWidth(),nome);
    }
   });
   robot.waitForIdle();robot.delay(500);
   SwingUtilities.invokeAndWait(()->{var b=achar(ref.get(),JButton.class,"Iniciar");Point p=b.getLocationOnScreen();p.translate(b.getWidth()/2,b.getHeight()/2);ponto.set(p);});
   robot.mouseMove(ponto.get().x,ponto.get().y);robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);robot.waitForIdle();
   SwingUtilities.invokeAndWait(()->{
    assertTrue(ref.get().lembreteService.ativo());
    assertTrue(ref.get().lembreteService.getSegundosRestantes()>0);
    achar(ref.get(),JButton.class,"Pausar").requestFocusInWindow();
   });
   robot.waitForIdle();robot.keyPress(KeyEvent.VK_SPACE);robot.keyRelease(KeyEvent.VK_SPACE);robot.waitForIdle();
   SwingUtilities.invokeAndWait(()->{
    var f=ref.get();assertFalse(f.lembreteService.ativo());
    f.setVisible(false);assertEquals(0,AnimacoesOrvalho.quantidadeAtiva());
   });
  }finally{SwingUtilities.invokeAndWait(()->{if(ref.get()!=null){ref.get().lembreteService.pausar();ref.get().dispose();}AnimacoesOrvalho.sistemaParaTeste(null);TemaOrvalho.outubroRosa(false);AnimacoesOrvalho.configurar(true);});}
 }
 private static <T extends Component>T achar(Container c,Class<T> tipo,String nome){
  for(Component f:c.getComponents()){
   if(tipo.isInstance(f)&&(nome.equals(f.getName())||f instanceof AbstractButton b&&nome.equals(b.getText())))return tipo.cast(f);
   if(f instanceof Container filho){T achado=achar(filho,tipo,nome);if(achado!=null)return achado;}
  }return null;
 }
}
