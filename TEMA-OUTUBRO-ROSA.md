# Outubro Rosa — tema e efeitos (1.2.23)

## Estrutura preservada

Java 26 / Swing; entrada `Main.java`, sem bibliotecas novas. Preferências em
`%USERPROFILE%\.orvalho\preferencias.properties`. Não foram encontradas instruções
AGENTS.md no repositório na análise inicial. A regra de manter a versão até pedido explícito foi registrada posteriormente em AGENTS.md. A lógica de lembretes, horários, pausas, registros,
competição e sincronização continua nos serviços existentes.

## Usar

Instale `distribuicao/final-1.2.23/instalador/Orvalho-1.2.23.exe`. Em Configurações > Tema:

- Tema original.
- Tema original escuro.
- Outubro Rosa (aparência suave).

Efeitos pode ser alterado apenas em Configurações > Efeitos e animações. A escolha é salva.
O tema original azul é o padrão. Na primeira abertura da 1.2.22, instalações anteriores também voltam ao tema original uma única vez. Outubro Rosa pode ser escolhido em Configurações > Tema e essa escolha fica salva nos próximos reinícios. A preferência anterior de aparência escura do tema original é preservada. O selo continua abrindo a tela educativa quando o tema Outubro Rosa está ativo.

Para desenvolver, na raiz do projeto, com JDK 26 e Maven:

```powershell
mvn test
mvn package
java --enable-native-access=ALL-UNNAMED -jar target/BebaAgua-1.0-SNAPSHOT.jar
```

O instalador inclui Java e a opção de acesso nativo. Ela permite apenas a consulta
implementada à preferência de animações do Windows; não muda configurações do sistema.

## Visual e animações

- Fundo #FFF7FA, cartões brancos, rosa #C73C75, rosa claro #FCE4EE, texto #42102D.
- Apenas um tema Outubro Rosa, com cartão de lembrete suave. A preferência antiga rosaVibrante é ignorada na leitura e removida no próximo salvamento, preservando o tema e as demais opções.
- Segoe UI, controles reais, foco visível, cartões arredondados e sombras discretas.
- 12 pétalas vetoriais (8 em janelas estreitas), arrays fixos, ciclos de 12–24 s,
  tamanhos de 10–26 px e opacidade de aproximadamente 0,15–0,39. Desenho restrito às
  margens, excluindo a barra de rolagem; não intercepta eventos nem recebe foco.
- Uma única fonte de pulsos Swing de 40 ms atende os efeitos. O relógio dos
  lembretes permanece independente. Componentes ocultos, minimizados ou descartados
  deixam de animar. Ao desativar Efeitos, o progresso vai direto ao valor real.
- No Outubro Rosa, garrafa, boneco, litros e barra de progresso compartilham o rosa #C73C75 e o gradiente claro #F5A9CC. No tema original, os indicadores continuam azuis.
- A garrafa continua representando **tempo até o lembrete**, conforme sua dica;
  não representa litros. O boneco e a barra usam os registros reais e o peso.
- Barra de hidratação: transição de 550 ms quando a fração muda, sem reiniciar
  em consultas com o mesmo valor. Botões: transições de 180 ms. Ondas lentas.
- Não foi adicionada celebração extra (era opcional).
- Janela inicial com largura aproximada de 590 px. Largura mínima de 460 px,
  garrafa menor em largura reduzida, rolagem exclusivamente vertical.

## Movimento reduzido

No Windows, consulta somente leitura de `SystemParametersInfoW` com
`SPI_GETCLIENTAREAANIMATION`, usando a API nativa do próprio JDK 26. A consulta é
feita na configuração, ao ativar a janela e a cada cinco segundos enquanto há
animação. Quando os efeitos já estão suspensos, voltar o foco para o aplicativo
reconsulta a preferência do sistema. Em outros sistemas ou ao executar sem a
opção de acesso nativo, o controle manual Efeitos permanece disponível.

Referência: https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-systemparametersinfoa

## Arquivos desta revisão

Novos: `AnimacoesOrvalho.java`, `MovimentoWindows.java`, `PetalasOrvalho.java`,
`PainelRolavel.java` e `EfeitosOrvalhoTest.java`.

