# Orvalho

Lembretes de água para Windows, com execução na bandeja.

## Usar

- Abra distribuicao/saida/Orvalho/Orvalho.exe.
- Iniciar começa a contagem. Pausar congela o tempo; Iniciar retoma.
- Se você mudar o intervalo enquanto estiver pausado, Iniciar começa um intervalo novo.
- O X da janela principal esconde o aplicativo. Use Abrir ou Fechar na bandeja.
- O alerta fecha somente pelo botão Ok, obrigado! e não recebe foco de teclado.
- Outra execução mostra uma mensagem para usar a bandeja.
- Iniciar com o Windows abre na bandeja e começa os lembretes ao entrar na conta.
- Ative a inicialização pelo executável, depois de colocá-lo em uma pasta fixa.
- Se mudar a pasta, abra o novo executável e marque a opção indicada como atualizar caminho.
- Para remover o app, primeiro desmarque a inicialização, encerre pela bandeja e remova a pasta.
- Compartilhe a pasta Orvalho inteira: ela inclui o Java. Não compartilhe apenas o EXE.
- Ao reabrir o programa, o intervalo salvo começa inteiro. A pausa não é persistida entre execuções.

## Desenvolver e testar

Requisitos: Windows, JDK 26 e Maven (os incluídos no NetBeans podem ser usados).
Abra o pom.xml no NetBeans. Use Main.java para executar em desenvolvimento.
Para testes e geração do JAR, na pasta do projeto:

    mvn test
    mvn package

Os testes não modificam o registro de inicialização do usuário.
target/ contém arquivos gerados, não o código-fonte.

## Empacotar

No PowerShell, configure JAVA_HOME para o JDK 26 e execute:

    .\empacotar.ps1

O script usa uma pasta de saída com data/hora, para não sobrescrever um aplicativo aberto.
Encerre a versão anterior pela bandeja antes de substituir a pasta instalada.
Mantenha o destino antigo ao atualizar para preservar o caminho de inicialização.
Se escolher outro destino, abra o novo EXE e atualize a opção Iniciar com o Windows.

O ícone da janela e bandeja fica em src/main/resources/imagens/agua.png.
O ícone do executável fica em distribuicao/orvalho.ico.
O identificador interno BebaAgua e o arquivo .pausa-azul.lock foram preservados
para manter compatibilidade com as configurações e execuções anteriores.

## Instalador Windows

O instalador atual fica em `distribuicao/instalador/Orvalho-1.1.3.exe` e inclui Java.
Instala somente para o usuário atual, por padrão em `%LOCALAPPDATA%\Orvalho`,
com seleção da pasta e dos atalhos. A desinstalação é feita pelos Aplicativos do Windows.
As preferências em `%USERPROFILE%\.orvalho` são preservadas na desinstalação.
Antes de desinstalar, desmarque Iniciar com o Windows e encerre pela bandeja.

Para gerar novamente, atualize primeiro a imagem com `empacotar.ps1` e execute:

    .\criar-instalador.ps1 -Imagem 'caminho\da\nova\Orvalho'

Requer JDK 26 e os binários do WiX 3.14.1 (padrão: `%LOCALAPPDATA%\OrvalhoBuild\wix-3.14.1`).
Use `-Wix` para outro local. O UUID de atualização do script deve permanecer estável;
para futuras versões, incremente a versão tanto na imagem quanto no instalador.
Ao migrar da versão portátil, encerre a antiga, abra a instalada e atualize
Iniciar com o Windows para registrar o novo caminho.

## Distribuicao e atualizacoes automaticas

- Site e manifesto: https://orvalho-forest.pages.dev (Cloudflare Pages, conta ti@forest.ind.br).
- Instaladores publicos: https://github.com/BrunoTrech/Orvalho-Releases/releases . O codigo continua no repositorio privado.
- O aplicativo consulta apos 30 segundos e a cada 6 horas. Nao existe envio instantaneo: o computador precisa estar ligado, conectado e com o app em execucao.
- Quem usa a versao 1.0.0 deve instalar a nova versao manualmente uma vez.
- Antes de executar, o atualizador valida a assinatura Ed25519 do manifesto, tamanho e SHA-256 do instalador. A conexao usa HTTPS e redirecionamento restrito ao armazenamento de releases do GitHub.
- O instalador roda por usuario, fecha/reabre o aplicativo e restaura intervalo, contagem e estado ativo/pausado. O tempo gasto instalando funciona como uma breve pausa.
- Falhas de rede nao interrompem os lembretes. Falhas de instalacao sao registradas em `%USERPROFILE%\.orvalho\atualizacoes`; o aplicativo anterior e reaberto se ainda existir. Uma tentativa de instalacao com falha aguarda 6 horas para repetir.

Para uma proxima versao, altere/teste o codigo e execute, por exemplo:

    .\publicar-versao.ps1 -Versao 1.1.4 -Notas 'Descreva as mudancas desta versao.'

O script compila/testa, gera a imagem e instalador, publica no GitHub, assina o manifesto e publica no Cloudflare. Use sempre um numero maior e novo. Requer Git Credential Manager autenticado no GitHub e Wrangler autenticado na conta correta. Nao publique o projeto inteiro como site: somente `site/`.

A chave privada de publicacao fica em `%USERPROFILE%\.orvalho-publicacao\ed25519.pk8`, fora do projeto e com acesso restrito ao usuario. Guarde uma copia de seguranca protegida: perder a chave impede que as instalacoes existentes aceitem novas atualizacoes. Nunca envie essa chave para o GitHub, Cloudflare ou dentro do instalador.

## Conferencias em outros computadores

Validacao da publicacao 1.1.2: 23 testes passaram; consulta HTTPS ao Cloudflare,
assinatura do manifesto, download publico pelo GitHub e SHA-256 foram verificados
com o mesmo codigo de rede do aplicativo. O auxiliar empacotado rejeitou um arquivo
adulterado sem executar o instalador.

Piloto completo validado em 25/09/2026: a instalacao local ainda era 1.0.0, sem
atualizador. Foi migrada manualmente para 1.1.2; depois o proprio aplicativo
consultou o Cloudflare, baixou e validou a 1.1.3, encerrou, instalou e reabriu
automaticamente. O MSI retornou 0 e a JVM reaberta informou 1.1.3. Foram preservados
o intervalo de 30 minutos, o estado pausado, a janela oculta e o registro de inicio
com o Windows. Logs em `%USERPROFILE%\.orvalho\atualizacoes\instalacao.log` e `msi.log`.

- Sair e entrar novamente na conta com inicialização marcada e desmarcada.
- Suspender/retomar o computador e observar o próximo alerta. Durante a suspensão
  nenhum alerta pode ser exibido; o comportamento do relógio depende do ambiente.
- Executar em outro computador Windows sem Java instalado, compatível com a arquitetura do pacote.
- Conferir visualmente com as escalas reais de tela de 100%, 150% e 200%.
- Se o Windows desativar a entrada pela própria configuração de aplicativos de inicialização,
  verificar e reativar ali: a opção do Orvalho acompanha o registro Run, não todas as políticas do sistema.

O intervalo é salvo ao clicar em Iniciar, no arquivo .orvalho/intervalo.txt dentro da pasta do usuário. A gravação é confirmada antes de iniciar os lembretes; falhas aparecem na tela. O registro Java antigo só é usado para migrar a primeira configuração, quando esse arquivo não existe.
