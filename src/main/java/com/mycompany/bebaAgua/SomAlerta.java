package com.mycompany.bebaAgua;

import java.io.IOException;
import javax.sound.sampled.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Áudio local, sem bloquear a interface nem depender de conexão com a internet. */
final class SomAlerta {
    private static final AtomicBoolean tocando = new AtomicBoolean();
    private SomAlerta() {}
    static AudioInputStream abrirGota() throws IOException, UnsupportedAudioFileException {
        var recurso = SomAlerta.class.getResource("/sons/gota.wav");
        if (recurso == null) throw new IOException("Som de gota ausente.");
        return AudioSystem.getAudioInputStream(recurso);
    }
    static void tocarGota() {
        if (!tocando.compareAndSet(false,true)) return;
        Thread audio = new Thread(() -> {
            try (AudioInputStream entrada = abrirGota(); Clip clip = AudioSystem.getClip()) {
                CountDownLatch terminou = new CountDownLatch(1);
                clip.addLineListener(e -> { if (e.getType() == LineEvent.Type.STOP) terminou.countDown(); });
                clip.open(entrada); clip.start();
                // Limite também libera o dispositivo se o driver não enviar STOP.
                terminou.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                // Ausência de saída de áudio não impede o lembrete visual.
                System.getLogger(SomAlerta.class.getName()).log(System.Logger.Level.DEBUG,"Áudio indisponível",e);
            } finally { tocando.set(false); }
        }, "orvalho-som-gota");
        audio.setDaemon(true); audio.start();
    }
}