Atualizados: `DadosAgua.java`, `TemaOrvalho.java`, `JanelaPrincipal.java`,
`PainelAgua.java`, `BotaoOrvalho.java`, `PopupLembrete.java`, `GarrafaAnimada.java`,
`CorpoHidratacao.java`, `PainelHidratacao.java`, `TemaOutubroRosaTest.java`,
`GarrafaAnimadaTest.java`, `pom.xml`, `empacotar.ps1`, `atualizacao.properties`,
`README.md` e este documento. Fontes Java em `src/main/java/com/mycompany/bebaAgua`,
testes em `src/test/java/com/mycompany/bebaAgua`.

## Validação

- Suíte Java completa: 99 testes aprovados, incluindo lembretes, confirmação única, adiamento, histórico,
  competição, peso, sincronização e seleção de temas.
- Teste com AWT Robot: clique em Iniciar e Espaço em Pausar através da camada decorativa. O controle Efeitos fica somente nas configurações.
- Preferências de tema e efeitos persistem entre instâncias; leitura das antigas preferências vibrantes preserva as demais opções. Simulação de
  movimento reduzido e retomada; ocultação e descarte encerram a atividade visual.
- Reciclagem de 10.000 passos mantém o número fixo de partículas.
- Geometria dos controles em 460 px e ausência de barra horizontal.
- Inspeção visual dos temas Outubro Rosa e original em 590 px e 460 px, com dados
  locais de demonstração. Consulta nativa disponível no Windows desta máquina.

Revisão publicada no Cloudflare em 06/10/2026, mantendo a versão 1.2.20. Não foram feitas
transações de teste no ranking de produção. Não há afirmações de certificação,
conformidade com NR-1 ou benefícios médicos decorrentes do tema.

## Publicação da revisão 1.2.20 — 06/10/2026

- Produção: https://orvalho-forest.pages.dev
- Implantação: https://42a374c3.orvalho-forest.pages.dev
- Descrição: Tema Outubro Rosa refinado: garrafa rosa e efeitos nas configuracoes.
- Instalador: https://github.com/BrunoTrech/Orvalho-Releases/releases/download/v1.2.20/Orvalho-1.2.20-27851388a44f.exe
- SHA-256: `27851388a44f94f652fda8611453118ebe356188f2845af66086f57b36daeae9` (28.980.344 bytes).
- Download remoto conferido com o arquivo validado; assinatura Authenticode válida nesta máquina. Manifesto Ed25519 e link do site de produção verificados.
- O instalador anterior permanece disponível no seu endereço, preservando manifestos antigos e downloads em andamento.
- Como a versão do aplicativo continua 1.2.20, computadores já nessa versão não recebem esta revisão automaticamente; é necessário baixar e reinstalar manualmente. Instalações locais 1.2.21/1.2.22 exigem desinstalar a versão maior antes.
- `publicar-versao.ps1` aceita `-Instalador`, `-Imagem` e `-PermitirRevisaoMesmaVersao` com `-UsarInstaladorExistente`. Uma revisão usa um nome de arquivo com hash, preservando o artefato anterior, sem mudar a versão do aplicativo.
## Publicação 1.2.21 — atualização automática — 06/10/2026

Versão elevada para 1.2.21 a pedido explícito do usuário, para distribuir as mesmas melhorias aos computadores que estão na 1.2.20. A regra de não incrementar sem pedido permanece em AGENTS.md.

- Produção: https://orvalho-forest.pages.dev
- Implantação: https://476ce3bf.orvalho-forest.pages.dev
- Descrição: Tema Outubro Rosa atualizado: efeitos nas configuracoes e garrafa rosa.
- Instalador: https://github.com/BrunoTrech/Orvalho-Releases/releases/download/v1.2.21/Orvalho-1.2.21.exe
- Compilação validada: `distribuicao/publicacao-rosa-1.2.21/imagem/Orvalho`.
- Instalador validado: `distribuicao/publicacao-rosa-1.2.21/instalador/Orvalho-1.2.21.exe`. Usar estes caminhos ao republicar; as pastas antigas de 1.2.21 contêm um protótipo local anterior.
- SHA-256: `d1f04aeea787b34e7b50ce87c816e8d2b167afbd79bf94afdf7d5458425b84d7`; 28.980.344 bytes.
- 99 testes aprovados; executável e versão interna do JAR verificados; assinatura Authenticode com carimbo de tempo e configuração de instalação por usuário verificadas.
- Download público conferido byte a byte por SHA-256 e tamanho, assinatura validada nesta máquina. Site HTTP 200 aponta para 1.2.21; manifesto Ed25519 de produção validado e servido sem cache.
- Computadores em 1.2.20 ou anterior, no canal empresa, passam a detectar 1.2.21 nas consultas automáticas com aplicativo aberto e conexão. A instalação aguarda o fechamento de janelas em uso e confirmação de lembretes. Não foi realizada verificação remota da instalação nos computadores dos funcionários.
- Instalações locais com o protótipo 1.2.21 ou com 1.2.22 não recebem esta compilação automaticamente; exigem reinstalação assistida. Não são as versões públicas anteriores.
## Revisão local — hidratação rosa — 06/10/2026

