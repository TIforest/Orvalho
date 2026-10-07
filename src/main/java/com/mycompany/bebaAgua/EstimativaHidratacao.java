package com.mycompany.bebaAgua;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.Locale;

/** Referência educativa de 30–35 ml/kg/dia; não representa um mínimo individual. */
final class EstimativaHidratacao {
    // Fontes: https://doclibrary-rcht.cornwall.nhs.uk/GET/d10358773
    // https://www.mayoclinic.org/healthy-lifestyle/nutrition-and-healthy-eating/in-depth/water/art-20044256
    static BigDecimal peso(String valor) {
        String s=valor.strip().replace(',','.');
        if(!s.matches("\\d{1,3}(\\.\\d{1,2})?"))throw new IllegalArgumentException("Informe o peso em kg, por exemplo: 70 ou 70,5.");
        BigDecimal p=new BigDecimal(s);
        if(p.compareTo(new BigDecimal("20"))<0||p.compareTo(new BigDecimal("300"))>0)throw new IllegalArgumentException("Informe um peso entre 20 e 300 kg.");
        return p;
    }
    static String faixa(BigDecimal kg) {
        BigDecimal valido=peso(kg.toPlainString());
        return String.format(Locale.forLanguageTag("pt-BR"),"%.2f a %.2f L/dia",valido.multiply(new BigDecimal("0.030")),valido.multiply(new BigDecimal("0.035")));
    }
    static String carregar(DadosAgua dados)throws IOException {
        Path f=dados.pasta().resolve("peso-kg.txt");if(!Files.exists(f))return "";
        String valor=Files.readString(f).strip();if(valor.isEmpty())return "";
        try{return peso(valor).stripTrailingZeros().toPlainString().replace('.',',');}
        catch(IllegalArgumentException e){throw new IOException("O peso salvo é inválido. Você pode informar e salvar um novo valor.",e);}
    }
    static void salvar(DadosAgua dados,String valor)throws IOException {
        dados.gravar(dados.pasta().resolve("peso-kg.txt"),peso(valor).stripTrailingZeros().toPlainString()+"\n");
    }
    static void apagar(DadosAgua dados)throws IOException {Files.deleteIfExists(dados.pasta().resolve("peso-kg.txt"));}
}
