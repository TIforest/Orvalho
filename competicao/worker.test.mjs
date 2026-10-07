import {test} from 'node:test';
import assert from 'node:assert/strict';
import {DatabaseSync} from 'node:sqlite';
import {readFileSync} from 'node:fs';
import worker,{hash,day} from './worker.mjs';
const password='Senha teste com MAIUSCULAS!';
const passwordHash=await hash(password);
function fixture(){
 const sql=new DatabaseSync(':memory:');sql.exec('PRAGMA foreign_keys=ON');sql.exec(readFileSync(new URL('migrations/0001_competicao.sql',import.meta.url),'utf8'));
 sql.exec('BEGIN;'+readFileSync(new URL('migrations/0002_senha_compartilhada.sql',import.meta.url),'utf8')+'COMMIT;');
 sql.exec(readFileSync(new URL('migrations/0003_reentrada.sql',import.meta.url),'utf8'));
 sql.exec(readFileSync(new URL('migrations/0004_conquistas.sql',import.meta.url),'utf8'));
 sql.exec(readFileSync(new URL('migrations/0005_identidade.sql',import.meta.url),'utf8'));
 const DB={prepare(q){const stmt=sql.prepare(q);let args=[];return {bind(...a){args=a;return this;},async first(){return stmt.get(...args)||null;},async all(){return {results:stmt.all(...args)};},async run(){return {meta:stmt.run(...args)};}};}};
 const env={DB,COMPETITION_PASSWORD_HASH:passwordHash};
 const call=(path,fields=null,token=null,method=null)=>worker.fetch(new Request('https://competicao.test/v1/'+path,{method:method||(fields?'POST':'GET'),headers:{...(fields?{'content-type':'application/x-www-form-urlencoded'}:{}),...(token?{authorization:'Bearer '+token}:{})},body:fields?new URLSearchParams(fields):undefined}),env);
 return {sql,DB,call,env};
}
const name='Ana Teste',sector='TI';
test('bloqueia duplicidade normalizada e simultânea, inclusive cadastro antigo e bloqueado',async()=>{
 const f=fixture(),a=await enroll(f,'Gustávo Silva','TI');
 const again={...a.fields,name:'  GUSTAVO   SILVA ',sector:' ti ',token:'d'.repeat(64)};
 assert.equal((await f.call('register',again)).status,409);
 f.sql.exec('UPDATE participants SET identity_key=NULL,active=0');
 assert.equal((await f.call('register',again)).status,409);
 const requests=['e','f'].map(t=>f.call('register',{...a.fields,name:'Pessoa Nova',token:t.repeat(64)}));
 assert.deepEqual((await Promise.all(requests)).map(r=>r.status).sort(),[200,409]);
 assert.equal(f.sql.prepare('SELECT COUNT(*) n FROM participants').get().n,2);
});
test('alias consolidado mantém acesso e reentrada mas respeita bloqueio do titular',async()=>{
 const f=fixture(),a=await enroll(f),alias='f'.repeat(64);
 f.sql.prepare('INSERT INTO participant_aliases VALUES (?,(SELECT id FROM participants))').run(await hash(alias));
 assert.equal((await f.call('register',{...a.fields,token:alias})).status,200);
 assert.equal((await f.call('events',event(300),alias)).status,200);
 assert.match(await(await f.call('ranking',null,a.token)).text(),/\t300/);
 f.sql.exec('UPDATE participants SET active=0,self_withdrawn=0');
 assert.equal((await f.call('ranking',null,alias)).status,401);
 assert.equal((await f.call('register',{...a.fields,token:alias})).status,403);
});
test('localização pública aproximada sem banco, sem coordenadas inventadas',async()=>{
 const req=new Request('https://test/v1/location');
 Object.defineProperty(req,'cf',{value:{city:'São Paulo',latitude:'-23.5505',longitude:'-46.6333'}});
 const r=await worker.fetch(req,{});assert.equal(r.status,200);
 const p=new URLSearchParams(await r.text());assert.equal(p.get('city'),'São Paulo');assert.equal(p.get('lat'),'-23.55');
 assert.equal((await worker.fetch(new Request('https://test/v1/location'),{})).status,503);
});
async function enroll(f,n=name,s=sector){
 const token=crypto.randomUUID().replaceAll('-','')+crypto.randomUUID().replaceAll('-','');
 const fields={name:n,sector:s,password,token,consent:'yes'};assert.equal((await f.call('register',fields)).status,200);
 return {fields,token};
}
function event(ml=200){return {id:crypto.randomUUID(),at:new Date().toISOString(),ml:String(ml),canceled:'0'};}
test('sair permite voltar com o mesmo cadastro e preserva historico; bloqueio do TI permanece',async()=>{
 const f=fixture(),{fields,token}=await enroll(f);
 await f.call('events',event(250),token);
 const original=f.sql.prepare('SELECT id FROM participants').get().id;
 assert.equal((await f.call('participation',null,token,'DELETE')).status,200);
 assert.equal((await f.call('register',{...fields,password:'errada'})).status,403);
 assert.equal((await f.call('register',{...fields,name:'Outra Pessoa'})).status,403);
 assert.equal((await f.call('register',fields)).status,200);
 assert.equal((await f.call('register',fields)).status,200);
 assert.equal(f.sql.prepare('SELECT COUNT(*) n FROM participants').get().n,1);
 assert.equal(f.sql.prepare('SELECT id FROM participants').get().id,original);
 assert.match(await (await f.call('ranking',null,token)).text(),/\t250/);
 f.sql.exec('UPDATE participants SET active=0,self_withdrawn=0');
 assert.equal((await f.call('register',fields)).status,403);
 assert.equal((await f.call('ranking',null,token)).status,401);
});
test('senha compartilhada, credenciais individuais e repetição segura',async()=>{
 const f=fixture();const {fields,token}=await enroll(f);
 assert.equal((await f.call('register',fields)).status,200);
 assert.equal(f.sql.prepare('SELECT COUNT(*) AS n FROM participants').get().n,1);
 assert.equal((await f.call('register',{...fields,name:'Outra Pessoa',token:'b'.repeat(64)})).status,200);
 assert.equal((await f.call('register',{...fields,name:'Outra Pessoa'})).status,403);
 assert.equal((await f.call('register',{...fields,password:password.toLowerCase(),token:'c'.repeat(64)})).status,403);
 assert.equal(f.sql.prepare('SELECT COUNT(*) AS n FROM invites').get().n,0);
 assert.equal((await f.call('ranking')).status,401);
 assert.equal((await f.call('ranking',null,token)).status,200);
});
test('eventos idempotentes, ranking por litros, empates e cancelamento irreversível',async()=>{
 const f=fixture(),a=await enroll(f),b=await enroll(f,'Bruno Teste','Produção');
 const e=event(300),e2=event(300);
 assert.equal((await f.call('events',e,a.token)).status,200);
 assert.equal((await f.call('events',e,a.token)).status,200);
 assert.equal((await f.call('events',{...e,ml:'500'},a.token)).status,409);
 await f.call('events',e2,b.token);
 let lines=(await (await f.call('ranking',null,a.token)).text()).split('\n');assert.equal(lines[1].split('\t')[0],'1');assert.equal(lines[2].split('\t')[0],'2');
 await f.call('events',{...e,canceled:'1'},a.token);await f.call('events',e,a.token);
 lines=(await (await f.call('ranking',null,a.token)).text()).split('\n');assert.equal(decodeURIComponent(lines[1].split('\t')[1]),'Bruno Teste');assert.equal(lines[2].split('\t')[3],'0');
 // Um participante não consegue cancelar o evento de outro: ids pertencem ao token autenticado.
 await f.call('events',{...e2,canceled:'1'},a.token);
 assert.equal(f.sql.prepare('SELECT canceled FROM events WHERE participant_id=(SELECT id FROM participants WHERE name=?) AND id=?').get('Bruno Teste',e2.id).canceled,0);
});
test('datas, volumes, desistência e limite de tentativas',async()=>{
 const f=fixture(),{token}=await enroll(f);
 for(const ml of ['0','-1','2001','12.3','NaN'])assert.equal((await f.call('events',event(ml),token)).status,400);
 assert.equal((await f.call('events',{...event(),at:new Date(Date.now()+3600000).toISOString()},token)).status,422);
 assert.equal((await f.call('events',{...event(),at:'2020-01-01T00:00:00Z'},token)).status,422);
 assert.equal(day('2026-10-01T02:59:59Z'),'2026-09-30');assert.equal(day('2026-10-01T03:00:00Z'),'2026-10-01');
 assert.equal((await f.call('participation',null,token,'DELETE')).status,200);
 assert.equal((await f.call('ranking',null,token)).status,401);
 let last;for(let i=0;i<21;i++)last=await f.call('register',{name,sector,password:'errada',token:'b'.repeat(64),consent:'yes'});
 assert.equal(last.status,429);
});
test('senha obrigatória, aceite, rotação e participante desativado',async()=>{
 const f=fixture(),{fields,token}=await enroll(f);
 assert.equal((await f.call('register',{...fields,password:''})).status,400);
 assert.equal((await f.call('register',{...fields,consent:'no'})).status,400);
 assert.equal((await f.call('register',{...fields,password:undefined,invite:'a'.repeat(64)})).status,403);
 const old={...fields,invite:'a'.repeat(64)};delete old.password;
 assert.equal((await f.call('register',old)).status,426);
 f.env.COMPETITION_PASSWORD_HASH=await hash('nova senha');
 assert.equal((await f.call('register',fields)).status,403);
 assert.equal((await f.call('ranking',null,token)).status,200);
 assert.equal((await f.call('register',{...fields,password:'nova senha'})).status,200);
 f.sql.exec('UPDATE participants SET active=0');
 assert.equal((await f.call('register',{...fields,password:'nova senha'})).status,403);
 f.env.COMPETITION_PASSWORD_HASH='';
 assert.equal((await f.call('register',fields)).status,503);
});
test('migração preserva participantes antigos, credenciais e registros',()=>{
 const sql=new DatabaseSync(':memory:');sql.exec('PRAGMA foreign_keys=ON;');
 sql.exec(readFileSync(new URL('migrations/0001_competicao.sql',import.meta.url),'utf8'));
 sql.exec("INSERT INTO invites VALUES ('convite','Nome','TI','2099',0); INSERT INTO participants VALUES ('p','convite','token','Nome','TI','2026',1,'v1'); INSERT INTO events VALUES ('p','e','2026','2026-09-30',250,0);");
 sql.exec('BEGIN;'+readFileSync(new URL('migrations/0002_senha_compartilhada.sql',import.meta.url),'utf8')+'COMMIT;');
 assert.equal(sql.prepare('SELECT token_hash FROM participants').get().token_hash,'token');
 assert.equal(sql.prepare('SELECT ml FROM events JOIN participants ON participants.id=events.participant_id').get().ml,250);
 assert.equal(sql.prepare('PRAGMA foreign_key_check').all().length,0);
 sql.exec("INSERT INTO participants(id,token_hash,name,sector,joined_at,consent_version) VALUES ('novo','novotoken','Outro','TI','2026','v1')");
 assert.equal(sql.prepare('SELECT COUNT(*) n FROM participants').get().n,2);
});

