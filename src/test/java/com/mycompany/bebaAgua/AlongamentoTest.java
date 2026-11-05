package com.mycompany.bebaAgua;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import static org.junit.jupiter.api.Assertions.*;

class AlongamentoTest {
    @TempDir Path pasta;
    static class Relogio extends Clock {
        Instant agora=Instant.parse("2026-11-05T12:59:59Z");
        public ZoneId getZone(){return ZoneId.of("Asia/Tokyo");}
        public Clock withZone(ZoneId z){return this;}
        public Instant instant(){return agora;}
        void em(String hora){agora=Instant.parse(hora);}
    }
    @Test void doisHorariosBrasiliaSemDuplicarAoReiniciar()throws Exception{
        var relogio=new Relogio();var avisos=new AtomicInteger();var dados=new DadosAgua(pasta);
        var agenda=AlongamentoService.Agenda.carregar();
        var s=new AlongamentoService(dados,agenda,relogio,avisos::incrementAndGet);
        s.verificar();assertEquals(0,avisos.get());
        relogio.em("2026-11-05T13:00:00Z");s.verificar();s.verificar();assertEquals(1,avisos.get());
        var reiniciado=new AlongamentoService(dados,agenda,relogio,avisos::incrementAndGet);
        reiniciado.verificar();assertEquals(1,avisos.get());
        relogio.em("2026-11-05T17:59:59Z");reiniciado.verificar();
        relogio.em("2026-11-05T18:00:00Z");reiniciado.verificar();assertEquals(2,avisos.get());
        relogio.em("2026-11-06T12:59:59Z");reiniciado.verificar();
        relogio.em("2026-11-06T13:00:00Z");reiniciado.verificar();assertEquals(3,avisos.get());
    }
    @Test void naoExibeAtrasadoNemNoFimDeSemana()throws Exception{
        var r=new Relogio();var n=new AtomicInteger();var s=new AlongamentoService(new DadosAgua(pasta),AlongamentoService.Agenda.carregar(),r,n::incrementAndGet);
        r.em("2026-11-07T13:00:00Z");s.verificar();
        r.em("2026-11-08T18:00:00Z");s.verificar();
        r.em("2026-11-09T13:01:00Z");s.verificar();assertEquals(0,n.get());
        // Suspensão do PC que termina durante o minuto do horário: não dispara atrasado.
        r.em("2026-11-09T17:55:00Z");s.verificar();r.em("2026-11-09T18:00:20Z");s.verificar();s.verificar();assertEquals(0,n.get());
    }
    @Test void falhaAoPersistirNaoExibeAvisoSemProtecaoContraDuplicidade()throws Exception{
        var r=new Relogio();var n=new AtomicInteger();var dados=new DadosAgua(pasta);
        var s=new AlongamentoService(dados,AlongamentoService.Agenda.carregar(),r,n::incrementAndGet);
        Files.createDirectory(pasta.resolve("alongamento-exibidos.properties"));
        r.em("2026-11-05T13:00:00Z");s.verificar();assertEquals(0,n.get());
    }
    @Test void agendaValidaEDesativadaNaoDispara()throws Exception{
        var agenda=AlongamentoService.Agenda.carregar();assertTrue(agenda.diasUteis());assertTrue(agenda.ativo());
        assertEquals(List.of(LocalTime.of(10,0),LocalTime.of(15,0)),agenda.horarios());
        assertThrows(IllegalArgumentException.class,()->new AlongamentoService.Agenda(true,true,List.of(LocalTime.NOON,LocalTime.NOON)));
        var n=new AtomicInteger();var r=new Relogio();r.em("2026-11-05T13:00:00Z");
        new AlongamentoService(new DadosAgua(pasta),new AlongamentoService.Agenda(false,true,agenda.horarios()),r,n::incrementAndGet).verificar();assertEquals(0,n.get());
    }
    @Test void popupSoFechaComOkETemExclusaoDeModalidade()throws Exception{
        SwingUtilities.invokeAndWait(()->{
            JFrame dono=new JFrame();JDialog modal=new JDialog(dono,"Ranking",true);
            var popup=new PopupAlongamento(true,false,false);
            try {
                assertEquals(Dialog.ModalExclusionType.APPLICATION_EXCLUDE,popup.getModalExclusionType());
                assertTrue(popup.isAlwaysOnTop());assertFalse(popup.isModal());
                assertEquals(2,botoes(popup));
                popup.dispatchEvent(new WindowEvent(popup,WindowEvent.WINDOW_CLOSING));assertTrue(popup.isDisplayable());
                assertEquals("OK",popup.getRootPane().getDefaultButton().getText());
                popup.getRootPane().getDefaultButton().doClick(0);assertFalse(popup.isDisplayable());
            }finally{popup.dispose();modal.dispose();dono.dispose();}
        });
    }
    @Test void contagemSoComecaAoIniciarNaoReiniciaEParaAoFechar()throws Exception{
        SwingUtilities.invokeAndWait(()->{
            var tempo=new java.util.concurrent.atomic.AtomicLong();
            var p=new PopupAlongamento(false,false,true,tempo::get);
            try {
                var contagem=NovosControlesTest.achar(p,JLabel.class,"contagemAlongamento");
                var iniciar=NovosControlesTest.achar(p,JButton.class,"Iniciar");
                tempo.set(60_000_000_000L);p.atualizarTempo();assertEquals("02:00",contagem.getText());
                iniciar.doClick(0);assertFalse(iniciar.isEnabled());
                tempo.addAndGet(61_000_000_000L);p.atualizarTempo();assertEquals("00:59",contagem.getText());
                iniciar.doClick(0);p.atualizarTempo();assertEquals("00:59",contagem.getText());
                tempo.addAndGet(59_000_000_000L);p.atualizarTempo();assertEquals("00:00",contagem.getText());
                assertEquals("Concluído",iniciar.getText());assertTrue(p.isDisplayable());
                tempo.addAndGet(60_000_000_000L);p.atualizarTempo();assertEquals("00:00",contagem.getText());
                p.getRootPane().getDefaultButton().doClick(0);assertFalse(p.isDisplayable());
            }finally{p.dispose();}
        });
    }
    @Test void alongamentoPreservaAlertaDeAguaAbertoESuaQuantidadeDigitada()throws Exception {
        var dados=new DadosAgua(pasta);dados.salvar(new DadosAgua.Opcoes(false,false,true));
        JanelaPrincipal[] janela=new JanelaPrincipal[1];
        try {
            SwingUtilities.invokeAndWait(()->{
                janela[0]=new JanelaPrincipal(false,new ConfiguracaoIntervalo(pasta.resolve("intervalo.txt")),new DadosAgua(pasta));
                var f=janela[0];f.lembreteService.start(30);f.restaurarAlertaPendente();
                var agua=java.util.Arrays.stream(java.awt.Window.getWindows()).filter(w->w instanceof PopupLembrete&&w.isShowing()).map(w->(PopupLembrete)w).findFirst().orElseThrow();
                var campo=spinnerVolume(agua);assertNotNull(campo);campo.setValue(450);
                f.mostrarAlongamento(false);assertFalse(agua.isShowing());
                assertTrue(f.interacaoPendente());assertTrue(f.lembreteService.aguardandoConfirmacao());
                var alongamento=(PopupAlongamento)java.util.Arrays.stream(Window.getWindows()).filter(w->w instanceof PopupAlongamento&&w.isShowing()).findFirst().orElseThrow();
                alongamento.getRootPane().getDefaultButton().doClick(0);
            });
            SwingUtilities.invokeAndWait(()->{
                var agua=java.util.Arrays.stream(java.awt.Window.getWindows()).filter(w->w instanceof PopupLembrete&&w.isShowing()).map(w->(PopupLembrete)w).findFirst().orElseThrow();
                assertEquals(450,spinnerVolume(agua).getValue());
                assertTrue(janela[0].lembreteService.aguardandoConfirmacao());
            });
        }finally{SwingUtilities.invokeAndWait(()->{if(janela[0]!=null){janela[0].lembreteService.pausar();janela[0].dispose();}});}
    }
    private JSpinner spinnerVolume(Container c){
        for(Component x:c.getComponents()){
            if(x instanceof JSpinner s&&s.getModel() instanceof SpinnerNumberModel m&&Integer.valueOf(2000).equals(m.getMaximum()))return s;
            if(x instanceof Container y){var s=spinnerVolume(y);if(s!=null)return s;}
        }return null;
    }
    private int botoes(Container c){int n=0;for(Component x:c.getComponents()){if(x instanceof JButton)n++;if(x instanceof Container y)n+=botoes(y);}return n;}
}
