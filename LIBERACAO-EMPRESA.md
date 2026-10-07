# Liberação do Orvalho na Forest

## Revisão de 02/10/2026 — candidato 1.2.13

Correções: canal de atualização separado para piloto; adiamento da instalação enquanto há lembrete pendente ou janela em uso; exclusão mútua na sincronização dos registros; preservação do limite de altura da janela ao iniciar.

### 1. Piloto: TI e 3 a 5 computadores

- Usar o instalador assinado gerado em `distribuicao/instalador/Orvalho-1.2.13.exe`.
- Conferir a assinatura e o hash do arquivo entregue. A assinatura interna não garante ausência de bloqueios pelo antivírus.
- Instalar manualmente o candidato nos computadores de teste. Versões antigas ainda não reconhecem os canais; criar o arquivo do canal nelas não muda seu comportamento.
- Executar no perfil Windows de cada participante do piloto:

```powershell
.\tools\DefinirCanalAtualizacao.ps1 -Canal piloto
```

- Publicar futuras versões de teste com `publicar-versao.ps1 -Versao X.Y.Z -Canal piloto -Notas 'Descrição'`. O padrão agora é piloto. Este comando cria uma pré-release no GitHub e publica na branch Pages `piloto`, sem modificar o site de produção.
- O manifesto de teste fica em `https://piloto.orvalho-forest.pages.dev/updates/pilot.properties`. A primeira publicação desse canal é necessária para o endereço existir. Conferir se a configuração Pages permite acesso a esse manifesto pelos computadores de teste.
- O piloto usa a mesma competição e banco de produção: evitar nomes fictícios e registrar somente volumes reais. Não é um ambiente isolado de dados.

### 2. Critérios antes de liberar para todos

Testar com usuário comum e a política GravityZone aplicada:

- Instalação limpa, reparo e desinstalação sem travar; nenhum arquivo pessoal deve ser removido.
- Atualização a partir da versão atualmente instalada, preservando nome, histórico, preferências e estado do lembrete.
- Inicialização com Windows e abertura pelo ícone da bandeja.
- Registro offline e posterior sincronização sem duplicar litros; cancelamento refletido no ranking.
- Lembrete aberto ou configurações em edição adiam a instalação; fechar a janela/confirmar o lembrete permite tentar novamente.
- Tema claro/escuro e tela pequena sem cortar os controles.
- Cadastro com senha compartilhada e bloqueio administrativo de um participante de teste.

No GravityZone, o TI deve conferir os eventos do arquivo exato e investigar/submeter falso positivo quando necessário. Não desativar a proteção nem excluir pastas inteiras do usuário. Validar novamente cada binário atualizado. O certificado interno precisa ser distribuído por um mecanismo administrado pela empresa; não distribuir a chave privada.

### 3. Expansão por setores

Após ao menos dois dias úteis de piloto sem falhas impeditivas, liberar um setor e acompanhar chamados antes de ampliar. Registrar computador, versão, responsável e resultado de instalação/atualização.

Promover exatamente o instalador validado, sem recompilá-lo:

```powershell
.\publicar-versao.ps1 -Versao X.Y.Z -Canal empresa -UsarInstaladorExistente -Notas 'Descrição aprovada'
```

O script exige assinatura com carimbo de tempo e confere o hash do artefato no GitHub. O canal empresa altera o manifesto e o download públicos; aplicativos antigos também recebem essa publicação. Por isso, não promover enquanto o piloto estiver em validação. Para implantação inicial gradual, distribuir o instalador aos setores escolhidos.

### 4. Suporte e contingência

- Diagnósticos locais: `%USERPROFILE%\.orvalho\atualizacao.log`, `consulta.properties` e subpasta `atualizacoes`. Tratar os logs como internos.
- Em falha geral, suspender novas publicações e restaurar o manifesto assinado da última versão aprovada. Isso impede novas atualizações para a versão problemática, mas não rebaixa quem já a instalou. Corrigir com uma versão superior ou fazer reinstalação assistida preservando os dados.
- Não apagar `%USERPROFILE%\.orvalho`, tokens ou registros do banco para solucionar falhas de instalação.
- A senha compartilhada controla adesão por conhecimento da senha; não comprova vínculo empregatício. Compartilhar somente internamente e manter o bloqueio administrativo disponível.
- Participação e compartilhamento das conquistas são opcionais. Explicar quais dados ficam visíveis no ranking. Evitar transformar litros ou conquistas em cobrança de desempenho.

Esta revisão não publica automaticamente nem comprova compatibilidade com todas as políticas de segurança dos computadores da empresa.

## Evidências desta revisão

- 84 testes Java e 12 testes do Worker aprovados, sem falhas ou testes ignorados.
- Instalador 1.2.13: assinatura Authenticode válida nesta máquina, com carimbo RFC3161.
- SHA-256: `c75027ce42244153d5436bfb4d002d9936c5333e2cc39e17fff5e1f79d595fba`.
- MSI inspecionado: 151 arquivos, sem remoção recursiva de diretórios, destino protegido.
- Cópia MSI com identidade exclusiva: destino inválido rejeitado; instalação limpa 5,6 s; reparo 5,8 s; desinstalação 5,9 s. Arquivo pessoal de teste preservado.
- Logs locais do teste: `C:\Users\Public\OrvalhoInstallerTests\2142d1a45616490a8f3f9e27ed6ef858`.
- A migração da instalação legada não foi repetida nesta revisão. Atualização real e política GravityZone seguem como critérios do piloto.
- Nada foi publicado no Cloudflare ou GitHub nesta revisão. A versão pública permanece 1.2.12.
