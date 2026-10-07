(function(){var e=Object.create,t=Object.defineProperty,n=Object.getOwnPropertyDescriptor,r=Object.getOwnPropertyNames,i=Object.getPrototypeOf,a=Object.prototype.hasOwnProperty,o=(e,t)=>()=>(t||(e((t={exports:{}}).exports,t),e=null),t.exports),s=(e,i,o,s)=>{if(i&&typeof i==`object`||typeof i==`function`)for(var c=r(i),l=0,u=c.length,d;l<u;l++)d=c[l],!a.call(e,d)&&d!==o&&t(e,d,{get:(e=>i[e]).bind(null,d),enumerable:!(s=n(i,d))||s.enumerable});return e},c=(n,r,o)=>(o=n==null?{}:e(i(n)),s(r||!n||!n.__esModule||!a.call(n,`default`)?t(o,`default`,{value:n,enumerable:!0}):o,n)),l=globalThis,u=l.ShadowRoot&&(l.ShadyCSS===void 0||l.ShadyCSS.nativeShadow)&&`adoptedStyleSheets`in Document.prototype&&`replace`in CSSStyleSheet.prototype,d=Symbol(),f=new WeakMap,p=class{constructor(e,t,n){if(this._$cssResult$=!0,n!==d)throw Error("CSSResult is not constructable. Use `unsafeCSS` or `css` instead.");this.cssText=e,this.t=t}get styleSheet(){let e=this.o,t=this.t;if(u&&e===void 0){let n=t!==void 0&&t.length===1;n&&(e=f.get(t)),e===void 0&&((this.o=e=new CSSStyleSheet).replaceSync(this.cssText),n&&f.set(t,e))}return e}toString(){return this.cssText}},m=e=>new p(typeof e==`string`?e:e+``,void 0,d),h=(e,...t)=>new p(e.length===1?e[0]:t.reduce((t,n,r)=>t+(e=>{if(!0===e._$cssResult$)return e.cssText;if(typeof e==`number`)return e;throw Error(`Value passed to 'css' function must be a 'css' function result: `+e+`. Use 'unsafeCSS' to pass non-literal values, but take care to ensure page security.`)})(n)+e[r+1],e[0]),e,d),g=(e,t)=>{if(u)e.adoptedStyleSheets=t.map(e=>e instanceof CSSStyleSheet?e:e.styleSheet);else for(let n of t){let t=document.createElement(`style`),r=l.litNonce;r!==void 0&&t.setAttribute(`nonce`,r),t.textContent=n.cssText,e.appendChild(t)}},_=u?e=>e:e=>e instanceof CSSStyleSheet?(e=>{let t=``;for(let n of e.cssRules)t+=n.cssText;return m(t)})(e):e,{is:v,defineProperty:ee,getOwnPropertyDescriptor:y,getOwnPropertyNames:b,getOwnPropertySymbols:x,getPrototypeOf:S}=Object,C=globalThis,te=C.trustedTypes,ne=te?te.emptyScript:``,re=C.reactiveElementPolyfillSupport,w=(e,t)=>e,ie={toAttribute(e,t){switch(t){case Boolean:e=e?ne:null;break;case Object:case Array:e=e==null?e:JSON.stringify(e)}return e},fromAttribute(e,t){let n=e;switch(t){case Boolean:n=e!==null;break;case Number:n=e===null?null:Number(e);break;case Object:case Array:try{n=JSON.parse(e)}catch{n=null}}return n}},ae=(e,t)=>!v(e,t),oe={attribute:!0,type:String,converter:ie,reflect:!1,useDefault:!1,hasChanged:ae};Symbol.metadata??=Symbol(`metadata`),C.litPropertyMetadata??=new WeakMap;var T=class extends HTMLElement{static addInitializer(e){this._$Ei(),(this.l??=[]).push(e)}static get observedAttributes(){return this.finalize(),this._$Eh&&[...this._$Eh.keys()]}static createProperty(e,t=oe){if(t.state&&(t.attribute=!1),this._$Ei(),this.prototype.hasOwnProperty(e)&&((t=Object.create(t)).wrapped=!0),this.elementProperties.set(e,t),!t.noAccessor){let n=Symbol(),r=this.getPropertyDescriptor(e,n,t);r!==void 0&&ee(this.prototype,e,r)}}static getPropertyDescriptor(e,t,n){let{get:r,set:i}=y(this.prototype,e)??{get(){return this[t]},set(e){this[t]=e}};return{get:r,set(t){let a=r?.call(this);i?.call(this,t),this.requestUpdate(e,a,n)},configurable:!0,enumerable:!0}}static getPropertyOptions(e){return this.elementProperties.get(e)??oe}static _$Ei(){if(this.hasOwnProperty(w(`elementProperties`)))return;let e=S(this);e.finalize(),e.l!==void 0&&(this.l=[...e.l]),this.elementProperties=new Map(e.elementProperties)}static finalize(){if(this.hasOwnProperty(w(`finalized`)))return;if(this.finalized=!0,this._$Ei(),this.hasOwnProperty(w(`properties`))){let e=this.properties,t=[...b(e),...x(e)];for(let n of t)this.createProperty(n,e[n])}let e=this[Symbol.metadata];if(e!==null){let t=litPropertyMetadata.get(e);if(t!==void 0)for(let[e,n]of t)this.elementProperties.set(e,n)}this._$Eh=new Map;for(let[e,t]of this.elementProperties){let n=this._$Eu(e,t);n!==void 0&&this._$Eh.set(n,e)}this.elementStyles=this.finalizeStyles(this.styles)}static finalizeStyles(e){let t=[];if(Array.isArray(e)){let n=new Set(e.flat(1/0).reverse());for(let e of n)t.unshift(_(e))}else e!==void 0&&t.push(_(e));return t}static _$Eu(e,t){let n=t.attribute;return!1===n?void 0:typeof n==`string`?n:typeof e==`string`?e.toLowerCase():void 0}constructor(){super(),this._$Ep=void 0,this.isUpdatePending=!1,this.hasUpdated=!1,this._$Em=null,this._$Ev()}_$Ev(){this._$ES=new Promise(e=>this.enableUpdating=e),this._$AL=new Map,this._$E_(),this.requestUpdate(),this.constructor.l?.forEach(e=>e(this))}addController(e){(this._$EO??=new Set).add(e),this.renderRoot!==void 0&&this.isConnected&&e.hostConnected?.()}removeController(e){this._$EO?.delete(e)}_$E_(){let e=new Map,t=this.constructor.elementProperties;for(let n of t.keys())this.hasOwnProperty(n)&&(e.set(n,this[n]),delete this[n]);e.size>0&&(this._$Ep=e)}createRenderRoot(){let e=this.shadowRoot??this.attachShadow(this.constructor.shadowRootOptions);return g(e,this.constructor.elementStyles),e}connectedCallback(){this.renderRoot??=this.createRenderRoot(),this.enableUpdating(!0),this._$EO?.forEach(e=>e.hostConnected?.())}enableUpdating(e){}disconnectedCallback(){this._$EO?.forEach(e=>e.hostDisconnected?.())}attributeChangedCallback(e,t,n){this._$AK(e,n)}_$ET(e,t){let n=this.constructor.elementProperties.get(e),r=this.constructor._$Eu(e,n);if(r!==void 0&&!0===n.reflect){let i=(n.converter?.toAttribute===void 0?ie:n.converter).toAttribute(t,n.type);this._$Em=e,i==null?this.removeAttribute(r):this.setAttribute(r,i),this._$Em=null}}_$AK(e,t){let n=this.constructor,r=n._$Eh.get(e);if(r!==void 0&&this._$Em!==r){let e=n.getPropertyOptions(r),i=typeof e.converter==`function`?{fromAttribute:e.converter}:e.converter?.fromAttribute===void 0?ie:e.converter;this._$Em=r;let a=i.fromAttribute(t,e.type);this[r]=a??this._$Ej?.get(r)??a,this._$Em=null}}requestUpdate(e,t,n,r=!1,i){if(e!==void 0){let a=this.constructor;if(!1===r&&(i=this[e]),n??=a.getPropertyOptions(e),!((n.hasChanged??ae)(i,t)||n.useDefault&&n.reflect&&i===this._$Ej?.get(e)&&!this.hasAttribute(a._$Eu(e,n))))return;this.C(e,t,n)}!1===this.isUpdatePending&&(this._$ES=this._$EP())}C(e,t,{useDefault:n,reflect:r,wrapped:i},a){n&&!(this._$Ej??=new Map).has(e)&&(this._$Ej.set(e,a??t??this[e]),!0!==i||a!==void 0)||(this._$AL.has(e)||(this.hasUpdated||n||(t=void 0),this._$AL.set(e,t)),!0===r&&this._$Em!==e&&(this._$Eq??=new Set).add(e))}async _$EP(){this.isUpdatePending=!0;try{await this._$ES}catch(e){Promise.reject(e)}let e=this.scheduleUpdate();return e!=null&&await e,!this.isUpdatePending}scheduleUpdate(){return this.performUpdate()}performUpdate(){if(!this.isUpdatePending)return;if(!this.hasUpdated){if(this.renderRoot??=this.createRenderRoot(),this._$Ep){for(let[e,t]of this._$Ep)this[e]=t;this._$Ep=void 0}let e=this.constructor.elementProperties;if(e.size>0)for(let[t,n]of e){let{wrapped:e}=n,r=this[t];!0!==e||this._$AL.has(t)||r===void 0||this.C(t,void 0,n,r)}}let e=!1,t=this._$AL;try{e=this.shouldUpdate(t),e?(this.willUpdate(t),this._$EO?.forEach(e=>e.hostUpdate?.()),this.update(t)):this._$EM()}catch(t){throw e=!1,this._$EM(),t}e&&this._$AE(t)}willUpdate(e){}_$AE(e){this._$EO?.forEach(e=>e.hostUpdated?.()),this.hasUpdated||(this.hasUpdated=!0,this.firstUpdated(e)),this.updated(e)}_$EM(){this._$AL=new Map,this.isUpdatePending=!1}get updateComplete(){return this.getUpdateComplete()}getUpdateComplete(){return this._$ES}shouldUpdate(e){return!0}update(e){this._$Eq&&=this._$Eq.forEach(e=>this._$ET(e,this[e])),this._$EM()}updated(e){}firstUpdated(e){}};T.elementStyles=[],T.shadowRootOptions={mode:`open`},T[w(`elementProperties`)]=new Map,T[w(`finalized`)]=new Map,re?.({ReactiveElement:T}),(C.reactiveElementVersions??=[]).push(`2.1.2`);var E=globalThis,se=e=>e,D=E.trustedTypes,ce=D?D.createPolicy(`lit-html`,{createHTML:e=>e}):void 0,le=`$lit$`,O=`lit$${Math.random().toFixed(9).slice(2)}$`,ue=`?`+O,de=`<${ue}>`,k=document,A=()=>k.createComment(``),j=e=>e===null||typeof e!=`object`&&typeof e!=`function`,fe=Array.isArray,pe=e=>fe(e)||typeof e?.[Symbol.iterator]==`function`,me=`[ 	
\f\r]`,M=/<(?:(!--|\/[^a-zA-Z])|(\/?[a-zA-Z][^>\s]*)|(\/?$))/g,he=/-->/g,ge=/>/g,N=RegExp(`>|${me}(?:([^\\s"'>=/]+)(${me}*=${me}*(?:[^ \t\n\f\r"'\`<>=]|("|')|))|$)`,`g`),_e=/'/g,ve=/"/g,ye=/^(?:script|style|textarea|title)$/i,P=(e=>(t,...n)=>({_$litType$:e,strings:t,values:n}))(1),F=Symbol.for(`lit-noChange`),I=Symbol.for(`lit-nothing`),be=new WeakMap,L=k.createTreeWalker(k,129);function xe(e,t){if(!fe(e)||!e.hasOwnProperty(`raw`))throw Error(`invalid template strings array`);return ce===void 0?t:ce.createHTML(t)}var Se=(e,t)=>{let n=e.length-1,r=[],i,a=t===2?`<svg>`:t===3?`<math>`:``,o=M;for(let t=0;t<n;t++){let n=e[t],s,c,l=-1,u=0;for(;u<n.length&&(o.lastIndex=u,c=o.exec(n),c!==null);)u=o.lastIndex,o===M?c[1]===`!--`?o=he:c[1]===void 0?c[2]===void 0?c[3]!==void 0&&(o=N):(ye.test(c[2])&&(i=RegExp(`</`+c[2],`g`)),o=N):o=ge:o===N?c[0]===`>`?(o=i??M,l=-1):c[1]===void 0?l=-2:(l=o.lastIndex-c[2].length,s=c[1],o=c[3]===void 0?N:c[3]===`"`?ve:_e):o===ve||o===_e?o=N:o===he||o===ge?o=M:(o=N,i=void 0);let d=o===N&&e[t+1].startsWith(`/>`)?` `:``;a+=o===M?n+de:l>=0?(r.push(s),n.slice(0,l)+le+n.slice(l)+O+d):n+O+(l===-2?t:d)}return[xe(e,a+(e[n]||`<?>`)+(t===2?`</svg>`:t===3?`</math>`:``)),r]},R=class e{constructor({strings:t,_$litType$:n},r){let i;this.parts=[];let a=0,o=0,s=t.length-1,c=this.parts,[l,u]=Se(t,n);if(this.el=e.createElement(l,r),L.currentNode=this.el.content,n===2||n===3){let e=this.el.content.firstChild;e.replaceWith(...e.childNodes)}for(;(i=L.nextNode())!==null&&c.length<s;){if(i.nodeType===1){if(i.hasAttributes())for(let e of i.getAttributeNames())if(e.endsWith(le)){let t=u[o++],n=i.getAttribute(e).split(O),r=/([.?@])?(.*)/.exec(t);c.push({type:1,index:a,name:r[2],strings:n,ctor:r[1]===`.`?we:r[1]===`?`?Te:r[1]===`@`?Ee:V}),i.removeAttribute(e)}else e.startsWith(O)&&(c.push({type:6,index:a}),i.removeAttribute(e));if(ye.test(i.tagName)){let e=i.textContent.split(O),t=e.length-1;if(t>0){i.textContent=D?D.emptyScript:``;for(let n=0;n<t;n++)i.append(e[n],A()),L.nextNode(),c.push({type:2,index:++a});i.append(e[t],A())}}}else if(i.nodeType===8){if(i.data===ue)c.push({type:2,index:a});else{let e=-1;for(;(e=i.data.indexOf(O,e+1))!==-1;)c.push({type:7,index:a}),e+=O.length-1}}a++}}static createElement(e,t){let n=k.createElement(`template`);return n.innerHTML=e,n}};function z(e,t,n=e,r){if(t===F)return t;let i=r===void 0?n._$Cl:n._$Co?.[r],a=j(t)?void 0:t._$litDirective$;return i?.constructor!==a&&(i?._$AO?.(!1),a===void 0?i=void 0:(i=new a(e),i._$AT(e,n,r)),r===void 0?n._$Cl=i:(n._$Co??=[])[r]=i),i!==void 0&&(t=z(e,i._$AS(e,t.values),i,r)),t}var Ce=class{constructor(e,t){this._$AV=[],this._$AN=void 0,this._$AD=e,this._$AM=t}get parentNode(){return this._$AM.parentNode}get _$AU(){return this._$AM._$AU}u(e){let{el:{content:t},parts:n}=this._$AD,r=(e?.creationScope??k).importNode(t,!0);L.currentNode=r;let i=L.nextNode(),a=0,o=0,s=n[0];for(;s!==void 0;){if(a===s.index){let t;s.type===2?t=new B(i,i.nextSibling,this,e):s.type===1?t=new s.ctor(i,s.name,s.strings,this,e):s.type===6&&(t=new De(i,this,e)),this._$AV.push(t),s=n[++o]}a!==s?.index&&(i=L.nextNode(),a++)}return L.currentNode=k,r}p(e){let t=0;for(let n of this._$AV)n!==void 0&&(n.strings===void 0?n._$AI(e[t]):(n._$AI(e,n,t),t+=n.strings.length-2)),t++}},B=class e{get _$AU(){return this._$AM?._$AU??this._$Cv}constructor(e,t,n,r){this.type=2,this._$AH=I,this._$AN=void 0,this._$AA=e,this._$AB=t,this._$AM=n,this.options=r,this._$Cv=r?.isConnected??!0}get parentNode(){let e=this._$AA.parentNode,t=this._$AM;return t!==void 0&&e?.nodeType===11&&(e=t.parentNode),e}get startNode(){return this._$AA}get endNode(){return this._$AB}_$AI(e,t=this){e=z(this,e,t),j(e)?e===I||e==null||e===``?(this._$AH!==I&&this._$AR(),this._$AH=I):e!==this._$AH&&e!==F&&this._(e):e._$litType$===void 0?e.nodeType===void 0?pe(e)?this.k(e):this._(e):this.T(e):this.$(e)}O(e){return this._$AA.parentNode.insertBefore(e,this._$AB)}T(e){this._$AH!==e&&(this._$AR(),this._$AH=this.O(e))}_(e){this._$AH!==I&&j(this._$AH)?this._$AA.nextSibling.data=e:this.T(k.createTextNode(e)),this._$AH=e}$(e){let{values:t,_$litType$:n}=e,r=typeof n==`number`?this._$AC(e):(n.el===void 0&&(n.el=R.createElement(xe(n.h,n.h[0]),this.options)),n);if(this._$AH?._$AD===r)this._$AH.p(t);else{let e=new Ce(r,this),n=e.u(this.options);e.p(t),this.T(n),this._$AH=e}}_$AC(e){let t=be.get(e.strings);return t===void 0&&be.set(e.strings,t=new R(e)),t}k(t){fe(this._$AH)||(this._$AH=[],this._$AR());let n=this._$AH,r,i=0;for(let a of t)i===n.length?n.push(r=new e(this.O(A()),this.O(A()),this,this.options)):r=n[i],r._$AI(a),i++;i<n.length&&(this._$AR(r&&r._$AB.nextSibling,i),n.length=i)}_$AR(e=this._$AA.nextSibling,t){for(this._$AP?.(!1,!0,t);e!==this._$AB;){let t=se(e).nextSibling;se(e).remove(),e=t}}setConnected(e){this._$AM===void 0&&(this._$Cv=e,this._$AP?.(e))}},V=class{get tagName(){return this.element.tagName}get _$AU(){return this._$AM._$AU}constructor(e,t,n,r,i){this.type=1,this._$AH=I,this._$AN=void 0,this.element=e,this.name=t,this._$AM=r,this.options=i,n.length>2||n[0]!==``||n[1]!==``?(this._$AH=Array(n.length-1).fill(new String),this.strings=n):this._$AH=I}_$AI(e,t=this,n,r){let i=this.strings,a=!1;if(i===void 0)e=z(this,e,t,0),a=!j(e)||e!==this._$AH&&e!==F,a&&(this._$AH=e);else{let r=e,o,s;for(e=i[0],o=0;o<i.length-1;o++)s=z(this,r[n+o],t,o),s===F&&(s=this._$AH[o]),a||=!j(s)||s!==this._$AH[o],s===I?e=I:e!==I&&(e+=(s??``)+i[o+1]),this._$AH[o]=s}a&&!r&&this.j(e)}j(e){e===I?this.element.removeAttribute(this.name):this.element.setAttribute(this.name,e??``)}},we=class extends V{constructor(){super(...arguments),this.type=3}j(e){this.element[this.name]=e===I?void 0:e}},Te=class extends V{constructor(){super(...arguments),this.type=4}j(e){this.element.toggleAttribute(this.name,!!e&&e!==I)}},Ee=class extends V{constructor(e,t,n,r,i){super(e,t,n,r,i),this.type=5}_$AI(e,t=this){if((e=z(this,e,t,0)??I)===F)return;let n=this._$AH,r=e===I&&n!==I||e.capture!==n.capture||e.once!==n.once||e.passive!==n.passive,i=e!==I&&(n===I||r);r&&this.element.removeEventListener(this.name,this,n),i&&this.element.addEventListener(this.name,this,e),this._$AH=e}handleEvent(e){typeof this._$AH==`function`?this._$AH.call(this.options?.host??this.element,e):this._$AH.handleEvent(e)}},De=class{constructor(e,t,n){this.element=e,this.type=6,this._$AN=void 0,this._$AM=t,this.options=n}get _$AU(){return this._$AM._$AU}_$AI(e){z(this,e)}},Oe=E.litHtmlPolyfillSupport;Oe?.(R,B),(E.litHtmlVersions??=[]).push(`3.3.3`);var ke=(e,t,n)=>{let r=n?.renderBefore??t,i=r._$litPart$;if(i===void 0){let e=n?.renderBefore??null;r._$litPart$=i=new B(t.insertBefore(A(),e),e,void 0,n??{})}return i._$AI(e),i},H=globalThis,U=class extends T{constructor(){super(...arguments),this.renderOptions={host:this},this._$Do=void 0}createRenderRoot(){let e=super.createRenderRoot();return this.renderOptions.renderBefore??=e.firstChild,e}update(e){let t=this.render();this.hasUpdated||(this.renderOptions.isConnected=this.isConnected),super.update(e),this._$Do=ke(t,this.renderRoot,this.renderOptions)}connectedCallback(){super.connectedCallback(),this._$Do?.setConnected(!0)}disconnectedCallback(){super.disconnectedCallback(),this._$Do?.setConnected(!1)}render(){return F}};U._$litElement$=!0,U.finalized=!0,H.litElementHydrateSupport?.({LitElement:U});var Ae=H.litElementPolyfillSupport;Ae?.({LitElement:U}),(H.litElementVersions??=[]).push(`4.2.2`);var je=o(((e,t)=>{t.exports=function(){return typeof Promise==`function`&&Promise.prototype&&Promise.prototype.then}})),W=o((e=>{var t,n=[0,26,44,70,100,134,172,196,242,292,346,404,466,532,581,655,733,815,901,991,1085,1156,1258,1364,1474,1588,1706,1828,1921,2051,2185,2323,2465,2611,2761,2876,3034,3196,3362,3532,3706];e.getSymbolSize=function(e){if(!e)throw Error(`"version" cannot be null or undefined`);if(e<1||e>40)throw Error(`"version" should be in range from 1 to 40`);return e*4+17},e.getSymbolTotalCodewords=function(e){return n[e]},e.getBCHDigit=function(e){let t=0;for(;e!==0;)t++,e>>>=1;return t},e.setToSJISFunction=function(e){if(typeof e!=`function`)throw Error(`"toSJISFunc" is not a valid function.`);t=e},e.isKanjiModeEnabled=function(){return t!==void 0},e.toSJIS=function(e){return t(e)}})),G=o((e=>{e.L={bit:1},e.M={bit:0},e.Q={bit:3},e.H={bit:2};function t(t){if(typeof t!=`string`)throw Error(`Param is not a string`);switch(t.toLowerCase()){case`l`:case`low`:return e.L;case`m`:case`medium`:return e.M;case`q`:case`quartile`:return e.Q;case`h`:case`high`:return e.H;default:throw Error(`Unknown EC Level: `+t)}}e.isValid=function(e){return e&&e.bit!==void 0&&e.bit>=0&&e.bit<4},e.from=function(n,r){if(e.isValid(n))return n;try{return t(n)}catch{return r}}})),Me=o(((e,t)=>{function n(){this.buffer=[],this.length=0}n.prototype={get:function(e){let t=Math.floor(e/8);return(this.buffer[t]>>>7-e%8&1)==1},put:function(e,t){for(let n=0;n<t;n++)this.putBit((e>>>t-n-1&1)==1)},getLengthInBits:function(){return this.length},putBit:function(e){let t=Math.floor(this.length/8);this.buffer.length<=t&&this.buffer.push(0),e&&(this.buffer[t]|=128>>>this.length%8),this.length++}},t.exports=n})),Ne=o(((e,t)=>{function n(e){if(!e||e<1)throw Error(`BitMatrix size must be defined and greater than 0`);this.size=e,this.data=new Uint8Array(e*e),this.reservedBit=new Uint8Array(e*e)}n.prototype.set=function(e,t,n,r){let i=e*this.size+t;this.data[i]=n,r&&(this.reservedBit[i]=!0)},n.prototype.get=function(e,t){return this.data[e*this.size+t]},n.prototype.xor=function(e,t,n){this.data[e*this.size+t]^=n},n.prototype.isReserved=function(e,t){return this.reservedBit[e*this.size+t]},t.exports=n})),Pe=o((e=>{var t=W().getSymbolSize;e.getRowColCoords=function(e){if(e===1)return[];let n=Math.floor(e/7)+2,r=t(e),i=r===145?26:Math.ceil((r-13)/(2*n-2))*2,a=[r-7];for(let e=1;e<n-1;e++)a[e]=a[e-1]-i;return a.push(6),a.reverse()},e.getPositions=function(t){let n=[],r=e.getRowColCoords(t),i=r.length;for(let e=0;e<i;e++)for(let t=0;t<i;t++)e===0&&t===0||e===0&&t===i-1||e===i-1&&t===0||n.push([r[e],r[t]]);return n}})),Fe=o((e=>{var t=W().getSymbolSize,n=7;e.getPositions=function(e){let r=t(e);return[[0,0],[r-n,0],[0,r-n]]}})),Ie=o((e=>{e.Patterns={PATTERN000:0,PATTERN001:1,PATTERN010:2,PATTERN011:3,PATTERN100:4,PATTERN101:5,PATTERN110:6,PATTERN111:7};var t={N1:3,N2:3,N3:40,N4:10};e.isValid=function(e){return e!=null&&e!==``&&!isNaN(e)&&e>=0&&e<=7},e.from=function(t){return e.isValid(t)?parseInt(t,10):void 0},e.getPenaltyN1=function(e){let n=e.size,r=0,i=0,a=0,o=null,s=null;for(let c=0;c<n;c++){i=a=0,o=s=null;for(let l=0;l<n;l++){let n=e.get(c,l);n===o?i++:(i>=5&&(r+=t.N1+(i-5)),o=n,i=1),n=e.get(l,c),n===s?a++:(a>=5&&(r+=t.N1+(a-5)),s=n,a=1)}i>=5&&(r+=t.N1+(i-5)),a>=5&&(r+=t.N1+(a-5))}return r},e.getPenaltyN2=function(e){let n=e.size,r=0;for(let t=0;t<n-1;t++)for(let i=0;i<n-1;i++){let n=e.get(t,i)+e.get(t,i+1)+e.get(t+1,i)+e.get(t+1,i+1);(n===4||n===0)&&r++}return r*t.N2},e.getPenaltyN3=function(e){let n=e.size,r=0,i=0,a=0;for(let t=0;t<n;t++){i=a=0;for(let o=0;o<n;o++)i=i<<1&2047|e.get(t,o),o>=10&&(i===1488||i===93)&&r++,a=a<<1&2047|e.get(o,t),o>=10&&(a===1488||a===93)&&r++}return r*t.N3},e.getPenaltyN4=function(e){let n=0,r=e.data.length;for(let t=0;t<r;t++)n+=e.data[t];return Math.abs(Math.ceil(n*100/r/5)-10)*t.N4};function n(t,n,r){switch(t){case e.Patterns.PATTERN000:return(n+r)%2==0;case e.Patterns.PATTERN001:return n%2==0;case e.Patterns.PATTERN010:return r%3==0;case e.Patterns.PATTERN011:return(n+r)%3==0;case e.Patterns.PATTERN100:return(Math.floor(n/2)+Math.floor(r/3))%2==0;case e.Patterns.PATTERN101:return n*r%2+n*r%3==0;case e.Patterns.PATTERN110:return(n*r%2+n*r%3)%2==0;case e.Patterns.PATTERN111:return(n*r%3+(n+r)%2)%2==0;default:throw Error(`bad maskPattern:`+t)}}e.applyMask=function(e,t){let r=t.size;for(let i=0;i<r;i++)for(let a=0;a<r;a++)t.isReserved(a,i)||t.xor(a,i,n(e,a,i))},e.getBestMask=function(t,n){let r=Object.keys(e.Patterns).length,i=0,a=1/0;for(let o=0;o<r;o++){n(o),e.applyMask(o,t);let r=e.getPenaltyN1(t)+e.getPenaltyN2(t)+e.getPenaltyN3(t)+e.getPenaltyN4(t);e.applyMask(o,t),r<a&&(a=r,i=o)}return i}})),Le=o((e=>{var t=G(),n=[1,1,1,1,1,1,1,1,1,1,2,2,1,2,2,4,1,2,4,4,2,4,4,4,2,4,6,5,2,4,6,6,2,5,8,8,4,5,8,8,4,5,8,11,4,8,10,11,4,9,12,16,4,9,16,16,6,10,12,18,6,10,17,16,6,11,16,19,6,13,18,21,7,14,21,25,8,16,20,25,8,17,23,25,9,17,23,34,9,18,25,30,10,20,27,32,12,21,29,35,12,23,34,37,12,25,34,40,13,26,35,42,14,28,38,45,15,29,40,48,16,31,43,51,17,33,45,54,18,35,48,57,19,37,51,60,19,38,53,63,20,40,56,66,21,43,59,70,22,45,62,74,24,47,65,77,25,49,68,81],r=[7,10,13,17,10,16,22,28,15,26,36,44,20,36,52,64,26,48,72,88,36,64,96,112,40,72,108,130,48,88,132,156,60,110,160,192,72,130,192,224,80,150,224,264,96,176,260,308,104,198,288,352,120,216,320,384,132,240,360,432,144,280,408,480,168,308,448,532,180,338,504,588,196,364,546,650,224,416,600,700,224,442,644,750,252,476,690,816,270,504,750,900,300,560,810,960,312,588,870,1050,336,644,952,1110,360,700,1020,1200,390,728,1050,1260,420,784,1140,1350,450,812,1200,1440,480,868,1290,1530,510,924,1350,1620,540,980,1440,1710,570,1036,1530,1800,570,1064,1590,1890,600,1120,1680,1980,630,1204,1770,2100,660,1260,1860,2220,720,1316,1950,2310,750,1372,2040,2430];e.getBlocksCount=function(e,r){switch(r){case t.L:return n[(e-1)*4+0];case t.M:return n[(e-1)*4+1];case t.Q:return n[(e-1)*4+2];case t.H:return n[(e-1)*4+3];default:return}},e.getTotalCodewordsCount=function(e,n){switch(n){case t.L:return r[(e-1)*4+0];case t.M:return r[(e-1)*4+1];case t.Q:return r[(e-1)*4+2];case t.H:return r[(e-1)*4+3];default:return}}})),Re=o((e=>{var t=new Uint8Array(512),n=new Uint8Array(256);(function(){let e=1;for(let r=0;r<255;r++)t[r]=e,n[e]=r,e<<=1,e&256&&(e^=285);for(let e=255;e<512;e++)t[e]=t[e-255]})(),e.log=function(e){if(e<1)throw Error(`log(`+e+`)`);return n[e]},e.exp=function(e){return t[e]},e.mul=function(e,r){return e===0||r===0?0:t[n[e]+n[r]]}})),ze=o((e=>{var t=Re();e.mul=function(e,n){let r=new Uint8Array(e.length+n.length-1);for(let i=0;i<e.length;i++)for(let a=0;a<n.length;a++)r[i+a]^=t.mul(e[i],n[a]);return r},e.mod=function(e,n){let r=new Uint8Array(e);for(;r.length-n.length>=0;){let e=r[0];for(let i=0;i<n.length;i++)r[i]^=t.mul(n[i],e);let i=0;for(;i<r.length&&r[i]===0;)i++;r=r.slice(i)}return r},e.generateECPolynomial=function(n){let r=new Uint8Array([1]);for(let i=0;i<n;i++)r=e.mul(r,new Uint8Array([1,t.exp(i)]));return r}})),Be=o(((e,t)=>{var n=ze();function r(e){this.genPoly=void 0,this.degree=e,this.degree&&this.initialize(this.degree)}r.prototype.initialize=function(e){this.degree=e,this.genPoly=n.generateECPolynomial(this.degree)},r.prototype.encode=function(e){if(!this.genPoly)throw Error(`Encoder not initialized`);let t=new Uint8Array(e.length+this.degree);t.set(e);let r=n.mod(t,this.genPoly),i=this.degree-r.length;if(i>0){let e=new Uint8Array(this.degree);return e.set(r,i),e}return r},t.exports=r})),Ve=o((e=>{e.isValid=function(e){return!isNaN(e)&&e>=1&&e<=40}})),He=o((e=>{var t=`[0-9]+`,n=`[A-Z $%*+\\-./:]+`,r=`(?:[u3000-u303F]|[u3040-u309F]|[u30A0-u30FF]|[uFF00-uFFEF]|[u4E00-u9FAF]|[u2605-u2606]|[u2190-u2195]|u203B|[u2010u2015u2018u2019u2025u2026u201Cu201Du2225u2260]|[u0391-u0451]|[u00A7u00A8u00B1u00B4u00D7u00F7])+`;r=r.replace(/u/g,`\\u`);var i=`(?:(?![A-Z0-9 $%*+\\-./:]|`+r+`)(?:.|[\r
]))+`;e.KANJI=new RegExp(r,`g`),e.BYTE_KANJI=RegExp(`[^A-Z0-9 $%*+\\-./:]+`,`g`),e.BYTE=new RegExp(i,`g`),e.NUMERIC=new RegExp(t,`g`),e.ALPHANUMERIC=new RegExp(n,`g`);var a=RegExp(`^`+r+`$`),o=RegExp(`^[0-9]+$`),s=RegExp(`^[A-Z0-9 $%*+\\-./:]+$`);e.testKanji=function(e){return a.test(e)},e.testNumeric=function(e){return o.test(e)},e.testAlphanumeric=function(e){return s.test(e)}})),K=o((e=>{var t=Ve(),n=He();e.NUMERIC={id:`Numeric`,bit:1,ccBits:[10,12,14]},e.ALPHANUMERIC={id:`Alphanumeric`,bit:2,ccBits:[9,11,13]},e.BYTE={id:`Byte`,bit:4,ccBits:[8,16,16]},e.KANJI={id:`Kanji`,bit:8,ccBits:[8,10,12]},e.MIXED={bit:-1},e.getCharCountIndicator=function(e,n){if(!e.ccBits)throw Error(`Invalid mode: `+e);if(!t.isValid(n))throw Error(`Invalid version: `+n);return n>=1&&n<10?e.ccBits[0]:n<27?e.ccBits[1]:e.ccBits[2]},e.getBestModeForData=function(t){return n.testNumeric(t)?e.NUMERIC:n.testAlphanumeric(t)?e.ALPHANUMERIC:n.testKanji(t)?e.KANJI:e.BYTE},e.toString=function(e){if(e&&e.id)return e.id;throw Error(`Invalid mode`)},e.isValid=function(e){return e&&e.bit&&e.ccBits};function r(t){if(typeof t!=`string`)throw Error(`Param is not a string`);switch(t.toLowerCase()){case`numeric`:return e.NUMERIC;case`alphanumeric`:return e.ALPHANUMERIC;case`kanji`:return e.KANJI;case`byte`:return e.BYTE;default:throw Error(`Unknown mode: `+t)}}e.from=function(t,n){if(e.isValid(t))return t;try{return r(t)}catch{return n}}})),Ue=o((e=>{var t=W(),n=Le(),r=G(),i=K(),a=Ve(),o=7973,s=t.getBCHDigit(o);function c(t,n,r){for(let i=1;i<=40;i++)if(n<=e.getCapacity(i,r,t))return i}function l(e,t){return i.getCharCountIndicator(e,t)+4}function u(e,t){let n=0;return e.forEach(function(e){let r=l(e.mode,t);n+=r+e.getBitsLength()}),n}function d(t,n){for(let r=1;r<=40;r++)if(u(t,r)<=e.getCapacity(r,n,i.MIXED))return r}e.from=function(e,t){return a.isValid(e)?parseInt(e,10):t},e.getCapacity=function(e,r,o){if(!a.isValid(e))throw Error(`Invalid QR Code version`);o===void 0&&(o=i.BYTE);let s=(t.getSymbolTotalCodewords(e)-n.getTotalCodewordsCount(e,r))*8;if(o===i.MIXED)return s;let c=s-l(o,e);switch(o){case i.NUMERIC:return Math.floor(c/10*3);case i.ALPHANUMERIC:return Math.floor(c/11*2);case i.KANJI:return Math.floor(c/13);case i.BYTE:default:return Math.floor(c/8)}},e.getBestVersionForData=function(e,t){let n,i=r.from(t,r.M);if(Array.isArray(e)){if(e.length>1)return d(e,i);if(e.length===0)return 1;n=e[0]}else n=e;return c(n.mode,n.getLength(),i)},e.getEncodedBits=function(e){if(!a.isValid(e)||e<7)throw Error(`Invalid QR Code version`);let n=e<<12;for(;t.getBCHDigit(n)-s>=0;)n^=o<<t.getBCHDigit(n)-s;return e<<12|n}})),We=o((e=>{var t=W(),n=1335,r=21522,i=t.getBCHDigit(n);e.getEncodedBits=function(e,a){let o=e.bit<<3|a,s=o<<10;for(;t.getBCHDigit(s)-i>=0;)s^=n<<t.getBCHDigit(s)-i;return(o<<10|s)^r}})),Ge=o(((e,t)=>{var n=K();function r(e){this.mode=n.NUMERIC,this.data=e.toString()}r.getBitsLength=function(e){return 10*Math.floor(e/3)+(e%3?e%3*3+1:0)},r.prototype.getLength=function(){return this.data.length},r.prototype.getBitsLength=function(){return r.getBitsLength(this.data.length)},r.prototype.write=function(e){let t,n,r;for(t=0;t+3<=this.data.length;t+=3)n=this.data.substr(t,3),r=parseInt(n,10),e.put(r,10);let i=this.data.length-t;i>0&&(n=this.data.substr(t),r=parseInt(n,10),e.put(r,i*3+1))},t.exports=r})),Ke=o(((e,t)=>{var n=K(),r=`0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:`.split(``);function i(e){this.mode=n.ALPHANUMERIC,this.data=e}i.getBitsLength=function(e){return 11*Math.floor(e/2)+e%2*6},i.prototype.getLength=function(){return this.data.length},i.prototype.getBitsLength=function(){return i.getBitsLength(this.data.length)},i.prototype.write=function(e){let t=0;for(;t+2<=this.data.length;t+=2){let n=r.indexOf(this.data[t])*45;n+=r.indexOf(this.data[t+1]),e.put(n,11)}this.data.length%2&&e.put(r.indexOf(this.data[t]),6)},t.exports=i})),qe=o(((e,t)=>{var n=K();function r(e){this.mode=n.BYTE,this.data=typeof e==`string`?new TextEncoder().encode(e):new Uint8Array(e)}r.getBitsLength=function(e){return e*8},r.prototype.getLength=function(){return this.data.length},r.prototype.getBitsLength=function(){return r.getBitsLength(this.data.length)},r.prototype.write=function(e){for(let t=0,n=this.data.length;t<n;t++)e.put(this.data[t],8)},t.exports=r})),Je=o(((e,t)=>{var n=K(),r=W();function i(e){this.mode=n.KANJI,this.data=e}i.getBitsLength=function(e){return e*13},i.prototype.getLength=function(){return this.data.length},i.prototype.getBitsLength=function(){return i.getBitsLength(this.data.length)},i.prototype.write=function(e){let t=0;for(;t<this.data.length;t++){let n=r.toSJIS(this.data[t]);if(n>=33088&&n<=40956)n-=33088;else if(n>=57408&&n<=60351)n-=49472;else throw Error(`Invalid SJIS character: `+this.data[t]+`
Make sure your charset is UTF-8`);n=(n>>>8&255)*192+(n&255),e.put(n,13)}},t.exports=i})),Ye=o(((e,t)=>{var n={single_source_shortest_paths:function(e,t,r){var i={},a={};a[t]=0;var o=n.PriorityQueue.make();o.push(t,0);for(var s,c,l,u,d,f,p,m,h;!o.empty();)for(l in s=o.pop(),c=s.value,u=s.cost,d=e[c]||{},d)d.hasOwnProperty(l)&&(f=d[l],p=u+f,m=a[l],h=a[l]===void 0,(h||m>p)&&(a[l]=p,o.push(l,p),i[l]=c));if(r!==void 0&&a[r]===void 0){var g=[`Could not find a path from `,t,` to `,r,`.`].join(``);throw Error(g)}return i},extract_shortest_path_from_predecessor_list:function(e,t){for(var n=[],r=t;r;)n.push(r),e[r],r=e[r];return n.reverse(),n},find_path:function(e,t,r){var i=n.single_source_shortest_paths(e,t,r);return n.extract_shortest_path_from_predecessor_list(i,r)},PriorityQueue:{make:function(e){var t=n.PriorityQueue,r={},i;for(i in e||={},t)t.hasOwnProperty(i)&&(r[i]=t[i]);return r.queue=[],r.sorter=e.sorter||t.default_sorter,r},default_sorter:function(e,t){return e.cost-t.cost},push:function(e,t){var n={value:e,cost:t};this.queue.push(n),this.queue.sort(this.sorter)},pop:function(){return this.queue.shift()},empty:function(){return this.queue.length===0}}};t!==void 0&&(t.exports=n)})),Xe=o((e=>{var t=K(),n=Ge(),r=Ke(),i=qe(),a=Je(),o=He(),s=W(),c=Ye();function l(e){return unescape(encodeURIComponent(e)).length}function u(e,t,n){let r=[],i;for(;(i=e.exec(n))!==null;)r.push({data:i[0],index:i.index,mode:t,length:i[0].length});return r}function d(e){let n=u(o.NUMERIC,t.NUMERIC,e),r=u(o.ALPHANUMERIC,t.ALPHANUMERIC,e),i,a;return s.isKanjiModeEnabled()?(i=u(o.BYTE,t.BYTE,e),a=u(o.KANJI,t.KANJI,e)):(i=u(o.BYTE_KANJI,t.BYTE,e),a=[]),n.concat(r,i,a).sort(function(e,t){return e.index-t.index}).map(function(e){return{data:e.data,mode:e.mode,length:e.length}})}function f(e,o){switch(o){case t.NUMERIC:return n.getBitsLength(e);case t.ALPHANUMERIC:return r.getBitsLength(e);case t.KANJI:return a.getBitsLength(e);case t.BYTE:return i.getBitsLength(e)}}function p(e){return e.reduce(function(e,t){let n=e.length-1>=0?e[e.length-1]:null;return n&&n.mode===t.mode?(e[e.length-1].data+=t.data,e):(e.push(t),e)},[])}function m(e){let n=[];for(let r=0;r<e.length;r++){let i=e[r];switch(i.mode){case t.NUMERIC:n.push([i,{data:i.data,mode:t.ALPHANUMERIC,length:i.length},{data:i.data,mode:t.BYTE,length:i.length}]);break;case t.ALPHANUMERIC:n.push([i,{data:i.data,mode:t.BYTE,length:i.length}]);break;case t.KANJI:n.push([i,{data:i.data,mode:t.BYTE,length:l(i.data)}]);break;case t.BYTE:n.push([{data:i.data,mode:t.BYTE,length:l(i.data)}])}}return n}function h(e,n){let r={},i={start:{}},a=[`start`];for(let o=0;o<e.length;o++){let s=e[o],c=[];for(let e=0;e<s.length;e++){let l=s[e],u=``+o+e;c.push(u),r[u]={node:l,lastCount:0},i[u]={};for(let e=0;e<a.length;e++){let o=a[e];r[o]&&r[o].node.mode===l.mode?(i[o][u]=f(r[o].lastCount+l.length,l.mode)-f(r[o].lastCount,l.mode),r[o].lastCount+=l.length):(r[o]&&(r[o].lastCount=l.length),i[o][u]=f(l.length,l.mode)+4+t.getCharCountIndicator(l.mode,n))}}a=c}for(let e=0;e<a.length;e++)i[a[e]].end=0;return{map:i,table:r}}function g(e,o){let c,l=t.getBestModeForData(e);if(c=t.from(o,l),c!==t.BYTE&&c.bit<l.bit)throw Error(`"`+e+`" cannot be encoded with mode `+t.toString(c)+`.
 Suggested mode is: `+t.toString(l));switch(c===t.KANJI&&!s.isKanjiModeEnabled()&&(c=t.BYTE),c){case t.NUMERIC:return new n(e);case t.ALPHANUMERIC:return new r(e);case t.KANJI:return new a(e);case t.BYTE:return new i(e)}}e.fromArray=function(e){return e.reduce(function(e,t){return typeof t==`string`?e.push(g(t,null)):t.data&&e.push(g(t.data,t.mode)),e},[])},e.fromString=function(t,n){let r=h(m(d(t,s.isKanjiModeEnabled())),n),i=c.find_path(r.map,`start`,`end`),a=[];for(let e=1;e<i.length-1;e++)a.push(r.table[i[e]].node);return e.fromArray(p(a))},e.rawSplit=function(t){return e.fromArray(d(t,s.isKanjiModeEnabled()))}})),Ze=o((e=>{var t=W(),n=G(),r=Me(),i=Ne(),a=Pe(),o=Fe(),s=Ie(),c=Le(),l=Be(),u=Ue(),d=We(),f=K(),p=Xe();function m(e,t){let n=e.size,r=o.getPositions(t);for(let t=0;t<r.length;t++){let i=r[t][0],a=r[t][1];for(let t=-1;t<=7;t++)if(!(i+t<=-1||n<=i+t))for(let r=-1;r<=7;r++)a+r<=-1||n<=a+r||(t>=0&&t<=6&&(r===0||r===6)||r>=0&&r<=6&&(t===0||t===6)||t>=2&&t<=4&&r>=2&&r<=4?e.set(i+t,a+r,!0,!0):e.set(i+t,a+r,!1,!0))}}function h(e){let t=e.size;for(let n=8;n<t-8;n++){let t=n%2==0;e.set(n,6,t,!0),e.set(6,n,t,!0)}}function g(e,t){let n=a.getPositions(t);for(let t=0;t<n.length;t++){let r=n[t][0],i=n[t][1];for(let t=-2;t<=2;t++)for(let n=-2;n<=2;n++)t===-2||t===2||n===-2||n===2||t===0&&n===0?e.set(r+t,i+n,!0,!0):e.set(r+t,i+n,!1,!0)}}function _(e,t){let n=e.size,r=u.getEncodedBits(t),i,a,o;for(let t=0;t<18;t++)i=Math.floor(t/3),a=t%3+n-8-3,o=(r>>t&1)==1,e.set(i,a,o,!0),e.set(a,i,o,!0)}function v(e,t,n){let r=e.size,i=d.getEncodedBits(t,n),a,o;for(a=0;a<15;a++)o=(i>>a&1)==1,a<6?e.set(a,8,o,!0):a<8?e.set(a+1,8,o,!0):e.set(r-15+a,8,o,!0),a<8?e.set(8,r-a-1,o,!0):a<9?e.set(8,15-a-1+1,o,!0):e.set(8,15-a-1,o,!0);e.set(r-8,8,1,!0)}function ee(e,t){let n=e.size,r=-1,i=n-1,a=7,o=0;for(let s=n-1;s>0;s-=2)for(s===6&&s--;;){for(let n=0;n<2;n++)if(!e.isReserved(i,s-n)){let r=!1;o<t.length&&(r=(t[o]>>>a&1)==1),e.set(i,s-n,r),a--,a===-1&&(o++,a=7)}if(i+=r,i<0||n<=i){i-=r,r=-r;break}}}function y(e,n,i){let a=new r;i.forEach(function(t){a.put(t.mode.bit,4),a.put(t.getLength(),f.getCharCountIndicator(t.mode,e)),t.write(a)});let o=(t.getSymbolTotalCodewords(e)-c.getTotalCodewordsCount(e,n))*8;for(a.getLengthInBits()+4<=o&&a.put(0,4);a.getLengthInBits()%8!=0;)a.putBit(0);let s=(o-a.getLengthInBits())/8;for(let e=0;e<s;e++)a.put(e%2?17:236,8);return b(a,e,n)}function b(e,n,r){let i=t.getSymbolTotalCodewords(n),a=i-c.getTotalCodewordsCount(n,r),o=c.getBlocksCount(n,r),s=o-i%o,u=Math.floor(i/o),d=Math.floor(a/o),f=d+1,p=u-d,m=new l(p),h=0,g=Array(o),_=Array(o),v=0,ee=new Uint8Array(e.buffer);for(let e=0;e<o;e++){let t=e<s?d:f;g[e]=ee.slice(h,h+t),_[e]=m.encode(g[e]),h+=t,v=Math.max(v,t)}let y=new Uint8Array(i),b=0,x,S;for(x=0;x<v;x++)for(S=0;S<o;S++)x<g[S].length&&(y[b++]=g[S][x]);for(x=0;x<p;x++)for(S=0;S<o;S++)y[b++]=_[S][x];return y}function x(e,n,r,a){let o;if(Array.isArray(e))o=p.fromArray(e);else if(typeof e==`string`){let t=n;if(!t){let n=p.rawSplit(e);t=u.getBestVersionForData(n,r)}o=p.fromString(e,t||40)}else throw Error(`Invalid data`);let c=u.getBestVersionForData(o,r);if(!c)throw Error(`The amount of data is too big to be stored in a QR Code`);if(!n)n=c;else if(n<c)throw Error(`
