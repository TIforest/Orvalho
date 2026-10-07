package com.mycompany.bebaAgua;
import org.junit.jupiter.api.Test;import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;import java.awt.*;import java.time.*;import javax.swing.*;
import static org.junit.jupiter.api.Assertions.*;
class PainelConquistasTest {
 @TempDir Path pasta;
 @Test void primeiraAberturaSemSelecaoInicialCriaTelaNosDoisTemas()throws Exception{
  var servico=new CompeticaoService(new DadosAgua(pasta),"https://teste.invalid",(m,r,c,t)->{throw new AssertionError("Construção não deve consultar a rede");});
  SwingUtilities.invokeAndWait(()->{
   for(boolean escuro:new boolean[]{false,true}){
    var painel=new PainelConquistas(servico,escuro);
    JComboBox<?> meses=encontrar(painel);
    var esperados=MesesConquistas.disponiveis(Clock.systemUTC());
    assertNotNull(meses);assertEquals(esperados.size(),meses.getItemCount());
    if(!esperados.isEmpty())assertEquals(esperados.getFirst(),meses.getSelectedItem());
    for(int i=0;i<meses.getItemCount();i++)assertFalse(((YearMonth)meses.getItemAt(i)).isBefore(YearMonth.of(2026,10)));
   }
  });
 }
 private static JComboBox<?> encontrar(Container pai){
  for(Component c:pai.getComponents()){
   if(c instanceof JComboBox<?> combo)return combo;
   if(c instanceof Container filho){var resultado=encontrar(filho);if(resultado!=null)return resultado;}
  }return null;
 }
}
