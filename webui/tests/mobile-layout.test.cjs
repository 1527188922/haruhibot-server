const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const load = async () => import('data:text/javascript;base64,' + Buffer.from(fs.readFileSync(path.join(__dirname, '../src/util/mobile-layout.js'), 'utf8')).toString('base64'));
test('phone and compact boundaries agree at 768 and 992', async () => {
  const { viewportMode } = await load();
  for (const [width, phone, compact] of [[320,true,true],[767,true,true],[768,false,true],[991,false,true],[992,false,false],[1200,false,false]]) {
    assert.deepEqual(viewportMode(width), { phone, compact });
  }
});
test('phone pagination keeps page state without mutating desktop options', async () => {
  const { paginationForViewport } = await load();
  const source = { layout:'total, sizes, prev, pager, next, jumper', currentPage:4, pageSize:30, total:301 };
  const mobile = paginationForViewport(source, true);
  assert.equal(mobile.layout,'prev, pager, next');
  assert.equal(mobile.pagerCount,5);
  assert.equal(mobile.currentPage,4);
  assert.equal(mobile.pageSize,30);
  assert.equal(mobile.total,301);
  assert.equal(source.layout,'total, sizes, prev, pager, next, jumper');
  assert.deepEqual(paginationForViewport(source,false),source);
});
test('filter count includes zero and false but excludes empty values', async () => {
  const { countActiveFilters } = await load();
  assert.equal(countActiveFilters({a:'',b:null,c:undefined,d:[],e:0,f:false,g:'name',h:['start','end']}),4);
});
