package com.mycompany.bebaAgua;
import org.junit.jupiter.api.Test;
import java.time.*;import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class MesesConquistasTest {
    private List<YearMonth> meses(String instante){return MesesConquistas.disponiveis(Clock.fixed(Instant.parse(instante),ZoneOffset.UTC));}
    @Test void limitaInicioEMudaSomenteNaViradaEmBrasilia(){
        assertTrue(meses("2026-10-01T02:59:59Z").isEmpty());
        assertEquals(List.of(YearMonth.of(2026,10)),meses("2026-11-01T02:59:59Z"));
        assertEquals(List.of(YearMonth.of(2026,11),YearMonth.of(2026,10)),meses("2026-11-01T03:00:00Z"));
        assertEquals(List.of(YearMonth.of(2027,1),YearMonth.of(2026,12),YearMonth.of(2026,11),YearMonth.of(2026,10)),meses("2027-01-01T03:00:00Z"));
    }
    @Test void nomesEmPortuguesComAno(){
        assertEquals("outubro de 2026",MesesConquistas.nome(YearMonth.of(2026,10)));
        assertEquals("novembro de 2026",MesesConquistas.nome(YearMonth.of(2026,11)));
        assertEquals("março de 2027",MesesConquistas.nome(YearMonth.of(2027,3)));
    }
}
