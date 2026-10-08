package com.mycompany.bebaAgua;
import javax.swing.Timer;
import java.util.function.LongSupplier;

public class LembreteService {
    private final Timer temporizador;
    private final LongSupplier relogio;
    private long proximoAlertaNanos;
    private long restantePausadoNanos;
    private int intervaloMinutos;
    private boolean habilitado;
    private boolean aguardandoConfirmacao;

    public LembreteService(Runnable aoAlertar) { this(aoAlertar, System::nanoTime); }

    // Permite testar a passagem do tempo sem esperar minutos.
    LembreteService(Runnable aoAlertar, LongSupplier relogio) {
        this.relogio = relogio;
        temporizador = new Timer(5000, e -> {
            if (!habilitado || aguardandoConfirmacao) return;
            ((Timer) e.getSource()).stop();
            restantePausadoNanos = 0;
            aguardandoConfirmacao = true;
            aoAlertar.run();
        });
        temporizador.setRepeats(false);
    }
    public void start(int minutos) {
        if (minutos < 1 || minutos > 240)
            throw new IllegalArgumentException("O valor deve estar entre 1 e 240 minutos.");
        habilitado = true;
        if (aguardandoConfirmacao && minutos == intervaloMinutos) return;
        aguardandoConfirmacao = false;
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
        habilitado = false;
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

    boolean ativo() { return habilitado; }
    boolean aguardandoConfirmacao() { return aguardandoConfirmacao; }
    boolean confirmarAlerta() {
        if (!aguardandoConfirmacao) return false;
        aguardandoConfirmacao = false;
        restantePausadoNanos = intervaloMinutos * 60_000_000_000L;
        if (habilitado) start(intervaloMinutos);
        return true;
    }
    boolean adiarAlerta() {
        return adiarAlerta(5);
    }
    boolean adiarAlerta(int minutos) {
        if (minutos < 1 || minutos > 240)
            throw new IllegalArgumentException("O valor deve estar entre 1 e 240 minutos.");
        if (!aguardandoConfirmacao) return false;
        aguardandoConfirmacao = false;
        restantePausadoNanos = minutos * 60_000_000_000L;
        if (habilitado) start(intervaloMinutos);
        return true;
    }
    void restaurarAlertaPendente() {
        if (intervaloMinutos == 0) return;
        temporizador.stop();
        restantePausadoNanos = 0;
        aguardandoConfirmacao = true;
    }
    // Fração precisa para a animação, sem arredondar para segundos inteiros.
    double proporcaoRestante() {
        if (intervaloMinutos == 0) return 1.0;
        long restante = temporizador.isRunning()
                ? proximoAlertaNanos - relogio.getAsLong() : restantePausadoNanos;
        return Math.max(0.0, Math.min(1.0, restante / (intervaloMinutos * 60_000_000_000.0)));
    }
    int intervalo() { return intervaloMinutos; }
    void restaurar(int minutos, long segundos, boolean ativo) {
        if (minutos < 1 || minutos > 240 || segundos < 0 || segundos > 240 * 60L)
            throw new IllegalArgumentException("Estado do lembrete inválido");
        temporizador.stop();
        habilitado = false;
        aguardandoConfirmacao = false;
        intervaloMinutos = minutos;
        restantePausadoNanos = Math.max(1, segundos) * 1_000_000_000L;
        if (ativo) start(minutos);
    }
}
