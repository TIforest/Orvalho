# Competição interna do Orvalho — senha compartilhada

## Como o funcionário entra

No Orvalho 1.2.1 ou posterior: **Participar da competição → nome → setor → senha da competição → aceite**. Todos usam a mesma senha. Não é necessário gerar convites, cadastrar funcionários no Cloudflare ou repetir a senha ao abrir o aplicativo.

Instalador interno assinado atual: `distribuicao/instalador-1.2.3/Orvalho-1.2.3.exe`. O serviço já está publicado; o instalador 1.2.3 foi publicado no GitHub Releases e no canal de atualização automática do Cloudflare em 30/09/2026, com a descrição "Sistema de ranking implementado.".

Na versão 1.2.3, o ranking voltou a aparecer somente na janela aberta pelo botão **Ver competição**, sem painel lateral. A janela principal voltou à largura compacta. A sincronização dos registros ocorre a cada cinco minutos, além de registrar/desfazer e reabrir a janela. A engrenagem mantém a opção de iniciar com o Windows e o tooltip das mensagens humoradas. Duplo clique esquerdo na bandeja restaura a janela existente.

O cadastro cria uma credencial individual neste computador. Os registros de cada funcionário continuam separados. A senha compartilhada não é salva no computador do participante nem embutida no executável. Nome, setor e volumes são autodeclarados; conhecer a senha não comprova vínculo com a Forest. Compartilhe a senha apenas por canais internos. Ela não é a senha da conta Cloudflare.

## Serviço ativo

- Conta: ti@forest.ind.br (Bruno Rech), ID `9ebe6f7a9beb0de8a8cb3a1d63468d12`.
- Worker: https://orvalho-competicao.forestpaper.workers.dev
- D1: `orvalho-competicao`, ID `c19ed6c7-62fe-4452-b16b-e4d4fa4e24fd`.
- Configuração do aplicativo: `src/main/resources/competicao.properties`.
- Senha: apenas seu SHA-256 no secret `COMPETITION_PASSWORD_HASH` do Worker. Nunca colocar senha ou hash no repositório, no site ou no aplicativo. Nenhum token administrativo é distribuído.

## Trocar a senha uma vez para todos

