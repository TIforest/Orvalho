package com.mycompany.bebaAgua;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Frases leves para os lembretes, alternadas durante esta execução. */
final class MensagensLembrete {
    private static final List<String> FRASES = List.of(
            "Um gole agora. A planilha espera.",
            "Sua garrafa também quer participar da reunião.",
            "Café tem seu lugar. Água também quer um espacinho.",
            "Pausa rápida: o teclado não vai fugir.",
            "Sua próxima tarefa: dar uma atenção à garrafa.",
            "A caixa de entrada pode esperar um gole.",
            "Um brinde às pequenas pausas. Com água, claro.",
            "Menos uma aba aberta, mais um gole dado.",
            "Sua garrafa não vai se esvaziar sozinha.",
            "Pausa rápida: reunião particular com a garrafa.",
            "Este lembrete poderia ser um e-mail. Ainda bem que não é.",
            "A garrafa está por perto. Falta só o convite.");
    private static final List<String> restantes = new ArrayList<>();
    private static String ultima;

    private MensagensLembrete() { }

    static synchronized String proxima() {
        if (restantes.isEmpty()) {
            restantes.addAll(FRASES);
            Collections.shuffle(restantes);
            // Evita repetição também na passagem de um ciclo para o próximo.
            if (restantes.getLast().equals(ultima)) {
                Collections.swap(restantes, 0, restantes.size() - 1);
            }
        }
        ultima = restantes.removeLast();
        return ultima;
    }
}
