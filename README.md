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

Os testes não modificam o registro de inicialização do usuário. O teste nativo usa uma chave temporária isolada em HKCU\Software\OrvalhoTeste-<UUID> e a remove ao terminar.
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
O identificador interno BebaAgua foi preservado para manter compatibilidade com as configurações.
O arquivo `.orvalho.lock` impede duas execuções simultâneas desta versão.
Ao migrar de uma versão com outro nome de trava, encerre a anterior pela bandeja antes de abrir a nova.

## Instalador Windows

O instalador publicado fica em `distribuicao/instalador/Orvalho-1.1.5.exe` e inclui Java. A versão 1.1.5 foi publicada sem assinatura Authenticode; os manifestos de atualização continuam assinados com Ed25519.
Instala somente para o usuário atual, por padrão em `%LOCALAPPDATA%\Orvalho`,
com seleção da pasta e dos atalhos. A desinstalação é feita pelos Aplicativos do Windows.
As preferências em `%USERPROFILE%\.orvalho` são preservadas na desinstalação.
Encerre pela bandeja antes de desinstalar. Nas versões anteriores à 1.1.4, desmarque também Iniciar com o Windows; a partir da 1.1.4 o instalador limpa a entrada correspondente.

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

## Correções de distribuição preparadas para 1.1.4

- Menu da bandeja **Sobre e atualizações** (também F1 na janela): versão, última consulta persistida, resultado, consulta manual e abertura da pasta de logs. A consulta manual permite repetir uma instalação que falhou sem esperar seis horas.
- O processo auxiliar verifica o download e confirma prontidão antes de o aplicativo autorizar o fechamento. Cancelamento ou falha antes dessa confirmação mantém a janela original funcionando. A versão instalada é conferida no Orvalho.cfg depois do MSI.
- Histórico de atualização com rotação de aproximadamente 1 MiB por arquivo. Diagnóstico de instalação em `.orvalho/atualizacoes/resultado.txt`, `instalacao.log` e `msi.log`; o MSI anterior é mantido em `msi.log.1`.
- Atualizações consultam o proxy configurado no Windows. TLS continua verificando os certificados e o nome do servidor, com confiança nas raízes do Java e nos certificados administrados no Windows (conta e máquina). O manifesto Ed25519 e o hash continuam obrigatórios. Proxy com autenticação e bloqueios por política podem exigir ação do TI; não há desativação de HTTPS nem do antivírus.
- Inicialização com Windows usa `reg.exe`, sem PowerShell codificado. Continua limitada ao usuário atual. Políticas do Windows/EDR e a desativação pelo Gerenciador de Tarefas podem prevalecer sobre o registro Run.
- A desinstalação das novas versões remove a entrada Run somente quando corresponde ao caminho desinstalado. Em upgrades ela é preservada; há ação de rollback para restaurá-la se a desinstalação falhar. O modelo `packaging/windows/main.wxs` é específico do jpackage 26 e WiX 3.
- Estimativa de espaço livre nos volumes de download, instalação e temporários antes do download. O auxiliar copia somente Orvalho.exe, app e runtime, em pasta exclusiva. Sessões concluídas e sem trava são removidas ao iniciar e antes de atualizar; arquivos alheios não são apagados.
- O pacote inclui somente os módulos Java necessários, incluindo `jdk.crypto.mscapi`. O pacote continua Windows x64, por usuário, com Java incluído. Não é um instalador universal nem instala para todos os usuários da empresa.

### Assinatura digital do Windows — pendente de certificado

A empresa ainda não possui certificado de assinatura de código. Nenhuma assinatura confiável foi criada e os alertas de reputação/antivírus ainda podem aparecer. O processo está preparado para um certificado confiável disponível em `Cert:\CurrentUser\My` e SignTool do Windows SDK:

    $env:ORVALHO_CERT_THUMBPRINT = 'impressao digital do certificado'
    $env:ORVALHO_SIGNTOOL = 'caminho completo para signtool.exe'
    .\publicar-versao.ps1 -Versao 1.1.4 -Notas 'Correções de instalação e atualização'