Na pasta do projeto, execute:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\competicao\definir-senha.ps1
```

Digite e confirme a nova senha (10 a 128 caracteres). A digitação fica oculta. O script envia somente o hash ao Cloudflare. O ajuste de ExecutionPolicy vale apenas para esse processo. Ele exige que o TI esteja autenticado no Wrangler com acesso a Workers.

A troca passa a valer para novos cadastros sem precisar lançar outra versão do app. Participantes existentes permanecem conectados. Para bloquear alguém já cadastrado, desative o participante pelo identificador; trocar a senha não encerra sessões existentes.

## Banco e compatibilidade

- `participants`: id, hash da credencial individual, nome, setor, adesão, active e versão do aceite. `invite_hash` agora aceita NULL e é mantido apenas para cadastros antigos.
- `events`: participante, identificador único do registro, instante UTC, dia em São Paulo, mililitros e cancelamento.
- `rate_limits`: contadores temporários, usados para limitar requisições. Limite de cadastro: 20 tentativas por IP/minuto; computadores da empresa podem compartilhar o mesmo IP.
- `invites`: tabela legada preservada. Os convites antigos não autorizam novos cadastros; não gere mais convites.

A migração `0002_senha_compartilhada.sql` foi aplicada em 30/09/2026, após backup fora do repositório em `C:\Users\Brunorech\Orvalho-TI\backups`. Cadastros e eventos anteriores foram preservados e as chaves estrangeiras verificadas.

As tabelas iniciais foram criadas manualmente, e a migração 0002 foi aplicada diretamente. **Não execute novamente essas migrações neste banco.** A tabela padrão de controle de migrações do Wrangler não foi usada. Para um banco novo, aplique 0001 e depois 0002, nessa ordem.

Participantes já cadastrados na versão 1.2.0 continuam sincronizando com sua credencial individual. Novos cadastros precisam da versão 1.2.1; a API responde com instrução de atualização quando recebe o antigo campo de convite. Instaladores locais não acionam atualizações automáticas de toda a empresa.

## Consultas pelo Console do Cloudflare

Abra **D1 → orvalho-competicao → Console**.

Participantes:

```sql
SELECT id, name AS nome, sector AS setor, joined_at AS adesao, active AS ativo
FROM participants ORDER BY name;
```

Últimos registros:

```sql
SELECT p.name AS nome, p.sector AS setor, e.day AS dia, e.ml, e.canceled AS cancelado
FROM events e JOIN participants p ON p.id = e.participant_id
ORDER BY e.occurred_at DESC LIMIT 100;
```

Suspender uma pessoa específica (substitua o identificador pelo id da consulta anterior):

```sql
UPDATE participants SET active = 0, self_withdrawn = 0 WHERE id = 'IDENTIFICADOR_DO_PARTICIPANTE';
```

Nomes não são identificadores únicos. Pessoas com o mesmo nome podem existir. Sem identidade corporativa, quem conhece a senha também pode criar outro cadastro em outra instalação. Não trate o ranking como medição ou comprovação de consumo.

## Registro de água e ranking

No alerta, **Bebi [200] ml → Já bebi** salva a quantidade. Adiar e testar alerta não contabilizam. Apenas registros após adesão vão para o ranking. A última quantidade fica lembrada. O intervalo de 10 a 2000 ml valida preenchimento por registro; não é meta nem recomendação de consumo.

A API soma registros não cancelados do dia em America/Sao_Paulo; maior litragem vem primeiro, com empates na mesma posição. Desfazer no histórico cancela o registro no servidor. Reenvios usam o mesmo ID e não duplicam litros. Sair desativa a participação e retira o nome do ranking, sem apagar o histórico. Após sair voluntariamente, é possível retornar com o mesmo nome, setor, senha vigente e identificação local. Troca de computador e recuperação de credenciais continuam exigindo suporte do TI.

Sem conexão, os registros permanecem localmente; sincronização ao registrar/desfazer, abrir o app, consultar ranking e a cada cinco minutos. Novos eventos podem ter até sete dias, posteriores à adesão, com pequena tolerância de relógio. Cancelamentos de eventos já enviados continuam possíveis. O aviso de consumo consciente permanece no cadastro e no ranking.

## Publicar alterações no serviço

Na pasta do projeto, neste computador:

```powershell
$nodeOrvalho = "$env:USERPROFILE\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe"
$wranglerOrvalho = "$PWD\competicao\node_modules\wrangler\bin\wrangler.js"
& $nodeOrvalho $wranglerOrvalho deploy --config competicao/wrangler.toml
```

A versão do Wrangler está fixada em package.json/package-lock.json. Em outra cópia, restaure com `npm ci` na pasta competicao. Mantenha o secret existente ao publicar. Testes: Maven (`mvn test`) e `node --test competicao/worker.test.mjs`.

Documentação: https://developers.cloudflare.com/workers/configuration/secrets/ e https://developers.cloudflare.com/d1/reference/migrations/ .

## Verificação desta alteração

54 testes Java e 5 testes do Worker passaram. Piloto com o cliente Java e o Worker publicado: dois cadastros com a mesma senha, recusa de senha errada, reenvio idempotente, ranking, cancelamento, persistência e saída. Os participantes fictícios foram removidos; o participante real e seus dois registros foram preservados. A tela foi renderizada e conferida. O script de troca de senha passou na validação sintática; a configuração inicial e o teste integrado usaram o comando de secret do Wrangler diretamente.


## Correção de reentrada

Migração 0003_reentrada aplicada e Worker atualizado. self_withdrawn=1 distingue saída voluntária de bloqueio administrativo. O cadastro original pode ser reativado sem duplicar participantes ou transferir histórico para outro nome. A senha é conferida antes da reativação. Para suspender pelo TI, use active=0 e self_withdrawn=0. Não reaplique a migração 0003 neste banco. Seis testes do serviço passaram, incluindo reentrada, preservação de histórico e bloqueio administrativo.


## Conquistas diárias e histórico mensal

- Migração aditiva: `migrations/0004_conquistas.sql`. Em instalações que aplicaram as migrações antigas manualmente, execute somente esse arquivo; não repita 0001–0003.
- `POST /v1/achievements/sharing`: aceite opcional `achievements-v1`; ocultar preserva histórico, mas o remove das consultas públicas.
- `POST /v1/achievements/reference`: primeira referência de cada dia, 35 ml/kg arredondados para cima. O valor fica imutável no servidor e não é retornado nas listagens. Peso não é enviado, mas a referência permite inferi-lo ao administrador do banco.
- `GET /v1/achievements?month=AAAA-MM`: somente participantes autenticados; retorna nome, setor e datas atingidas. Participantes inativos ou sem compartilhamento ficam ocultos.
- Resultado calculado pelos eventos não cancelados da competição: uma conquista por dia, sem bônus pelo excedente. Correções e cancelamentos recalculam o histórico.
- Fuso: America/Sao_Paulo. Referências capturadas localmente enquanto o app está em uso podem ser reenviadas por até sete dias. Não reconstrói referências de dias em que o app não estava em uso ou sem peso configurado. Não preenche retroativamente meses anteriores ao aceite.
- A primeira referência sincronizada do dia vale para a competição mesmo se o peso for alterado depois. O boneco pessoal continua seguindo o peso atual.
- Compatível com clientes antigos: todas as rotas existentes permanecem iguais.

## Consulta de rankings anteriores

`GET /v1/ranking?format=2&day=AAAA-MM-DD` consulta o dia informado; omitir `day` mantém o ranking de hoje para clientes antigos. Datas impossíveis, futuras ou anteriores a 2026 são rejeitadas. O fim do dia é calculado no horário de Brasília. Só aparecem participantes atualmente ativos e cadastrados até o dia escolhido. O resultado usa os eventos armazenados, descontando cancelamentos; não é uma fotografia imutável, portanto correções posteriores alteram o histórico. Nenhuma migração adicional é necessária.
