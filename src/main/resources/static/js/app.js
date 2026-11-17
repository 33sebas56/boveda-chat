/* All user-supplied content is inserted with textContent, never innerHTML. */
(() => {
  'use strict';
  const $ = id => document.getElementById(id);
  const state = {me:null,conversations:[],active:null,vault:[],messages:[],attachment:null,busy:false,polling:false,tab:'chats',searchVersion:0};
  const csrf = document.querySelector('meta[name="_csrf"]').content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
  const date = value => new Date(value).toLocaleString('es-CO',{day:'2-digit',month:'short',year:'numeric'});
  const time = value => new Date(value).toLocaleTimeString('es-CO',{hour:'2-digit',minute:'2-digit'});
  const size = bytes => bytes < 1024 ? `${bytes} B` : bytes < 1024*1024 ? `${(bytes/1024).toFixed(1)} KB` : `${(bytes/1024/1024).toFixed(1)} MB`;
  function el(tag,cls,text) { const node=document.createElement(tag); if(cls) node.className=cls;if(text!==undefined)node.textContent=text;return node; }
  function icon(name) { const svg=document.createElementNS('http://www.w3.org/2000/svg','svg');const use=document.createElementNS('http://www.w3.org/2000/svg','use');use.setAttribute('href',`/icons.svg#${name}`);svg.setAttribute('aria-hidden','true');svg.append(use);return svg; }
  function initials(name) { return name.split(/\s+/).slice(0,2).map(s=>Array.from(s)[0]||'').join('').toUpperCase(); }
  let noticeTimer;
  function notify(message,error=false) { clearTimeout(noticeTimer);const node=$('global-notice');node.textContent=message;node.classList.remove('hidden');node.classList.toggle('error',error);noticeTimer=setTimeout(()=>node.classList.add('hidden'),6500); }
  async function api(path,options={}) {
    const headers={...options.headers};
    if(options.method && options.method!=='GET')headers[csrfHeader]=csrf;
    if(options.body && !(options.body instanceof FormData)){headers['Content-Type']='application/json';options.body=JSON.stringify(options.body);}
    const response=await fetch('/api'+path,{...options,headers,credentials:'same-origin',cache:'no-store'});
    if(response.status===401){window.location.assign('/login');throw new Error('Tu sesión terminó.');}
    if(!response.ok){let message='No pudimos completar la solicitud.';try {message=(await response.json()).message||message;}catch(_){}throw new Error(message);}
    return response.status===204?null:response.json();
  }
  function setTab(tab) {state.tab=tab;document.querySelectorAll('[data-tab]').forEach(b=>{const active=b.dataset.tab===tab;b.classList.toggle('active',active);b.setAttribute('aria-selected',active);});$('chats-section').classList.toggle('hidden',tab!=='chats');$('vault-section').classList.toggle('hidden',tab!=='vault');if(tab==='vault')loadVault().catch(e=>notify(e.message,true));}
  async function loadConversations() {state.conversations=await api('/conversations');renderConversations();}
  function renderConversations() {
    const list=$('conversation-list');list.replaceChildren();const filter=$('filter-chats').value.trim().toLowerCase();
    const items=state.conversations.filter(c=>`${c.peer.name} ${c.peer.username}`.toLowerCase().includes(filter));
    if(!items.length){list.append(el('div','sidebar-empty',filter?'No encontramos esa conversación.':'Todavía no tienes conversaciones. Usa el botón + para buscar a alguien.'));return;}
    for(const c of items){const button=el('button','conversation-item'+(state.active?.id===c.id?' selected':''));button.type='button';const info=el('div','conversation-info');info.append(el('strong','',c.peer.name),el('small','',`@${c.peer.username}`));button.append(el('span','avatar',initials(c.peer.name)),info);if(c.unread>0)button.append(el('span','unread-badge',c.unread>99?'99+':c.unread));button.addEventListener('click',()=>openChat(c).catch(e=>notify(e.message,true)));list.append(button);}
  }
  async function openChat(conversation) {
    state.active=conversation;state.messages=[];state.attachment=null;renderAttachment();$('message-list').replaceChildren();$('message-input').value='';$('message-input').style.height='auto';
    $('peer-name').textContent=conversation.peer.name;$('peer-handle').textContent='@'+conversation.peer.username;$('peer-avatar').textContent=initials(conversation.peer.name);
    $('chat-empty').classList.add('hidden');$('active-chat').classList.remove('hidden');$('chats-section').classList.add('chat-open');setTab('chats');renderConversations();
    const page=await api(`/conversations/${conversation.id}/messages`);if(state.active?.id!==conversation.id)return;
    state.messages=page.messages;renderMessages(page.peerReadUpTo);$('load-older').classList.toggle('hidden',!page.hasOlder);scrollBottom();await markRead();
    if(window.innerWidth>700)$('message-input').focus();
  }
  function renderMessages(peerReadUpTo) {
    const list=$('message-list');list.replaceChildren();
    if(!state.messages.length){list.append(el('p','sidebar-empty','Este es el comienzo de su conversación. Saluda o comparte un archivo.'));return;}
    for(const m of state.messages){const mine=m.senderId===state.me.id;const row=el('article','message-row'+(mine?' mine':''));row.dataset.messageId=m.id;const bubble=el('div','message-bubble');if(m.text)bubble.append(el('p','message-text',m.text));
      if(m.attachment){const file=m.attachment;if(file.revoked&&!file.mine){bubble.append(el('div','access-revoked','El propietario retiró el acceso a este archivo.'));}
        else {const a=el('a','attachment-card');a.href=`/documentos/${file.id}/descargar`;a.title='Descargar '+file.name;if(file.mime.startsWith('image/')){a.classList.add('image-attachment');const img=el('img');img.src=`/api/files/${file.id}/preview`;img.alt=file.name;img.loading='lazy';img.addEventListener('load',()=>{const scroll=$('messages-scroll');if(scroll.scrollHeight-scroll.scrollTop-scroll.clientHeight<280)scrollBottom();});img.addEventListener('error',()=>img.remove());a.append(img,el('strong','',file.name));}
        else{const info=el('div');info.append(el('strong','',file.name),el('small','',size(file.size)));a.append(icon('file'),info,icon('download'));}bubble.append(a);}}
      const meta=el('div','message-meta');meta.append(el('time','',time(m.createdAt)));if(mine)meta.append(el('span','receipt',m.id<=peerReadUpTo?'Leído':'Enviado'));row.append(bubble,meta);list.append(row);}
  }
  function scrollBottom() {const s=$('messages-scroll');s.scrollTop=s.scrollHeight;}
  async function markRead(){if(!state.active||!state.messages.length||document.hidden||state.tab!=='chats')return;const lastSeenId=state.messages.at(-1).id;await api(`/conversations/${state.active.id}/read`,{method:'POST',body:{lastSeenId}});}
  async function poll() {
    if(document.hidden||state.polling||state.busy)return;state.polling=true;
    try {await loadConversations();if(state.active&&state.tab==='chats'){const id=state.active.id;const after=state.messages.at(-1)?.id||0;const page=await api(`/conversations/${id}/messages?after=${after}`);if(state.active?.id!==id)return;
      const s=$('messages-scroll'),nearBottom=s.scrollHeight-s.scrollTop-s.clientHeight<140;const known=new Set(state.messages.map(m=>m.id));const fresh=page.messages.filter(m=>!known.has(m.id));
      if(fresh.length){state.messages.push(...fresh);renderMessages(page.peerReadUpTo);if(nearBottom)scrollBottom();}
      else $('message-list').querySelectorAll('.message-row.mine').forEach(row=>{row.querySelector('.receipt').textContent=Number(row.dataset.messageId)<=page.peerReadUpTo?'Leído':'Enviado';});
      if(nearBottom)await markRead();}
      $('connection-state').textContent='Se actualiza automáticamente';
    }catch(_){$('connection-state').textContent='Reconectando… Tus mensajes enviados siguen guardados.';}finally{state.polling=false;}
  }
  function renderAttachment(){const file=state.attachment;$('pending-attachment').classList.toggle('hidden',!file);$('pending-name').textContent=file?.name||'';}
  async function send(event){event.preventDefault();if(state.busy||!state.active)return;const text=$('message-input').value.trim();if(!text&&!state.attachment)return;
    state.busy=true;$('send-button').disabled=true;const id=state.active.id;
    try{const message=await api(`/conversations/${id}/messages`,{method:'POST',body:{text,attachmentId:state.attachment?.id||null}});if(state.active?.id===id){state.messages.push(message);state.attachment=null;$('message-input').value='';$('message-input').style.height='auto';renderAttachment();renderMessages(state.conversations.find(c=>c.id===id)?.peerReadUpTo||0);scrollBottom();await markRead();}await loadConversations();}
    catch(e){notify(e.message,true);}finally{state.busy=false;$('send-button').disabled=false;$('message-input').focus();}}
  async function loadVault(){const vault=await api('/files');state.vault=vault.files;$('storage-used').textContent=size(vault.usedBytes)+' utilizados';$('storage-quota').textContent=size(vault.quotaBytes)+' de capacidad por cuenta';$('storage-fill').style.width=Math.min(100,vault.usedBytes/vault.quotaBytes*100)+'%';renderVault();return vault;}
  function renderVault(){const list=$('vault-list');list.replaceChildren();if(!state.vault.length){const empty=el('div','vault-empty');empty.append(icon('folder'),el('h2','','Tu bóveda está esperando.'),el('p','','Sube tu primer documento o imagen. Tú decides con quién compartirlo.'));list.append(empty);return;}
    for(const file of state.vault){const tile=el('article','file-tile');const preview=el('div','file-preview');if(file.mime.startsWith('image/')){const img=el('img');img.src=`/api/files/${file.id}/preview`;img.alt='';img.loading='lazy';preview.append(img);}else preview.append(icon('file'));preview.append(el('span','file-type',file.name.split('.').at(-1).toUpperCase()));const info=el('div','file-tile-info');const name=el('button','file-name-button',file.name);name.title=file.name;name.addEventListener('click',()=>fileDetails(file));info.append(name,el('p','',`${size(file.size)} · ${date(file.createdAt)}`));const actions=el('div','file-tile-actions');actions.append(el('span','file-status'+(file.revoked?' revoked':''),file.revoked?'Acceso retirado':file.shares?'Compartido':'Solo para ti'));const more=el('button','icon-button');more.title='Detalles del archivo';more.setAttribute('aria-label','Detalles de '+file.name);more.append(icon('arrow'));more.addEventListener('click',()=>fileDetails(file));actions.append(more);info.append(actions);tile.append(preview,info);list.append(tile);}}
  let uploadToChat=false;
  function chooseUpload(forChat){uploadToChat=forChat;$('file-input').value='';$('file-input').click();}
  async function upload(){const file=$('file-input').files[0];if(!file)return;if(file.size>5*1024*1024){notify('El archivo supera el límite de 5 MB.',true);return;}const destination=uploadToChat;const form=new FormData();form.append('file',file);$('upload-button').disabled=true;
    try{notify('Guardando tu archivo…');const saved=await api('/files',{method:'POST',body:form});await loadVault();if(destination&&state.active){state.attachment=saved;renderAttachment();$('attachment-dialog').close();$('message-input').focus();notify('Archivo listo para enviar.');}else notify('Archivo guardado en tu bóveda.');}
    catch(e){notify(e.message,true);}finally{$('upload-button').disabled=false;}}
  async function attachmentDialog(){try{await loadVault();const list=$('attachment-files');list.replaceChildren();for(const file of state.vault.filter(f=>!f.revoked)){const button=el('button','attachment-choice');button.append(icon(file.mime.startsWith('image/')?'image':'file'),el('span','',file.name),el('small','',size(file.size)));button.addEventListener('click',()=>{state.attachment=file;renderAttachment();$('attachment-dialog').close();$('message-input').focus();});list.append(button);}if(!list.childElementCount)list.append(el('p','small muted','Sube un archivo para adjuntarlo a esta conversación.'));$('attachment-dialog').showModal();}catch(e){notify(e.message,true);}}
  function fileDetails(file){$('detail-name').textContent=file.name;$('detail-size').textContent=size(file.size);$('detail-date').textContent=date(file.createdAt);$('detail-shares').textContent=file.shares+' mensaje(s)';$('detail-hash').textContent=file.sha256;$('detail-download').href=`/documentos/${file.id}/descargar`;$('detail-revoke').textContent=file.revoked?'Permitir acceso':'Retirar acceso';$('detail-revoke').classList.toggle('hidden',file.shares===0);$('detail-delete').classList.toggle('hidden',file.shares>0);
    $('detail-revoke').onclick=async()=>{try{await api(`/files/${file.id}/sharing`,{method:'PATCH',body:{revoked:!file.revoked}});$('file-dialog').close();await loadVault();notify(file.revoked?'Se restauró el acceso a las personas del chat.':'Se retiró el acceso para nuevas descargas.');}catch(e){notify(e.message,true);}};
    $('detail-delete').onclick=async()=>{if(!window.confirm('¿Eliminar este archivo de tu bóveda?'))return;try{await api(`/files/${file.id}`,{method:'DELETE'});$('file-dialog').close();await loadVault();notify('Archivo eliminado.');}catch(e){notify(e.message,true);}};
    $('file-dialog').showModal();}
  function newChat(){if(state.busy)return;$('people-query').value='';$('people-results').replaceChildren(el('p','small muted','Busca a la persona por el usuario que eligió al registrarse.'));$('new-chat-dialog').showModal();$('people-query').focus();}
  let searchTimer;
  function searchPeople(){clearTimeout(searchTimer);const version=++state.searchVersion;searchTimer=setTimeout(async()=>{const q=$('people-query').value.trim().replace(/^@/,'');if(q.length<2){$('people-results').replaceChildren(el('p','small muted','Escribe al menos 2 caracteres.'));return;}
    try{const people=await api('/people?q='+encodeURIComponent(q));if(version!==state.searchVersion)return;const list=$('people-results');list.replaceChildren();for(const person of people){const button=el('button','person-result');const info=el('div');info.append(el('strong','',person.name),el('small','',`@${person.username}`));button.append(el('span','avatar',initials(person.name)),info,icon('arrow'));button.addEventListener('click',async()=>{button.disabled=true;try{const c=await api('/conversations',{method:'POST',body:{username:person.username}});$('new-chat-dialog').close();await loadConversations();await openChat(c);}catch(e){notify(e.message,true);}finally{button.disabled=false;}});list.append(button);}if(!people.length)list.append(el('p','small muted','No encontramos usuarios con ese nombre.'));}catch(e){notify(e.message,true);}},350);}
  async function older(){if(!state.active||!state.messages.length)return;const id=state.active.id;const button=$('load-older');button.disabled=true;try{const s=$('messages-scroll'),previousHeight=s.scrollHeight;const page=await api(`/conversations/${id}/messages?before=${state.messages[0].id}`);if(state.active?.id!==id)return;state.messages=[...page.messages,...state.messages];renderMessages(page.peerReadUpTo);button.classList.toggle('hidden',!page.hasOlder);s.scrollTop=s.scrollHeight-previousHeight;}catch(e){notify(e.message,true);}finally{button.disabled=false;}}
  async function init(){try{state.me=await api('/me');$('my-name').textContent=state.me.name;$('my-avatar').textContent=initials(state.me.name);await loadConversations();}catch(e){notify(e.message,true);}document.querySelectorAll('[data-tab]').forEach(b=>b.addEventListener('click',()=>setTab(b.dataset.tab)));document.querySelectorAll('[data-close-dialog]').forEach(b=>b.addEventListener('click',()=>b.closest('dialog').close()));
    $('new-chat').addEventListener('click',newChat);$('empty-new-chat').addEventListener('click',newChat);$('filter-chats').addEventListener('input',renderConversations);$('people-query').addEventListener('input',searchPeople);$('message-form').addEventListener('submit',send);$('message-input').addEventListener('keydown',event=>{if(event.key==='Enter'&&!event.shiftKey){event.preventDefault();$('message-form').requestSubmit();}});$('message-input').addEventListener('input',()=>{const input=$('message-input');input.style.height='auto';input.style.height=Math.min(input.scrollHeight,130)+'px';});$('attach-button').addEventListener('click',attachmentDialog);$('remove-attachment').addEventListener('click',()=>{state.attachment=null;renderAttachment();});$('upload-button').addEventListener('click',()=>chooseUpload(false));$('upload-for-chat').addEventListener('click',()=>chooseUpload(true));$('file-input').addEventListener('change',upload);$('back-to-list').addEventListener('click',()=>{$('chats-section').classList.remove('chat-open');});$('load-older').addEventListener('click',older);document.addEventListener('visibilitychange',()=>{if(!document.hidden)poll();});setInterval(poll,5000);}
  init();
})();