O launcher é assinado antes de empacotar; o instalador é assinado depois, com SHA-256 e carimbo de tempo RFC 3161. A assinatura é verificada antes de publicar. Serviços de assinatura em nuvem podem exigir integração própria em `tools/AssinarWindows.ps1`.

A publicação normal recusa arquivos sem assinatura válida. Para um piloto conscientemente sem assinatura existe `-PermitirSemAssinatura`. Isso NÃO evita bloqueios do Windows ou Bitdefender. Não publique novamente um número de versão que já foi distribuído.

### Validação e limites

Foram testados cálculo de espaço, histórico de logs, confirmação/cancelamento do auxiliar, limpeza com trava, rejeição de certificados não confiáveis, leitura de caminhos com acentos e preservação do comportamento do aplicativo. O auxiliar empacotado foi iniciado em perfil de teste e cancelado antes da instalação. As condições de limpeza do Run foram avaliadas dentro do MSI: desinstalação própria permite a ação; upgrade e caminho alheio a impedem.

Ainda é necessário um piloto em outro computador com usuário comum, sem Java externo, com o antivírus e proxy reais da empresa, incluindo instalar, atualizar, reiniciar e desinstalar. Esses ambientes não podem ser simulados integralmente pelos testes locais. A versão instalada neste computador e a publicação no Cloudflare não são alteradas apenas por compilar o projeto.
Validação local da implementação 1.1.4: 34 testes passaram, incluindo gravação/leitura real de aspas e acentos em uma chave temporária do registro. A consulta HTTPS ao manifesto público funcionou. O pacote final contém os módulos Java de desktop, preferências, logs e certificados do Windows. O bloqueio de publicação sem certificado foi conferido antes de qualquer envio.

## Garrafa e confirmação do lembrete

A garrafa começa vazia e enche durante a contagem. Ao terminar o intervalo, fica cheia e o serviço aguarda o botão **Ok, obrigado!**. A confirmação esvazia a garrafa em aproximadamente 650 ms e inicia o próximo intervalo. Pausar durante o alerta mantém os lembretes pausados mesmo depois de confirmar; Iniciar retoma o próximo ciclo. Testar alerta não reinicia a contagem real. O estado de alerta pendente também é preservado ao reiniciar após uma atualização.

## Publicação 1.1.5 — 28/09/2026

Publicado em https://orvalho-forest.pages.dev e https://github.com/BrunoTrech/Orvalho-Releases/releases/tag/v1.1.5 . Foram executados 38 testes sem falhas. Após a propagação do Pages, a consulta com o código Java autenticou o manifesto 1.1.5 e baixou o instalador público completo (28.985.856 bytes), com SHA-256 igual ao arquivo local: 7715556b18cd688aa90318e940ee3efb26d1707cdc0bc65330dad88f0b33de59. Inclui garrafa enchendo e esvaziando na confirmação, espera pela confirmação antes do próximo ciclo e trava com o nome Orvalho.

## Patch 1.1.7 — segurança do instalador (29/09/2026)

- Instalação limitada a `%LOCALAPPDATA%\Orvalho`, sem seletor de pasta. Destinos diferentes são rejeitados antes da remoção da versão anterior.
- O empacotamento remove `util:RemoveFolderEx` do fragmento gerado pelo jpackage. A desinstalação remove arquivos registrados no MSI e diretórios vazios; preserva arquivos alheios.
- Antes do upgrade, neutraliza o valor de remoção recursiva das versões antigas relacionadas ao produto. A proteção não altera chaves de outros aplicativos e falha se não conseguir retirar o valor perigoso.
- Remoção da entrada de inicialização automática não bloqueia a desinstalação se o Windows negar a operação.
- `tools/VerificarInstalador.ps1` audita o MSI final; `tools/TestarInstalador.ps1` cria produtos com identidades e caminhos exclusivos para testar instalação, reparo, desinstalação e migração.

Validação: 38 testes Java passaram. Os sete cenários MSI passaram, incluindo rejeição de destino, instalação, reparo, remoção, instalação legada, upgrade e remoção posterior. Arquivos pessoais de controle sobreviveram e o upgrade não registrou varredura recursiva. Logs: `C:\Users\Public\OrvalhoInstallerTests\6d201d0ffb9e48f79334099fdaea6743`.

