package com.mycompany.bebaAgua;
import java.awt.*;
import java.nio.file.*;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class TemaOutubroRosaTest {
 @TempDir Path pasta;
 @AfterEach void normal(){TemaOrvalho.outubroRosa(false);}
 @Test void atualizacaoRestauraAzulUmaVezEPreservaEscolhaPosterior() throws Exception {
  Files.writeString(pasta.resolve("preferencias.properties"),"escuro=false\nsom=true\nhumor=false\noutubroRosa=true\ntemaRosaPadrao126Aplicado=true\ntemaAzulPadrao122Aplicado=true\nefeitos=false\ncampanhaOutubroRosa2026Aplicada=true\n");
  DadosAgua d=new DadosAgua(pasta);assertEquals(new DadosAgua.Opcoes(false,true,false,false,false),d.carregar());
  assertEquals(d.carregar(),new DadosAgua(pasta).carregar());
  d.salvar(new DadosAgua.Opcoes(false,true,false,true,false));
  assertTrue(new DadosAgua(pasta).carregar().outubroRosa());assertFalse(d.carregar().escuro());
  assertFalse(new DadosAgua(pasta).carregar().efeitos());
  d.salvar(new DadosAgua.Opcoes(true,true,false));assertFalse(d.carregar().outubroRosa());assertTrue(d.carregar().escuro());
 }
 @Test void instalacaoNovaComecaAzulELegadoMantemDemaisPreferencias() throws Exception {
  DadosAgua d=new DadosAgua(pasta);assertEquals(new DadosAgua.Opcoes(false,false,true),d.carregar());
  Files.writeString(pasta.resolve("preferencias.properties"),"escuro=true\nsom=true\nhumor=false\noutubroRosa=false\nvolume=350\n");
  assertEquals(new DadosAgua.Opcoes(false,true,false),d.carregar());
  assertTrue(Files.readString(pasta.resolve("preferencias.properties")).contains("volume=350"));
  d.salvar(new DadosAgua.Opcoes(false,true,false));
  assertFalse(new DadosAgua(pasta).carregar().outubroRosa());
 }
 @Test void alternarTemasAplicaRosaNaAguaERestauraAzulOriginal() throws Exception {
  SwingUtilities.invokeAndWait(()->{
   JPanel painel=new JPanel();painel.setBackground(new Color(0xF4F8FC));
   JButton b=new JButton("Salvar");BotaoOrvalho.aplicar(b,true);painel.add(b);
   JLabel agua=new JLabel("1,20 L");agua.setForeground(new Color(0x176BDA));painel.add(agua);
   var borda=BorderFactory.createLineBorder(new Color(0xD7E4F1));painel.setBorder(borda);
   JButton selo=TemaOrvalho.selo();painel.add(selo);
   TemaOrvalho.aplicar(painel,false);Color original=b.getBackground();
   TemaOrvalho.outubroRosa(true);TemaOrvalho.aplicar(painel,false);
   assertEquals(new Color(0xFFF7FA),painel.getBackground());assertEquals(new Color(0xC73C75),b.getBackground());assertEquals(Color.WHITE,b.getForeground());assertTrue(selo.isVisible());assertEquals(new Color(0xC73C75),agua.getForeground());
   TemaOrvalho.outubroRosa(false);TemaOrvalho.aplicar(painel,true);assertEquals(new Color(0x102131),painel.getBackground());assertFalse(selo.isVisible());
   TemaOrvalho.aplicar(painel,false);assertEquals(new Color(0xF4F8FC),painel.getBackground());assertEquals(original,b.getBackground());assertEquals(new Color(0x176BDA),agua.getForeground());assertSame(borda,painel.getBorder());
  });
 }
 @Test void configuracoesSalvamTemaEContinuamEmJanelaSeparada() throws Exception {
  SwingUtilities.invokeAndWait(()->{
   JFrame f=new JFrame();DadosAgua dados=new DadosAgua(pasta);
   try{
    PainelAgua p=new PainelAgua(f,dados,()->TemaOrvalho.aplicar(f.getContentPane(),false),new JCheckBox(),()->{});
    f.setContentPane(p);f.pack();
    for(int indice:new int[]{1,0}){
     java.util.concurrent.atomic.AtomicReference<Throwable> falha=new java.util.concurrent.atomic.AtomicReference<>();
     Timer timer=new Timer(100,e->{
      for(Window w:f.getOwnedWindows())if(w instanceof JDialog d&&d.isShowing()){
       try{assertEquals("Configurações",d.getTitle());assertSame(p,f.getContentPane());
        JComboBox<?> tema=(JComboBox<?>)achar(d,"temaAplicativo");assertNotNull(tema);assertEquals(2,tema.getItemCount());assertEquals("Tema original escuro",tema.getItemAt(1));assertEquals("Tema original",tema.getItemAt(0));
        assertEquals(p.opcoes().escuro()?1:0,tema.getSelectedIndex());tema.setSelectedIndex(indice);
        JCheckBox efeitos=(JCheckBox)achar(d,"efeitosConfiguracoes");assertNotNull(efeitos);efeitos.setSelected(indice==0);
        ((JButton)achar(d,"Salvar")).doClick(0);
       }catch(Throwable ex){falha.set(ex);d.dispose();}
      }
     });timer.setRepeats(false);timer.start();((JButton)achar(f,"abrirConfiguracoes")).doClick(0);
     if(falha.get()!=null)throw new AssertionError(falha.get());
     assertFalse(dados.carregar().outubroRosa());assertEquals(indice==1,dados.carregar().escuro());
     assertEquals(indice==0,new DadosAgua(pasta).carregar().efeitos());
    }
   }catch(java.io.IOException e){throw new RuntimeException(e);}finally{f.dispose();}
  });
 }
 private static Component achar(Container c,String nome){
  for(Component f:c.getComponents()){
   if(nome.equals(f.getName())||(f instanceof JButton b&&nome.equals(b.getText())))return f;
   if(f instanceof Container filho){Component achado=achar(filho,nome);if(achado!=null)return achado;}
  }return null;
 }
}
