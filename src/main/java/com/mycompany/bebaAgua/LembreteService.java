package com.mycompany.bebaAgua;
import javax.swing.Timer;
import java.util.function.LongSupplier;

public class LembreteService {
    private final Timer temporizador;
    private final LongSupplier relogio;
    private long proximoAlertaNanos;
    private long restantePausadoNanos;
    private int intervaloMinutos;

    public LembreteService(Runnable aoAlertar) { this(aoAlertar, System::nanoTime); }

    // Permite testar a passagem do tempo sem esperar minutos.
    LembreteService(Runnable aoAlertar, LongSupplier relogio) {
        this.relogio = relogio;
        temporizador = new Timer(5000, e -> {
            proximoAlertaNanos = relogio.getAsLong() + intervaloMinutos * 60_000_000_000L;
            aoAlertar.run();
        });
    }
    public void start(int minutos) {
        if (minutos < 1 || minutos > 240)
            throw new IllegalArgumentException("O valor deve estar entre 1 e 240 minutos.");
        boolean retomar = !temporizador.isRunning()
                && minutos == intervaloMinutos && restantePausadoNanos > 0;
        long espera = retomar ? restantePausadoNanos : minutos * 60_000_000_000L;
        intervaloMinutos = minutos;
        temporizador.setDelay(minutos * 60 * 1000);
        temporizador.setInitialDelay((int) Math.max(1, (espera + 999_999L) / 1_000_000L));
        proximoAlertaNanos = relogio.getAsLong() + espera;
        restantePausadoNanos = 0;
        temporizador.restart();
    }
    public void pausar() {
        if (temporizador.isRunning()) {
            restantePausadoNanos = Math.max(1, proximoAlertaNanos - relogio.getAsLong());
            temporizador.stop();
        }
    }
    public long getSegundosRestantes() {
        long restante = temporizador.isRunning()
                ? proximoAlertaNanos - relogio.getAsLong() : restantePausadoNanos;
        return Math.max(0, (restante + 999_999_999L) / 1_000_000_000L);
    }

    boolean ativo() { return temporizador.isRunning(); }
    int intervalo() { return intervaloMinutos; }
    void restaurar(int minutos, long segundos, boolean ativo) {
        if (minutos < 1 || minutos > 240 || segundos < 0 || segundos > minutos * 60L)
            throw new IllegalArgumentException("Estado do lembrete inválido");
        temporizador.stop();
        intervaloMinutos = minutos;
        restantePausadoNanos = Math.max(1, segundos) * 1_000_000_000L;
        if (ativo) start(minutos);
    }
}