The chosen QR Code version cannot contain this amount of data.
Minimum version required to store current data is: `+c+`.
`);let l=y(n,r,o),d=new i(t.getSymbolSize(n));return m(d,n),h(d),g(d,n),v(d,r,0),n>=7&&_(d,n),ee(d,l),isNaN(a)&&(a=s.getBestMask(d,v.bind(null,d,r))),s.applyMask(a,d),v(d,r,a),{modules:d,version:n,errorCorrectionLevel:r,maskPattern:a,segments:o}}e.create=function(e,r){if(e===void 0||e===``)throw Error(`No input text`);let i=n.M,a,o;return r!==void 0&&(i=n.from(r.errorCorrectionLevel,n.M),a=u.from(r.version),o=s.from(r.maskPattern),r.toSJISFunc&&t.setToSJISFunction(r.toSJISFunc)),x(e,a,i,o)}})),Qe=o((e=>{function t(e){if(typeof e==`number`&&(e=e.toString()),typeof e!=`string`)throw Error(`Color should be defined as hex string`);let t=e.slice().replace(`#`,``).split(``);if(t.length<3||t.length===5||t.length>8)throw Error(`Invalid hex color: `+e);(t.length===3||t.length===4)&&(t=Array.prototype.concat.apply([],t.map(function(e){return[e,e]}))),t.length===6&&t.push(`F`,`F`);let n=parseInt(t.join(``),16);return{r:n>>24&255,g:n>>16&255,b:n>>8&255,a:n&255,hex:`#`+t.slice(0,6).join(``)}}e.getOptions=function(e){e||={},e.color||(e.color={});let n=e.margin===void 0||e.margin===null||e.margin<0?4:e.margin,r=e.width&&e.width>=21?e.width:void 0,i=e.scale||4;return{width:r,scale:r?4:i,margin:n,color:{dark:t(e.color.dark||`#000000ff`),light:t(e.color.light||`#ffffffff`)},type:e.type,rendererOpts:e.rendererOpts||{}}},e.getScale=function(e,t){return t.width&&t.width>=e+t.margin*2?t.width/(e+t.margin*2):t.scale},e.getImageWidth=function(t,n){let r=e.getScale(t,n);return Math.floor((t+n.margin*2)*r)},e.qrToImageData=function(t,n,r){let i=n.modules.size,a=n.modules.data,o=e.getScale(i,r),s=Math.floor((i+r.margin*2)*o),c=r.margin*o,l=[r.color.light,r.color.dark];for(let e=0;e<s;e++)for(let n=0;n<s;n++){let u=(e*s+n)*4,d=r.color.light;if(e>=c&&n>=c&&e<s-c&&n<s-c){let t=Math.floor((e-c)/o),r=Math.floor((n-c)/o);d=l[+!!a[t*i+r]]}t[u++]=d.r,t[u++]=d.g,t[u++]=d.b,t[u]=d.a}}})),$e=o((e=>{var t=Qe();function n(e,t,n){e.clearRect(0,0,t.width,t.height),t.style||={},t.height=n,t.width=n,t.style.height=n+`px`,t.style.width=n+`px`}function r(){try{return document.createElement(`canvas`)}catch{throw Error(`You need to specify a canvas element`)}}e.render=function(e,i,a){let o=a,s=i;o===void 0&&(!i||!i.getContext)&&(o=i,i=void 0),i||(s=r()),o=t.getOptions(o);let c=t.getImageWidth(e.modules.size,o),l=s.getContext(`2d`),u=l.createImageData(c,c);return t.qrToImageData(u.data,e,o),n(l,s,c),l.putImageData(u,0,0),s},e.renderToDataURL=function(t,n,r){let i=r;i===void 0&&(!n||!n.getContext)&&(i=n,n=void 0),i||={};let a=e.render(t,n,i),o=i.type||`image/png`,s=i.rendererOpts||{};return a.toDataURL(o,s.quality)}})),et=o((e=>{var t=Qe();function n(e,t){let n=e.a/255,r=t+`="`+e.hex+`"`;return n<1?r+` `+t+`-opacity="`+n.toFixed(2).slice(1)+`"`:r}function r(e,t,n){let r=e+t;return n!==void 0&&(r+=` `+n),r}function i(e,t,n){let i=``,a=0,o=!1,s=0;for(let c=0;c<e.length;c++){let l=Math.floor(c%t),u=Math.floor(c/t);!l&&!o&&(o=!0),e[c]?(s++,c>0&&l>0&&e[c-1]||(i+=o?r(`M`,l+n,.5+u+n):r(`m`,a,0),a=0,o=!1),l+1<t&&e[c+1]||(i+=r(`h`,s),s=0)):a++}return i}e.render=function(e,r,a){let o=t.getOptions(r),s=e.modules.size,c=e.modules.data,l=s+o.margin*2,u=o.color.light.a?`<path `+n(o.color.light,`fill`)+` d="M0 0h`+l+`v`+l+`H0z"/>`:``,d=`<path `+n(o.color.dark,`stroke`)+` d="`+i(c,s,o.margin)+`"/>`,f=`viewBox="0 0 `+l+` `+l+`"`,p=`<svg xmlns="http://www.w3.org/2000/svg" `+(o.width?`width="`+o.width+`" height="`+o.width+`" `:``)+f+` shape-rendering="crispEdges">`+u+d+`</svg>
`;return typeof a==`function`&&a(null,p),p}})),tt=c(o((e=>{var t=je(),n=Ze(),r=$e(),i=et();function a(e,r,i,a,o){let s=[].slice.call(arguments,1),c=s.length,l=typeof s[c-1]==`function`;if(!l&&!t())throw Error(`Callback required as last argument`);if(l){if(c<2)throw Error(`Too few arguments provided`);c===2?(o=i,i=r,r=a=void 0):c===3&&(r.getContext&&o===void 0?(o=a,a=void 0):(o=a,a=i,i=r,r=void 0))}else{if(c<1)throw Error(`Too few arguments provided`);return c===1?(i=r,r=a=void 0):c===2&&!r.getContext&&(a=i,i=r,r=void 0),new Promise(function(t,o){try{t(e(n.create(i,a),r,a))}catch(e){o(e)}})}try{let t=n.create(i,a);o(null,e(t,r,a))}catch(e){o(e)}}e.create=n.create,e.toCanvas=a.bind(null,r.render),e.toDataURL=a.bind(null,r.renderToDataURL),e.toString=a.bind(null,function(e,t,n){return i.render(e,n)})}))(),1),q=window.__UNI_HALO_APP_SHOWCASE_WIDGET__,nt=`uh-asw-closed`,J=`uh-asw-state`;function rt(){return q?[q.position,q.offsetX,q.offsetY,q.panelWidth].join(`|`):``}function it(){try{let e=sessionStorage.getItem(J);if(!e)return null;let t=JSON.parse(e);return t.fp===rt()?t:(sessionStorage.removeItem(J),null)}catch{return null}}function at(e){try{e.fp=rt(),sessionStorage.setItem(J,JSON.stringify(e))}catch{}}var Y=`/apis/api.unihalo.ialley.cn/v1alpha1`,ot=Y+`/captcha/generate`,st=Y+`/mini-program-links/-/submissions`,ct=Y+`/getConfigs`;function lt(e){let t=window.location.pathname;if(!e||!e.trim())return t===`/`||t===``;let n=e.split(`
`).map(e=>e.trim()).filter(Boolean);return n.length===1&&n[0]===`/`?t===`/`||t===``:n.some(e=>{let n=e.replace(/[.+?^${}()|[\]\\]/g,`\\$&`).replace(/\*\*/g,`{{DOUBLE}}`).replace(/\*/g,`[^/]*`).replace(/\{\{DOUBLE\}\}/g,`.*`);try{return RegExp(`^`+n+`$`).test(t)}catch{return!1}})}function ut(){let e=q?.pageScope||`all`;if(e===`all`)return!0;let t=lt(q?.pagePatterns);return e===`only`?t:!t}var dt=h`
  :host {
    display: block;
  }

  /* ===== 展开面板 ===== */
  .uh-asw {
    position: fixed;
    z-index: 9999;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    width: 340px;
    max-width: calc(100vw - 24px);
    max-height: min(530px, calc(100vh - 48px));
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC",
      "Microsoft YaHei", sans-serif;
    background: rgba(255, 255, 255, 1);
    border: 2px solid rgba(255, 255, 255, 0.65);
    border-radius: 14px;
    box-shadow: 0 16px 60px rgba(0, 0, 0, 0.1);
    user-select: none;
    -webkit-user-select: none;
    line-height: 1.4;
  }
  .uh-asw-minimized {
    display: none !important;
  }
  /* 拖拽进行中：关闭过渡，指针移动直接跟手 */
  .uh-asw.uh-asw-dragging {
    transition: none !important;
  }
  /* 关闭动画：淡出后由 JS remove */
  .uh-asw.uh-asw-closing {
    opacity: 0;
    transition: opacity 0.2s ease;
  }

  /* ===== 9 向锚点（配合 JS inline transform 偏移） ===== */
  .uh-asw.uh-asw-pos-top-left { top: 8px; left: 8px; }
  .uh-asw.uh-asw-pos-top-center { top: 8px; left: 50%; }
  .uh-asw.uh-asw-pos-top-right { top: 8px; right: 8px; }
  .uh-asw.uh-asw-pos-right-center { top: 50%; right: 8px; }
  .uh-asw.uh-asw-pos-bottom-right { bottom: 8px; right: 8px; }
  .uh-asw.uh-asw-pos-bottom-center { bottom: 8px; left: 50%; }
  .uh-asw.uh-asw-pos-bottom-left { bottom: 8px; left: 8px; }
  .uh-asw.uh-asw-pos-left-center { top: 50%; left: 8px; }
  .uh-asw.uh-asw-pos-center { top: 50%; left: 50%; }

  /* ===== 顶部标题栏（拖拽手柄） ===== */
  .uh-asw-header {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 14px 10px;
    cursor: default;
  }
  .uh-asw-draggable .uh-asw-header {
    cursor: grab;
  }
  .uh-asw-dragging .uh-asw-header {
    cursor: grabbing;
  }
  .uh-asw-header-icon {
    width: 16px;
    height: 16px;
    flex: none;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .uh-asw-header-icon img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
  .uh-asw-title {
    flex: 1;
    font-size: 15px;
    font-weight: 600;
    color: #1a1a1a;
  }
  /* 右上角操作按钮（最小化/关闭统一定位，任一隐藏不位移） */
  .uh-asw-topbar {
    display: flex;
    gap: 4px;
  }
  .uh-asw-topbar-btn {
    width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
    border-radius: 6px;
    border: 1px solid rgba(255, 255, 255, 0.5);
    background: rgba(255, 255, 255, 0.75);
    box-shadow: 0 0 12px rgba(0, 0, 0, 0.075);
    color: #999999;
    font-size: 14px;
    line-height: 20px;
    text-align: center;
    cursor: pointer;
    padding: 0;
  }
  .uh-asw-topbar-btn:hover {
    color: #333;
  }

  /* ===== 分段器（shadcn Tabs 风格） ===== */
  .uh-asw-body {
    flex: 1;
    min-height: 0;
    display: flex;
    flex-direction: column;
    padding: 0 12px 12px;
  }
  /* 列表滚动区：与页签（uh-asw-segmented）同级，页签常驻、仅列表滚动 */
  .uh-asw-content {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    scrollbar-width: thin;
    scrollbar-color: #8b8b8b transparent;
  }
  .uh-asw-content::-webkit-scrollbar {
    width: 6px;
  }
  .uh-asw-content::-webkit-scrollbar-track {
    background: transparent;
  }
  .uh-asw-content::-webkit-scrollbar-thumb {
    background: #8b8b8b;
    border-radius: 3px;
  }
  .uh-asw-segmented {
    display: flex;
    gap: 4px;
    padding: 4px;
    background: rgba(0, 0, 0, 0.05);
    border-radius: 8px;
    margin-bottom: 10px;
  }
  .uh-asw-seg-item {
    flex: 1;
    box-sizing: border-box;
    padding: 6px 0;
    border: none;
    border-radius: 8px;
    background: transparent;
    color: #666666;
    font-size: 13px;
    cursor: pointer;
    font-family: inherit;
  }
  .uh-asw-seg-active {
    background: #1A1B1D;
    color: #ffffff;
    font-weight: 600;
    box-shadow: 0 1px 2px rgba(14, 23, 49, 0.35);
  }

  /* ===== 条目列表（info-card 灰底卡） ===== */
  .uh-asw-group-name {
    font-size: 11px;
    color: rgba(0, 0, 0, 0.45);
    margin: 6px 2px 4px;
  }
  .uh-asw-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    border-radius: 10px;
    background: rgba(0, 0, 0, 0.03);
    cursor: pointer;
    margin-bottom: 6px;
    transition: background 0.15s ease;
  }
  .uh-asw-item:hover {
    background: rgba(0, 0, 0, 0.06);
  }
  .uh-asw-item-icon {
    width: 40px;
    height: 40px;
    border-radius: 7px;
    flex: none;
    overflow: hidden;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #ffffff;
    font-size: 15px;
    font-weight: 600;
    background: #1A1B1D;
  }
  .uh-asw-item-icon.app {
    background: #c6f921;
    color: #1A1B1D;
  }
  .uh-asw-item-icon.other {
    background: #92cf57;
    color: #1A1B1D;
  }
  .uh-asw-item-icon img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  .uh-asw-item-info {
    flex: 1;
    min-width: 0;
  }
  .uh-asw-item-name {
    font-size: 13px;
    font-weight: 600;
    color: #1a1a1d;
    display: flex;
    align-items: center;
    gap: 6px;
  }
  .uh-asw-tag {
    flex: none;
    font-size: 10px;
    font-weight: 400;
    padding: 1px 6px;
    border-radius: 4px;
    background: #c6f91f;
    color: #1f2a05;
  }
  /* 详情弹窗标题后置的类型标签间距（列表内标签位于名称前，间距由 item-name 的 gap 提供） */
  .uh-asw-modal-title .uh-asw-tag {
    margin-left: 6px;
  }
  .uh-asw-item-desc {
    font-size: 11px;
    color: rgba(0, 0, 0, 0.45);
    margin-top: 2px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .uh-asw-item-thumb {
    width: 34px;
    height: 34px;
    flex: none;
    border-radius: 8px;
    border: 1px solid rgba(0, 0, 0, 0.08);
    background: #ffffff;
    padding: 2px;
    box-sizing: border-box;
  }
  .uh-asw-item-thumb img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
  .uh-asw-empty {
    padding: 20px 0;
    text-align: center;
    font-size: 13px;
    color: #999999;
  }

  /* ===== 底部操作区（申请入口开关控制显隐） ===== */
  .uh-asw-actions {
    flex-shrink: 0;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    border-top: 1px solid rgba(0, 0, 0, 0.06);
    padding: 10px 14px 12px;
  }
  .uh-asw-hint {
    flex-basis: 100%;
    text-align: center;
    font-size: 10px;
    line-height: 1;
    color: rgba(0, 0, 0, 0.45);
  }

  /* ===== 按钮（对齐 uh-fpw-btn） ===== */
  .uh-asw-btn {
    flex: 1;
    box-sizing: border-box;
    border: 1px solid rgba(0, 0, 0, 0.05);
    border-radius: 6px;
    background: #ffffff;
    color: #1A1B1D;
    font-size: 12px;
    line-height: 1;
    padding: 8px 0;
    cursor: pointer;
    text-align: center;
    font-family: inherit;
    box-shadow: 0 0 12px rgba(0, 0, 0, 0.05);
    transition: background 0.15s ease;
  }
  .uh-asw-btn:hover {
    background: #f1f5f9;
  }
  .uh-asw-btn-primary {
    background: #1A1B1D;
    border-color: transparent;
    color: #ffffff;
  }
  .uh-asw-btn-primary:hover {
    background: #1a1b1de3;
  }

  /* ===== 最小化小球（独立 fixed 元素，无条件可拖） ===== */
  .uh-asw-mini-dot {
    display: flex;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
    width: 40px;
    height: 40px;
    padding: 0;
    border: 2px solid rgba(255, 255, 255, 1);
    border-radius: 50%;
    overflow: hidden;
    position: fixed;
    z-index: 2147482999; /* 兜底层级，实际以内联 z-index（后台 zIndex 配置）为准 */
    cursor: pointer;
    background: rgba(255, 255, 255, 0.9);
    -webkit-backdrop-filter: blur(2px);
    backdrop-filter: blur(2px);
    box-shadow: 0 0 16px rgba(0, 0, 0, 0.25);
    font-family: inherit;
  }
  .uh-asw-mini-dot img {
    display: block;
    width: 60%;
    height: 60%;
    object-fit: contain;
  }
  /* 小球默认态复用面板的 9 向锚点（不依赖 JS 测量，规避主题初始化期间 body 隐藏） */
  .uh-asw-mini-dot.uh-asw-pos-top-left { top: 8px; left: 8px; }
  .uh-asw-mini-dot.uh-asw-pos-top-center { top: 8px; left: 50%; }
  .uh-asw-mini-dot.uh-asw-pos-top-right { top: 8px; right: 8px; }
  .uh-asw-mini-dot.uh-asw-pos-right-center { top: 50%; right: 8px; }
  .uh-asw-mini-dot.uh-asw-pos-bottom-right { bottom: 8px; right: 8px; }
  .uh-asw-mini-dot.uh-asw-pos-bottom-center { bottom: 8px; left: 50%; }
  .uh-asw-mini-dot.uh-asw-pos-bottom-left { bottom: 8px; left: 8px; }
  .uh-asw-mini-dot.uh-asw-pos-left-center { top: 50%; left: 8px; }
  .uh-asw-mini-dot.uh-asw-pos-center { top: 50%; left: 50%; }
  .uh-asw-mini-plus {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    font-weight: 600;
    color: #ffffff;
    background: rgba(0, 0, 0, 0.45);
    opacity: 0;
    transition: opacity 0.15s ease;
  }
  .uh-asw-mini-dot:hover .uh-asw-mini-plus {
    opacity: 1;
  }

  /* ===== 弹窗（遮罩 + 居中卡片，渲染于 shadow DOM 内） ===== */
  .uh-asw-overlay {
    position: fixed;
    inset: 0;
    z-index: 2147483000;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.45);
    -webkit-backdrop-filter: blur(4px);
    backdrop-filter: blur(4px);
    padding: 16px;
    box-sizing: border-box;
  }
  .uh-asw-modal {
    box-sizing: border-box;
    width: 400px;
    max-height: 80vh;
    display: flex;
    flex-direction: column;
    background: rgba(255, 255, 255, 0.98);
    -webkit-backdrop-filter: blur(16px);
    backdrop-filter: blur(16px);
    border: 1px solid rgba(255, 255, 255, 0.6);
    border-radius: 14px;
    box-shadow: 0 16px 60px rgba(0, 0, 0, 0.18);
    overflow: hidden;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC",
      "Microsoft YaHei", sans-serif;
  }
  .uh-asw-modal-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 14px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  }
  .uh-asw-modal-title {
    font-size: 15px;
    font-weight: 600;
    color: #1a1a1a;
  }
  .uh-asw-modal-close {
    width: 20px;
    height: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
    border-radius: 6px;
    border: 1px solid rgba(255, 255, 255, 0.5);
    background: rgba(255, 255, 255, 0.75);
    box-shadow: 0 0 12px rgba(0, 0, 0, 0.075);
    font-size: 14px;
    color: #999999;
    cursor: pointer;
    padding: 0;
  }
  .uh-asw-modal-close:hover {
    color: #333333;
  }
  .uh-asw-modal-body {
    padding: 14px;
    overflow-y: auto;
    scrollbar-width: thin;
    scrollbar-color: #8b8b8b transparent;
  }

  /* ===== 条目详情弹窗 ===== */
  .uh-asw-detail-head {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 14px;
  }
  .uh-asw-detail-head .uh-asw-item-icon {
    width: 40px;
    height: 40px;
  }
  .uh-asw-detail-name {
    font-size: 14px;
    font-weight: 600;
    color: #1a1a1d;
  }
  .uh-asw-detail-desc {
    font-size: 11px;
    color: rgba(0, 0, 0, 0.45);
    margin-top: 2px;
  }
  .uh-asw-qr-card {
    width: 360px;
    height: 360px;
    max-width: calc(100vw - 48px);
    max-height: calc(100vw - 48px);
    margin: 0 auto;
    border: 1px solid rgba(0, 0, 0, 0.08);
    border-radius: 10px;
    background: #ffffff;
    padding: 10px;
    box-sizing: border-box;
  }
  .uh-asw-qr-card img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
  .uh-asw-qr-tip {
    text-align: center;
    font-size: 11px;
    color: rgba(0, 0, 0, 0.45);
    margin: 10px 0 12px;
  }
  .uh-asw-link-row {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 12px;
  }
  .uh-asw-link-row .uh-asw-btn {
    flex: none;
    width: 60px;
  }
  .uh-asw-detail-actions {
    display: flex;
    gap: 8px;
  }

  /* ===== 申请表单 ===== */
  .uh-asw-form {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
  .uh-asw-field {
    display: flex;
    flex-direction: column;
    gap: 4px;
    font-size: 12px;
    color: #666666;
  }
  .uh-asw-apply-panel .uh-asw-field {
    margin-bottom: 10px;
  }
  .uh-asw-apply-panel .uh-asw-field:last-child {
    margin-bottom: 0;
  }
  .uh-asw-field input[type="text"] {
    box-sizing: border-box;
    width: 100%;
    height: 32px;
    padding: 0 10px;
    border: 1px solid rgba(0, 0, 0, 0.12);
    border-radius: 8px;
    font-size: 13px;
    color: #1a1a1a;
    outline: none;
    font-family: inherit;
    background: #ffffff;
  }
  .uh-asw-field input[type="text"]:focus,
  .uh-asw-field textarea:focus {
    border-color: #c6f921;
  }
  .uh-asw-field textarea {
    box-sizing: border-box;
    width: 100%;
    border: 1px solid rgba(0, 0, 0, 0.12);
    border-radius: 8px;
    font-size: 13px;
    color: #1a1a1a;
    outline: none;
    font-family: inherit;
    background: #ffffff;
    padding: 6px 10px;
    resize: vertical;
  }
  .uh-asw-captcha-input {
    display: flex;
    gap: 8px;
  }
  .uh-asw-captcha-input input {
    flex: 1;
  }
  .uh-asw-captcha-img {
    width: 100px;
    height: 32px;
    border-radius: 8px;
    border: 1px solid rgba(0, 0, 0, 0.1);
    cursor: pointer;
    object-fit: cover;
  }
  .uh-asw-form-actions {
    display: flex;
    gap: 8px;
    margin-top: 12px;
  }
  /* 预览图动态行 */
  .uh-asw-shot-row {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: 6px;
  }
  .uh-asw-shot-input {
    flex: 1;
    min-width: 0;
    box-sizing: border-box;
    height: 32px;
    padding: 0 10px;
    border: 1px solid rgba(0, 0, 0, 0.12);
    border-radius: 8px;
    font-size: 13px;
    color: #1a1a1a;
    outline: none;
    font-family: inherit;
    background: #ffffff;
  }
  .uh-asw-shot-input:focus {
    border-color: #c6f921;
  }
  .uh-asw-shot-remove {
    flex-shrink: 0;
    width: 28px;
    height: 28px;
    border: none;
    border-radius: 6px;
    background: rgba(0, 0, 0, 0.05);
    color: #999999;
    font-size: 16px;
    line-height: 1;
    cursor: pointer;
    padding: 0;
  }
  .uh-asw-shot-remove:hover {
    background: rgba(0, 0, 0, 0.1);
    color: #333333;
  }

  /* ===== 申请弹窗：分段器 + 面板 + 底部固定操作区 ===== */
  .uh-asw-segmented-modal {
    display: flex;
    gap: 4px;
    margin: 8px 14px 0;
    padding: 4px;
    background: rgba(0, 0, 0, 0.05);
    border-radius: 8px;
  }
  .uh-asw-apply-body {
    display: flex;
    flex-direction: column;
    padding: 10px 14px 14px;
    overflow: hidden;
  }
  .uh-asw-apply-panels {
    flex: 1;
    min-height: 0;
    max-height: 40vh;
    overflow-y: auto;
    scrollbar-width: thin;
    scrollbar-color: #8b8b8b transparent;
  }
  .uh-asw-apply-footer {
    flex-shrink: 0;
    border-top: 1px solid rgba(0, 0, 0, 0.06);
    padding-top: 10px;
    margin-top: 10px;
  }

  /* ===== 友链信息（小程序信息 + 博主信息，输入框行 + 复制） ===== */
  .uh-asw-info-card {
    display: flex;
    flex-direction: column;
    gap: 6px;
    padding: 12px;
    border-radius: 10px;
    background: rgba(0, 0, 0, 0.03);
    margin-bottom: 10px;
  }
  .uh-asw-info-card-title {
    font-size: 13px;
    font-weight: 600;
    color: #1a1a1a;
    margin-bottom: 2px;
  }
  .uh-asw-copy-row {
    display: flex;
    align-items: center;
    gap: 6px;
  }
  .uh-asw-copy-label {
    flex-shrink: 0;
    font-size: 12px;
    color: #000000;
    min-width: 64px;
    text-align: right;
  }
  .uh-asw-copy-input {
    flex: 1;
    min-width: 0;
    box-sizing: border-box;
    height: 28px;
    padding: 0 8px;
    border: 1px solid #ffffff;
    border-radius: 6px;
    font-size: 12px;
    color: #1a1a1a;
    background: rgba(255, 255, 255, 0.75);
    outline: none;
    font-family: inherit;
  }
  .uh-asw-copy-input:focus {
    border-color: #c6f921;
  }
  .uh-asw-copy-textarea {
    height: auto;
    min-height: 40px;
    padding: 5px 8px;
    line-height: 1.4;
    resize: none;
    font-family: inherit;
  }
  .uh-asw-copy-btn {
    flex-shrink: 0;
    flex: none;
    width: 60px;
    padding: 8px 0;
  }
  .uh-asw-copy-all {
    width: 100%;
  }
  .uh-asw-loading {
    padding: 20px 0;
    text-align: center;
    font-size: 13px;
    color: #999999;
  }
`,X=[{key:`displayName`,label:`应用名称`,required:!0},{key:`miniProgramCode`,label:`太阳码（图片地址）`,required:!0},{key:`appId`,label:`小程序 AppID`,required:!0,placeholder:`小程序 AppID，如 wx1234567890abcdef`},{key:`path`,label:`跳转页面路径`,required:!1,placeholder:`如 pages/index/index`},{key:`link`,label:`应用地址`,required:!1,placeholder:`#小程序://xxx`},{key:`description`,label:`应用描述`,required:!1,type:`textarea`},{key:`applyRemark`,label:`申请说明`,required:!1,type:`textarea`}],Z=[{key:`avatar`,label:`作者头像（图片地址）`,required:!1},{key:`authorName`,label:`作者昵称`,required:!1},{key:`website`,label:`作者网站`,required:!1},{key:`email`,label:`邮箱（选填，用于审核结果通知）`,required:!1}],ft=`uh-asw-apply-draft`,Q=new Map;function $(e){return e?/^(https?:)?\/\//.test(e)||/^data:/i.test(e)?e:window.location.origin+(e.startsWith(`/`)?e:`/`+e):``}var pt=class extends U{static{this.styles=[dt]}static{this.properties={tab:{state:!0},minimized:{state:!0},panelStyle:{state:!0},dotStyle:{state:!0},detail:{state:!0},detailQr:{state:!0},applyOpen:{state:!0},applySubmitting:{state:!0},applyTab:{state:!0},captchaId:{state:!0},captchaSrc:{state:!0},screenshotRows:{state:!0},linksOpen:{state:!0},linksLoading:{state:!0},linksError:{state:!0},miniInfo:{state:!0},blogger:{state:!0}}}constructor(){super(),this.panelDrag=null,this.miniDotDrag=null,this.miniDotDragged=!1,this.config=q,this.tab=`all`,this.minimized=!1,this.panelStyle=``,this.dotStyle=``,this.detail=null,this.detailQr=``,this.applyOpen=!1,this.applySubmitting=!1,this.applyTab=`basic`,this.captchaId=``,this.captchaSrc=``,this.screenshotRows=[``],this.linksOpen=!1,this.linksLoading=!1,this.linksError=!1,this.miniInfo=null,this.blogger=null}connectedCallback(){super.connectedCallback(),(!ut()||this.isClosed())&&this.remove()}firstUpdated(){this.applyPosition(),requestAnimationFrame(()=>this.applyDefaultState())}applyDefaultState(){if(!this.panelEl)return;let e=it();if(e){if(e.panelPos&&this.setFree(e.panelPos.left,e.panelPos.top),e.minimized){e.dotPos&&(this.dotStyle=`left:${Math.round(e.dotPos.left)}px;top:${Math.round(e.dotPos.top)}px;`),this.minimized=!0;return}return}this.config.defaultState===`minimized`&&(this.minimized=!0)}persistState(e){at({...it()??{minimized:!1},...e})}isClosed(){if(this.config.rememberClosed===!1)return!1;try{return sessionStorage.getItem(nt)===`1`}catch{return!1}}get panelEl(){return this.renderRoot.querySelector(`.uh-asw`)}get miniDotEl(){return this.renderRoot.querySelector(`.uh-asw-mini-dot`)}applyPosition(){let e=this.panelEl;e&&(e.style.width=(Number(this.config.panelWidth)||340)+`px`,e.style.zIndex=String(this.config.zIndex||9999),e.style.transform=this.anchorTransform(this.config.position||`bottom-right`))}anchorTransform(e){let t=Number(this.config.offsetX)||0,n=Number(this.config.offsetY)||0,r=e===`center`||e===`top-center`||e===`bottom-center`?`calc(-50% + `+t+`px)`:t+`px`,i=e===`center`||e===`left-center`||e===`right-center`?`calc(-50% + `+n+`px)`:n+`px`;return`translate(`+r+`, `+i+`)`}setFree(e,t){let n=this.panelEl;n&&(n.style.left=e+`px`,n.style.top=t+`px`,n.style.right=`auto`,n.style.bottom=`auto`,n.style.transform=`translate(0, 0)`)}clampToViewport(e,t,n){return[Math.max(0,Math.min(e,window.innerWidth-n.offsetWidth)),Math.max(0,Math.min(t,window.innerHeight-n.offsetHeight))]}onHeaderPointerDown(e){if(!this.config.dragEnabled)return;let t=e.target;if(t&&t.closest(`button`))return;let n=this.panelEl;if(!n)return;let r=n.getBoundingClientRect();this.panelDrag={startX:e.clientX,startY:e.clientY,left:r.left,top:r.top},n.classList.add(`uh-asw-dragging`),e.currentTarget.setPointerCapture(e.pointerId),e.preventDefault()}onHeaderPointerMove(e){let t=this.panelDrag,n=this.panelEl;if(!t||!n)return;let[r,i]=this.clampToViewport(t.left+(e.clientX-t.startX),t.top+(e.clientY-t.startY),n);this.setFree(r,i)}onHeaderPointerEnd(){let e=this.panelEl;if(!this.panelDrag||!e)return;this.panelDrag=null,e.classList.remove(`uh-asw-dragging`);let t=e.getBoundingClientRect();this.persistState({minimized:!1,panelPos:{left:Math.round(t.left),top:Math.round(t.top)}})}onMinimizeClick(){let e=this.panelEl;if(!e)return;let t=e.getBoundingClientRect();this.dotStyle=`left:${Math.round(t.left)}px;top:${Math.round(t.top)}px;`,this.minimized=!0,this.persistState({minimized:!0,dotPos:{left:Math.round(t.left),top:Math.round(t.top)},panelPos:{left:Math.round(t.left),top:Math.round(t.top)}})}onRestoreClick(){if(this.miniDotDragged){this.miniDotDragged=!1;return}if(!this.dotStyle){this.minimized=!1,this.persistState({minimized:!1});return}let e=this.miniDotEl,t=this.panelEl;if(e&&t){let n=e.getBoundingClientRect();t.style.left=Math.round(n.left)+`px`,t.style.top=Math.round(n.top)+`px`,t.style.right=`auto`,t.style.bottom=`auto`,t.style.transform=``}this.minimized=!1,this.updateComplete.then(()=>{let e=this.panelEl;if(!e)return;let t=Number.parseInt(e.style.left,10)||0,n=Number.parseInt(e.style.top,10)||0,[r,i]=this.clampToViewport(t,n,e);(r!==t||i!==n)&&this.setFree(r,i),this.persistState({minimized:!1,panelPos:{left:r,top:i}})})}onMiniDotPointerDown(e){let t=e.currentTarget,n=t.getBoundingClientRect();this.miniDotDrag={startX:e.clientX,startY:e.clientY,left:n.left,top:n.top,moved:!1},t.setPointerCapture(e.pointerId)}onMiniDotPointerMove(e){let t=this.miniDotDrag;if(!t)return;let n=e.clientX-t.startX,r=e.clientY-t.startY;if(!t.moved&&(Math.abs(n)>3||Math.abs(r)>3)&&(t.moved=!0),!t.moved)return;let i=e.currentTarget,[a,o]=this.clampToViewport(t.left+n,t.top+r,i);i.style.left=a+`px`,i.style.top=o+`px`}onMiniDotPointerEnd(){if(this.miniDotDrag?.moved){this.miniDotDragged=!0;let e=this.miniDotEl;e&&e.style.left&&e.style.top&&this.persistState({dotPos:{left:Number.parseFloat(e.style.left),top:Number.parseFloat(e.style.top)}})}this.miniDotDrag=null}onCloseClick(){if(this.config.rememberClosed!==!1)try{sessionStorage.setItem(nt,`1`)}catch{}try{sessionStorage.removeItem(J)}catch{}let e=this.panelEl;e?(e.classList.add(`uh-asw-closing`),setTimeout(()=>this.remove(),200)):this.remove()}get entries(){let e=this.config,t=(e.miniProgramItems||[]).filter(e=>e.displayName?.trim()).map(e=>({type:`miniprogram`,typeLabel:`小程序`,displayName:e.displayName?.trim()||``,group:e.group?.trim()||``,icon:$(e.icon),codeImage:$(e.codeImage),appId:e.appId?.trim()||``,path:e.path?.trim()||``,link:``,description:e.description?.trim()||``,priority:Number(e.priority)||0})),n=(e.appItems||[]).filter(e=>e.displayName?.trim()).map(e=>({type:`app`,typeLabel:`App`,displayName:e.displayName?.trim()||``,group:e.group?.trim()||``,icon:$(e.icon),codeImage:$(e.codeImage),appId:``,path:``,link:e.link?.trim()||``,description:e.description?.trim()||``,priority:Number(e.priority)||0})),r=(e.otherItems||[]).filter(e=>e.displayName?.trim()).map(e=>({type:`other`,typeLabel:e.typeName?.trim()||`其他`,displayName:e.displayName?.trim()||``,group:e.group?.trim()||``,icon:$(e.icon),codeImage:$(e.codeImage),appId:``,path:``,link:e.link?.trim()||``,description:e.description?.trim()||``,priority:Number(e.priority)||0}));return[...t,...n,...r].sort((e,t)=>t.priority-e.priority)}get visibleEntries(){return this.tab===`all`?this.entries:this.entries.filter(e=>e.type===this.tab)}async openDetail(e){if(this.detail=e,this.detailQr=``,(e.type===`app`||e.type===`other`)&&!e.codeImage&&e.link){let t=Q.get(e.link);if(t){this.detailQr=t;return}try{let t=await tt.toDataURL(e.link,{margin:1,width:512});Q.set(e.link,t),this.detail===e&&(this.detailQr=t)}catch{}}}closeDetail(){this.detail=null,this.detailQr=``}onOverlayClick(e){e.target.classList.contains(`uh-asw-overlay`)&&this.closeModals()}closeModals(){this.applyOpen=!1,this.linksOpen=!1,this.closeDetail()}openApply(){this.applyOpen=!0,this.refreshCaptcha(),this.updateComplete.then(()=>this.restoreDraft())}restoreDraft(){let e=this.renderRoot.querySelector(`.uh-asw-form`);if(!e)return;let t=this.loadDraft();[...X,...Z].forEach(n=>{let r=e.elements.namedItem(n.key);r&&t[n.key]&&(r.value=String(t[n.key]))});let n=t.screenshots;Array.isArray(n)?this.screenshotRows=n.length?n.slice():[``]:typeof n==`string`&&n.trim()&&(this.screenshotRows=n.split(`
`).map(e=>e.trim()).filter(Boolean),this.screenshotRows.length||(this.screenshotRows=[``]))}loadDraft(){try{let e=localStorage.getItem(ft);return e?JSON.parse(e):{}}catch{return{}}}saveDraft(){let e=this.renderRoot.querySelector(`.uh-asw-form`);if(!e)return;let t={};[...X,...Z].forEach(n=>{let r=e.elements.namedItem(n.key);t[n.key]=r?.value||``}),t.screenshots=this.screenshotRows.filter(e=>e.trim());try{localStorage.setItem(ft,JSON.stringify(t))}catch{}}clearDraft(){try{localStorage.removeItem(ft)}catch{}}resetApply(){let e=this.renderRoot.querySelector(`.uh-asw-form`);e&&e.reset(),this.screenshotRows=[``],this.applyTab=`basic`,this.clearDraft()}onFormInput(){this.saveDraft()}refreshCaptcha(){fetch(ot).then(e=>e.json()).then(e=>{e&&e.imageBase64&&this.setCaptcha(e)}).catch(()=>{})}setCaptcha(e){this.captchaId=e.id,this.captchaSrc=e.imageBase64}async onApplySubmit(e){e.preventDefault();let t=e.target;for(let e of[{key:`displayName`,message:`请填写应用名称`,panel:`basic`},{key:`miniProgramCode`,message:`请填写太阳码图片地址`,panel:`basic`},{key:`captchaCode`,message:`请输入验证码`}]){let n=t.elements.namedItem(e.key);if(!n||!n.value.trim()){e.panel&&(this.applyTab=e.panel),alert(e.message);return}}let n={};[...X,...Z].forEach(e=>{let r=t.elements.namedItem(e.key);r&&r.value.trim()&&(n[e.key]=r.value.trim())});let r=this.screenshotRows.map(e=>e.trim()).filter(Boolean).map($);r.length&&(n.screenshots=r);let i=(t.elements.namedItem(`captchaCode`)?.value||``).trim(),a=`?captchaId=`+encodeURIComponent(this.captchaId||``)+`&captchaCode=`+encodeURIComponent(i);this.applySubmitting=!0;try{let e=await fetch(st+a,{method:`POST`,headers:{"Content-Type":`application/json`},body:JSON.stringify({spec:n})}),r=await e.json().catch(()=>({}));if(e.status===200||e.status===201){this.applyOpen=!1,this.clearDraft(),t.reset(),this.screenshotRows=[``],this.applyTab=`basic`,alert(`申请提交成功，请等待审核`);return}e.status===403&&r.captcha&&this.setCaptcha(r.captcha),alert(r.message||`提交失败，请重试`)}catch{alert(`网络异常，请稍后重试`)}finally{this.applySubmitting=!1}}openLinks(){this.linksOpen=!0,this.linksLoading=!0,this.linksError=!1,this.miniInfo=null,this.blogger=null,fetch(ct).then(e=>e.json()).then(e=>{let t=e?.featureConfig;this.miniInfo=t?.linkInfo?.miniInfo||null,this.blogger=t?.profile?.blogger||null}).catch(()=>{this.linksError=!0}).finally(()=>{this.linksLoading=!1})}render(){let e=this.config,t=$(e.entryIcon),n=`z-index:`+(Number(e.zIndex)||9999)+`;`,r=this.dotStyle?`uh-asw-mini-dot`:`uh-asw-mini-dot uh-asw-pos-${e.position||`bottom-right`}`,i=(this.dotStyle||this.anchorTransform(e.position||`bottom-right`))+n;return P`
      ${this.applyOpen?this.renderApplyModal():``}
      ${this.linksOpen?this.renderLinksModal():``}
      ${this.detail?this.renderDetailModal(this.detail):``}
      <div
        class="uh-asw uh-asw-pos-${e.position||`bottom-right`} ${e.dragEnabled?`uh-asw-draggable`:``} ${this.minimized?`uh-asw-minimized`:``}"
        style=${this.panelStyle}
      >
        <div
          class="uh-asw-header"
          @pointerdown=${this.onHeaderPointerDown}
          @pointermove=${this.onHeaderPointerMove}
          @pointerup=${this.onHeaderPointerEnd}
          @pointercancel=${this.onHeaderPointerEnd}
        >
          ${t?P`<span class="uh-asw-header-icon"><img src=${t} alt="" /></span>`:``}
          <span class="uh-asw-title">应用展示</span>
          <span class="uh-asw-topbar">
            <button type="button" class="uh-asw-topbar-btn" aria-label="最小化" @click=${this.onMinimizeClick}>&minus;</button>
            ${e.closeEnabled===!1?``:P`<button type="button" class="uh-asw-topbar-btn" aria-label="关闭悬浮面板" @click=${this.onCloseClick}>&times;</button>`}
          </span>
        </div>
        <div class="uh-asw-body">
          <div class="uh-asw-segmented">
            <button
              type="button"
              class="uh-asw-seg-item ${this.tab===`all`?`uh-asw-seg-active`:``}"
              @click=${()=>this.tab=`all`}
            >全部</button>
            <button
              type="button"
              class="uh-asw-seg-item ${this.tab===`miniprogram`?`uh-asw-seg-active`:``}"
              @click=${()=>this.tab=`miniprogram`}
            >小程序</button>
            <button
              type="button"
              class="uh-asw-seg-item ${this.tab===`app`?`uh-asw-seg-active`:``}"
              @click=${()=>this.tab=`app`}
            >APP</button>
            <button
              type="button"
              class="uh-asw-seg-item ${this.tab===`other`?`uh-asw-seg-active`:``}"
              @click=${()=>this.tab=`other`}
            >其他</button>
          </div>
          <div class="uh-asw-content">
            ${this.renderEntries()}
          </div>
        </div>
        ${e.applyEntryEnabled?P`
              <div class="uh-asw-actions">
                <button type="button" class="uh-asw-btn" @click=${this.openApply}>我要申请</button>
                <button type="button" class="uh-asw-btn" @click=${this.openLinks}>友链信息</button>
                <div class="uh-asw-hint">小程序申请和友链信息</div>
              </div>`:``}
      </div>
      ${this.minimized?P`
            <button
              type="button"
              class=${r}
              style=${i}
              @click=${this.onRestoreClick}
              @pointerdown=${this.onMiniDotPointerDown}
              @pointermove=${this.onMiniDotPointerMove}
              @pointerup=${this.onMiniDotPointerEnd}
              @pointercancel=${this.onMiniDotPointerEnd}
              aria-label="恢复应用展示面板"
            >
              ${t?P`<img src=${t} alt="" />`:``}
              <span class="uh-asw-mini-plus">+</span>
            </button>`:``}
    `}renderEntries(){let e=this.visibleEntries;if(!e.length)return P`<div class="uh-asw-empty">暂无内容</div>`;let t=[];return e.forEach(e=>{let n=e.group||`未分组`,r=t.find(e=>e.name===n);r||(r={name:n,items:[]},t.push(r)),r.items.push(e)}),t.map(e=>P`
        <div class="uh-asw-group-name">${e.name}</div>
        ${e.items.map(e=>this.renderEntry(e))}
      `)}renderEntry(e){let t=e.displayName.slice(0,1),n=e.codeImage||((e.type===`app`||e.type===`other`)&&e.link?Q.get(e.link):``);return P`
      <div class="uh-asw-item" @click=${()=>this.openDetail(e)}>
        <div class="uh-asw-item-icon ${e.type}">${e.icon?P`<img src=${e.icon} alt="" />`:t}</div>
        <div class="uh-asw-item-info">
          <div class="uh-asw-item-name"><span class="uh-asw-tag">${e.typeLabel}</span>${e.displayName}</div>
          <div class="uh-asw-item-desc">${e.description||`该应用暂无描述`}</div>
        </div>
        ${n?P`<div class="uh-asw-item-thumb"><img src=${n} alt="" /></div>`:``}
      </div>
    `}renderDetailModal(e){let t=e.type===`miniprogram`,n=t?e.codeImage:e.codeImage||this.detailQr,r=e.displayName.slice(0,1);return P`
      <div class="uh-asw-overlay" @click=${this.onOverlayClick}>
        <div class="uh-asw-modal">
          <div class="uh-asw-modal-header">
            <div class="uh-asw-modal-title">${t?`小程序详情`:`应用详情`}<span class="uh-asw-tag">${e.typeLabel}</span></div>
            <button type="button" class="uh-asw-modal-close" aria-label="关闭" @click=${this.closeDetail}>&times;</button>
          </div>
          <div class="uh-asw-modal-body">
            <div class="uh-asw-detail-head">
              <div class="uh-asw-item-icon ${e.type}">${e.icon?P`<img src=${e.icon} alt="" />`:r}</div>
              <div>
                <div class="uh-asw-detail-name">${e.displayName}</div>
                <div class="uh-asw-detail-desc">${e.description||e.typeLabel}</div>
              </div>
            </div>
            ${n?P`<div class="uh-asw-qr-card"><img src=${n} alt=${t?`小程序太阳码`:`二维码`} /></div>`:e.link?P`<div class="uh-asw-qr-card"></div>`:``}
            <div class="uh-asw-qr-tip">
              ${t?`微信扫码打开小程序`:e.link?`手机扫码直接打开链接`:`该条目未配置链接，仅作展示`}
            </div>
            ${!t&&e.link?P`
                  <div class="uh-asw-link-row">
                    <input class="uh-asw-copy-input" type="text" readonly value=${e.link} @click=${e=>e.target.select()} />
                    <button type="button" class="uh-asw-btn" @click=${t=>this.copyText(e.link,t.target)}>复制</button>
                  </div>
                  <div class="uh-asw-detail-actions">
                    <button type="button" class="uh-asw-btn uh-asw-btn-primary" @click=${()=>window.open(e.link,`_blank`,`noopener`)}>立即打开</button>
                    <button type="button" class="uh-asw-btn" @click=${this.closeDetail}>关闭</button>
                  </div>`:P`
                  <div class="uh-asw-detail-actions">
                    <button type="button" class="uh-asw-btn" @click=${this.closeDetail}>关闭</button>
                  </div>`}
          </div>
        </div>
      </div>
    `}renderApplyField(e){let t=P`<span>${e.label}${e.required?` *`:``}</span>`;return e.type===`textarea`?P`
        <label class="uh-asw-field">
          ${t}
          <textarea name=${e.key} rows="2" placeholder=${e.placeholder??``}></textarea>
        </label>`:P`
      <label class="uh-asw-field">
        ${t}
        <input type="text" name=${e.key} ?required=${e.required} placeholder=${e.placeholder??``} />
      </label>`}renderApplyModal(){return P`
      <div class="uh-asw-overlay" @click=${this.onOverlayClick}>
        <div class="uh-asw-modal">
          <div class="uh-asw-modal-header">
            <div class="uh-asw-modal-title">小程序申请</div>
            <button type="button" class="uh-asw-modal-close" aria-label="关闭" @click=${this.closeModals}>&times;</button>
          </div>
          <div class="uh-asw-segmented-modal">
            <button
              type="button"
              class="uh-asw-seg-item ${this.applyTab===`basic`?`uh-asw-seg-active`:``}"
              @click=${()=>this.applyTab=`basic`}
            >基础信息</button>
            <button
              type="button"
              class="uh-asw-seg-item ${this.applyTab===`author`?`uh-asw-seg-active`:``}"
              @click=${()=>this.applyTab=`author`}
            >作者信息</button>
          </div>
          <div class="uh-asw-modal-body uh-asw-apply-body">
            <!-- novalidate：隐藏面板的 required 不参与原生校验，由提交时手动校验 -->
            <form class="uh-asw-form" novalidate @submit=${this.onApplySubmit} @input=${this.onFormInput}>
              <div class="uh-asw-apply-panels">
                <div class="uh-asw-apply-panel" ?hidden=${this.applyTab!==`basic`}>
                  ${X.map(e=>this.renderApplyField(e))}
                  ${this.renderScreenshotRows()}
                </div>
                <div class="uh-asw-apply-panel" ?hidden=${this.applyTab!==`author`}>
                  ${Z.map(e=>this.renderApplyField(e))}
                </div>
              </div>
              <div class="uh-asw-apply-footer">
                <label class="uh-asw-field uh-asw-captcha-row">
                  <span>验证码 *</span>
                  <span class="uh-asw-captcha-input">
                    <input type="text" name="captchaCode" required autocomplete="off" />
                    <img
                      class="uh-asw-captcha-img"
                      alt="验证码"
                      title="看不清？点击刷新"
                      src=${this.captchaSrc}
                      @click=${this.refreshCaptcha}
                    />
                  </span>
                </label>
                <div class="uh-asw-form-actions">
                  <button type="button" class="uh-asw-btn" @click=${this.resetApply}>重置</button>
                  <button type="button" class="uh-asw-btn" @click=${this.closeModals}>取消</button>
                  <button type="submit" class="uh-asw-btn uh-asw-btn-primary" ?disabled=${this.applySubmitting}>
                    ${this.applySubmitting?`提交中…`:`提交申请`}
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      </div>
    `}renderScreenshotRows(){return P`
      <div class="uh-asw-field">
        <span>预览截图(可选)</span>
        ${this.screenshotRows.map((e,t)=>P`
            <div class="uh-asw-shot-row">
              <input
                class="uh-asw-shot-input"
                type="text"
                name="screenshots"
                placeholder="https://…/image.png"
                value=${e}
                @input=${e=>this.updateScreenshotRow(t,e.target.value)}
              />
              <button
                type="button"
                class="uh-asw-shot-remove"
                aria-label="删除该预览图"
                @click=${()=>this.removeScreenshotRow(t)}
              >&times;</button>
            </div>`)}
        <button type="button" class="uh-asw-btn uh-asw-shot-add" @click=${this.addScreenshotRow}>
          + 添加一张预览图
        </button>
      </div>`}updateScreenshotRow(e,t){let n=this.screenshotRows.slice();n[e]=t,this.screenshotRows=n}addScreenshotRow(){this.screenshotRows=[...this.screenshotRows,``],this.updateComplete.then(()=>{let e=this.renderRoot.querySelectorAll(`.uh-asw-shot-row`),t=e[e.length-1];t&&t.scrollIntoView({block:`nearest`,behavior:`smooth`})})}removeScreenshotRow(e){let t=this.screenshotRows.filter((t,n)=>n!==e);this.screenshotRows=t.length?t:[``]}renderLinksModal(){let e=[{label:`小程序名称`,value:this.miniInfo?.displayName},{label:`太阳码地址`,value:$(this.miniInfo?.miniProgramCode)},{label:`小程序 AppID`,value:this.miniInfo?.appId},{label:`跳转页面路径`,value:this.miniInfo?.path},{label:`小程序地址`,value:this.miniInfo?.link},{label:`小程序描述`,value:this.miniInfo?.description,textarea:!0},{label:`申请说明`,value:this.miniInfo?.applyRemark,textarea:!0,copyable:!1}],t=[{label:`博主昵称`,value:this.blogger?.nickname},{label:`博主头像`,value:$(this.blogger?.avatar)},{label:`博主主页`,value:this.blogger?.website},{label:`博主简介`,value:this.blogger?.description}],n=e.some(e=>e.value)||t.some(e=>e.value);return P`
      <div class="uh-asw-overlay" @click=${this.onOverlayClick}>
        <div class="uh-asw-modal">
          <div class="uh-asw-modal-header">
            <div class="uh-asw-modal-title">小程序友链信息</div>
            <button type="button" class="uh-asw-modal-close" aria-label="关闭" @click=${this.closeModals}>&times;</button>
          </div>
          <div class="uh-asw-modal-body">
            ${this.linksLoading?P`<div class="uh-asw-loading">加载中…</div>`:this.linksError?P`<div class="uh-asw-loading">加载失败，请稍后重试</div>`:n?P`
                      <div class="uh-asw-info-card">
                        <div class="uh-asw-info-card-title">小程序信息</div>
                        ${e.map(e=>this.renderCopyRow(e.label,e.value,e))}
                      </div>
                      <div class="uh-asw-info-card">
                        <div class="uh-asw-info-card-title">博主信息</div>
                        ${t.map(e=>this.renderCopyRow(e.label,e.value,e))}
                      </div>
                      <button
                        type="button"
                        class="uh-asw-btn uh-asw-copy-all"
                        @click=${e=>this.copyText(this.collectLinkText(),e.target)}
                      >复制全部</button>
                    `:P`<div class="uh-asw-loading">暂无友链信息</div>`}
          </div>
        </div>
      </div>
    `}renderCopyRow(e,t,n){if(!t)return``;let r=!!n?.textarea,i=n?.copyable??!0;return P`
      <div class="uh-asw-copy-row">
        <span class="uh-asw-copy-label">${e}</span>
        ${r?P`
          <textarea
            class="uh-asw-copy-input uh-asw-copy-textarea"
            readonly
            rows="2"
            @click=${e=>e.target.select()}
          >${t}</textarea>`:P`
          <input
            class="uh-asw-copy-input"
            type="text"
            readonly
            value=${t}
            @click=${e=>e.target.select()}
          />`}
        ${i?P`
              <button
                type="button"
                class="uh-asw-btn uh-asw-copy-btn"
                @click=${e=>this.copyText(t,e.target)}
              >复制</button>`:``}
      </div>`}async copyText(e,t){try{await navigator.clipboard.writeText(e)}catch{let t=document.createElement(`textarea`);t.value=e,t.style.position=`fixed`,t.style.opacity=`0`,document.body.appendChild(t),t.select();try{document.execCommand(`copy`)}catch{}t.remove()}if(t){let e=t.textContent;t.textContent=`已复制`,setTimeout(()=>{t.isConnected&&(t.textContent=e)},800)}}collectLinkText(){let e=[],t=(t,n)=>{n&&e.push(`${t}：${n}`)},n=this.miniInfo;t(`小程序名称`,n?.displayName),t(`太阳码地址`,$(n?.miniProgramCode)),t(`小程序 AppID`,n?.appId),t(`跳转页面路径`,n?.path),t(`小程序地址`,n?.link),t(`描述`,n?.description),t(`申请说明`,n?.applyRemark);let r=this.blogger;return t(`博主昵称`,r?.nickname),t(`博主头像`,$(r?.avatar)),t(`博主主页`,r?.website),t(`博主简介`,r?.description),e.join(`
`)}};customElements.define(`uh-app-showcase-widget`,pt);function mt(){if(document.querySelector(`uh-app-showcase-widget`))return;ht();let e=document.createElement(`uh-app-showcase-widget`);document.body.appendChild(e)}function ht(){let e=q?.version||``,t=`color:#92cf57;`,n=`background:#c6f921;color:#1f2a05;font-weight:600;border-radius:4px;`;console.log(``),console.log(`%c UniHalo %c v${e} `,n+`padding:2px 8px;border-radius:4px 0 0 4px;`,`background:#f4fbe3;color:#55700a;padding:2px 8px;border-radius:0 4px 4px 0;`),console.log(`%c作者：小莫唐尼丨Apache-2.0 License`,`color:#8A6F38;font-size:11px;`),console.log(`%c基于 UniApp 与 Halo 构建的跨平台客户端 + 配置插件，
支持 40+ 功能，优雅、轻量、跨平台，让你的内容触达每一个角落。`,`color:#92cf57;`);let r=e=>e+` `.repeat(31-e.length),i=(e,i)=>{console.log(`%c│%c ${e} %c ${r(i)}%c  │`,t,n,``,t)};console.log(`%c┌${`─`.repeat(20)}┐`,t),i(`官网`,`https://uni-halo.ialley.cn`),i(`文档`,`https://uni-halo-doc.ialley.cn`),i(`作者`,`https://www.xiaoxiaomo.cn`),console.log(`%c└${`─`.repeat(20)}┘`,t),console.log(``)}q&&typeof q==`object`&&(document.body?mt():document.addEventListener(`DOMContentLoaded`,mt))})();