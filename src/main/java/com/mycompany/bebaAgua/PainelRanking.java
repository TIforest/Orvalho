package com.mycompany.bebaAgua;

import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.*;

/** Ranking diário: dados do servidor, sem inferir a identidade pelo nome. */
final class PainelRanking extends JPanel {
    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
    private final boolean escuro;
    private final Color fundo, cartao, borda, texto, secundario;
    private final JLabel posicao, litros, participantes, atualizado, estado;
    private final JButton data=new JButton();
    private final JLabel tituloRanking;
    private LocalDate diaSelecionado;
    private final DefaultListModel<CompeticaoService.Participante> modelo = new DefaultListModel<>();
    private final JList<CompeticaoService.Participante> lista = new JList<>(modelo);
    private final JButton atualizar = new JButton("Atualizar ranking");
    private final JButton sair = new JButton("Sair da competição");
    private final JButton conquistas=new JButton("Conquistas do mês");
    private boolean carregado;

    PainelRanking(boolean escuro) {
        this.escuro = escuro;
        fundo = TemaOrvalho.cor(new Color(escuro ? 0x102131 : 0xF4F8FC));
        cartao = TemaOrvalho.cor(new Color(escuro ? 0x172B3D : 0xFFFFFF));
        borda = TemaOrvalho.cor(new Color(escuro ? 0x315169 : 0xD7E4EF));
        texto = TemaOrvalho.cor(new Color(escuro ? 0xE7F0F8 : 0x12304A));
        secundario = TemaOrvalho.cor(new Color(escuro ? 0xA6BBD0 : 0x52748E));
        setLayout(new BorderLayout(0, 14)); setBackground(fundo);
        setBorder(BorderFactory.createEmptyBorder(20, 22, 16, 22));

        JPanel topo = vertical();
        topo.add(label("ORVALHO", 11, true, secundario)); topo.add(Box.createVerticalStrut(6));
        JPanel titulo = transparente(new BorderLayout(12, 0));
        tituloRanking=label("Ranking do dia",27,true,texto);titulo.add(tituloRanking,BorderLayout.WEST);
        BotaoOrvalho.aplicar(data,false);TemaOrvalho.aplicar(data,escuro);
        data.setText("Hoje · "+LocalDate.now(BRASILIA).format(DateTimeFormatter.ofPattern("dd/MM")));
        data.setBorder(BorderFactory.createEmptyBorder(8,12,8,12));data.setFont(new Font("Segoe UI",Font.BOLD,12));
        data.setToolTipText("Escolher a data do ranking");data.getAccessibleContext().setAccessibleName("Escolher a data do ranking");
        titulo.add(data,BorderLayout.EAST);
        topo.add(titulo); topo.add(Box.createVerticalStrut(6));
        topo.add(label("Uma pequena pausa. Um bom hábito.", 13, false, secundario));
        topo.add(Box.createVerticalStrut(16));
        JPanel resumo = card(cartao, borda); resumo.setLayout(new GridLayout(1, 2, 16, 0));
        resumo.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        posicao = label("—", 23, true, texto); litros = label("—", 23, true, texto);
        resumo.add(metrica("Sua posição", posicao)); resumo.add(metrica("Você registrou", litros));
        topo.add(resumo); topo.add(Box.createVerticalStrut(16));
        JPanel cabecalho = transparente(new BorderLayout());
        participantes = label("Participantes", 14, true, texto);
        cabecalho.add(participantes, BorderLayout.WEST);
        cabecalho.add(label("Litros registrados", 12, false, secundario), BorderLayout.EAST);
        topo.add(cabecalho); add(topo, BorderLayout.NORTH);

        lista.setBackground(fundo); lista.setFixedCellHeight(64);
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setCellRenderer((view, p, index, selected, focus) -> linha(p, focus));
        lista.getAccessibleContext().setAccessibleName("Ranking diário de participantes e litros registrados");
        JScrollPane scroll = new JScrollPane(lista);
        scroll.setBorder(BorderFactory.createEmptyBorder()); scroll.getViewport().setBackground(fundo);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(32);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() { thumbColor = borda; trackColor = fundo; }
            @Override protected JButton createDecreaseButton(int orientation) { return semSeta(); }
            @Override protected JButton createIncreaseButton(int orientation) { return semSeta(); }
            private JButton semSeta() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
        });
        scroll.setPreferredSize(new Dimension(480, 320)); add(scroll, BorderLayout.CENTER);

        JPanel rodape = vertical();
        atualizado = label("Consultando ranking…", 11, false, secundario);
        rodape.add(atualizado); rodape.add(Box.createVerticalStrut(3));
        rodape.add(label("O ranking recomeça à meia-noite, no horário de Brasília.", 11, false, secundario));
        estado = label(" ", 11, false, secundario); rodape.add(estado);
        rodape.add(Box.createVerticalStrut(8));
        JPanel aviso = card(cartao, borda); aviso.setLayout(new BorderLayout(0, 4));
        aviso.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        aviso.add(label("Beba água com consciência.", 12, true, texto), BorderLayout.NORTH);
        JTextArea cuidado = new JTextArea("Não beba em excesso para subir no ranking. Respeite suas necessidades e orientações de saúde.");
        cuidado.setEditable(false); cuidado.setOpaque(false); cuidado.setLineWrap(true); cuidado.setWrapStyleWord(true);
        cuidado.setFont(new Font("Segoe UI", Font.PLAIN, 11)); cuidado.setForeground(secundario);
        cuidado.setRows(2); aviso.add(cuidado, BorderLayout.CENTER); rodape.add(aviso);
        rodape.add(Box.createVerticalStrut(12));
        BotaoOrvalho.aplicar(atualizar, true); TemaOrvalho.aplicar(atualizar, escuro);
        atualizar.setAlignmentX(LEFT_ALIGNMENT); atualizar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        JPanel acoes=transparente(new GridLayout(1,2,10,0));
        BotaoOrvalho.aplicar(conquistas,false);TemaOrvalho.aplicar(conquistas,escuro);
        acoes.add(atualizar);acoes.add(conquistas);rodape.add(acoes); rodape.add(Box.createVerticalStrut(3));
        JPanel sairLinha = transparente(new FlowLayout(FlowLayout.CENTER, 0, 0));
        BotaoOrvalho.link(sair); TemaOrvalho.aplicar(sair, escuro); sairLinha.add(sair); rodape.add(sairLinha);
        JLabel nota = label("Participação voluntária · Volumes autodeclarados", 10, false, secundario);
        JPanel notaLinha = transparente(new FlowLayout(FlowLayout.CENTER, 0, 0)); notaLinha.add(nota); rodape.add(notaLinha);
        add(rodape, BorderLayout.SOUTH);
    }

    LocalDate diaSelecionado(){return diaSelecionado;}
    JButton botaoData(){return data;}
    boolean escolherDia(){
        LocalDate dia=PainelDataRanking.escolher(this,diaSelecionado==null?LocalDate.now(BRASILIA):diaSelecionado,escuro);
        if(dia==null)return false;
        diaSelecionado=dia.equals(LocalDate.now(BRASILIA))?null:dia;return true;
    }
    static LocalDate validarDia(String valor,LocalDate hoje){
        try{LocalDate dia=LocalDate.parse(valor.strip(),DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT));
            if(dia.isAfter(hoje)||dia.isBefore(LocalDate.of(2026,1,1)))throw new IllegalArgumentException();return dia;
        }catch(java.time.DateTimeException e){throw new IllegalArgumentException("Data inválida",e);}
    }
    JButton botaoConquistas(){return conquistas;}
    JButton botaoAtualizar() { return atualizar; }
    JButton botaoSair() { return sair; }
    void carregando() {
        atualizar.setEnabled(false);data.setEnabled(false);estado.setText("Consultando…");
        modelo.clear();posicao.setText("—");litros.setText("—");participantes.setText("Participantes");carregado=false;
        LocalDate selecionado=diaSelecionado==null?LocalDate.now(BRASILIA):diaSelecionado;
        data.setText((diaSelecionado==null?"Hoje · ":"")+selecionado.format(DateTimeFormatter.ofPattern(diaSelecionado==null?"dd/MM":"dd/MM/yyyy")));
        tituloRanking.setText(diaSelecionado==null?"Ranking do dia":"Ranking por dia");
    }
    void mostrar(CompeticaoService.Ranking ranking) {
        modelo.clear(); ranking.participantes().forEach(modelo::addElement);
        participantes.setText("Participantes · " + modelo.size());
        String dia = ranking.dia().format(DateTimeFormatter.ofPattern(diaSelecionado==null?"dd/MM":"dd/MM/yyyy"));
        data.setText((ranking.dia().equals(LocalDate.now(BRASILIA)) ? "Hoje · " : "Dia · ") + dia);
        var proprio = ranking.participantes().stream().filter(CompeticaoService.Participante::proprio).findFirst();
        posicao.setText(proprio.map(p -> p.posicao() + "º lugar").orElse("—"));
        litros.setText(proprio.map(p -> volume(p.ml())).orElse("—"));
        atualizado.setText("Atualizado às " + ZonedDateTime.now(BRASILIA).format(DateTimeFormatter.ofPattern("HH:mm")) + " · Horário de Brasília");
        estado.setText(!ranking.aviso().isBlank() ? ranking.aviso() : modelo.isEmpty() ? "Ainda não há participantes neste ranking." : " ");
        estado.setToolTipText(estado.getText()); atualizar.setEnabled(true);data.setEnabled(true); carregado = true;
    }
    void falhou() {
        estado.setText(carregado ? "Falha ao atualizar. Exibindo a última consulta; tente novamente." : "Não foi possível carregar. Seus registros locais continuam salvos.");
        estado.setToolTipText(estado.getText());
        if (!carregado) atualizado.setText("Ranking indisponível");
        atualizar.setEnabled(true);data.setEnabled(true);
    }
    private JPanel linha(CompeticaoService.Participante p, boolean focus) {
        Color corBorda = p.proprio() || focus ? TemaOrvalho.cor(new Color(0x2785F2)) : borda;
        JPanel linha = card(p.proprio() ? TemaOrvalho.cor(new Color(escuro ? 0x163957 : 0xE7F2FF)) : cartao, corBorda);
        linha.setLayout(new BorderLayout(12, 0)); linha.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        JPanel icones = transparente(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JLabel lugar = p.posicao() <= 3 ? trofeu(p.posicao()) : badge(Integer.toString(p.posicao()),
                p.proprio() ? TemaOrvalho.cor(new Color(0x176BDA)) : TemaOrvalho.cor(new Color(escuro ? 0x284861 : 0xDFEAF4)),
                p.proprio() ? Color.WHITE : texto, 32);
        icones.add(lugar); icones.add(badge(iniciais(p.nome()), TemaOrvalho.cor(new Color(escuro ? 0x2B5276 : 0xD5E7F8)), texto, 36));
        linha.add(icones, BorderLayout.WEST);
        JPanel pessoa = transparente(new BorderLayout(8, 2));
        JPanel nomeLinha = transparente(new BorderLayout(6, 0));
        JLabel nome = label(p.nome(), 13, true, texto); nome.setToolTipText(p.nome()); nomeLinha.add(nome, BorderLayout.CENTER);
        if (p.proprio()) nomeLinha.add(label("Você", 11, true, TemaOrvalho.cor(new Color(escuro ? 0x82B5F5 : 0x176BDA))), BorderLayout.EAST);
        pessoa.add(nomeLinha, BorderLayout.NORTH);
        JLabel setor = label(p.setor(), 11, false, secundario); setor.setToolTipText(p.setor()); pessoa.add(setor, BorderLayout.SOUTH);
        linha.add(pessoa, BorderLayout.CENTER);
        linha.add(label(volume(p.ml()), 17, true, texto), BorderLayout.EAST);
        linha.getAccessibleContext().setAccessibleName(p.posicao()+"º, "+p.nome()+", "+p.setor()+", "+volume(p.ml())+(p.proprio()?", você":""));
        return linha;
    }
    private JPanel metrica(String titulo, JLabel valor) {
        JPanel p = vertical(); p.add(label(titulo, 12, false, secundario)); p.add(Box.createVerticalStrut(3)); p.add(valor); return p;
    }
    private static String volume(long ml) { return String.format(Locale.forLanguageTag("pt-BR"), "%.2f L", ml / 1000.0); }
    private static String iniciais(String nome) {
        String[] partes = nome.strip().split("\\s+");
        if (partes.length == 0 || partes[0].isEmpty()) return "?";
        String primeiro = new String(Character.toChars(partes[0].codePointAt(0)));
        return (primeiro + (partes.length > 1 ? new String(Character.toChars(partes[partes.length-1].codePointAt(0))) : "")).toUpperCase(Locale.forLanguageTag("pt-BR"));
    }
    private static JLabel label(String valor, int tamanho, boolean negrito, Color cor) {
        JLabel l = new JLabel(); l.putClientProperty("html.disable", true); l.setText(valor);
        l.setFont(new Font("Segoe UI", negrito ? Font.BOLD : Font.PLAIN, tamanho)); l.setForeground(cor); l.setAlignmentX(LEFT_ALIGNMENT); return l;
    }
    private JPanel vertical() {
        JPanel p = transparente(null); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); return p;
    }
    private static JPanel transparente(LayoutManager layout) {
        JPanel p = new JPanel(layout); p.setOpaque(false); p.setAlignmentX(LEFT_ALIGNMENT); return p;
    }
    private static JPanel card(Color fundo, Color borda) {
        return new JPanel() {
            { setOpaque(false); setAlignmentX(LEFT_ALIGNMENT); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D d = (Graphics2D)g.create(); d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                d.setColor(fundo); d.fillRoundRect(1,1,getWidth()-3,getHeight()-3,16,16);
                d.setColor(borda); d.drawRoundRect(1,1,getWidth()-3,getHeight()-3,16,16); d.dispose(); super.paintComponent(g);
            }
        };
    }
    private static JLabel trofeu(int posicao) {
        Color metal = switch (posicao) {
            case 1 -> new Color(0xF2C568);
            case 2 -> new Color(0xC2CDD7);
            default -> new Color(0xC58A5B);
        };
        String nome = posicao + "º lugar — troféu de " + switch (posicao) {
            case 1 -> "ouro"; case 2 -> "prata"; default -> "bronze";
        };
        JLabel l = new JLabel(new Icon() {
            public int getIconWidth() { return 32; }
            public int getIconHeight() { return 36; }
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D d = (Graphics2D)g.create(); d.translate(x, y);
                d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Alças, taça, haste e base: desenho vetorial igual em qualquer Windows.
                d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                d.setColor(metal.darker());
                d.drawRoundRect(2, 6, 28, 13, 8, 8);
                d.setColor(metal);
                var taca = new java.awt.geom.Path2D.Double();
                taca.moveTo(7, 4); taca.lineTo(25, 4); taca.lineTo(24, 14);
                taca.curveTo(23, 25, 9, 25, 8, 14); taca.closePath(); d.fill(taca);
                d.fillRoundRect(14, 21, 4, 8, 2, 2);
                d.fillRoundRect(9, 28, 14, 3, 2, 2);
                d.setColor(metal.darker()); d.fillRoundRect(6, 31, 20, 4, 2, 2);
                d.setColor(TemaOrvalho.cor(new Color(0x172B3D))); d.setFont(new Font("Segoe UI", Font.BOLD, 12));
                String numero = Integer.toString(posicao);
                TipografiaOrvalho.suavizar(d);d.drawString(numero, (32 - d.getFontMetrics().stringWidth(numero)) / 2, 17);
                d.dispose();
            }
        });
        l.setPreferredSize(new Dimension(32, 36)); l.setToolTipText(nome);
        l.getAccessibleContext().setAccessibleName(nome); return l;
    }
    private static JLabel badge(String valor, Color fundo, Color frente, int tamanho) {
        JLabel l = new JLabel(valor, SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D d = (Graphics2D)g.create(); d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                d.setColor(fundo); d.fillOval(0,0,getWidth(),getHeight()); d.dispose(); super.paintComponent(g);
            }
        };
        l.setPreferredSize(new Dimension(tamanho,tamanho)); l.setFont(new Font("Segoe UI",Font.BOLD,12)); l.setForeground(frente); return l;
    }
}
