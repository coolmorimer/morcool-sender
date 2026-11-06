(function(){
 'use strict';
 if(window.__morcoolDriver)return;
 const clean=x=>String(x||'').replace(/[\u200e\u200f\u200b]/g,'').trim();
 const normal=x=>clean(x).toLowerCase();
 const text=e=>clean(e&&(e.innerText||e.textContent||e.value));
 const visible=e=>{if(!e)return false;for(let n=e;n;n=n.parentElement){const css=getComputedStyle(n);if(n.hidden||css.display==='none'||css.visibility==='hidden')return false;}return true;};
 const nodes=s=>Array.from(document.querySelectorAll(s)).filter(visible);
 const digits=s=>clean(s).replace(/\D/g,'');
 let state=null;
 const response=(kind,reason)=>({kind,reason:reason||''});
 function login(){return nodes('input[type="tel"],input[name="phone_number"],input[autocomplete="one-time-code"],.auth-pages,.auth-phone-number-form').length>0&&!search();}
 function search(){return nodes('input[type="search"],input.input-search-input,input[placeholder],input[aria-label]')
  .find(e=>/search|поиск|найти/i.test((e.placeholder||'')+' '+(e.getAttribute('aria-label')||'')+' '+e.className));}
 function composer(){return nodes('[contenteditable="true"],textarea').filter(e=>!e.closest('header,aside,[role="search"]')).find(e=>/message|сообщени|input-message|composer/i.test((e.getAttribute('aria-label')||'')+' '+(e.getAttribute('data-placeholder')||'')+' '+(e.getAttribute('placeholder')||'')+' '+e.className+' '+e.getAttribute('data-tab')))||nodes('[contenteditable="true"][data-tab="10"]').find(Boolean);}
 function headerNodes(){return nodes('[data-testid="conversation-info-header-chat-title"],.chat-info .peer-title,.topbar .peer-title,.chat-info .user-title,header [title],header h1,header h2,[role="banner"] h1,[role="banner"] h2,[data-testid="chat-header-title"]');}
 function header(){return headerNodes().map(e=>clean(e.getAttribute('title')||text(e))).filter(Boolean);}
 // A display name is not an address. A result must show the exact username/phone/profile link.
 function addressEvidence(target,candidateText,href,channel){
  const wanted=clean(target),value=clean(candidateText),telegram=channel==='telegram_web';
  const same=(a,b)=>telegram?normal(a)===normal(b):clean(a)===clean(b);
  let link=null;try{if(href)link=new URL(href,telegram?'https://web.telegram.org/':'https://web.max.ru/');}catch(ignored){}
  if(wanted.startsWith('+')){
   const expected=digits(wanted);const numbers=value.match(/\+[0-9][0-9() .\-]{6,}/g)||[];
   if(numbers.some(n=>digits(n)===expected))return true;
   if(link&&link.protocol==='tel:'&&digits(link.pathname)===expected)return true;
   if(link&&telegram&&(link.hostname==='t.me'||link.hostname==='telegram.me')&&link.pathname==='/+'+expected)return true;
   if(link&&telegram&&link.protocol==='tg:'&&digits(link.searchParams.get('phone'))===expected)return true;
   return false;
  }
  if(wanted.startsWith('https://max.ru/')){
   const canonical=x=>clean(x).replace(/\/$/,'');
   if(canonical(value)===canonical(wanted))return true;
   return !!link&&link.protocol==='https:'&&(link.hostname==='max.ru'||link.hostname==='web.max.ru')&&canonical(link.pathname)===canonical(new URL(wanted).pathname);
  }
  const username=wanted.replace(/^@/,'');
  const usernames=value.match(/(?:^|[^A-Za-z0-9_@])@[A-Za-z][A-Za-z0-9_]{3,63}(?![A-Za-z0-9_])/g)||[];
  if(usernames.some(u=>same(u.substring(u.indexOf('@')+1),username)))return true;
  if(!link)return false;
  if(telegram){
   if((link.hostname==='t.me'||link.hostname==='telegram.me')&&same(link.pathname.replace(/^\//,''),username))return true;
   if(link.protocol==='tg:'&&same(link.searchParams.get('domain'),username))return true;
   if(link.hostname==='web.telegram.org'&&same(link.hash,'#@'+username))return true;
  }else if(link.protocol==='https:'&&(link.hostname==='max.ru'||link.hostname==='web.max.ru')&&same(link.pathname.replace(/^\/u\//,'').replace(/^\//,'').replace(/\/$/,''),username))return true;
  return false;
 }
 function peerIds(element){const found=[];for(let n=element,depth=0;n&&n!==document.body&&depth<7;n=n.parentElement,depth++){for(const key of ['data-peer-id','data-user-id','data-chat-id']){const id=clean(n.getAttribute&&n.getAttribute(key));if(id)found.push(id);}}return found;}
 function validChat(){
  if(!state||nodes('[role="dialog"],.popup,.modal').length>0)return false;const headings=headerNodes();
  if(headings.some(e=>addressEvidence(state.target,e.getAttribute('title')||text(e),e.getAttribute('href'),state.channel)))return true;
  const edit=composer();if(!edit)return false;const currentIds=peerIds(edit);for(const h of headings)currentIds.push.apply(currentIds,peerIds(h));
  if(state.channel==='whatsapp_web'){
   const number=digits(state.target);return currentIds.some(id=>id===number||id===number+'@c.us'||id===number+'@s.whatsapp.net');
  }
  if(!state.selected||!state.addressVerified)return false;
  if(state.peerIds.some(id=>currentIds.indexOf(id)>=0))return true;
  const titleMatches=header().some(title=>state.selectedNames.indexOf(clean(title))>=0);
  if(!titleMatches)return false;
  // If another chat already had this display name, require address or peer-ID proof instead.
  if(state.duplicateNameBeforeSelection)return false;
  return true;
 }
 function resultNames(element){
  const titles=Array.from(element.querySelectorAll('.peer-title,.user-title,[data-testid="peer-title"],[data-testid="user-name"]')).map(text).filter(Boolean);
  if(titles.length)return titles;
  return text(element).split(/\n/).map(line=>clean(line.replace(/@[A-Za-z][A-Za-z0-9_]{3,63}/g,''))).filter(line=>line&&!addressEvidence(state.target,line,'',state.channel));
 }
 function fill(e,value){e.focus();if(e.tagName==='INPUT'||e.tagName==='TEXTAREA'){const p=e.tagName==='INPUT'?HTMLInputElement.prototype:HTMLTextAreaElement.prototype;const setter=Object.getOwnPropertyDescriptor(p,'value').set;setter.call(e,value);e.dispatchEvent(new Event('input',{bubbles:true}));e.dispatchEvent(new Event('change',{bubbles:true}));}
  else{document.execCommand('selectAll',false,null);document.execCommand('insertText',false,value);if(text(e)!==value){e.textContent=value;e.dispatchEvent(new InputEvent('input',{bubbles:true,inputType:'insertText',data:value}));}}}
 function sendButton(){return nodes('button,[role="button"]')
  .find(e=>!e.disabled&&e.getAttribute('aria-disabled')!=='true'&&(/^(send|send message|отправить|отправить сообщение)$/i.test(clean(e.getAttribute('aria-label')||e.getAttribute('title')||text(e)))||e.querySelector('[data-icon="send"],.icon-send')||e.classList.contains('btn-send')));}
 function unavailable(){return nodes('[role="dialog"],.popup,.modal').some(e=>/not (?:on|registered)|isn.t on whatsapp|invalid (?:phone|number)|user(?:name)? not found|не зарегистрирован|номер.*(?:не найден|недействител)|пользователь не найден/i.test(text(e)));}
 function choose(){
  const selector='[role="listitem"],.chatlist-chat,[data-testid="search-result"],[data-testid="chat-list-item"],.search-result';
  let candidates=nodes(selector+',a[href]').filter(e=>!e.closest('header')&&e!==state.searchInput&&text(e).length<600);
  let found=[];
  for(const candidate of candidates){
   if(!addressEvidence(state.target,text(candidate),candidate.getAttribute('href')||'',state.channel))continue;
   const row=candidate.closest(selector)||candidate;if(found.indexOf(row)<0)found.push(row);
  }
  if(found.length>1)return response('AMBIGUOUS','Найдено несколько результатов с этим адресом. Проверьте получателя.');
  if(found.length===1){
   const selected=found[0],names=resultNames(selected),before=header();
   state.selectedNames=names;state.peerIds=peerIds(selected);state.duplicateNameBeforeSelection=before.some(title=>names.indexOf(clean(title))>=0);
   state.selected=true;state.addressVerified=true;state.stage='CHAT';selected.click();return response('WAIT');
  }
  return response('WAIT');
 }
 window.__morcoolDriver={
  identifies(target,candidateText,href,channel){return addressEvidence(target,candidateText,href,channel);},
  authState(){return {connected:!!search()||!!composer(),login:login()};},
  inspect(){return {stage:state&&state.stage,selected:!!(state&&state.selected),hasSearch:!!search(),hasComposer:!!composer(),validChat:!!(state&&validChat()),hasSend:!!sendButton()};},
  begin(nonce,channel,target,name,message){state={nonce,channel,target,name,message,selected:channel==='whatsapp_web',addressVerified:false,selectedNames:[],peerIds:[],duplicateNameBeforeSelection:false,stage:channel==='whatsapp_web'?'CHAT':'SEARCH',clicked:false,clear:0};return response('WAIT');},
  probe(nonce){
   if(!state||state.nonce!==nonce)return response('LOST','Сессия страницы обновилась');
   if(login())return response('AUTH','Войдите в личный аккаунт в разделе «Аккаунты»');
   if(unavailable()&&!state.clicked)return response('MISSING','Получатель не найден или недоступен');
   if(state.stage==='SEARCH'){
    const input=search();if(!input)return response('WAIT');
    if(!state.searched){state.searchInput=input;fill(input,state.target.replace(/^@/,''));state.searched=true;return response('WAIT');}
    return choose();
   }
   if(!validChat())return response('WAIT');
   const edit=composer();if(!edit)return response('WAIT');
   if(state.clicked){
    const empty=!text(edit)||(/^Сообщение$|^Message$|^Type a message$/i.test(text(edit))&&!sendButton());
    state.clear=empty?state.clear+1:0;return state.clear>=2?response('DONE'):response('WAIT');
   }
   if(text(edit)!==state.message){fill(edit,state.message);return response('WAIT');}
   return sendButton()?response('READY'):response('WAIT');
  },
  commit(nonce){
   if(!state||state.nonce!==nonce)return response('LOST');
   if(state.clicked)return response('WAIT');
   const edit=composer(),button=sendButton();
   if(!state.selected||!validChat()||!edit||text(edit)!==state.message||!button)return response('NOT_READY','Чат или текст изменились до отправки');
   state.clicked=true;button.click();return response('WAIT');
  }
 };
})();
