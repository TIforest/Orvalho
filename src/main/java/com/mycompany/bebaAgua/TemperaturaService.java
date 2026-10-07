package com.mycompany.bebaAgua;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HttpsURLConnection;

/** Previsão por cidade; cache isolado por localização e revalidado no MET Norway. */
final class TemperaturaService {
    static final TemperaturaService PADRAO=new TemperaturaService(TemperaturaService::baixar,Clock.systemUTC(),LocalClima::consultar);
    private static final ZoneId LOCAL=ZoneId.of("America/Sao_Paulo");
    record Clima(double celsius,Instant horario,String cidade){
        Clima(double celsius,Instant horario){this(celsius,horario,"Telêmaco Borba");}
        String texto(){return String.format(Locale.forLanguageTag("pt-BR"),"%s · %.1f °C",cidade,celsius);}
        String fonte(){return "MET Norway · estimativa para "+horario.atZone(LOCAL).format(DateTimeFormatter.ofPattern("HH:mm"));}
    }
    record Resposta(int status,String corpo,String modificado,Instant expira){}
    @FunctionalInterface interface Transporte {Resposta buscar(String modificado)throws IOException;}
    @FunctionalInterface interface TransporteLocal {Resposta buscar(String modificado,String url)throws IOException;}
    @FunctionalInterface interface Localizacao {LocalClima.Local buscar()throws IOException;}
    private final TransporteLocal transporte;private final Clock clock;private final Localizacao localizacao;
    private String corpo,modificado="";private Instant proxima=Instant.MIN,recebido=Instant.MIN;
    private LocalClima.Local local=LocalClima.TB;
    TemperaturaService(Transporte transporte,Clock clock){this((m,u)->transporte.buscar(m),clock,()->LocalClima.TB);}
    TemperaturaService(TransporteLocal transporte,Clock clock,Localizacao localizacao){this.transporte=transporte;this.clock=clock;this.localizacao=localizacao;}
    synchronized Optional<Clima> consultar(){
        try{
            var novo=localizacao.buscar();
            if(!novo.equals(local)){corpo=null;modificado="";proxima=Instant.MIN;local=novo;}
        }catch(IOException|RuntimeException e){corpo=null;modificado="";proxima=Instant.MIN;return Optional.empty();}
        Instant agora=clock.instant();
        if(!agora.isBefore(proxima)){
            proxima=agora.plusSeconds(1800);
            try{
                Resposta r=transporte.buscar(modificado,local.url());
                if(r.expira()!=null&&r.expira().isAfter(proxima))proxima=r.expira();
                if(r.status()==200){
                    // Não substitui um cache válido por uma resposta inválida.
                    interpretar(r.corpo(),agora);corpo=r.corpo();modificado=r.modificado()==null?"":r.modificado();recebido=agora;
                }else if(r.status()==304&&corpo!=null){recebido=agora;}
                else if(r.status()==429||r.status()==403){proxima=agora.plusSeconds(3600);}
            }catch(IOException|RuntimeException e){ /* O lembrete não depende da rede. */ }
        }
        if(corpo==null||agora.isAfter(recebido.plusSeconds(7200)))return Optional.empty();
        try{var c=interpretar(corpo,agora);return Optional.of(new Clima(c.celsius(),c.horario(),local.cidade()));}catch(IOException e){return Optional.empty();}
    }
    static Clima interpretar(String json,Instant agora)throws IOException{
        if(json==null||!Pattern.compile("\"air_temperature\"\\s*:\\s*\"celsius\"").matcher(json).find())throw new IOException("Unidade meteorológica inválida.");
        // A resposta compacta contém uma série de objetos time/data. Delimitamos cada instante
        // antes de extrair a temperatura, para nunca combinar o horário de um ponto com outro.
        var horario=Pattern.compile("\"time\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        List<Integer> inicios=new ArrayList<>(),fins=new ArrayList<>();List<String> datas=new ArrayList<>();
        while(horario.find()){inicios.add(horario.start());fins.add(horario.end());datas.add(horario.group(1));}
        Clima melhor=null;long distancia=Long.MAX_VALUE;
        for(int i=0;i<datas.size();i++)try{
            Instant instante=Instant.parse(datas.get(i));long delta=Math.abs(Duration.between(agora,instante).toSeconds());
            if(delta>3600||!instante.atZone(LOCAL).toLocalDate().equals(agora.atZone(LOCAL).toLocalDate()))continue;
            String trecho=json.substring(fins.get(i),i+1<datas.size()?inicios.get(i+1):json.length());
            var temp=Pattern.compile("\"air_temperature\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)(?=\\s*[,}])").matcher(trecho);
            if(!temp.find())continue;double valor=Double.parseDouble(temp.group(1));
            if(!Double.isFinite(valor)||valor< -60||valor>60)continue;
            if(delta<distancia){melhor=new Clima(valor,instante);distancia=delta;}
        }catch(RuntimeException ignored){}
        if(melhor==null)throw new IOException("Temperatura recente indisponível.");return melhor;
    }
    private static Resposta baixar(String modificado,String url)throws IOException{
        URI uri=URI.create(url);
        for(int salto=0;salto<4;salto++){
            HttpsURLConnection c=(HttpsURLConnection)uri.toURL().openConnection();
            try{
                try{c.setSSLSocketFactory(RedeAtualizacao.fabrica());}catch(java.security.GeneralSecurityException e){throw new IOException(e);}
                c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setInstanceFollowRedirects(false);
                c.setRequestProperty("User-Agent","Orvalho-Forest (https://github.com/BrunoTrech/Orvalho-Releases)");
                c.setRequestProperty("Accept","application/json");c.setRequestProperty("Accept-Encoding","gzip");
                if(!modificado.isBlank())c.setRequestProperty("If-Modified-Since",modificado);
                int status=c.getResponseCode();
                if(status==301||status==302||status==307||status==308){
                    String location=c.getHeaderField("Location");if(location==null)throw new IOException("Redirecionamento inválido.");
                    URI destino=uri.resolve(location);
                    if(!"https".equals(destino.getScheme())||!"api.met.no".equals(destino.getHost())||destino.getUserInfo()!=null)throw new IOException("Destino meteorológico inválido.");
                    uri=destino;continue;
                }
                Instant expira=null;long expiration=c.getHeaderFieldDate("Expires",-1);if(expiration>0)expira=Instant.ofEpochMilli(expiration);
                if(status!=200)return new Resposta(status,"",c.getHeaderField("Last-Modified"),expira);
                try(InputStream raw=c.getInputStream();InputStream in="gzip".equalsIgnoreCase(c.getContentEncoding())?new GZIPInputStream(raw):raw){
                    byte[] bytes=in.readNBytes(524289);if(bytes.length>524288)throw new IOException("Resposta meteorológica muito grande.");
                    return new Resposta(status,new String(bytes,StandardCharsets.UTF_8),c.getHeaderField("Last-Modified"),expira);
                }
            }finally{c.disconnect();}
        }
        throw new IOException("Muitos redirecionamentos meteorológicos.");
    }
}
