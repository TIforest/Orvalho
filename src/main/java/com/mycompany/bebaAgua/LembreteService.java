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
    Runnable aoAlertar;
    
    
    
    public LembreteService(Runnable aoAlertar){
        temporizador = new Timer(5000, (e) -> {
            aoAlertar.run();
            
            
        });
        
    }
    public void start(int minutos){
       int milissegundos = minutos * 60 * 1000;
       temporizador.setInitialDelay(milissegundos);
       temporizador.setDelay(milissegundos);
       temporizador.restart();
    }
    
    public void pausar(){
        temporizador.stop();
    }
    
}
