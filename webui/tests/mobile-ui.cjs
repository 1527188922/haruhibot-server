// Run with PLAYWRIGHT_MODULE pointing to an installed playwright package if not on NODE_PATH.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const compiler = require('vue-template-compiler');
const babel = require('@babel/core');
const sass = require('sass');
const root = path.resolve(__dirname, '..');
const read = p => fs.readFileSync(path.join(root, p), 'utf8');
function component(file) {
  const parsed = compiler.parseComponent(read(file));
  const imports = [];
  const code = babel.transformSync(parsed.script.content, { configFile:false, babelrc:false, plugins:[({types:t})=>({visitor:{
    ImportDeclaration(p) { for(const s of p.node.specifiers) imports.push(s.local.name); p.remove(); },
    ExportDefaultDeclaration(p) { p.replaceWith(t.expressionStatement(t.assignmentExpression('=', t.memberExpression(t.identifier('window'),t.identifier('__component')),p.node.declaration))); }
  }})] }).code;
  const dependencies = imports.map(name => 'const '+name+' = window.__deps['+JSON.stringify(name)+'] || '+(/^[A-Z]/.test(name)?'{render:h=>h("div")}':'(()=>null)')+';').join('\n');
  const compiled = compiler.compile(parsed.template.content);
  assert.deepEqual(compiled.errors, [], file);
  return { code:dependencies+'\n'+code, render:compiled.render, staticRenderFns:compiled.staticRenderFns,
    styles:parsed.styles.map(s=>s.lang==='scss'?sass.compileString(s.content,{logger:sass.Logger.silent}).css:s.content).join('\n') };
}
(async()=>{
  const browser = await chromium.launch({channel:process.env.BROWSER_CHANNEL || 'chrome',headless:true});
  try {
    const page = await browser.newPage();
    const errors=[];
    page.on('pageerror',e=>errors.push(e.message));
    await page.route('**/*',route=>route.abort()); // fixtures never contact backend or image hosts
    await page.setContent('<html><head><meta name="viewport" content="width=device-width, initial-scale=1"></head><body><div id="fixture"></div></body></html>');
    await page.addScriptTag({content:read('node_modules/vue/dist/vue.js')});
    await page.addScriptTag({content:read('node_modules/element-ui/lib/index.js')});
    await page.addStyleTag({content:read('node_modules/element-ui/lib/theme-chalk/index.css')});
    const shared = sass.compileString(read('src/styles/variables.scss')+'\n'+read('src/styles/media.scss')+'\n'+read('src/styles/mobile-controls.scss'),{logger:sass.Logger.silent}).css;
    await page.addStyleTag({content:shared+'\nbody{margin:0;font-family:Arial} .basic-container{padding:10px;box-sizing:border-box} .form-input{width:180px}.pagination-box{text-align:center}'});
    await page.evaluate(helper=>{
      window.__deps={};
      Function(helper.replace(/export /g,'')+';Object.assign(window.__deps,{viewportMode,paginationForViewport,countActiveFilters})')();
      Vue.use(ELEMENT);
      window.viewport=Vue.observable({width:390});
      Vue.mixin({computed:{isMobileView(){return viewport.width<768},isCompactView(){return viewport.width<992}},methods:{responsivePagination(options){return __deps.paginationForViewport(options,this.isMobileView)}}});
      Vue.component('basic-container',{template:'<section class="basic-container"><slot name="header"/><slot/></section>'});
      __deps.getStore=()=>null;
      __deps.parseIds=()=>[];
      __deps.formatNumberToEmoji=n=>String(n);
      window.mountFixture=(options,data={})=>{
        if(window.fixtureVm)window.fixtureVm.$destroy();
        document.getElementById('fixture').innerHTML='<div id="mount"></div>';
        const copy={...options};
        for(const hook of ['created','mounted','activated','deactivated','beforeDestroy'])delete copy[hook];
        const original=copy.data;
        copy.data=function(){return {...(original?original.call(this):{}),...data}};
        window.fixtureVm=new Vue(copy).$mount('#mount');
      };
    },read('src/util/mobile-layout.js'));
    await page.evaluate(helper=>{
      Function(helper.replace(/export /g,'')+';Object.assign(window.__deps,{controlOf,controlTypeOf,isMultiControl,splitList,listValue,optionsOf,decorateItem})')();
    },read('src/util/config-item.js'));
    async function load(file,name) {
      const data=component(file);
      if (data.styles.trim()) await page.addStyleTag({content:data.styles});
      await page.evaluate(({data,name})=>{
        Function(data.code)();
        __component.render=Function(data.render);
        __component.staticRenderFns=data.staticRenderFns.map(s=>Function(s));
        window.__deps[name]=__component;
        Vue.component(name,__component);
      },{data,name});
    }
    await load('src/components/query-form.vue','QueryForm');
    await load('src/components/mobile-record-list.vue','MobileRecordList');
    await load('src/components/input/numberInput.vue','numberInput');
    await load('src/components/multi-cell.vue','MultiCell');
    await load('src/components/select/group-select.vue','GroupSelect');
    await page.evaluate(()=>{__deps.codeNameList=()=>Promise.resolve({data:{code:200,data:[]}})});
    // GroupSelect's import is bound at evaluation time, so reload with the API fixture.
    await load('src/components/select/group-select.vue','GroupSelect');
    const long='超长名称测试ABCDEFGHIJKLMNOPQRSTUVWXYZ'.repeat(6);
    const row={id:1,groupId:123456,groupName:long,selfId:56789,memberCount:100,uid:123,uname:long,nickname:long,userId:456,remark:long,content:long,time:'2026-10-04 12:00:00',createTime:'2026-10-04 12:00:00',level:'ERROR',message:long,businessModule:'测试模块',enableStatus:0,offNotify:0,groupInfos:[],friendInfos:[]};
    for(const [file,name] of [['group-list','GroupPage'],['friend-list','FriendPage'],['chat-record','ChatPage'],['bilibili-subscribe','SubscriptionPage'],['system/log-list','LogPage']]) {
      await load('src/views/'+file+'/index.vue',name);
      for(const width of [320,375,390,430,768,992]) {
        await page.setViewportSize({width,height:844});
        await page.evaluate(({width,name,row})=>{viewport.width=width; mountFixture(__deps[name],{tableData:[row],memberTableData:[row]})},{width,name,row});
        await page.waitForTimeout(80);
        if (name === 'ChatPage' && width === 390) {
          const out=path.join(root,'node_modules/.cache/mobile-ui'); fs.mkdirSync(out,{recursive:true});
          await page.screenshot({path:path.join(out,'chat-390.png'),fullPage:true});
        }
        assert.equal(await page.locator('.mobile-record:visible').count(),width<768?1:0,name+' cards at '+width);
        const overflow=await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1);
        assert.equal(overflow,false,name+' page overflow at '+width);
        if(width<768 && await page.locator('.query-form-toggle').count()) {
          await page.locator('.query-form-toggle').first().click();
          assert.equal(await page.locator('.query-form--collapsed').count(),file==='group-list'?1:0);
        }
      }
      console.log(name+': widths 320/375/390/430/768/992 passed');
      if (file === 'group-list') {
        await page.setViewportSize({width:320,height:844});
        await page.evaluate(()=>{viewport.width=320;fixtureVm.activeTab='member'});
        await page.waitForTimeout(50);
        assert.equal(await page.locator('.mobile-record:visible').count(),1,'member cards');
        assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1),false,'member overflow');
      }
      if(file==='bilibili-subscribe') {
        await page.setViewportSize({width:390,height:844});
        await page.evaluate(()=>{viewport.width=390});
        await page.locator('.mobile-record-header .el-checkbox').click();
        assert.equal(await page.evaluate(()=>fixtureVm.multipleSelection.length),1,'mobile batch selection');
        await page.evaluate(()=>{viewport.width=992});
        assert.equal(await page.evaluate(()=>fixtureVm.$refs.dataTable.selection.length),1,'desktop selection retained');
      }
      if(file==='chat-record') {
        await page.setViewportSize({width:320,height:844});
        await page.evaluate(()=>{viewport.width=320});
        await page.locator('.query-form-toggle').first().click();
        await page.locator('.el-date-editor input').first().click();
        const box=await page.locator('.responsive-date-range:visible').boundingBox();
        assert.ok(box && box.x>=-1 && box.x+box.width<=321,'date popup fits 320px');
        await page.keyboard.press('Escape');
        await page.evaluate(()=>{fixtureVm.queryFormObj.content='test';fixtureVm.$refs.queryForm.resetFields()});
        assert.equal(await page.evaluate(()=>fixtureVm.queryFormObj.content),'','filter reset forwarded');
        await page.evaluate(()=>{fixtureVm.view=row=>window.clickedRow=row});
        await page.getByRole('button',{name:'查看消息',exact:true}).click();
        assert.equal(await page.evaluate(()=>window.clickedRow.id),1,'card uses existing row action');
      }
    }
    await load('src/components/context-menu.vue','ContextMenu');
    await load('src/views/system/file/index.vue','FilePage');
    await page.setViewportSize({width:320,height:844});
    await page.evaluate(long=>{
      viewport.width=320;
      const node={absolutePath:'/long.txt',fileName:long+'.txt',leaf:true,isDirectory:false,showDel:true,showPreview:true};
      const options={...__deps.FilePage,methods:{...__deps.FilePage.methods,loadNode(n,resolve){resolve(n.level===0?[node]:[])}}};
      mountFixture(options,{fileNodes:[node]});
    },long);
    const more=page.getByRole('button',{name:'更多',exact:true});
    const moreBox=await more.boundingBox();
    assert.ok(moreBox && moreBox.x>=0 && moreBox.x+moreBox.width<=320,'file More button fits long filename');
    await more.click();
    assert.equal(await page.locator('.contextmenu:visible').count(),1,'file menu opens');
    // Real edit form should have zero content offset after changing to label-position=top.
    await page.setViewportSize({width:320,height:844});
    await page.evaluate(()=>{viewport.width=320; mountFixture({template:'<el-form :label-position="isMobileView ? \'top\' : \'right\'" label-width="100px"><el-form-item label="机器人"><el-input/></el-form-item></el-form>'})});
    assert.equal(await page.locator('.el-form-item__content').evaluate(e=>getComputedStyle(e).marginLeft),'0px');
    // Long group options fit even with caller-supplied popper classes.
    await page.evaluate(long=>{
      mountFixture({template:'<group-select ref="group" popper-class="custom-popper"/>'});
      Vue.nextTick(()=>{fixtureVm.$refs.group.options=[{code:123,name:long}];fixtureVm.$refs.group.handleVisibleChange=()=>{};});
    },long);
    await page.locator('.group-select input').click();
    const groupBox=await page.locator('.group-select-popper:visible').boundingBox();
    console.log('Group popup geometry:',groupBox,await page.locator('.group-select-popper:visible').evaluate(e=>({min:getComputedStyle(e).minWidth,max:getComputedStyle(e).maxWidth})));
    assert.ok(groupBox && groupBox.x>=-1 && groupBox.x+groupBox.width<=321,'group popup fits');
    // Select rows must center plain text and avatar content, including custom option styles.
    await load('src/views/bilibili-subscribe/edit-dialog.vue','SubscribeEdit');
    for (const width of [320,390,768,991]) {
      await page.setViewportSize({width,height:844});
      for (const kind of ['plain','group','bot','long']) {
        await page.evaluate(({width,kind})=>{
          viewport.width=width;
          const content=kind==='group'?'<div class="option-item"><span class="option-name">测试群</span></div>':kind==='bot'?'<img class="bot-avatar" src="data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"><span class="bot-name">机器人</span>':'<span>普通选项</span>';
          const cls=kind==='group'?'group-select-popper':kind==='bot'?'bili-bot-select-popper':'plain-popper';
          mountFixture({data:()=>({value:''}),template:'<el-select v-model="value" popper-class="'+cls+'"><el-option value="1">'+(kind==='long'?'<span>'+('长文本选项'.repeat(40))+'</span>':content)+'</el-option></el-select>'});
        },{width,kind});
        await page.locator('.el-select input').click();
        await page.waitForTimeout(250);
        const geometry=await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').evaluate(e=>{
          const row=e.getBoundingClientRect(),child=e.firstElementChild.getBoundingClientRect();
          return {gap:Math.abs((child.top+child.bottom-row.top-row.bottom)/2),fits:child.top>=row.top && child.bottom<=row.bottom};
        });
        assert.ok(geometry.gap<=2 && geometry.fits,kind+' option is vertically centered at '+width+': '+JSON.stringify(geometry));
      }
      await page.evaluate(()=>mountFixture({data:()=>({value:''}),template:'<el-select v-model="value" no-data-text="无数据" />'}));
      await page.locator('.el-select input').click();
      await page.waitForTimeout(250);
      const empty=await page.locator('.el-select-dropdown:visible .el-select-dropdown__empty').evaluate(e=>{const s=getComputedStyle(e);return {left:parseFloat(s.paddingLeft),right:parseFloat(s.paddingRight),width:e.parentElement.getBoundingClientRect().width}});
      assert.ok(empty.left>=20 && empty.right>=20 && empty.width>=160,'empty select has breathing room: '+JSON.stringify(empty));
    }
    assert.deepEqual(errors,[],'browser runtime errors before config page');
    console.log('Selection, date popup, form labels, long group options, option centering and empty-state spacing passed');

    // ---------- 配置管理：手机端文件列表抽屉 + 配置项卡片 ----------
    await load('src/views/config/config-value-editor.vue','ConfigValueEditor');
    await load('src/views/config/index.vue','ConfigPage');
    const configFileA={
      fileName:'application.properties',displayName:'主配置',path:'D:/config/application.properties',
      exists:true,count:3,hotCount:1,remark:'主配置说明',
      items:[
        {key:'bot.enable',displayName:'启用机器人',type:'BOOL',control:{type:'SWITCH',multiple:false,allowCustom:false,options:[]},value:'true',hot:true,configured:true,source:'FILE',remark:'关闭后bot不再响应消息',defaultValue:'true'},
        {key:'bot.mode',displayName:'运行模式',type:'STRING',control:{type:'SELECT',multiple:false,allowCustom:false,options:[{value:'0',label:'自动'},{value:'1',label:'强制'}]},value:'0',hot:false,configured:false,source:'DEFAULT',remark:'模式说明',defaultValue:'0'},
        {key:'druid.filters',displayName:'过滤器',type:'LIST',control:{type:'SELECT',multiple:true,allowCustom:true,options:[]},value:'stat,wall',hot:true,configured:true,source:'FILE',remark:'多个值用逗号分隔',defaultValue:''}
      ]
    };
    const configFileB={
      fileName:'jm.properties',displayName:'JM配置',path:'D:/config/jm.properties',
      exists:true,count:2,hotCount:0,remark:'JM配置说明',
      items:[
        {key:'jm.enable',displayName:'启用JM',type:'BOOL',control:{type:'SWITCH',multiple:false,allowCustom:false,options:[]},value:'true',hot:true,configured:true,source:'FILE',remark:'JM开关',defaultValue:'true'},
        {key:'jm.mode',displayName:'下载模式',type:'STRING',control:{type:'SELECT',multiple:false,allowCustom:false,options:[{value:'0',label:'自动'},{value:'1',label:'手动'}]},value:'0',hot:false,configured:true,source:'FILE',remark:'下载模式说明',defaultValue:'0'}
      ]
    };
    const configFiles=[configFileA,configFileB];
    const mountConfig=async({width,fileIndex=0})=>{
      await page.setViewportSize({width,height:844});
      await page.evaluate(({width,files,fileIndex})=>{
        viewport.width=width;
        mountFixture(__deps.ConfigPage,{files});
        fixtureVm.applyFile(files[fileIndex]);
      },{width,files:configFiles,fileIndex});
      await page.waitForTimeout(60);
    };
    for(const width of [320,375,390,430,768,992]) {
      await mountConfig({width});
      // 手机端表格换成卡片，平板/桌面端仍用表格
      assert.equal(await page.locator('.mobile-record:visible').count(),width<768?3:0,'config cards at '+width);
      assert.equal(await page.locator('.el-table:visible').count(),width<768?0:1,'config table at '+width);
      assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1),false,'config page overflow at '+width);
      // 抽屉只在手机端由按钮唤出，桌面端文件列表常驻
      assert.equal(await page.locator('.main-toolbar__file-btn').count(),width<768?1:0,'config drawer button at '+width);
      assert.equal(await page.locator('.config-manage__aside:visible').count(),width<768?0:1,'config aside at '+width);
    }
    console.log('ConfigPage: widths 320/375/390/430/768/992 passed');

    // 手机端抽屉开关、选文件后自动收起
    await mountConfig({width:390});
    assert.equal(await page.locator('.config-manage__aside--open').count(),0,'config drawer starts closed');
    await page.locator('.main-toolbar__file-btn').click();
    await page.waitForTimeout(300);
    assert.equal(await page.locator('.config-manage__aside--open:visible').count(),1,'config drawer opens');
    await page.locator('.config-manage__mask').click({position:{x:378,y:60}});
    await page.waitForTimeout(300);
    assert.equal(await page.locator('.config-manage__aside--open').count(),0,'config mask closes drawer');
    await page.locator('.main-toolbar__file-btn').click();
    await page.waitForTimeout(300);
    await page.locator('.file-list li').nth(1).click();
    await page.waitForTimeout(300);
    assert.equal(await page.evaluate(()=>fixtureVm.current.fileName),configFileB.fileName,'config drawer switches file');
    assert.equal(await page.locator('.config-manage__aside--open').count(),0,'config drawer closes after picking a file');
    assert.equal(await page.locator('.mobile-record:visible').count(),2,'config cards follow selected file');

    // 编辑控件写回编辑值、标记未保存（卡片高亮 + 保存按钮解锁）
    assert.equal(await page.locator('.mobile-record').first().locator('.mobile-record-header .el-tag').count(),1,'config status tag before edit');
    assert.equal(await page.getByRole('button',{name:'保存',exact:true}).first().isDisabled(),true,'config save disabled before edit');
    await page.locator('.mobile-record').first().locator('.el-switch').click();
    await page.waitForTimeout(60);
    assert.equal(await page.evaluate(()=>fixtureVm.current.items[0].dirty),true,'config edit marks dirty');
    assert.equal(await page.evaluate(()=>fixtureVm.current.items[0].editValue),'false','config edit writes value');
    assert.equal(await page.locator('.mobile-record.config-item--dirty').count(),1,'config dirty card highlighted');
    assert.equal(await page.locator('.mobile-record').first().locator('.mobile-record-header .el-tag').count(),2,'config unsaved tag after edit');
    assert.equal(await page.getByRole('button',{name:'保存',exact:true}).first().isDisabled(),false,'config save enabled when dirty');

    // 卡片里的控件铺满卡片宽度（表格形态靠单元格撑开，卡片形态必须自己铺满）
    await mountConfig({width:320,fileIndex:1});
    const cardBox=await page.locator('.mobile-record').nth(1).boundingBox();
    const selectBox=await page.locator('.mobile-record').nth(1).locator('.el-select').boundingBox();
    assert.ok(cardBox && selectBox && selectBox.width>=cardBox.width-30,
      'config select fills card: '+(selectBox&&selectBox.width)+'/'+(cardBox&&cardBox.width));

    // 文件原始内容弹窗在手机端整屏（custom-class + is-fullscreen 的样式覆盖）
    await page.setViewportSize({width:320,height:844});
    await page.evaluate(()=>{
      viewport.width=320;
      mountFixture({template:'<el-dialog :visible="true" custom-class="config-file-dialog" :fullscreen="isMobileView"><pre class="file-preview">bot.enable=true</pre></el-dialog>'});
    });
    await page.waitForTimeout(80);
    const dialogBox=await page.locator('.config-file-dialog').boundingBox();
    assert.ok(dialogBox && dialogBox.width>=320 && dialogBox.height>=800,
      'config file dialog is fullscreen on phone: '+JSON.stringify(dialogBox));
    assert.deepEqual(errors,[],'browser runtime errors');
    console.log('ConfigPage: drawer, card controls, dirty state and fullscreen file dialog passed');
  } finally { await browser.close(); }
})().catch(e=>{console.error(e);process.exitCode=1});
