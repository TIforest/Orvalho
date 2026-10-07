package com.mycompany.bebaAgua;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Properties;

/** O canal é escolhido pelo TI neste perfil, nunca por conteúdo remoto não assinado. */
final class CanalAtualizacao {
    static String ler(Path pasta)throws IOException {
        Path arquivo=pasta.resolve("canal-atualizacao.txt");
        if(!Files.exists(arquivo))return "empresa";
        if(Files.size(arquivo)>64)throw new IOException("Configuração do canal de atualização inválida.");
        String canal=Files.readString(arquivo).strip();
        if(!canal.equals("empresa")&&!canal.equals("piloto"))throw new IOException("Canal inválido. Use empresa ou piloto em canal-atualizacao.txt.");
        return canal;
    }
    static URI manifesto(Properties config,String canal)throws IOException {
        if(!"empresa".equals(canal)&&!"piloto".equals(canal))throw new IOException("Canal de atualização inválido.");
        String endereco=config.getProperty(canal.equals("piloto")?"manifestPilot":"manifest","");
        if(endereco.isBlank())throw new IOException("Canal de atualização não configurado: "+canal);
        try{
            URI uri=URI.create(endereco);
            if(!"https".equals(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getFragment()!=null)throw new IllegalArgumentException();
            return uri;
        }catch(IllegalArgumentException e){throw new IOException("Endereço de atualização inválido.",e);}
    }
}
