package com.mycompany.bebaAgua;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.swing.Timer;

/** Agenda de Brasília independente dos lembretes de água, inclusive na bandeja. */
final class AlongamentoService {
    static final ZoneId BRASILIA=ZoneId.of("America/Sao_Paulo");
    record Agenda(boolean ativo, boolean diasUteis, List<LocalTime> horarios) {
        Agenda {
            horarios=List.copyOf(horarios);
            if(ativo && (horarios.size()!=2 || horarios.get(0).equals(horarios.get(1))
                    || horarios.stream().anyMatch(h->h.getSecond()!=0||h.getNano()!=0)))
                throw new IllegalArgumentException("Defina dois horários distintos em HH:mm.");
        }
        static Agenda carregar() throws IOException {
            Properties p=new Properties();
            try(var in=AlongamentoService.class.getResourceAsStream("/alongamento.properties")) {
                if(in==null)throw new IOException("Agenda de alongamento ausente.");
                p.load(in);
            }
            try {
                var formato=DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(java.time.format.ResolverStyle.STRICT);
                return new Agenda(Boolean.parseBoolean(p.getProperty("ativo")),
                        Boolean.parseBoolean(p.getProperty("diasUteis","true")),
                        List.of(LocalTime.parse(p.getProperty("horario1"),formato),LocalTime.parse(p.getProperty("horario2"),formato)));
            }catch(RuntimeException e){throw new IOException("Agenda de alongamento inválida.",e);}
        }
    }
    private final DadosAgua dados;
    private final Agenda agenda;
    private final Clock relogio;
    private final Runnable alertar;
    private final Properties exibidos=new Properties();
    private final Timer timer;
    private Instant ultimaVerificacao;

    AlongamentoService(DadosAgua dados,Agenda agenda,Clock relogio,Runnable alertar)throws IOException {
        this.dados=dados;this.agenda=agenda;this.relogio=relogio;this.alertar=alertar;
        var arquivo=dados.pasta().resolve("alongamento-exibidos.properties");
        if(java.nio.file.Files.exists(arquivo))try(var r=java.nio.file.Files.newBufferedReader(arquivo)){exibidos.load(r);}
        timer=new Timer(1000,e->verificar());
    }
    void iniciar(){verificar();timer.start();}
    void parar(){timer.stop();}
    void verificar(){
        Instant agora=relogio.instant();
        boolean retomou=ultimaVerificacao!=null && Duration.between(ultimaVerificacao,agora).getSeconds()>90;
        ultimaVerificacao=agora;
        if(!agenda.ativo())return;
        var local=agora.atZone(BRASILIA);
        if(agenda.diasUteis() && local.getDayOfWeek().getValue()>5)return;
        LocalTime minuto=local.toLocalTime().withSecond(0).withNano(0);
        if(!agenda.horarios().contains(minuto))return;
        String chave=minuto.toString(),dia=local.toLocalDate().toString();
        if(dia.equals(exibidos.getProperty(chave)))return;
        // Persiste antes de exibir: reiniciar ou atualizar no mesmo minuto não duplica o aviso.
        Properties novos=new Properties();novos.putAll(exibidos);novos.setProperty(chave,dia);
        try {
            StringWriter texto=new StringWriter();novos.store(texto,null);
            dados.gravar(dados.pasta().resolve("alongamento-exibidos.properties"),texto.toString());
            exibidos.putAll(novos);
            if(!retomou)alertar.run();
        }catch(IOException e){System.getLogger(getClass().getName()).log(System.Logger.Level.WARNING,"Não foi possível salvar o aviso de alongamento",e);}
    }
}
