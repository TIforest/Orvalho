const enc = new TextEncoder();
export async function hash(text) {
 return [...new Uint8Array(await crypto.subtle.digest('SHA-256',enc.encode(text)))].map(x=>x.toString(16).padStart(2,'0')).join('');
}
export function day(instant) {return new Intl.DateTimeFormat('en-CA',{timeZone:'America/Sao_Paulo',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date(instant));}
function reply(body,status=200,type='text/plain; charset=utf-8') {
 return new Response(body,{status,headers:{'content-type':type,'cache-control':'no-store','x-content-type-options':'nosniff'}});
}
const validToken = x => /^[a-f0-9]{64}$/.test(x || '');
const validId = x => /^[a-f0-9-]{36}$/.test(x || '');
const normal = x => x.trim().replace(/\s+/gu,' ').normalize('NFD').replace(/\p{M}/gu,'').toLocaleLowerCase('pt-BR');
function text(value,min,max){return typeof value==='string' && value.trim().length>=min && value.trim().length<=max && !/[\x00-\x1f\x7f]/.test(value);}
async function form(request) {
 if(!(request.headers.get('content-type')||'').startsWith('application/x-www-form-urlencoded'))throw {status:415,message:'Formato não aceito.'};
 const reader=request.body?.getReader();let size=0;const chunks=[];
 if(reader)while(true){const {value,done}=await reader.read();if(done)break;size+=value.length;if(size>4096){await reader.cancel();throw {status:413,message:'Solicitação muito grande.'};}chunks.push(value);}
 const bytes=new Uint8Array(size);let off=0;for(const c of chunks){bytes.set(c,off);off+=c.length;}
 return new URLSearchParams(new TextDecoder().decode(bytes));
}
async function limited(db,key,limit) {
 const minute=Math.floor(Date.now()/60000),bucket=key+':'+minute;
 const row=await db.prepare('INSERT INTO rate_limits(bucket,count,expires) VALUES (?,1,?) ON CONFLICT(bucket) DO UPDATE SET count=count+1 RETURNING count').bind(bucket,minute+10).first();
 await db.prepare('DELETE FROM rate_limits WHERE expires < ?').bind(minute).run();
 return row.count>limit;
}
export default {
 async fetch(request,env) {
  try {
   const url=new URL(request.url),db=env.DB,now=new Date().toISOString();
   if(url.pathname==='/v1/location' && request.method==='GET') {
    const cf=request.cf||{},lat=Number(cf.latitude),lon=Number(cf.longitude);
    if(!cf.latitude||!cf.longitude||!cf.city||!Number.isFinite(lat)||!Number.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)return reply('Localização indisponível.',503);
    return reply(new URLSearchParams({city:cf.city,lat:lat.toFixed(2),lon:lon.toFixed(2)}).toString());
   }
   if(url.pathname==='/v1/register' && request.method==='POST') {
    const ip=await hash(request.headers.get('CF-Connecting-IP')||'local');
    if(await limited(db,'register:'+ip,20))return reply('Muitas tentativas. Aguarde um minuto.',429);
    const f=await form(request),name=f.get('name'),sector=f.get('sector'),password=f.get('password'),token=f.get('token');
    if(f.has('invite')&&!password)return reply('Atualize o Orvalho para entrar com a senha da competição.',426);
    if(!text(name,2,80)||!text(sector,2,60)||typeof password!=='string'||password.length<1||password.length>128||!validToken(token)||f.get('consent')!=='yes')return reply('Confira nome, setor, senha e aceite.',400);
    if(!validToken(env.COMPETITION_PASSWORD_HASH))return reply('Cadastro temporariamente indisponível. Consulte o TI.',503);
    const supplied=await hash(password);let different=0;
    for(let i=0;i<64;i++)different|=supplied.charCodeAt(i)^env.COMPETITION_PASSWORD_HASH.charCodeAt(i);
    if(different)return reply('Senha da competição incorreta.',403);
    const th=await hash(token);
    const existing=await db.prepare('SELECT id,name,sector,active,self_withdrawn FROM participants WHERE id=COALESCE((SELECT participant_id FROM participant_aliases WHERE token_hash=?),(SELECT id FROM participants WHERE token_hash=?))').bind(th,th).first();
    if(existing){
     if(normal(existing.name)!==normal(name)||normal(existing.sector)!==normal(sector))return reply('Este computador está vinculado a outro nome ou setor. Use os dados do cadastro original.',403);
     if(!existing.active){
      if(!existing.self_withdrawn)return reply('Cadastro desativado pelo TI. Solicite a reativação.',403);
      const restored=await db.prepare('UPDATE participants SET active=1,self_withdrawn=0 WHERE id=? AND active=0 AND self_withdrawn=1 RETURNING id').bind(existing.id).first();
      if(!restored){
       const current=await db.prepare('SELECT active FROM participants WHERE id=?').bind(existing.id).first();
       if(!current?.active)return reply('Cadastro desativado pelo TI. Solicite a reativação.',403);
      }
     }
     return reply(new URLSearchParams({id:existing.id,name:existing.name,sector:existing.sector}).toString());
    }
    const id=crypto.randomUUID();
    const identity=JSON.stringify([normal(name),normal(sector)]);
    // Verifica também cadastros anteriores à chave única, sem alterar seus históricos.
    const previousNames=await db.prepare('SELECT name,sector FROM participants').all();
    if(previousNames.results.some(p=>normal(p.name)===normal(name)&&normal(p.sector)===normal(sector)))return reply('Já existe um cadastro com este nome e setor. Consulte o TI para recuperar seu acesso.',409);
    // Token individual persistido pelo app evita duplicar cadastros em reenvios.
    await db.prepare(`INSERT OR IGNORE INTO participants(id,token_hash,name,sector,joined_at,consent_version,identity_key)
      VALUES (?,?,?,?,?,?,?)`).bind(id,th,name.trim().replace(/\s+/gu,' '),sector.trim().replace(/\s+/gu,' '),now,'v1',identity).run();
    const participant=await db.prepare('SELECT id,name,sector,active FROM participants WHERE token_hash=?').bind(th).first();
    if(!participant)return reply('Já existe um cadastro com este nome e setor. Consulte o TI para recuperar seu acesso.',409);
    if(!participant.active||normal(participant.name)!==normal(name)||normal(participant.sector)!==normal(sector))return reply('Cadastro não autorizado. Consulte o TI.',403);
    return reply(new URLSearchParams({id:participant.id,name:participant.name,sector:participant.sector}).toString());
   }
   const token=(request.headers.get('Authorization')||'').replace(/^Bearer /,'');
   if(!validToken(token))return reply('Participação não autorizada.',401);
   const tokenHash=await hash(token);
   const participant=await db.prepare('SELECT * FROM participants WHERE id=COALESCE((SELECT participant_id FROM participant_aliases WHERE token_hash=?),(SELECT id FROM participants WHERE token_hash=?)) AND active=1').bind(tokenHash,tokenHash).first();
   if(!participant)return reply('Participação não autorizada. Consulte o TI.',401);
   if(await limited(db,'user:'+participant.id,120))return reply('Muitas solicitações. Aguarde um minuto.',429);
   if(url.pathname==='/v1/events' && request.method==='POST') {
    const f=await form(request),id=f.get('id'),occurred=f.get('at'),ml=Number(f.get('ml')),canceled=f.get('canceled');
    if(!validId(id)||!Number.isInteger(ml)||ml<10||ml>2000||!['0','1'].includes(canceled)||!/^\d{4}-\d\d-\d\dT.*Z$/.test(occurred||'')||!Number.isFinite(Date.parse(occurred)))return reply('Registro inválido.',400);
    const previous=await db.prepare('SELECT * FROM events WHERE participant_id=? AND id=?').bind(participant.id,id).first();
    if(previous && (previous.occurred_at!==occurred||previous.ml!==ml))return reply('Este registro já existe com outros dados.',409);
    if(!previous && (Date.parse(occurred)>Date.now()+300000 || Date.parse(occurred)<Date.now()-7*86400000 || Date.parse(occurred)<Date.parse(participant.joined_at)-60000))return reply('Horário fora do período aceito. Confira o relógio do computador.',422);
    await db.prepare(`INSERT INTO events(participant_id,id,occurred_at,day,ml,canceled) VALUES (?,?,?,?,?,?)
      ON CONFLICT(participant_id,id) DO UPDATE SET canceled=MAX(events.canceled,excluded.canceled)`)
      .bind(participant.id,id,occurred,day(occurred),ml,Number(canceled)).run();
    return reply('ok');
   }
   if(url.pathname==='/v1/achievements/sharing' && request.method==='POST') {
    const f=await form(request),enabled=f.get('enabled');
    if(!['0','1'].includes(enabled)||(enabled==='1'&&f.get('consent')!=='achievements-v1'))return reply('Confirme o compartilhamento das conquistas.',400);
    await db.prepare('UPDATE participants SET achievements_visible=?,achievements_consent_at=COALESCE(achievements_consent_at,?) WHERE id=?').bind(Number(enabled),now,participant.id).run();
    return reply('ok');
   }
   if(url.pathname==='/v1/achievements/reference' && request.method==='POST') {
    if(!participant.achievements_visible)return reply('Ative o compartilhamento de conquistas.',403);
    const f=await form(request),date=f.get('day'),target=Number(f.get('target_ml'));
    const today=day(now),oldest=day(new Date(Date.now()-7*86400000));
    if(!/^\d{4}-\d{2}-\d{2}$/.test(date||'')||!Number.isFinite(Date.parse(date+'T12:00:00Z'))||new Date(date+'T12:00:00Z').toISOString().slice(0,10)!==date||!Number.isInteger(target)||target<700||target>10500)return reply('Referência inválida.',400);
    if(date>today||date<oldest||date<day(participant.achievements_consent_at))return reply('Dia fora do período permitido.',422);
    // A primeira referência do dia é imutável: mudar o peso não altera conquistas passadas.
    await db.prepare('INSERT OR IGNORE INTO daily_references(participant_id,day,target_ml) VALUES (?,?,?)').bind(participant.id,date,target).run();
    return reply('ok');
   }
   if(url.pathname==='/v1/achievements' && request.method==='GET') {
    const month=url.searchParams.get('month')||day(now).slice(0,7);
    if(!/^\d{4}-(0[1-9]|1[0-2])$/.test(month)||month<'2026-01'||month>day(now).slice(0,7))return reply('Mês inválido.',400);
    const rows=await db.prepare(`SELECT p.id,p.name,p.sector,r.day FROM daily_references r
     JOIN participants p ON p.id=r.participant_id
     JOIN events e ON e.participant_id=r.participant_id AND e.day=r.day AND e.canceled=0
     WHERE p.active=1 AND p.achievements_visible=1 AND r.day BETWEEN ? AND ?
     GROUP BY p.id,r.day,r.target_ml HAVING SUM(e.ml)>=r.target_ml ORDER BY r.day,p.id`)
     .bind(month+'-01',month+'-31').all();
    const people=new Map();
    for(const row of rows.results){if(!people.has(row.id))people.set(row.id,{...row,days:[]});people.get(row.id).days.push(row.day);}
    const sorted=[...people.values()].sort((a,b)=>b.days.length-a.days.length||a.name.localeCompare(b.name,'pt-BR')||a.id.localeCompare(b.id));
    let previous=-1,rank=0;
    return reply('month='+month+'\nday='+day(now)+'\n'+sorted.map((p,i)=>{if(previous!==p.days.length){rank=i+1;previous=p.days.length;}
     return [rank,encodeURIComponent(p.name),encodeURIComponent(p.sector),p.days.join(','),p.id===participant.id?'1':'0'].join('\t');}).join('\n'));
   }
   if(url.pathname==='/v1/ranking' && request.method==='GET') {
    const today=day(now),selected=url.searchParams.get('day')||today;
    if(!/^\d{4}-\d{2}-\d{2}$/.test(selected)||!Number.isFinite(Date.parse(selected+'T12:00:00Z'))||new Date(selected+'T12:00:00Z').toISOString().slice(0,10)!==selected||selected>today||selected<'2026-01-01')return reply('Data do ranking inválida.',400);
    const rows=await db.prepare(`SELECT p.id,p.name,p.sector,COALESCE(SUM(CASE WHEN e.canceled=0 THEN e.ml ELSE 0 END),0) AS ml
      FROM participants p LEFT JOIN events e ON e.participant_id=p.id AND e.day=? WHERE p.active=1 AND p.joined_at < ?
      GROUP BY p.id ORDER BY ml DESC,p.name COLLATE NOCASE,p.id LIMIT 1000`)
      .bind(selected,new Date(Date.parse(selected+'T03:00:00Z')+86400000).toISOString()).all();
    const personalized=url.searchParams.get('format')==='2';
    return reply('day='+selected+'\n'+rows.results.map((r,i)=>{const fields=[i+1,encodeURIComponent(r.name),encodeURIComponent(r.sector),r.ml];if(personalized)fields.push(r.id===participant.id?'1':'0');return fields.join('\t');}).join('\n'));
   }
   if(url.pathname==='/v1/participation' && request.method==='DELETE') {
    await db.prepare('UPDATE participants SET active=0,self_withdrawn=1 WHERE id=? AND active=1').bind(participant.id).run();
    return reply('ok');
   }
   return reply('Rota não encontrada.',404);
  }catch(e){return reply(e.status?e.message:'Não foi possível concluir. Tente novamente.',e.status||500);}
 }
};
