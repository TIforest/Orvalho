package com.mycompany.bebaAgua;

import java.io.*;


import java.nio.file.*;

import java.util.*;


/** Cidade escolhida pelo usuário, sem detecção pela rede. */
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
    static final String[] OPCOES={"Telêmaco Borba — PR","São Paulo — SP"};
    static int opcao(DadosAgua dados){
        // Preserva os códigos anteriores: 1 = TB, 2 = SP. Automático e demais valores usam TB.
        try{return Integer.parseInt(Files.readString(dados.pasta().resolve("cidade-clima.txt")).trim())==2?1:0;}
        catch(IOException|NumberFormatException e){return 0;}
    }
    static void salvar(DadosAgua dados,int n)throws IOException{
        if(n<0||n>=OPCOES.length)throw new IllegalArgumentException();
        dados.gravar(dados.pasta().resolve("cidade-clima.txt"),Integer.toString(n+1));
    }
    static Local consultar(){return consultar(new DadosAgua());}
    static Local consultar(DadosAgua dados){return opcao(dados)==1?SP:TB;}
}