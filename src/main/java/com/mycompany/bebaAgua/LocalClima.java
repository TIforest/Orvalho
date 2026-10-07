package com.mycompany.bebaAgua;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;

/** Cidade aproximada por IP, sem persistir coordenadas ou vincular ao cadastro. */
final class LocalClima {
    record Local(String cidade,double latitude,double longitude) {
        Local {
            if(cidade==null||cidade.isBlank()||cidade.length()>100||cidade.chars().anyMatch(Character::isISOControl)
                    ||!Double.isFinite(latitude)||!Double.isFinite(longitude)||Math.abs(latitude)>90||Math.abs(longitude)>180)
                throw new IllegalArgumentException("Localização inválida.");
        }
        String url(){return String.format(Locale.ROOT,"https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.2f&lon=%.2f",latitude,longitude);}
    }
    static final Local TB=new Local("Telêmaco Borba",-24.3239,-50.6158);
    static final Local SP=new Local("São Paulo",-23.5505,-46.6333);
    static final String[] OPCOES={"Automática (aproximada pela internet)","Telêmaco Borba — PR","São Paulo — SP","Não mostrar temperatura"};
    private static Local cache;private static Instant proxima=Instant.MIN;
    static int opcao(DadosAgua dados){
        try{int n=Integer.parseInt(Files.readString(dados.pasta().resolve("cidade-clima.txt")).trim());return n>=0&&n<OPCOES.length?n:0;}
        catch(IOException|NumberFormatException e){return 0;}
    }
    static void salvar(DadosAgua dados,int n)throws IOException{
        if(n<0||n>=OPCOES.length)throw new IllegalArgumentException();
        dados.gravar(dados.pasta().resolve("cidade-clima.txt"),Integer.toString(n));
    }
    static synchronized Local consultar()throws IOException{
        int modo=opcao(new DadosAgua());
        if(modo==1)return TB;if(modo==2)return SP;if(modo==3)throw new IOException("Temperatura desativada.");
        Instant agora=Instant.now();
        if(!agora.isBefore(proxima)){
            proxima=agora.plusSeconds(1800);cache=null;
            HttpsURLConnection c=(HttpsURLConnection)URI.create("https://orvalho-competicao.bruno-rech.workers.dev/v1/location").toURL().openConnection();
            try{
                try{c.setSSLSocketFactory(RedeAtualizacao.fabrica());}catch(java.security.GeneralSecurityException e){throw new IOException(e);}
                c.setConnectTimeout(4000);c.setReadTimeout(4000);c.setInstanceFollowRedirects(false);
                if(c.getResponseCode()!=200)throw new IOException("Cidade indisponível.");
                try(var in=c.getInputStream()){
                    byte[] bytes=in.readNBytes(4097);if(bytes.length>4096)throw new IOException("Resposta inválida.");
                    cache=interpretar(new String(bytes,StandardCharsets.UTF_8));
                }
            }finally{c.disconnect();}
        }
        if(cache==null)throw new IOException("Escolha a cidade nas configurações.");return cache;
    }
    static Local interpretar(String texto)throws IOException{
        try{
            Map<String,String> p=new HashMap<>();for(String item:texto.split("&")){String[] partes=item.split("=",2);if(partes.length==2)p.put(partes[0],URLDecoder.decode(partes[1],StandardCharsets.UTF_8));}
            return new Local(p.get("city"),Double.parseDouble(p.get("lat")),Double.parseDouble(p.get("lon")));
        }catch(RuntimeException e){throw new IOException("Localização inválida.",e);}
    }
}