Launcher e instalador usam assinatura interna Forest - Orvalho com carimbo de tempo. O certificado público precisa ser confiável nos computadores administrados; isso não garante reputação pública ou ausência de alertas do antivírus.

Instalações antigas fora do caminho padrão exigem migração manual pelo novo instalador. O atualizador antigo pode rejeitar o auxiliar quando instalado na raiz do perfil, e passa seu destino antigo ao instalador; o novo pacote recusa esse destino inseguro. Não execute novamente o desinstalador antigo sem a proteção quando a instalação ocupa a raiz do perfil. No computador do Eduardo, já desinstalado, instalar diretamente a nova versão.
Publicação 1.1.7 verificada no Pages: manifesto Ed25519 autenticado pelo código Java do aplicativo, download completo de 28.770.936 bytes, SHA-256 conferido e assinatura Windows Valid com carimbo de tempo. Deploy: https://a73aebdc.orvalho-forest.pages.dev .

## Preferências e pausas para beber água (implementação local)

- Configurações: tema claro/escuro, sinal sonoro do sistema e mensagens humoradas. Sem volume por copo ou meta de consumo.
- No alerta, **Já bebi** salva uma pausa com horário e inicia o próximo intervalo. **Daqui a 5 minutos** adia sem somar. A pausa do temporizador continua sendo respeitada.
- Na tela principal, o resumo do dia abre os horários. Selecione uma pausa e use **Desfazer pausa selecionada** para corrigir um registro acidental. Desfazer não altera o temporizador.
- **Testar alerta** não registra pausas nem altera a contagem. A garrafa representa o tempo até o próximo lembrete.
- Preferências em `%USERPROFILE%\.orvalho\preferencias.properties`; registros em `pausas-AAAA-MM-DD.tsv`. O dia usa o horário local. Registros antigos do protótipo são lidos como pausas, ignorando volumes e preservando os arquivos originais. Uma pausa desfeita não é reimportada.
- Validação: 48 testes passaram, incluindo persistência, dias diferentes, migração, desfazer por identificador, adiamento, falha de gravação e confirmação única. As telas clara/escura e o alerta foram conferidos por captura dos componentes reais.
## Publicação 1.1.9 — 30/09/2026

Publicada com a mensagem “Aba de configuração implementada”. Inclui tema claro/escuro, som opcional, humor configurável, resumo e histórico de pausas com desfazer, adiamento de cinco minutos e engrenagem de configurações. 48 testes passaram; MSI auditado, aplicativo e instalador assinados. Consulta pública autenticou o manifesto 1.1.9; download completo e hash conferidos com o código do aplicativo, assinatura Windows válida e carimbo de tempo. Deploy: https://9210f3a0.orvalho-forest.pages.dev .

### Liberação para a empresa e piloto

Consulte [LIBERACAO-EMPRESA.md](LIBERACAO-EMPRESA.md) antes de distribuir a próxima versão. A partir do candidato 1.2.13, `publicar-versao.ps1` usa `-Canal piloto` por padrão (branch Pages separada); a publicação para todos exige `-Canal empresa`. Aplicativos anteriores não reconhecem canais e continuam consultando o manifesto público. O script `tools/DefinirCanalAtualizacao.ps1` escolhe o canal para o perfil Windows em que for executado.

## Instalação por usuário sem elevação — 1.2.15

### Diagnóstico (05/10/2026)

O pacote 1.2.14 já era per-user: ALLUSERS ausente, SummaryInformation WordCount=10 (bit 8: não exige elevação), destino LocalAppData e registros HKCU. Não foi encontrada solicitação `runas`, serviço ou driver do aplicativo. Porém, tanto o launcher quanto o instalador EXE não tinham manifesto de execução incorporado. Isso foi corrigido com `asInvoker` e `uiAccess=false`, antes da assinatura. A ausência anterior é uma lacuna confirmada; não é prova conclusiva da causa do pedido de senha em uma máquina específica. Políticas, instalação antiga ou compatibilidade precisam ser verificados no computador afetado.

