const assert = require('node:assert/strict');
const {recognized} = require('../app/src/main/assets/app-promotion.js');
assert(recognized('Faça mais com a app Gemini','Continuar no navegador'));
assert(recognized('Do more with the Gemini app','Continue in browser'));
assert(!recognized('Allow microphone access','Continue in browser'));
assert(!recognized('Do more with the Gemini app','Allow'));
assert(!recognized('Something went wrong','Retry'));
console.log('PASS: exact promotion title/action recognition, unrelated dialogs excluded');
const {temporarilyHide} = require('../app/src/main/assets/app-promotion.js');
function node(initial={}) { const values=new Map(Object.entries(initial)); return {style:{getPropertyValue:k=>(values.get(k)||['',''])[0],getPropertyPriority:k=>(values.get(k)||['',''])[1],setProperty:(k,v,p)=>values.set(k,[v,p]),removeProperty:k=>values.delete(k)}}; }
const sheet=node({'visibility':['visible',''],'pointer-events':['auto','important']});
const backdrop=node(); const restore=temporarilyHide([sheet,backdrop]);
assert.equal(sheet.style.getPropertyValue('visibility'),'hidden');
restore(); assert.equal(sheet.style.getPropertyValue('visibility'),'visible');
assert.equal(sheet.style.getPropertyPriority('pointer-events'),'important');
assert.equal(backdrop.style.getPropertyValue('visibility'),'');
assert(recognized('Haz más con la app de Gemini','Continuar en el navegador'));
assert(!recognized('Permitir micrófono','Continuar en el navegador'));
console.log('PASS: localized exact matches and failed-dismissal style restoration');
const vm = require('node:vm');
const fs = require('node:fs');
function integration(title, action, removes, throws) {
  const timers=[];
  const backdrop=node();backdrop.isConnected=true;backdrop.classList={contains:x=>x==='cdk-overlay-backdrop'};
  const sheet=node();sheet.isConnected=true;sheet.querySelectorAll=()=>[{textContent:title}];sheet.closest=q=>q==='.cdk-overlay-pane'?pane:sheet;
  const pane={previousElementSibling:backdrop};
  const button={textContent:action,parentElement:sheet,getAttribute:()=>'',click(){this.clicked=(this.clicked||0)+1;if(throws)throw Error('handler failed');if(removes){sheet.isConnected=false;backdrop.isConnected=false;}}};
  const body={};const doc={body,documentElement:{},querySelectorAll:()=>[button]};sheet.parentElement=body;
  vm.runInNewContext(fs.readFileSync('app/src/main/assets/app-promotion.js','utf8'),{document:doc,window:{},MutationObserver:class{constructor(cb){this.cb=cb;}observe(){}},setTimeout:cb=>timers.push(cb),console});
  return {sheet,backdrop,button,timers};
}
let fixture=integration('Do more with the Gemini app','Continue in browser',true,false);
assert.equal(fixture.button.clicked,1);assert.equal(fixture.sheet.style.getPropertyValue('visibility'),'hidden');fixture.timers.forEach(t=>t());
fixture=integration('Faça mais com a app Gemini','Continuar no navegador',false,false);
assert.equal(fixture.sheet.style.getPropertyValue('visibility'),'hidden');fixture.timers.forEach(t=>t());assert.equal(fixture.sheet.style.getPropertyValue('visibility'),'');assert.equal(fixture.backdrop.style.getPropertyValue('visibility'),'');
fixture=integration('Do more with the Gemini app','Continue in browser',false,true);assert.equal(fixture.sheet.style.getPropertyValue('visibility'),'');
fixture=integration('Allow microphone access','Continue in browser',false,false);assert.equal(fixture.button.clicked,undefined);assert.equal(fixture.sheet.style.getPropertyValue('visibility'),'');
console.log('PASS: promotion DOM dismissal, thrown-handler recovery and unrelated modal safety');