test('ranking personalizado identifica pelo token, preserva homonimos e formato antigo',async()=>{
 const f=fixture(),a=await enroll(f,'Nome Igual','TI'),b=await enroll(f,'Nome Igual','Produção');
 await f.call('events',event(250),a.token);await f.call('events',event(500),b.token);
 const old=(await (await f.call('ranking',null,a.token)).text()).split('\n').slice(1);
 assert.ok(old.every(line=>line.split('\t').length===4));
 const rows=(await (await f.call('ranking?format=2',null,a.token)).text()).split('\n').slice(1).map(line=>line.split('\t'));
 assert.equal(rows.length,2);assert.deepEqual(rows.map(row=>[row[3],row[4]]),[['500','0'],['250','1']]);
 assert.ok(rows.every(row=>row.length===5));
 const other=(await (await f.call('ranking?format=2',null,b.token)).text()).split('\n').slice(1).map(line=>line.split('\t'));
 assert.deepEqual(other.map(row=>row[4]),['1','0']);
});

test('conquistas exigem aceite, referência fixa e no máximo um dia por pessoa',async()=>{
 const f=fixture(),a=await enroll(f),b=await enroll(f,'Bruno','TI'),today=day(new Date());
 assert.equal((await f.call('achievements/reference',{day:today,target_ml:'700'},a.token)).status,403);
 assert.equal((await f.call('achievements/sharing',{enabled:'1'},a.token)).status,400);
 assert.equal((await f.call('achievements/sharing',{enabled:'1',consent:'achievements-v1'},a.token)).status,200);
 assert.equal((await f.call('achievements/reference',{day:today,target_ml:'700'},a.token)).status,200);
 await f.call('achievements/reference',{day:today,target_ml:'900'},a.token);
 assert.equal(f.sql.prepare('SELECT target_ml FROM daily_references').get().target_ml,700);
 const e=event(700);await f.call('events',e,a.token);await f.call('events',e,a.token);await f.call('events',event(2000),a.token);
 let text=await(await f.call('achievements?month='+today.slice(0,7),null,b.token)).text();
 assert.equal(text.split('\n').length,3);assert.equal(text.split('\n')[2].split('\t')[3],today);
 assert.ok(!text.includes('700'));assert.ok(!text.includes('2700'));assert.ok(!text.includes(a.token));
 await f.call('achievements/sharing',{enabled:'0'},a.token);
 text=await(await f.call('achievements',null,b.token)).text();assert.equal(text.trim().split('\n').length,2);
});
test('cancelamento corrige conquista; limites, datas e autorização são verificados',async()=>{
 const f=fixture(),{token}=await enroll(f),today=day(new Date());
 await f.call('achievements/sharing',{enabled:'1',consent:'achievements-v1'},token);
 for(const target of ['0','699','10501','NaN','800.5'])assert.equal((await f.call('achievements/reference',{day:today,target_ml:target},token)).status,400);
 assert.equal((await f.call('achievements/reference',{day:'2026-02-30',target_ml:'700'},token)).status,400);
 assert.equal((await f.call('achievements/reference',{day:'2020-01-01',target_ml:'700'},token)).status,422);
 assert.equal((await f.call('achievements?month=2026-13',null,token)).status,400);
 assert.equal((await f.call('achievements')).status,401);
 await f.call('achievements/reference',{day:today,target_ml:'700'},token);
 const e=event(700);await f.call('events',e,token);
 assert.equal((await(await f.call('achievements',null,token)).text()).trim().split('\n').length,3);
 await f.call('events',{...e,canceled:'1'},token);
 assert.equal((await(await f.call('achievements',null,token)).text()).trim().split('\n').length,2);
 await f.call('events',event(700),token);
 f.sql.exec('UPDATE participants SET active=0');assert.equal((await f.call('achievements',null,token)).status,401);
});
test('histórico mensal preserva meses anteriores e empata por dias, não por litros',async()=>{
 const f=fixture(),a=await enroll(f,'Ana','TI'),b=await enroll(f,'Bruno','TI');
 const ids=f.sql.prepare('SELECT id,name FROM participants ORDER BY name').all();
 f.sql.exec("UPDATE participants SET achievements_visible=1,achievements_consent_at='2026-01-01T12:00:00Z'");
 for(const p of ids)for(const date of ['2026-01-02','2026-01-03','2026-02-01']){
 f.sql.prepare('INSERT INTO daily_references VALUES (?,?,?)').run(p.id,date,700);
 f.sql.prepare('INSERT INTO events VALUES (?,?,?,?,?,0)').run(p.id,crypto.randomUUID(),date+'T12:00:00Z',date,p.name==='Ana'?700:2000);
 }
 let lines=(await(await f.call('achievements?month=2026-01',null,a.token)).text()).trim().split('\n');
 assert.equal(lines.length,4);assert.equal(lines[2].split('\t')[0],'1');assert.equal(lines[3].split('\t')[0],'1');assert.equal(lines[2].split('\t')[3],'2026-01-02,2026-01-03');assert.equal(lines[2].split('\t')[4],'1');
 lines=(await(await f.call('achievements?month=2026-02',null,b.token)).text()).trim().split('\n');assert.equal(lines[2].split('\t')[3],'2026-02-01');
});

