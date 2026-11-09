package com.mycompany.bebaAgua;

import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDate;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;

/** Quadros dos vídeos guiados (um por segundo) para reprodução local em Swing. */
final class VideoAlongamento {
    static final int DURACAO=60;
    private static final LocalDate INICIO=LocalDate.of(2026,11,9); // Segunda-feira, parte 1.
    private final byte[][] quadros=new byte[DURACAO][];

    static int parteDoDia(LocalDate dia){
        long dias=dia.toEpochDay()-INICIO.toEpochDay();
        long uteis=Math.floorDiv(dias,7)*5+Math.min(Math.floorMod(dias,7),4);
        return 1+(int)Math.floorMod(uteis,2);
    }

    VideoAlongamento(int parte)throws IOException{
        if(parte!=1&&parte!=2)throw new IllegalArgumentException("Parte inválida.");
        try(var recurso=getClass().getResourceAsStream("/alongamento/parte-"+parte+".zip")){
            if(recurso==null)throw new IOException("Vídeo ausente.");
            try(var zip=new ZipInputStream(recurso)){
                for(var entrada=zip.getNextEntry();entrada!=null;entrada=zip.getNextEntry()){
                    String nome=entrada.getName();
                    if(!nome.matches("[0-5][0-9]\\.jpg"))throw new IOException("Quadro inválido.");
                    int segundo=Integer.parseInt(nome.substring(0,2));
                    quadros[segundo]=zip.readNBytes(256_001);
                    if(quadros[segundo].length>256_000)throw new IOException("Quadro muito grande.");
                }
            }
        }
        for(byte[] quadro:quadros)if(quadro==null)throw new IOException("Vídeo incompleto.");
    }

    BufferedImage quadro(int segundo)throws IOException{
        var imagem=ImageIO.read(new ByteArrayInputStream(quadros[segundo]));
        if(imagem==null)throw new IOException("Imagem do vídeo inválida.");
        return imagem;
    }
}
