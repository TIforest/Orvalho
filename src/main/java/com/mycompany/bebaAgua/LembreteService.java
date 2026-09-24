/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.bebaAgua;
import javax.swing.Timer;
/**
 *
 * @author bruno
 */
public class LembreteService {
    private Timer temporizador;
    private long proximoAlertaNanos;
    Runnable aoAlertar;
    
    
    
    public LembreteService(Runnable aoAlertar){
        temporizador = new Timer(5000, (e) -> {
            // Prepara a contagem do próximo intervalo antes de exibir o alerta.
            proximoAlertaNanos = System.nanoTime()
                    + ((Timer) e.getSource()).getDelay() * 1_000_000L;
            aoAlertar.run();
            
            
        });
        
    }
    public void start(int minutos){
       if(minutos < 1 || minutos > 240){
           throw new IllegalArgumentException("O valor é menor que 1 ou maior que 240!");
       }
       int milissegundos = minutos * 60 * 1000;
       temporizador.setInitialDelay(milissegundos);
       temporizador.setDelay(milissegundos);
       proximoAlertaNanos = System.nanoTime() + milissegundos * 1_000_000L;
       temporizador.restart();
    }
    
    public void pausar(){
        temporizador.stop();
    }

    // Calcula pelo tempo decorrido, sem depender da frequência de atualização da tela.
    public long getSegundosRestantes(){
        if (!temporizador.isRunning()) {
            return 0;
        }
        long nanosRestantes = proximoAlertaNanos - System.nanoTime();
        return Math.max(0, (nanosRestantes + 999_999_999L) / 1_000_000_000L);
    }
    
}
