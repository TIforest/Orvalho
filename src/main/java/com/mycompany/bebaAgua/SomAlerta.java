package com.mycompany.bebaAgua;

import java.io.IOException;
import javax.sound.sampled.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Áudio local, sem bloquear a interface nem depender de conexão com a internet. */
final class SomAlerta {
    private static final AtomicBoolean tocando = new AtomicBoolean();
    private SomAlerta() {}
    static void tocarFimExercicio() {
        Thread audio=new Thread(()->{
            try(var entrada=AudioSystem.getAudioInputStream(SomAlerta.class.getResource("/alongamento/fim-exercicio.wav"));
                Clip clip=AudioSystem.getClip()){
                var terminou=new CountDownLatch(1);
                clip.addLineListener(e->{if(e.getType()==LineEvent.Type.STOP)terminou.countDown();});
                clip.open(entrada);clip.start();terminou.await(1,TimeUnit.SECONDS);
            }catch(InterruptedException e){Thread.currentThread().interrupt();}
            catch(Exception e){System.getLogger(SomAlerta.class.getName()).log(System.Logger.Level.DEBUG,"Sinal de fim indisponível",e);}
        },"orvalho-fim-exercicio");audio.setDaemon(true);audio.start();
    }
    static void tocarAlongamento() {
        Thread audio=new Thread(()->{
            // Duas notas suaves distinguem a pausa da gota do lembrete de água.
            int taxa=22050;byte[] pcm=new byte[taxa*2];
            for(int i=0;i<taxa;i++){
                double t=i/(double)taxa,local=t<.45?t:t-.45;
                double amplitude=Math.min(1,local/.02)*Math.exp(-local*7)*.22;
                short valor=(short)(Math.sin(2*Math.PI*(t<.45?659.25:880)*local)*amplitude*32767);
                pcm[i*2]=(byte)valor;pcm[i*2+1]=(byte)(valor>>8);
            }
            try(Clip clip=AudioSystem.getClip()){
                clip.open(new AudioFormat(taxa,16,1,true,false),pcm,0,pcm.length);clip.start();
                Thread.sleep(1100);
            }catch(InterruptedException e){Thread.currentThread().interrupt();}
            catch(Exception e){System.getLogger(SomAlerta.class.getName()).log(System.Logger.Level.DEBUG,"Áudio de alongamento indisponível",e);}
        },"orvalho-som-alongamento");audio.setDaemon(true);audio.start();
    }
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