- Mantida a versão 1.2.21; esta revisão foi publicada posteriormente, conforme a seção abaixo.
- Removida a opção vibrante, incluindo seu estado e pintura. Instalações que usavam essa opção passam a exibir o tema Outubro Rosa suave.
- Cores da hidratação unificadas: litros, barra e boneco passam a acompanhar a garrafa no Outubro Rosa; o tema original preserva seus azuis.
- Arquivos desta revisão: DadosAgua.java, PainelAgua.java, TemaOrvalho.java, JanelaPrincipal.java, CorpoHidratacao.java, PainelHidratacao.java; ajustes nos testes existentes TemaOutubroRosaTest.java e EfeitosOrvalhoTest.java.
- 99 testes aprovados, incluindo a preservação de preferências antigas e a troca de tema. Capturas com dados de demonstração conferidas nos dois temas.
- Instalador local: `distribuicao/rosa-unificado-1.2.21/instalador/Orvalho-1.2.21.exe`. Não confundir com o instalador público, descrito na seção de publicação anterior.
## Publicação da hidratação rosa — revisão 1.2.21 — 06/10/2026

- Produção: https://orvalho-forest.pages.dev
- Implantação: https://ad5eca08.orvalho-forest.pages.dev
- Descrição: Hidratacao com cores do Outubro Rosa. Tema rosa vibrante removido.
- Instalador: https://github.com/BrunoTrech/Orvalho-Releases/releases/download/v1.2.21/Orvalho-1.2.21-2bf0964e7168.exe
- SHA-256: `2bf0964e7168982be8fb974929c31aeedcc09f47a6a634503402f1b220b56018`; 28.980.344 bytes.
- Publicado exatamente o instalador previamente validado com 99 testes, sem recompilação. Download público, SHA-256, tamanho e assinatura Authenticode conferidos. Manifesto Ed25519 e link do site principal verificados.
- Mantida a versão 1.2.21 conforme orientação do usuário. Quem já tem 1.2.21 não recebe esta revisão automaticamente; exige reinstalação manual. Versões públicas inferiores podem detectar 1.2.21 normalmente.
- Os instaladores publicados anteriormente foram preservados com suas URLs e hashes. O site e o manifesto apontam para o novo arquivo.
## Publicação 1.2.22 — tema azul por padrão — 06/10/2026

