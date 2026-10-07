/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.bebaAgua;

import java.io.IOException;
import java.nio.file.Path;
import javax.swing.SwingUtilities;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;

/**
 *
 *
 * @author bruno|
 */
public class Main {
    private static FileChannel canalTrava;
    private static FileLock trava;

    public static void main(String [] args){
    // Deve ser definido antes da primeira inicialização da rede do Java.
    if (System.getProperty("java.net.useSystemProxies") == null)
        System.setProperty("java.net.useSystemProxies", "true");
    if (java.util.Arrays.asList(args).contains("--instalar-atualizacao")) {
        InstaladorAtualizacao.executar();
        return;
    }
    String pastaUsuario = System.getProperty("user.home");
    TipografiaOrvalho.iniciar();

    Path caminhoTrava = Path.of(pastaUsuario, ".orvalho.lock");
    try{
    canalTrava = FileChannel.open(caminhoTrava, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    trava = canalTrava.tryLock() ;
    if(trava == null){
        SwingUtilities.invokeLater(() -> DialogosOrvalho.showMessageDialog(null,
                "O Orvalho já está aberto. Use Abrir no ícone perto do relógio.",
                "Orvalho", javax.swing.JOptionPane.INFORMATION_MESSAGE));
        canalTrava.close();
        return;
    }

    }catch(IOException e){
        SwingUtilities.invokeLater(() -> DialogosOrvalho.showMessageDialog(null,
                "Não foi possível iniciar: " + e.getMessage(), "Orvalho", javax.swing.JOptionPane.ERROR_MESSAGE));
        return;
    }



    SwingUtilities.invokeLater(() -> {
    boolean inicioAutomatico = java.util.Arrays.asList(args).contains("--iniciar-com-windows");
    JanelaPrincipal frame = new JanelaPrincipal(inicioAutomatico);
    frame.setTitle("Orvalho - " + Atualizacao.versaoAtual());

    // A janela já foi dimensionada de acordo com a área útil do monitor.
    frame.setLocationRelativeTo(null);
    frame.setVisible(!inicioAutomatico || !frame.temBandeja());
    var argumentos = java.util.Arrays.asList(args);
    if (argumentos.contains("--atualizado")) {
        try {
            int minutos = Integer.parseInt(argumentos.stream().filter(a -> a.startsWith("--intervalo="))
                    .findFirst().orElse("--intervalo=0").substring(12));
            long segundos = Long.parseLong(argumentos.stream().filter(a -> a.startsWith("--restante="))
                    .findFirst().orElse("--restante=0").substring(11));
            frame.restaurarAposAtualizacao(minutos, segundos, inicioAutomatico);
            if (argumentos.contains("--aguardando-alerta") && minutos > 0) frame.restaurarAlertaPendente();
        } catch (IllegalArgumentException ignored) { }
        frame.setVisible(!argumentos.contains("--oculto") || !frame.temBandeja());
    }
    Atualizacao.iniciar(frame);
    });


  }
}
