package com.mycompany.bebaAgua;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class MensagensLembreteTest {
    @Test void alternaSemRepetirNaTrocaDeCiclo() {
        String anterior = null;
        var vistas = new HashSet<String>();
        for (int i = 0; i < 80; i++) {
            String frase = MensagensLembrete.proxima();
            assertFalse(frase.isBlank());
            assertNotEquals(anterior, frase);
            vistas.add(frase);
            anterior = frase;
        }
        assertEquals(12, vistas.size());
    }
}