- Produção: https://orvalho-forest.pages.dev
- Implantação: https://f3c59aad.orvalho-forest.pages.dev
- Descrição: Tema azul por padrao e Outubro Rosa opcional nas configuracoes.
- Instalador: https://github.com/BrunoTrech/Orvalho-Releases/releases/download/v1.2.22/Orvalho-1.2.22.exe
- Compilação validada: `distribuicao/azul-padrao-1.2.22/imagem/Orvalho`.
- Instalador validado: `distribuicao/azul-padrao-1.2.22/instalador/Orvalho-1.2.22.exe`. As antigas pastas locais 1.2.22 contêm um protótipo anterior; usar os caminhos desta publicação.
- SHA-256: `422b54bee46303877ee695b516f84dc2012bc8c6901964c145678fa2c81f02a2`; 28.980.344 bytes.
- Novas instalações começam no tema original azul. A migração `temaAzulPadrao122Aplicado` desativa Outubro Rosa uma única vez nas instalações anteriores. Após escolher Outubro Rosa nas configurações, a preferência permanece nos próximos reinícios. Som, humor, efeitos e aparência escura anterior do tema original são preservados.
- Mantido o marcador antigo da campanha no arquivo apenas para compatibilidade; ele não ativa mais o Outubro Rosa nesta versão. Preferências antigas não distinguiam ativação automática de seleção manual, por isso a primeira migração desativa a campanha em ambos os casos.
- Arquivos alterados: DadosAgua.java, testes TemaOutubroRosaTest.java, DadosAguaTest.java e EfeitosOrvalhoTest.java, AGENTS.md, empacotar.ps1, criar-instalador.ps1, versão em atualizacao.properties, documentação e arquivos de publicação do site.
- 99 testes aprovados, incluindo nova instalação azul, migração de configurações legadas e persistência de escolha posterior. Versão interna do JAR e dos binários conferida. Instalador assinado e configuração por usuário validada pelo empacotamento.
- Download público, SHA-256, tamanho e assinatura Authenticode conferidos; manifesto Ed25519 e link no site principal verificados. A atualização automática está disponível para versões públicas anteriores, com o aplicativo aberto e conectado. Não foi verificada remotamente a instalação nos computadores dos funcionários.

## Publicação final do tema — revisão 1.2.22 — 07/10/2026

- Mensagem solicitada: **Versão final com o tema Outubro Rosa implementado.**
- Produção: https://orvalho-forest.pages.dev
- Instalador: https://github.com/BrunoTrech/Orvalho-Releases/releases/download/v1.2.22/Orvalho-1.2.22-c346ce377d02.exe
- Imagem validada: `distribuicao/outubro-final-1.2.22-20261007/imagem/Orvalho`.
- Instalador local: `distribuicao/outubro-final-1.2.22-20261007/instalador/Orvalho-1.2.22.exe`.
- SHA-256: `c346ce377d0226d9d352634521e0a81f422dc5102a80e1b94529e137e3739677`; 28.980.344 bytes.
- Tema original escuro agora está na lista de temas; o checkbox separado foi removido. As preferências existentes continuam compatíveis.
- Tela educativa reorganizada com títulos numerados, sinais em tópicos, orientações destacadas e textos que se ajustam à largura. Conteúdo revisado visualmente nas aparências rosa e escura, inclusive em janela estreita.
- Tema azul permanece padrão, com Outubro Rosa opcional e escolha persistida.
- 99 testes Java e 15 testes do Worker aprovados. Executável e instalador assinados com carimbo de tempo; MSI por usuário e sem elevação verificado. O JAR empacotado corresponde ao validado e contém as alterações desta revisão.
- Download público conferido pelo cliente Java: assinatura Ed25519 do manifesto, SHA-256 e tamanho corretos. Site HTTP 200 aponta para a revisão; manifesto servido com `Cache-Control: no-store, max-age=0`.
- Versão mantida em 1.2.22 por orientação do usuário. Computadores já na 1.2.22 exigem reinstalação manual para receber esta revisão; versões inferiores podem detectar a atualização. Instaladores anteriores e seus hashes foram preservados.

## Publicação 1.2.23 — atualização automática — 07/10/2026

- Número elevado a pedido explícito do usuário, sem alterações de interface ou funcionalidades em relação à revisão final da 1.2.22. Comparação dos JARs confirmou a mudança da versão e instruções compiladas equivalentes.
- Produção: https://orvalho-forest.pages.dev
- Instalador: https://github.com/BrunoTrech/Orvalho-Releases/releases/download/v1.2.23/Orvalho-1.2.23.exe
- Imagem: `distribuicao/final-1.2.23/imagem/Orvalho`; instalador: `distribuicao/final-1.2.23/instalador/Orvalho-1.2.23.exe`.
- SHA-256: `64bc6c3fae1497dd5dacc0eb665cae6affb39066b79847904be1436b278d07dd`; 28.984.440 bytes.
- 99 testes Java aprovados. Executável e instalador assinados com carimbo de tempo; configuração de instalação por usuário validada.
- O manifesto de produção anuncia 1.2.23. Computadores no canal empresa com versões inferiores, inclusive 1.2.22, podem atualizar automaticamente com o aplicativo aberto e conectado. A instalação aguarda o encerramento das interações pendentes; não foi verificada remotamente a instalação nos computadores dos funcionários.
