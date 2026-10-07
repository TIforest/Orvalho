package com.mycompany.bebaAgua;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class ProgressoHidratacaoTest {
    @TempDir Path pasta;
    @Test void registraCorrigeDesfazEReiniciaNoNovoDia() throws Exception {
        var dados = new DadosAgua(pasta);
        var dia = LocalDate.of(2026,10,1);
        EstimativaHidratacao.salvar(dados,"70");
        dados.registrar(dia.atTime(9,0),1000,false);
        dados.registrar(dia.atTime(10,0),1450,true);
        dados.registrar(dia.atTime(11,0),0,false);
        assertEquals(1,ProgressoHidratacao.carregar(dados,dia).fracao());
        var id = dados.historico(dia).get(0).id();
        dados.corrigir(dia,id,500);
        assertEquals(1950,ProgressoHidratacao.carregar(dados,dia).ml());
        var ativo = dados.historico(dia).stream().filter(p->p.ml()==500).findFirst().orElseThrow();
        dados.desfazer(dia,ativo.id());
        assertEquals(1450.0/2450,ProgressoHidratacao.carregar(dados,dia).fracao(),1e-9);
        assertEquals(0,ProgressoHidratacao.carregar(dados,dia.plusDays(1)).ml());
        assertEquals(1450,ProgressoHidratacao.carregar(new DadosAgua(pasta),dia).ml());
    }
    @Test void pesoAusenteAlteradoEConsumoAcimaDaReferencia() throws Exception {
        var dados = new DadosAgua(pasta); var dia=LocalDate.now();
        dados.registrar(dia.atTime(9,0),2000,false);
        assertEquals(0,ProgressoHidratacao.carregar(dados,dia).fracao());
        EstimativaHidratacao.salvar(dados,"50");
        assertEquals(1,ProgressoHidratacao.carregar(dados,dia).fracao());
        EstimativaHidratacao.salvar(dados,"100");
        assertEquals(2000.0/3500,ProgressoHidratacao.carregar(dados,dia).fracao(),1e-9);
        EstimativaHidratacao.apagar(dados);
        assertEquals(0,ProgressoHidratacao.carregar(dados,dia).fracao());
        assertEquals(2000,ProgressoHidratacao.carregar(dados,dia).ml());
    }
}