### O que foi preservado

- Programa: `%LOCALAPPDATA%\Orvalho`. Não houve mudança para outra pasta, evitando migração desnecessária.
- Histórico, credencial da competição, preferências, cache e logs: `%USERPROFILE%\.orvalho` e demais arquivos por usuário existentes. Não são apagados pela desinstalação.
- Runtime Java incluído na imagem, inclusive no auxiliar de atualização. Usuários não precisam de JDK, Maven, WiX, SDK ou scripts.
- Inicialização automática em HKCU; atalhos pelas pastas conhecidas do Windows Installer (Menu Iniciar e Área de Trabalho do usuário). Não há nomes de usuário ou pastas traduzidas fixados.
- Mesma identidade de upgrade. A atualização executa o instalador com o token atual e preserva o diretório da instalação. Não utiliza senha administrativa.
- Nenhuma política de UAC, Defender, SmartScreen, GravityZone, AppLocker ou WDAC é modificada.

### Proteções de geração

`packaging/windows/main.wxs` aceita somente perUser, declara InstallPrivileges=limited e rejeita ALLUSERS. `tools/ManifestoUsuario.ps1` incorpora e relê o manifesto real dos EXEs. Recusa modificar arquivos já assinados. O auxiliar de atualização utiliza uma cópia do launcher verificado.

`tools/VerificarInstalador.ps1` verifica escopo, bit de privilégios do MSI, HKCU, ausência de serviços e ações sem impersonação, destinos dos arquivos/atalhos, proteção contra remoção recursiva e identidade de upgrade. Com `-Executavel`, verifica também que o MSI validado permanece intacto dentro do EXE.

### Gerar a distribuição

