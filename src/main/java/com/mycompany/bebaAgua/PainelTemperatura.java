package com.mycompany.bebaAgua;
import java.awt.*;
import javax.swing.*;

/** Consulta assíncrona; o temporizador só existe enquanto o alerta está aberto. */
final class PainelTemperatura extends JPanel {
    private final JLabel valor=new JLabel("Consultando temperatura local…",SwingConstants.CENTER);
    private final JLabel fonte=new JLabel("Previsão meteorológica · MET Norway",SwingConstants.CENTER);
    private final TemperaturaService servico;
    private boolean consultando,ativo;
    private final Timer timer;
    PainelTemperatura(){this(TemperaturaService.PADRAO);}
    PainelTemperatura(TemperaturaService servico){
        this.servico=servico;setOpaque(false);setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));setAlignmentX(0.5f);
        valor.setName("temperaturaAtual");valor.setFont(new Font("Segoe UI",Font.BOLD,12));valor.setForeground(new Color(0x12304A));valor.setAlignmentX(0.5f);
        fonte.setFont(new Font("Segoe UI",Font.PLAIN,10));fonte.setForeground(new Color(0x52748E));fonte.setAlignmentX(0.5f);
        setToolTipText("<html>Estimativa externa para a cidade selecionada ou aproximada pela internet.<br>VPNs e redes corporativas podem indicar outra cidade.<br>Altere a cidade nas configurações.<br>Fonte: MET Norway — https://api.met.no/ · CC BY 4.0<br>Não mede a temperatura dentro da sala.</html>");
        valor.setToolTipText(getToolTipText());fonte.setToolTipText(getToolTipText());add(valor);add(Box.createVerticalStrut(3));add(fonte);
        setPreferredSize(new Dimension(320,34));setMaximumSize(new Dimension(320,34));
        timer=new Timer(300000,e->atualizar());
    }
    void iniciar(){if(LocalClima.opcao(new DadosAgua())==3){setVisible(false);return;}ativo=true;atualizar();timer.start();}
    void parar(){ativo=false;timer.stop();}
    private void atualizar(){
        if(consultando)return;consultando=true;
        new SwingWorker<java.util.Optional<TemperaturaService.Clima>,Void>(){
            protected java.util.Optional<TemperaturaService.Clima> doInBackground(){return servico.consultar();}
            protected void done(){consultando=false;if(!ativo)return;try{mostrar(get());}catch(Exception e){mostrar(java.util.Optional.empty());}}
        }.execute();
    }
    void mostrar(java.util.Optional<TemperaturaService.Clima> clima){
        valor.setText(clima.map(TemperaturaService.Clima::texto).orElse("Temperatura local indisponível"));
        fonte.setText(clima.map(TemperaturaService.Clima::fonte).orElse("Confira a cidade nas configurações."));
    }
}
