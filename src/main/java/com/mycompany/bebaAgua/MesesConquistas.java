package com.mycompany.bebaAgua;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

final class MesesConquistas {
    private static final YearMonth INICIO=YearMonth.of(2026,10);
    static List<YearMonth> disponiveis(Clock relogio){
        YearMonth atual=YearMonth.now(relogio.withZone(ZoneId.of("America/Sao_Paulo")));
        List<YearMonth> meses=new ArrayList<>();
        for(YearMonth mes=atual;!mes.isBefore(INICIO);mes=mes.minusMonths(1))meses.add(mes);
        return List.copyOf(meses);
    }
    static String nome(YearMonth mes){return mes.format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy",Locale.forLanguageTag("pt-BR")));}
}
