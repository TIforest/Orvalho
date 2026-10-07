package com.mycompany.bebaAgua;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Preferências e pausas confirmadas, armazenadas localmente por dia. */
final class DadosAgua {
    record Opcoes(boolean escuro, boolean som, boolean humor, boolean outubroRosa, boolean efeitos) {
        Opcoes(boolean escuro,boolean som,boolean humor){this(escuro,som,humor,false,true);}
        Opcoes(boolean escuro,boolean som,boolean humor,boolean rosa){this(escuro,som,humor,rosa,true);}
        Opcoes { if(outubroRosa)escuro=false; }
    }
    record Pausa(String id, LocalDateTime hora, int ml, boolean competicao, boolean cancelada, Instant instante) { }
    Path pasta(){return pasta;}
    private final Path pasta;
    DadosAgua() { this(Path.of(System.getProperty("user.home"), ".orvalho")); }
    DadosAgua(Path pasta) { this.pasta = pasta; }
    synchronized Opcoes carregar() throws IOException {
        Path f=pasta.resolve("preferencias.properties");
        Properties p=new Properties();
        if(Files.exists(f))try(var r=Files.newBufferedReader(f)){p.load(r);}
        // A partir da 1.2.22, a campanha exige escolha nas configurações.
        // Volta ao azul uma única vez; escolhas posteriores não são redefinidas.
        if(!Boolean.parseBoolean(p.getProperty("temaAzulPadrao122Aplicado","false"))){
            p.setProperty("outubroRosa","false");
            p.setProperty("temaAzulPadrao122Aplicado","true");
            p.remove("rosaVibrante");
            // Impede uma instalação anterior de reativar a campanha ao ler este arquivo.
            p.setProperty("campanhaOutubroRosa2026Aplicada","true");
            StringWriter texto=new StringWriter();p.store(texto,null);gravar(f,texto.toString());
        }
        return new Opcoes(Boolean.parseBoolean(p.getProperty("escuro","false")),
                Boolean.parseBoolean(p.getProperty("som","false")),
                Boolean.parseBoolean(p.getProperty("humor","true")),
                Boolean.parseBoolean(p.getProperty("outubroRosa","false")),
                Boolean.parseBoolean(p.getProperty("efeitos","true")));
    }
    synchronized void salvar(Opcoes o) throws IOException {
        gravar(pasta.resolve("preferencias.properties"),"escuro="+o.escuro()+"\nsom="+o.som()+"\nhumor="+o.humor()+"\noutubroRosa="+o.outubroRosa()+"\nefeitos="+o.efeitos()+"\ncampanhaOutubroRosa2026Aplicada=true\ntemaAzulPadrao122Aplicado=true\n");
    }
    synchronized List<Pausa> historico(LocalDate dia) throws IOException {
        return lerPausas(dia).stream().filter(p -> !p.cancelada()).toList();
    }
    synchronized List<LocalDate> diasHistorico() throws IOException {
        var dias=new TreeSet<LocalDate>(Comparator.reverseOrder());
        dias.add(LocalDate.now());
        if(Files.exists(pasta))try(var arquivos=Files.list(pasta)){
            for(Path arquivo:arquivos.filter(Files::isRegularFile).toList()){
                String nome=arquivo.getFileName().toString();
                if(!nome.matches("(?:pausas|consumo)-\\d{4}-\\d{2}-\\d{2}\\.tsv"))continue;
                try{dias.add(LocalDate.parse(nome.substring(nome.indexOf('-')+1,nome.length()-4)));}
                catch(java.time.format.DateTimeParseException ignored){ /* Não é um arquivo diário válido. */ }
            }
        }
        return List.copyOf(dias);
    }
    synchronized boolean corrigir(LocalDate dia,String id,int ml) throws IOException {
        if(ml<10||ml>2000)throw new IllegalArgumentException("Informe de 10 a 2000 ml.");
        var itens=new ArrayList<>(lerPausas(dia));
        for(int i=0;i<itens.size();i++){
            var p=itens.get(i);
            if(!p.id().equals(id)||p.cancelada())continue;
            if(p.ml()==ml)return false;
            if(p.competicao()&&p.instante().isBefore(Instant.now().minus(Duration.ofDays(7))))
                throw new IllegalArgumentException("Registros da competição com mais de sete dias não aceitam correção de quantidade.");
            // Uma única gravação preserva o cancelamento e a substituição, inclusive offline.
            itens.set(i,new Pausa(p.id(),p.hora(),p.ml(),p.competicao(),true,p.instante()));
            itens.add(new Pausa(UUID.randomUUID().toString(),p.hora(),ml,p.competicao(),false,p.instante()));
            salvarPausas(dia,itens);return true;
        }
        throw new IllegalArgumentException("Este registro já foi desfeito ou corrigido. Atualize o histórico.");
    }
    private List<Pausa> lerPausas(LocalDate dia) throws IOException {
        Path f=pasta.resolve("pausas-"+dia+".tsv");
        boolean legado=!Files.exists(f);
        if(legado) f=pasta.resolve("consumo-"+dia+".tsv");
        if (!Files.exists(f)) return List.of();
        List<Pausa> itens=new ArrayList<>();
        Set<String> ids=new HashSet<>();
        try {
            int indice=0;
            for(String linha:Files.readAllLines(f)) {
                if(linha.isBlank())continue;
                String[] partes=linha.split("\t",-1);
                if(partes.length!=2 && partes.length!=6)throw new IllegalArgumentException();
                String id=legado ? "legado-"+(indice++) : partes[0];
                var hora=LocalDateTime.parse(partes[legado?0:1]);
                if(legado && Integer.parseInt(partes[1])<1)throw new IllegalArgumentException();
                if(!hora.toLocalDate().equals(dia)||id.isBlank()||!ids.add(id))throw new IllegalArgumentException();
                int ml=partes.length==6?Integer.parseInt(partes[2]):0;
                boolean participa=partes.length==6&&Boolean.parseBoolean(partes[3]);
                boolean cancelada=partes.length==6&&Boolean.parseBoolean(partes[4]);
                Instant instante=partes.length==6?Instant.parse(partes[5]):hora.atZone(ZoneId.systemDefault()).toInstant();
                if(ml<0||ml>2000||(participa&&ml<10))throw new IllegalArgumentException();
                itens.add(new Pausa(id,hora,ml,participa,cancelada,instante));
            }
        }catch(RuntimeException e){throw new IOException("Histórico inválido; os registros foram preservados.",e);}
        return List.copyOf(itens);
    }
    synchronized void registrar(LocalDateTime hora) throws IOException {registrar(hora,0,false);}
    synchronized void registrar(LocalDateTime hora,int ml,boolean competir) throws IOException {
        if(ml<0||ml>2000||(ml>0&&ml<10)||(competir&&ml==0))throw new IllegalArgumentException("Informe de 10 a 2000 ml.");
        var itens=new ArrayList<>(lerPausas(hora.toLocalDate()));
        itens.add(new Pausa(UUID.randomUUID().toString(),hora,ml,competir,false,hora.atZone(ZoneId.systemDefault()).toInstant()));
        salvarPausas(hora.toLocalDate(),itens);
    }
    synchronized boolean desfazer(LocalDate dia,String id) throws IOException {
        var itens=new ArrayList<>(lerPausas(dia));
        for(int i=0;i<itens.size();i++){
            var p=itens.get(i);
            if(p.id().equals(id)&&!p.cancelada()){
                itens.set(i,new Pausa(p.id(),p.hora(),p.ml(),p.competicao(),true,p.instante()));
                salvarPausas(dia,itens);return true;
            }
        }
        return false;
    }
    synchronized List<Pausa> eventosCompeticao() throws IOException {
        List<Pausa> eventos=new ArrayList<>();
        if(!Files.exists(pasta))return eventos;
        try(var arquivos=Files.list(pasta)){
            for(Path arquivo:arquivos.filter(p->p.getFileName().toString().matches("pausas-\\d{4}-\\d{2}-\\d{2}\\.tsv")).sorted().toList()){
                var dia=LocalDate.parse(arquivo.getFileName().toString().substring(7,17));
                for(var pausa:lerPausas(dia))if(pausa.competicao())eventos.add(pausa);
            }
        }
        return eventos;
    }    private void salvarPausas(LocalDate dia,List<Pausa> itens) throws IOException {
        StringBuilder texto=new StringBuilder();
        for(var p:itens)texto.append(p.id()).append('\t').append(p.hora()).append('\t').append(p.ml()).append('\t').append(p.competicao()).append('\t').append(p.cancelada()).append('\t').append(p.instante()).append('\n');
        gravar(pasta.resolve("pausas-"+dia+".tsv"),texto.toString());
    }
    synchronized void gravar(Path destino,String texto) throws IOException {
        Files.createDirectories(pasta);
        Path temp=Files.createTempFile(pasta,"agua-",".tmp");
        try {
            Files.writeString(temp,texto);
            try(var canal=java.nio.channels.FileChannel.open(temp,StandardOpenOption.WRITE)){canal.force(true);}
            try {Files.move(temp,destino,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,destino,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temp);}
    }
}