test('ranking diário tem posições únicas sem saltos, mesmo com volumes iguais ou zero',async()=>{
 const f=fixture(),c=await enroll(f,'Carlos','TI'),b=await enroll(f,'Bruno','TI'),a=await enroll(f,'Ana','TI'),d=await enroll(f,'Daniel','TI');
 const ranking=async(format='2')=>(await(await f.call('ranking?format='+format,null,a.token)).text()).trim().split('\n').slice(1).map(l=>l.split('\t'));
 let rows=await ranking();assert.deepEqual(rows.map(r=>r[0]),['1','2','3','4']);assert.deepEqual(rows.map(r=>decodeURIComponent(r[1])),['Ana','Bruno','Carlos','Daniel']);
 await f.call('events',event(1000),c.token);await f.call('events',event(500),b.token);await f.call('events',event(500),a.token);await f.call('events',event(200),d.token);
 for(const format of ['1','2']){
  rows=await ranking(format);assert.deepEqual(rows.map(r=>r[0]),['1','2','3','4']);assert.deepEqual(rows.map(r=>decodeURIComponent(r[1])),['Carlos','Ana','Bruno','Daniel']);
  assert.deepEqual(rows.map(r=>r[3]),['1000','500','500','200']);
 }
 assert.deepEqual(await ranking(),await ranking());
});

