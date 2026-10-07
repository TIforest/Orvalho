package com.mycompany.bebaAgua;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
class PopupLembreteTest {
 @Test void somenteBotaoFechaAlerta() throws Exception {
  org.junit.jupiter.api.Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
  SwingUtilities.invokeAndWait(() -> {
   var confirmacoes=new java.util.concurrent.atomic.AtomicInteger();
   PopupLembrete p=new PopupLembrete(confirmacoes::incrementAndGet);
   try {
    p.pack();
    assertFalse(p.getFocusableWindowState());
    p.dispatchEvent(new WindowEvent(p,WindowEvent.WINDOW_CLOSING));
    assertTrue(p.isDisplayable(),"X não deve descartar o alerta");
    assertEquals(0,confirmacoes.get());
    JButton ok=encontrar(p);
    assertNotNull(ok);assertFalse(ok.isFocusable());
    ok.doClick(0);
    assertFalse(p.isDisplayable());
    assertEquals(1,confirmacoes.get());
    ok.doClick(0);assertEquals(1,confirmacoes.get());
   } finally {p.dispose();}
  });
 }
 private JButton encontrar(Container pai) {
  for(Component c:pai.getComponents()) {
   if(c instanceof JButton b && "Já bebi".equals(b.getText()))return b;
   if(c instanceof Container cont){JButton b=encontrar(cont);if(b!=null)return b;}
  }
  return null;
 }
}