Na máquina de desenvolvimento: JDK 26, Maven, WiX 3.14.1 e Windows SDK com mt.exe/SignTool. O SDK é necessário apenas para compilar. Configure JAVA_HOME, ORVALHO_CERT_THUMBPRINT e ORVALHO_SIGNTOOL com o certificado de assinatura autorizado. ORVALHO_MT é opcional se mt.exe puder ser localizado no SDK.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Apache NetBeans\jdk'
# Configure ORVALHO_CERT_THUMBPRINT e ORVALHO_SIGNTOOL no ambiente de compilação.
.\empacotar.ps1 -Versao 1.2.15 -Destino "$PWD\distribuicao\versao-1.2.15" -Assinar
.\criar-instalador.ps1 -Versao 1.2.15 -Assinar
```

O destino de empacotamento deve ser novo. Não substitua uma versão publicada; em nova compilação de distribuição, use nova versão quando necessário. Saída: `distribuicao\instalador\Orvalho-1.2.15.exe`. Estes comandos não publicam no Cloudflare.

### Instrução para o funcionário

Receba o instalador oficial liberado pelo TI, dê duplo clique normalmente e siga as telas. Não use “Executar como administrador”. Abra Orvalho pelo Menu Iniciar. A remoção é feita em Aplicativos instalados no Windows, com a mesma conta que instalou.

Se houver pedido de senha, cancele e encaminhe ao TI a versão e uma captura do aviso. Aviso de reputação do SmartScreen não é a mesma coisa que UAC ou detecção de ameaça. Políticas corporativas continuam sendo respeitadas.

### Transição de instalação antiga para todos os usuários

Este pacote não oferece instalação para todos. Não tente sobrescrever Program Files nem remover uma instalação de máquina com uma conta comum. O TI deve conferir o contexto do produto antigo, preservar os dados por usuário e planejar sua remoção autorizada antes de distribuir o pacote por usuário. Duas instalações podem coexistir se o legado não for removido; isso não é uma migração automática entre contextos.

### Validação obrigatória com conta padrão

A sessão de desenvolvimento usa uma conta do grupo Administradores com token limitado. Testes nessa sessão NÃO comprovam comportamento em conta padrão. O TI deve testar em máquina de homologação, com UAC e políticas corporativas habilitados:

1. Usar conta que não pertença a Administradores (conferir `whoami /groups`, inclusive SID S-1-5-32-544). Não elevar o terminal nem o instalador.
2. Instalar o EXE final por duplo clique. Nenhum pedido de senha deve aparecer. Confirmar destino no LocalAppData e entrada de desinstalação no contexto do usuário.
3. Abrir pelo Menu Iniciar e pelo atalho opcional; testar perfil com espaços/acentos e Área de Trabalho redirecionada.
4. Testar tema, lembrete, registro, reinício, histórico e configuração de início com Windows. Confirmar que a máquina não tem ferramentas de desenvolvimento como pré-requisito.
5. Instalar 1.2.14 nesse mesmo contexto e atualizar para 1.2.15. Verificar histórico, credencial, atalhos e retomada do lembrete. Testar o atualizador automático em ambiente controlado antes da publicação geral, sem alterar o manifesto público para um teste.
6. Desinstalar por Aplicativos instalados e confirmar preservação dos dados pessoais. Reinstalar e conferir recuperação do histórico.
7. Repetir com outro usuário: atalhos, programa, preferências e credencial devem permanecer separados. A trava local do app também é por perfil.
8. Se houver bloqueio externo, registrar versão, política e erro. Não desativar proteção nem conceder privilégios para passar no teste.

Teste automatizado MSI com identidades isoladas (não substitui o teste do EXE em conta padrão):

```powershell
.\tools\TestarInstalador.ps1 -MsiNovo '<MSI final 1.2.15 em target>' -MsiAnterior '<MSI final 1.2.14 em target>' -AnteriorPorUsuario
```

Os MSIs de teste recebem códigos de produto/componentes e chaves próprios. Logs e arquivos sentinela são preservados em `%PUBLIC%\OrvalhoInstallerTests`. O teste não instala/remove a identidade do Orvalho real.

### Resultado local da geração 1.2.15

- 86 testes Java aprovados, sem falhas ou ignorados.
- Manifestos reais do launcher e do instalador: asInvoker, uiAccess=false; assinaturas Authenticode válidas com carimbo de tempo.
- MSI final com 151 arquivos, por usuário; MSI embutido no EXE confirmado byte a byte após inclusão do manifesto.
- Testes MSI com identidades exclusivas e UAC habilitado: rejeição de destino inválido (1603 esperado), instalação (6,1 s), reparo (6,9 s), remoção (7,2 s), instalação da versão anterior (6,6 s), upgrade 1.2.14→1.2.15 (13,6 s), remoção final (7,4 s). Operações válidas retornaram 0; sentinelas preservadas.
- Logs: `C:\Users\Public\OrvalhoInstallerTests\4a9bf77bea9349c9b3ca7c0934df1a67`.
- SHA-256 do EXE: `082dde37ebae9b95e85cf47016080331cd7f9d13fc353350f7ff2249ab2d1eac`.
- Pendentes: duplo clique do EXE e ciclo real de atualização em conta padrão; primeiro uso, perfis acentuados/redirecionados e isolamento entre dois usuários. Os testes MSI desta sessão não equivalem a esses cenários.
- Não houve publicação no Cloudflare, mudança nas políticas ou instalação do produto real nesta revisão.

Referências técnicas: [jpackage 26](https://docs.oracle.com/en/java/javase/26/docs/specs/man/jpackage.html), [manifestos e privilégios](https://learn.microsoft.com/en-us/windows/win32/secbp/running-with-administrator-privileges), [bit de privilégios do MSI](https://learn.microsoft.com/en-us/windows/win32/msi/word-count-summary).

## Cadastro único e temperatura local — 1.2.16

- Nome + setor são comparados sem diferença de maiúsculas, acentos ou espaços repetidos. Outro token não pode criar a mesma identidade, mesmo se o cadastro anterior estiver desativado. O TI deve recuperar o acesso existente. Homônimos no mesmo setor precisam de identificação diferenciada (por exemplo, nome completo).
- Aplicar somente `competicao/migrations/0005_identidade.sql` em bancos que já possuem 0001–0004. Ela acrescenta `identity_key` com índice único e `participant_aliases`. Não apaga nem une participantes automaticamente. Cadastros legados também são verificados no Worker antes da inserção. O índice protege inserções simultâneas de novos cadastros.
- Um alias aponta um token antigo para o participante consolidado. Para bloquear uma pessoa consolidada, desative o cadastro principal; todos os seus aliases respeitam esse bloqueio. O cadastro secundário permanece inativo para auditoria. A consolidação de Gustavo/TI em 05/10/2026 foi autorizada e preservou registros e os dois acessos.
- Configurações > Cidade para temperatura no alerta: automática por IP, Telêmaco Borba, São Paulo ou desativada. A escolha fica em `.orvalho/cidade-clima.txt`.
- A opção automática consulta `/v1/location`, que devolve cidade e coordenadas aproximadas da conexão fornecidas por `request.cf`, sem consultar/gravar D1 nem exigir participação na competição. Não utiliza GPS/permissão de localização do Windows. VPNs e saídas de rede corporativas podem indicar outra cidade; use a seleção manual nesses casos.
- Localização automática fica em memória por 30 minutos. Se indisponível, não assume Telêmaco Borba. Mudança de cidade invalida o cache meteorológico. Coordenadas aproximadas são enviadas ao MET Norway para obter a previsão mais próxima do horário atual; isso não é um sensor de temperatura interna. Consultas são feitas fora da interface, com timeout e tamanho limitado.
- Referências: https://developers.cloudflare.com/workers/runtime-apis/request/ e https://docs.api.met.no/doc/locationforecast/HowTO.html
- A alteração do servidor já está ativa. O instalador 1.2.16 é preparado localmente; publicar o app é uma etapa separada. O download público 1.2.15 permanece o mesmo arquivo submetido à Microsoft.

## Lembrete com outras telas abertas — 1.2.17

O `PopupLembrete` usa `APPLICATION_EXCLUDE` para continuar recebendo cliques quando ranking, histórico, configurações ou diálogos filhos estiverem abertos. As demais telas mantêm sua modalidade e o alerta mantém a aparência, a confirmação única e a política de foco existentes.

Validação: 91 testes Java passaram, incluindo dois testes com cliques reais (AWT Robot) em confirmar e adiar enquanto uma ou duas janelas modais permanecem abertas. Instalador local assinado; publicação do aplicativo é realizada separadamente.

## Outubro Rosa — 1.2.19 (instalador local)

- Configurações > Tema oferece Claro, Escuro e Outubro Rosa. A escolha é salva em `preferencias.properties`, na chave `outubroRosa`, e aplicada ao iniciar. Preferências antigas continuam com seu tema anterior.
- Outubro Rosa usa fundo rosa suave, cartões brancos, botões rosa, texto ameixa e laço vetorial. A água da garrafa, do boneco e da barra de hidratação permanece azul. Ranking, histórico, conquistas, peso, alertas e diálogos usam a paleta da campanha.
- A opção é manual e pode ser desativada a qualquer momento. Não modifica registros, competição, lembretes, som ou localização.
- A experiência de navegação em janela única 1.2.18 foi rejeitada e removida. As telas voltam a abrir em janelas separadas; o lembrete mantém a exclusão modal da 1.2.17.
- Instalador preparado para avaliação local. Sem publicação no Cloudflare.
## Outubro Rosa por padrão — 1.2.20

Na primeira abertura desta versão, instalações novas e antigas ativam Outubro Rosa. A migração preserva som, mensagens humoradas e outras preferências existentes, e grava `campanhaOutubroRosa2026Aplicada=true`. Após essa ativação única, escolher Claro ou Escuro nas configurações é respeitado nos próximos inícios. A versão 1.2.20 permite atualizar também quem testou o instalador local 1.2.19.

## Informações sobre Outubro Rosa — 1.2.20 (revisão publicada)

O selo da campanha é um botão acessível por mouse e teclado na janela principal e no lembrete. Abre uma janela educativa sem bloquear a confirmação de água, reutilizando a janela já aberta. O conteúdo é local, com cartões sobre a campanha, sinais de atenção, redução de riscos e exames. A fonte oficial do INCA é aberta no navegador somente quando solicitada. Texto consultado em 06/10/2026: https://www.gov.br/inca/pt-br/assuntos/cancer/tipos/mama/versao-para-populacao. Esc ou Entendi fecham a tela. Conteúdo com rolagem para telas pequenas. Publicação feita separadamente.

## Variantes e efeitos — 1.2.20 (revisão publicada)

Implementação, arquivos alterados, comandos, validação e limitações em [TEMA-OUTUBRO-ROSA.md](TEMA-OUTUBRO-ROSA.md).


## Publicação 1.2.21

Publicada em 06/10/2026 para permitir atualização automática a partir da 1.2.20. Mantém as variantes Outubro Rosa, a garrafa rosa e o controle de efeitos somente nas configurações. Instalador e evidências em [TEMA-OUTUBRO-ROSA.md](TEMA-OUTUBRO-ROSA.md). Alterar novamente a versão apenas mediante pedido do usuário.

## Publicação 1.2.22 — tema azul por padrão

Publicada em 06/10/2026. Instalações novas começam no tema original azul; a primeira abertura após atualizar também desativa a campanha uma única vez. Outubro Rosa é opcional em Configurações > Tema, com escolha salva entre reinícios. Preserva som, humor, efeitos e a preferência escura do tema original. Evidências e instalador em [TEMA-OUTUBRO-ROSA.md](TEMA-OUTUBRO-ROSA.md).

## Revisão final do tema Outubro Rosa — 1.2.22 — 07/10/2026

Configurações > Tema reúne Tema original, Tema original escuro e Outubro Rosa. A tela educativa tem títulos numerados, sinais em tópicos e orientações destacadas, com ajuste de largura. Mantidos o azul como padrão e a preferência de tema entre reinícios.

Publicada com a mensagem "Versão final com o tema Outubro Rosa implementado.". Instalador assinado, 99 testes Java e 15 testes do serviço aprovados. A numeração continua 1.2.22: quem já usa essa versão precisa reinstalar para receber a revisão. O site e o manifesto apontam para o novo arquivo, preservando os instaladores anteriores. Links e verificações em [TEMA-OUTUBRO-ROSA.md](TEMA-OUTUBRO-ROSA.md).

## Atualização automática — 1.2.23 — 07/10/2026

Publicada a mesma revisão final com a numeração 1.2.23, a pedido do usuário. Não altera interface nem funcionalidades. O canal empresa passa a oferecer atualização automática para versões inferiores, incluindo 1.2.22, com o aplicativo aberto e conectado. Instalador assinado e 99 testes aprovados; detalhes em [TEMA-OUTUBRO-ROSA.md](TEMA-OUTUBRO-ROSA.md).

## Adiamento personalizado — revisão 1.2.23 — 08/10/2026

O alerta permite escolher de 1 a 240 minutos antes de clicar em Adiar, com 5 minutos como valor inicial. O adiamento não registra água e vale apenas para o próximo lembrete; após confirmar Já bebi, volta ao intervalo habitual. O tempo restante é preservado ao pausar ou atualizar. Os controles seguem o tema selecionado e aceitam digitação mesmo com outras telas abertas.

Validado com 101 testes Java. A versão permanece 1.2.23: computadores que já a utilizam precisam reinstalar a revisão pelo site para recebê-la. O canal empresa oferece atualização automática somente a versões inferiores. O instalador anterior permanece disponível em sua URL original.

## Publicação 1.2.24 — 08/10/2026

Adiamento personalizado publicado no canal empresa com nova numeração para atualização automática de versões anteriores, incluindo 1.2.23. Instalador assinado, 101 testes aprovados e download público verificado (manifesto autenticado, SHA-256 e assinatura Windows).

## Localização manual — 1.2.25 — 09/10/2026

A temperatura no alerta passa a ter somente duas cidades: Telêmaco Borba e São Paulo. Removida a consulta automática por IP do aplicativo. Preferências antigas de ambas as cidades são preservadas; modo automático, opção antiga de ocultar, arquivo ausente ou inválido usam Telêmaco Borba como padrão. A escolha pode ser alterada nas configurações. Validado com 101 testes Java, incluindo migração das preferências e troca de cidade.

## Outubro Rosa como tema principal — 1.2.26 — 22/10/2026

A primeira abertura desta versão ativa Outubro Rosa em instalações novas e existentes, uma única vez. Som, humor e efeitos são preservados. O usuário pode escolher outro tema nas configurações, e essa escolha persiste. Migração validada junto aos 101 testes Java. Versão autorizada para publicação no canal empresa.

## Correção do endereço do ranking — 1.2.27 — 28/10/2026

O subdomínio workers.dev da conta mudou de bruno-rech para forestpaper. Aplicativos anteriores mantinham o endereço antigo e deixaram de alcançar a API por falha de DNS. Esta versão aponta para https://orvalho-competicao.forestpaper.workers.dev e preserva os tokens locais, cadastros e históricos. O canal de atualização em pages.dev permanece acessível. Não houve alteração ou migração no banco de dados.

## Retorno ao azul original — 1.2.28 — 03/11/2026

A primeira abertura da versão 1.2.28 restaura o tema original azul em instalações novas e existentes. A migração ocorre uma vez; escolhas posteriores nas configurações persistem, inclusive Outubro Rosa e o tema escuro. As demais preferências são preservadas. O endereço do ranking permanece no subdomínio forestpaper.

## Seletor de temas — revisão 1.2.28 — 04/11/2026

Removida a opção Outubro Rosa das configurações. O seletor oferece Tema original e Tema original escuro. Publicação da revisão com instalador assinado em URL própria, preservando o arquivo anterior. Quem já utiliza 1.2.28 precisa reinstalar pelo site; versões inferiores recebem a revisão pela atualização automática.

## Alongamento — implementação local — 05/11/2026

Aviso às 10:00 e às 15:00, de segunda a sexta, no horário de Brasília. Agenda em src/main/resources/alongamento.properties. Funciona com o Orvalho aberto, inclusive na bandeja, sem depender de iniciar os lembretes de água. O aviso tem visual âmbar, ícone próprio, sinal sonoro distinto (respeita a opção de som) e botões Iniciar e OK. Iniciar começa uma contagem de 2 minutos; ao terminar, exibe Concluído e aguarda OK para fechar. O tempo não reinicia ao clicar novamente, e fechar cancela a contagem. Não registra presença ou água.

A confirmação de exibição fica em .orvalho/alongamento-exibidos.properties para impedir repetição no mesmo horário após reiniciar ou atualizar. Não recupera horários perdidos; abertura dentro do minuto programado é aceita. Retomada após pausa de execução superior a 90 segundos não dispara um aviso atrasado. Ao coincidir com água, a janela de água aguarda e reaparece após OK, preservando a quantidade digitada. Atualizações aguardam o fechamento do aviso. Configurações incluem Testar aviso de alongamento sem consumir o disparo real.

Versão mantida em 1.2.28. Não publicado no Cloudflare.

## Publicação 1.2.29 — 05/11/2026

Versão autorizada para disponibilizar os alertas de alongamento no canal empresa. Segunda a sexta às 10h e 15h de Brasília; Iniciar dispara contagem de dois minutos e OK fecha o aviso. O código desta implementação acompanha a publicação no GitHub. Não registra presença nem altera registros de água.

Publicação concluída em https://orvalho-forest.pages.dev com instalador assinado e 108 testes aprovados. Manifesto autenticado, download completo e SHA-256 verificados. Atualização automática disponível para versões anteriores com o app aberto e conectado.

## Alongamento guiado — 1.2.30 — 09/11/2026

Os avisos de 10h e 15h, de segunda a sexta no horário de Brasília, exibem a mesma parte do dia. A sequência alterna a cada dia útil, começando na parte 1 em 09/11/2026. Iniciar abre o vídeo guiado e começa a contagem de um minuto. Três toques indicam o fim dos exercícios, nos segundos 18, 38 e 58. Os quadros dos vídeos e o som estão em src/main/resources/alongamento e são reproduzidos localmente, sem depender de um player externo. A prévia em Configurações usa a parte do dia.

Publicada no canal empresa, com 110 testes aprovados, instalador assinado e download público verificado.
