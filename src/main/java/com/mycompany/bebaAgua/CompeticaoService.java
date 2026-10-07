package com.mycompany.bebaAgua;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;

/** API privada da competição. Não contém credencial administrativa do Cloudflare. */
final class CompeticaoService {
    private final DadosAgua dados;
    private final Properties estado=new Properties();
    private final java.util.concurrent.locks.ReentrantLock sincronizacao=new java.util.concurrent.locks.ReentrantLock();
    private final String base;
    private volatile String situacao="Ainda não sincronizado.";
    @FunctionalInterface interface Transporte {String enviar(String metodo,String rota,Map<String,String> campos,String token)throws IOException;}
    private final Transporte transporte;
    CompeticaoService(DadosAgua dados) throws IOException {this(dados,carregarUrl(),null);}
    CompeticaoService(DadosAgua dados,String base,Transporte transporte)throws IOException {
        this.dados=dados;this.base=base.strip().replaceAll("/+$","");this.transporte=transporte;
        Path f=dados.pasta().resolve("competicao.properties");
        if(Files.exists(f))try(var r=Files.newBufferedReader(f)){estado.load(r);}
    }
    private static String carregarUrl()throws IOException {
        Properties config=new Properties();
        try(var r=CompeticaoService.class.getResourceAsStream("/competicao.properties")){if(r!=null)config.load(r);}
        return config.getProperty("baseUrl","");
    }    synchronized boolean participa(){return "true".equals(estado.getProperty("active"));}
    boolean disponivel(){return !base.isBlank();}
    synchronized int ultimoVolume(){
        try {int v=Integer.parseInt(estado.getProperty("volume","200"));return v>=10&&v<=2000?v:200;}
        catch(NumberFormatException e){return 200;}
    }
    synchronized void volume(int ml)throws IOException{estado.setProperty("volume",Integer.toString(ml));salvar();}
    synchronized String situacao(){return situacao;}
    void participar(String nome,String setor,String senha) throws IOException {
        if(!disponivel())throw new IOException("A competição ainda precisa ser ativada pelo TI no Cloudflare.");
        String token=estado.getProperty("token");
        if(token==null){byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);token=HexFormat.of().formatHex(bytes);estado.setProperty("token",token);salvar();}
        // Credencial persistida antes do envio: repetir após falha de rede não cria outra pessoa.
        String response=requisicao("POST","/v1/register",Map.of("name",nome.strip(),"sector",setor.strip(),"password",senha,"token",token,"consent","yes"),false);
        var result=parseForm(response);
        if(!result.containsKey("id"))throw new IOException("Resposta de cadastro inválida.");
        estado.setProperty("id",result.get("id"));estado.setProperty("active","true");
        estado.setProperty("name",result.getOrDefault("name",nome));estado.setProperty("sector",result.getOrDefault("sector",setor));salvar();
        situacao="Participação confirmada. Apenas novos registros serão enviados.";
    }
    void sincronizar() throws IOException {
        if(!sincronizacao.tryLock())throw new IOException("Sincronização já em andamento. Aguarde a próxima consulta.");
        try{sincronizarExclusivo();}finally{sincronizacao.unlock();}
    }
    private void sincronizarExclusivo() throws IOException {
        if(!participa())return;
        try {
            IOException erroReferencia=null;
            try{capturarReferencia();}catch(IOException e){erroReferencia=e;}
            int enviados=0,rejeitados=0;
            for(var p:dados.eventosCompeticao()) {
                String valor=p.cancelada()?"cancelado":"enviado",chave="event."+p.id();
                if(valor.equals(estado.getProperty(chave)))continue;
                if (("rejeitado:"+valor).equals(estado.getProperty(chave))){rejeitados++;continue;}
                try{requisicao("POST","/v1/events",Map.of("id",p.id(),"at",p.instante().toString(),"ml",Integer.toString(p.ml()),"canceled",p.cancelada()?"1":"0"),true);}
                catch(ApiException e){
                    if(e.status==400||e.status==409||e.status==422){estado.setProperty(chave,"rejeitado:"+valor);salvar();rejeitados++;continue;}
                    throw e;
                }
                estado.setProperty(chave,valor);salvar();enviados++;
            }
            sincronizarReferencias();
            if(erroReferencia!=null)throw erroReferencia;
            situacao=rejeitados>0?rejeitados+" registros não aceitos pelo servidor; demais sincronizados.":"Sincronizado às "+java.time.LocalTime.now().withNano(0)+".";
        }catch(IOException e){situacao="Registros pendentes: "+e.getMessage();throw e;}
    }
    boolean compartilhaConquistas(){return "true".equals(estado.getProperty("achievements.enabled"));}
    void compartilharConquistas(boolean ativar)throws IOException {
        requisicao("POST","/v1/achievements/sharing",Map.of("enabled",ativar?"1":"0","consent","achievements-v1"),true);
        estado.setProperty("achievements.enabled",Boolean.toString(ativar));salvar();
    }
    private synchronized void capturarReferencia()throws IOException {
        if(!compartilhaConquistas())return;
        String dia=java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo")).toString();
        String chave="reference."+dia;
        if(estado.containsKey(chave))return;
        String peso=EstimativaHidratacao.carregar(dados);
        if(peso.isBlank())return;
        String ml=EstimativaHidratacao.peso(peso).multiply(new java.math.BigDecimal("35")).setScale(0,java.math.RoundingMode.CEILING).toPlainString();
        estado.setProperty(chave,ml);salvar();
    }
    private void sincronizarReferencias()throws IOException {
        if(!compartilhaConquistas())return;
        var hoje=java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo"));
        for(int i=0;i<=7;i++){
            String dia=hoje.minusDays(i).toString(),chave="reference."+dia;
            if(!estado.containsKey(chave)||"true".equals(estado.getProperty(chave+".sent")))continue;
            try{requisicao("POST","/v1/achievements/reference",Map.of("day",dia,"target_ml",estado.getProperty(chave)),true);}
            catch(ApiException e){if(e.status==400||e.status==422){estado.setProperty(chave+".sent","true");salvar();continue;}throw e;}
            estado.setProperty(chave+".sent","true");salvar();
        }
    }
    record Conquista(int posicao,String nome,String setor,List<java.time.LocalDate> dias,boolean proprio){}
    record Conquistas(java.time.YearMonth mes,java.time.LocalDate hoje,List<Conquista> pessoas,String aviso){}
    Conquistas consultarConquistas(java.time.YearMonth mes)throws IOException {
        String aviso="";try{sincronizar();}catch(IOException e){aviso="Há registros pendentes de sincronização.";}
        String raw=requisicao("GET","/v1/achievements?month="+mes,Map.of(),true);
        try{
            String[] linhas=raw.split("\n");
            if(linhas.length<2||!linhas[0].equals("month="+mes)||!linhas[1].startsWith("day="))throw new IllegalArgumentException();
            var hoje=java.time.LocalDate.parse(linhas[1].substring(4));
            List<Conquista> lista=new ArrayList<>();
            for(int i=2;i<linhas.length;i++){
                String[] c=linhas[i].split("\t",-1);if(c.length!=5||!c[4].matches("[01]"))throw new IllegalArgumentException();
                int pos=Integer.parseInt(c[0]);if(pos<1)throw new IllegalArgumentException();
                List<java.time.LocalDate> dias=new ArrayList<>();
                for(String valor:c[3].split(",")){var dia=java.time.LocalDate.parse(valor);if(!java.time.YearMonth.from(dia).equals(mes)||dia.isAfter(hoje)||dias.contains(dia))throw new IllegalArgumentException();dias.add(dia);}
                lista.add(new Conquista(pos,decode(c[1]),decode(c[2]),List.copyOf(dias),c[4].equals("1")));
            }
            return new Conquistas(mes,hoje,List.copyOf(lista),aviso);
        }catch(RuntimeException e){throw new IOException("Resposta de conquistas inválida.",e);}
    }
    record Participante(int posicao,String nome,String setor,long ml,boolean proprio) { }
    record Ranking(java.time.LocalDate dia,List<Participante> participantes,String aviso) { }
    Ranking consultarRanking() throws IOException {return consultarRanking(null);}
    Ranking consultarRanking(java.time.LocalDate data) throws IOException {
        String aviso="";
        try{sincronizar();}catch(IOException e){aviso="Há registros locais pendentes.\n\n";}
        String raw=requisicao("GET","/v1/ranking?format=2"+(data==null?"":"&day="+data),Map.of(),true);
        String[] linhas=raw.split("\n");
        if(linhas.length==0||!linhas[0].startsWith("day="))throw new IOException("Ranking inválido.");
        List<Participante> lista=new ArrayList<>();
        java.time.LocalDate dia;
        try{dia=java.time.LocalDate.parse(linhas[0].substring(4));}catch(RuntimeException e){throw new IOException("Data do ranking inválida.",e);}
        for(int i=1;i<linhas.length;i++){
            String[] campos=linhas[i].split("\t",-1);if(campos.length!=4&&campos.length!=5)throw new IOException("Ranking inválido.");
            try{int posicao=Integer.parseInt(campos[0]);long ml=Long.parseLong(campos[3]);
                if(posicao<1||ml<0)throw new IllegalArgumentException();
                if(campos.length==5&&!campos[4].matches("[01]"))throw new IllegalArgumentException();
                lista.add(new Participante(posicao,decode(campos[1]),decode(campos[2]),ml,campos.length==5&&campos[4].equals("1")));}
            catch(RuntimeException e){throw new IOException("Ranking inválido.",e);}
        }
        if(data!=null&&!data.equals(dia))throw new IOException("O servidor retornou outro dia do ranking.");
        return new Ranking(dia,List.copyOf(lista),aviso.strip());
    }
    String ranking() throws IOException {
        Ranking ranking=consultarRanking();StringBuilder texto=new StringBuilder();
        if(!ranking.aviso().isBlank())texto.append(ranking.aviso()).append("\n\n");
        texto.append("Ranking de ").append(ranking.dia()).append(" · horário de Brasília\nVolumes autodeclarados\n\n");
        for(Participante p:ranking.participantes())texto.append(p.posicao()).append("º  ").append(p.nome()).append(" · ").append(p.setor())
            .append(" — ").append(String.format(Locale.forLanguageTag("pt-BR"),"%.2f L",p.ml()/1000.0)).append('\n');
        return texto.toString();
    }
    void sair() throws IOException {
        requisicao("DELETE","/v1/participation",Map.of(),true);
        estado.setProperty("active","false");salvar();situacao="Participação encerrada. Procure o TI se desejar retornar.";
    }
    private synchronized void salvar() throws IOException {
        StringWriter out=new StringWriter();estado.store(out,"Credencial individual: nao compartilhar este arquivo.");
        dados.gravar(dados.pasta().resolve("competicao.properties"),out.toString());
    }
    static Map<String,String> parseForm(String raw){
        Map<String,String> out=new HashMap<>();for(String item:raw.split("&")){String[] p=item.split("=",2);if(p.length==2)out.put(decode(p[0]),decode(p[1]));}return out;
    }
    private static String decode(String s){return URLDecoder.decode(s,StandardCharsets.UTF_8);}
    private static final class ApiException extends IOException {
        final int status; ApiException(int status,String message){super(message);this.status=status;}
    }
    private String requisicao(String metodo,String rota,Map<String,String> campos,boolean auth)throws IOException {
        if(!disponivel())throw new IOException("Competição ainda não ativada pelo TI.");
        URI uri=URI.create(base+rota);
        if(!"https".equals(uri.getScheme())||uri.getUserInfo()!=null)throw new IOException("A competição exige endereço HTTPS.");
        if(transporte!=null)return transporte.enviar(metodo,rota,campos,auth?estado.getProperty("token",""):"");
        HttpsURLConnection c=(HttpsURLConnection)uri.toURL().openConnection();
        try {
            try{c.setSSLSocketFactory(RedeAtualizacao.fabrica());}catch(java.security.GeneralSecurityException e){throw new IOException("Falha ao preparar conexão segura.",e);}
            c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(false);c.setRequestMethod(metodo);
            if(auth)c.setRequestProperty("Authorization","Bearer "+estado.getProperty("token",""));
            if(metodo.equals("POST")){
                StringJoiner form=new StringJoiner("&");campos.forEach((k,v)->form.add(URLEncoder.encode(k,StandardCharsets.UTF_8)+"="+URLEncoder.encode(v,StandardCharsets.UTF_8)));
                byte[] bytes=form.toString().getBytes(StandardCharsets.UTF_8);c.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
                c.setDoOutput(true);c.setFixedLengthStreamingMode(bytes.length);try(var out=c.getOutputStream()){out.write(bytes);}
            }
            int code=c.getResponseCode();
            try(InputStream in=code==200?c.getInputStream():c.getErrorStream()){
                byte[] bytes=in==null?new byte[0]:in.readNBytes(262145);if(bytes.length>262144)throw new IOException("Resposta muito grande.");
                String body=new String(bytes,StandardCharsets.UTF_8);
                if(code!=200)throw new ApiException(code,"Servidor ("+code+"): "+body.substring(0,Math.min(200,body.length())));
                return body;
            }
        }finally{c.disconnect();}
    }
}
