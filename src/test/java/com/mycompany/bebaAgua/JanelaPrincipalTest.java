package com.mycompany.bebaAgua;
import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class JanelaPrincipalTest {
 @org.junit.jupiter.api.io.TempDir java.nio.file.Path pasta;
 @Test void confirmarPopupRealReiniciaMasTesteManualPreservaContagem() throws Exception {
  org.junit.jupiter.api.Assumptions.assumeTrue(SystemTray.isSupported());
  var config=new ConfiguracaoIntervalo(pasta.resolve("popup-intervalo.txt"));config.salvar(1);
  Set<TrayIcon> anteriores=Set.of(SystemTray.getSystemTray().getTrayIcons());
  SwingUtilities.invokeAndWait(()->{
   JanelaPrincipal f=new JanelaPrincipal(true,config,new DadosAgua(pasta));
   try {
    f.restaurarAposAtualizacao(1,20,true);
    achar(f,JButton.class,"Testar alerta").doClick(0);
    PopupLembrete teste=Arrays.stream(Window.getWindows()).filter(w->w instanceof PopupLembrete && w.isShowing())
            .map(w->(PopupLembrete)w).findFirst().orElseThrow();
    achar(teste,JButton.class,"Já bebi").doClick(0);
    assertTrue(f.lembreteService.getSegundosRestantes()<=20);
    assertTrue(f.lembreteService.getSegundosRestantes()>0);
    f.restaurarAlertaPendente();
    assertTrue(f.argumentosAposAtualizacao().contains("--aguardando-alerta"));
    PopupLembrete real=Arrays.stream(Window.getWindows()).filter(w->w instanceof PopupLembrete && w.isShowing())
            .map(w->(PopupLembrete)w).findFirst().orElseThrow();
    achar(real,JButton.class,"Já bebi").doClick(0);
    assertFalse(f.lembreteService.aguardandoConfirmacao());
    assertTrue(f.lembreteService.ativo());assertEquals(60,f.lembreteService.getSegundosRestantes());
   } finally {
    f.lembreteService.pausar();f.dispose();
    for(Window w:Window.getWindows())if(w instanceof PopupLembrete)w.dispose();
    for(TrayIcon i:SystemTray.getSystemTray().getTrayIcons())if(!anteriores.contains(i))SystemTray.getSystemTray().remove(i);
   }
  });
 }
 @Test void abreDaBandejaERetomaSemCriarOutraJanela() throws Exception {
  org.junit.jupiter.api.Assumptions.assumeTrue(SystemTray.isSupported());
  var janela=new AtomicReference<JanelaPrincipal>();
  var configuracao=new ConfiguracaoIntervalo(pasta.resolve("intervalo.txt"));
  configuracao.salvar(10);
  var prefs=java.util.prefs.Preferences.userNodeForPackage(JanelaPrincipal.class);
  String minutos=prefs.get("intervaloMinutos",null);
  Set<TrayIcon> anteriores=Set.of(SystemTray.getSystemTray().getTrayIcons());
  try {
   SwingUtilities.invokeAndWait(()-> {
    JanelaPrincipal f=new JanelaPrincipal(true,configuracao,new DadosAgua(pasta));janela.set(f);
    assertFalse(f.isVisible());
    assertTrue(f.lembreteService.getSegundosRestantes()>0);
    TrayIcon icone=Arrays.stream(SystemTray.getSystemTray().getTrayIcons())
       .filter(i->!anteriores.contains(i)).findFirst().orElseThrow();
    var origem=new java.awt.Canvas();
    for(MouseListener listener:icone.getMouseListeners()) {
     listener.mouseClicked(new MouseEvent(origem,MouseEvent.MOUSE_CLICKED,System.currentTimeMillis(),0,0,0,1,false,MouseEvent.BUTTON1));
     listener.mouseClicked(new MouseEvent(origem,MouseEvent.MOUSE_CLICKED,System.currentTimeMillis(),0,0,0,2,false,MouseEvent.BUTTON3));
    }
    assertFalse(f.isVisible(),"Clique simples e clique direito não abrem a janela");
    for(MouseListener listener:icone.getMouseListeners())listener.mouseClicked(new MouseEvent(origem,MouseEvent.MOUSE_CLICKED,System.currentTimeMillis(),0,0,0,2,false,MouseEvent.BUTTON1));
    assertTrue(f.isVisible(),"Duplo clique esquerdo abre a mesma janela");
    f.setVisible(false);
    for(ActionListener listener:icone.getActionListeners())listener.actionPerformed(new ActionEvent(icone,ActionEvent.ACTION_PERFORMED,"abrir"));
    assertTrue(f.isVisible(),"A ação nativa da bandeja também abre");
    JButton pausar=achar(f,JButton.class,"Pausar");
    pausar.doClick(0);
    long restante=f.lembreteService.getSegundosRestantes();
    assertTrue(restante>0);
    achar(f,JButton.class,"iniciarLembretes").doClick(0);
    assertTrue(f.lembreteService.getSegundosRestantes()<=restante);
    f.setVisible(true);f.setExtendedState(JFrame.ICONIFIED);
    MenuItem abrir=icone.getPopupMenu().getItem(0);
    for(ActionListener l:abrir.getActionListeners())l.actionPerformed(new ActionEvent(abrir,0,"Abrir"));
    assertTrue(f.isVisible());
    assertEquals(0,f.getExtendedState() & JFrame.ICONIFIED);
    f.dispatchEvent(new WindowEvent(f,WindowEvent.WINDOW_CLOSING));
    assertFalse(f.isVisible());
    assertTrue(f.lembreteService.getSegundosRestantes()>0);
   });
  } finally {
   SwingUtilities.invokeAndWait(()->{
    if(janela.get()!=null){janela.get().lembreteService.pausar();janela.get().dispose();}
    for(TrayIcon i:SystemTray.getSystemTray().getTrayIcons())if(!anteriores.contains(i))SystemTray.getSystemTray().remove(i);
    if(minutos==null)prefs.remove("intervaloMinutos");else prefs.put("intervaloMinutos",minutos);
   });
  }
 }
 @Test void salvaTextoDigitadoERecuperaAoIniciarAutomaticamente() throws Exception {
  org.junit.jupiter.api.Assumptions.assumeTrue(SystemTray.isSupported());
  var config=new ConfiguracaoIntervalo(pasta.resolve("intervalo.txt"));
  config.salvar(240);
  Set<TrayIcon> anteriores=Set.of(SystemTray.getSystemTray().getTrayIcons());
  SwingUtilities.invokeAndWait(()->{
   JanelaPrincipal primeira=null, segunda=null;
   try {
    primeira=new JanelaPrincipal(false,config,new DadosAgua(pasta));
    JSpinner spinner=spinner(primeira);
    ((JSpinner.NumberEditor)spinner.getEditor()).getTextField().setText("1");
    achar(primeira,JButton.class,"iniciarLembretes").doClick(0);
    assertEquals(1,spinner.getValue());
    try {assertEquals(1,config.carregar(240));}catch(Exception e){throw new RuntimeException(e);}
    primeira.lembreteService.pausar();primeira.dispose();
    segunda=new JanelaPrincipal(true,config,new DadosAgua(pasta));
    assertEquals(1,spinner(segunda).getValue());
    assertTrue(segunda.lembreteService.getSegundosRestantes()<=60);
    assertTrue(segunda.lembreteService.getSegundosRestantes()>0);
   } finally {
    if(primeira!=null){primeira.lembreteService.pausar();primeira.dispose();}
    if(segunda!=null){segunda.lembreteService.pausar();segunda.dispose();}
    for(TrayIcon i:SystemTray.getSystemTray().getTrayIcons())if(!anteriores.contains(i))SystemTray.getSystemTray().remove(i);
   }
  });
 }
 private JSpinner spinner(Container pai) {
  for(Component c:pai.getComponents()){
   if(c instanceof JSpinner s)return s;
   if(c instanceof Container cont){JSpinner s=spinner(cont);if(s!=null)return s;}
  }
  return null;
 }
 private <T extends Component>T achar(Container pai,Class<T> tipo,String texto) {
  for(Component c:pai.getComponents()){
   if(tipo.isInstance(c) && c instanceof JButton b && (texto.equals(b.getText()) || texto.equals(b.getName())))return tipo.cast(c);
   if(c instanceof Container filho){T t=achar(filho,tipo,texto);if(t!=null)return t;}
  }
  return null;
 }
}
