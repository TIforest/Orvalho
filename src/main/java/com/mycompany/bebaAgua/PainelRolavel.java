package com.mycompany.bebaAgua;
import java.awt.*;
import javax.swing.*;
/** A largura acompanha a janela; somente o conteúdo vertical pode rolar. */
final class PainelRolavel extends JPanel implements Scrollable {
    public Dimension getPreferredScrollableViewportSize(){return new Dimension(590,880);}
    public int getScrollableUnitIncrement(Rectangle r,int o,int d){return 24;}
    public int getScrollableBlockIncrement(Rectangle r,int o,int d){return Math.max(24,r.height-24);}
    public boolean getScrollableTracksViewportWidth(){return true;}
    public boolean getScrollableTracksViewportHeight(){return false;}
}