test('ranking por data separa dias, exclui cadastros posteriores e valida calendário',async()=>{
 const f=fixture(),a=await enroll(f,'Ana','TI'),b=await enroll(f,'Bruno','TI');
 const id=f.sql.prepare('SELECT id FROM participants WHERE name=?').get('Ana').id;
 f.sql.prepare('UPDATE participants SET joined_at=? WHERE id=?').run('2026-09-01T12:00:00Z',id);
 f.sql.prepare('INSERT INTO events VALUES (?,?,?,?,?,?)').run(id,crypto.randomUUID(),'2026-09-15T12:00:00Z','2026-09-15',300,0);
 f.sql.prepare('INSERT INTO events VALUES (?,?,?,?,?,?)').run(id,crypto.randomUUID(),'2026-09-16T12:00:00Z','2026-09-16',500,0);
 f.sql.prepare('INSERT INTO events VALUES (?,?,?,?,?,?)').run(id,crypto.randomUUID(),'2026-09-15T14:00:00Z','2026-09-15',900,1);
 let lines=(await(await f.call('ranking?format=2&day=2026-09-15',null,a.token)).text()).trim().split('\n');
 assert.equal(lines[0],'day=2026-09-15');assert.equal(lines.length,2);assert.equal(lines[1].split('\t')[3],'300');assert.equal(lines[1].split('\t')[4],'1');
 lines=(await(await f.call('ranking?day=2026-09-16',null,a.token)).text()).trim().split('\n');assert.equal(lines[1].split('\t')[3],'500');
 lines=(await(await f.call('ranking?day=2026-08-01',null,a.token)).text()).trim().split('\n');assert.equal(lines.length,1);
 for(const date of ['2026-02-30','2026-13-01','2100-01-01','ontem','2025-12-31'])assert.equal((await f.call('ranking?day='+date,null,a.token)).status,400);
 assert.equal((await f.call('ranking?day=2026-09-15')).status,401);
});
