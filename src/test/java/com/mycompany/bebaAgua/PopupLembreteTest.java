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
   PopupLembrete p=new PopupLembrete();
   try {
    p.pack();
    assertFalse(p.getFocusableWindowState());
    p.dispatchEvent(new WindowEvent(p,WindowEvent.WINDOW_CLOSING));
    assertTrue(p.isDisplayable(),"X não deve descartar o alerta");
    JButton ok=encontrar(p);
    assertNotNull(ok);assertFalse(ok.isFocusable());
    ok.doClick(0);
    assertFalse(p.isDisplayable());
   } finally {p.dispose();}
  });
 }
 private JButton encontrar(Container pai) {
  for(Component c:pai.getComponents()) {
   if(c instanceof JButton b && "Ok, obrigado!".equals(b.getText()))return b;
   if(c instanceof Container cont){JButton b=encontrar(cont);if(b!=null)return b;}
  }
  return null;
 }
}