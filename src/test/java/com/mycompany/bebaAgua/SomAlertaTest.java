package com.mycompany.bebaAgua;

import org.junit.jupiter.api.Test;
import javax.sound.sampled.AudioFormat;
import static org.junit.jupiter.api.Assertions.*;

class SomAlertaTest {
    @Test void recursoEmpacotadoTemPcmCurtoSemSilencioOuSaturacao() throws Exception {
        try(var audio=SomAlerta.abrirGota()) {
            var formato=audio.getFormat();
            assertEquals(AudioFormat.Encoding.PCM_SIGNED,formato.getEncoding());
            assertEquals(16,formato.getSampleSizeInBits());assertEquals(1,formato.getChannels());assertFalse(formato.isBigEndian());
            double duracao=audio.getFrameLength()/formato.getFrameRate();assertTrue(duracao>=0.3&&duracao<=1);
            byte[] bytes=audio.readAllBytes();int pico=0;
            for(int i=0;i<bytes.length;i+=2){int valor=(short)((bytes[i]&255)|(bytes[i+1]<<8));pico=Math.max(pico,Math.abs(valor));}
            assertTrue(pico>1000&&pico<32000,"O som deve ser audível e sem saturação digital.");
            assertEquals(0,bytes[0]);assertEquals(0,bytes[1]);
        }
    }
}
