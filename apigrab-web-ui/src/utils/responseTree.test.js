import { test } from 'node:test'
import assert from 'node:assert/strict'
import { buildJsonTree, createNodeBudget, flattenTree, MAX_TREE_NODES } from './responseTree.js'

/**
 * 响应结构树构建。
 *
 * 本文件存在的原因：此前的实现由 `el-tree` 的 `data` 属性接收一个**单个节点对象**，
 * 而该属性要求 `Array`（内部直接调用 `data.forEach(...)`）。结果是结构树**从不渲染**，
 * 用户看到「无法点击节点配路径」——而构建与单元测试都发现不了（它是运行期属性类型不匹配）。
 * 因此第一条断言就是「根级返回必须是数组」。
 */

test('根级返回必须是数组：el-tree 的 data 属性要求 Array', () => {
  const budget = createNodeBudget()
  assert.equal(Array.isArray(buildJsonTree({ a: 1 }, budget)), true)
  const budget2 = createNodeBudget()
  assert.equal(Array.isArray(buildJsonTree([1, 2], budget2)), true)
  const budget3 = createNodeBudget()
  assert.equal(Array.isArray(buildJsonTree('plain', budget3)), true)
})

test('响应根为数组时，根节点带展平标记（点根节点即可配出记录集路径）', () => {
  const [root] = buildJsonTree([{ id: 1 }, { id: 2 }], createNodeBudget())
  assert.equal(root.isArray, true)
  assert.equal(root.emitPath, '$[*]')
  assert.equal(root.path, '$')
  assert.match(root.label, /2 项/)
})

test('响应根为对象时，根节点路径为 $', () => {
  const [root] = buildJsonTree({ name: 'a' }, createNodeBudget())
  assert.equal(root.emitPath, '$')
  assert.equal(root.isArray, undefined)
  assert.equal(root.children.length, 1)
})

test('嵌套数组节点自动补展平标记，且只补数组', () => {
  const [root] = buildJsonTree({ data: { rows: [{ x: 1 }] } }, createNodeBudget())
  const dataNode = root.children[0]
  const rowsNode = dataNode.children[0]
  assert.equal(rowsNode.emitPath, '$.data.rows[*]')
  assert.equal(rowsNode.isArray, true)
  // 对象节点不补
  assert.equal(dataNode.emitPath, '$.data')
})

test('特殊键走括号写法（与 jsonPath 的唯一实现一致）', () => {
  const [root] = buildJsonTree({ '@id': 1, '#text': 'x', 'user-name': 'n' }, createNodeBudget())
  const emits = root.children.map(c => c.emitPath)
  assert.deepEqual(emits, ["$['@id']", "$['#text']", "$['user-name']"])
})

test('标量节点为叶子，路径即自身', () => {
  const [root] = buildJsonTree({ n: 5 }, createNodeBudget())
  const leaf = root.children[0]
  assert.equal(leaf.isLeaf, true)
  assert.equal(leaf.emitPath, '$.n')
  assert.match(leaf.label, /5/)
})

test('空数组节点可展开但无子节点', () => {
  const [root] = buildJsonTree({ list: [] }, createNodeBudget())
  const arr = root.children[0]
  assert.equal(arr.isArray, true)
  assert.equal(arr.children.length, 0)
  assert.equal(arr.emitPath, '$.list[*]')
})

test('节点 id 在整棵树内唯一（el-tree 的 node-key 依赖唯一性）', () => {
  const [root] = buildJsonTree({ a: { b: [1, 2] }, c: 3, d: { e: 4 } }, createNodeBudget())
  const ids = []
  const walk = n => { ids.push(n.id); (n.children || []).forEach(walk) }
  walk(root)
  assert.equal(new Set(ids).size, ids.length, '节点 id 必须唯一')
})

test('节点预算耗尽时截断且不抛异常', () => {
  const deep = { a: { b: { c: { d: { e: 1 } } } } }
  const budget = createNodeBudget(3)
  const result = buildJsonTree(deep, budget)
  assert.equal(Array.isArray(result), true)
  assert.ok(budget.left <= 0)
})

test('默认预算上限为 2000', () => {
  assert.equal(MAX_TREE_NODES, 2000)
})

/**
 * flattenTree 用于以纯 HTML 渲染结构（不依赖任何树组件）。
 * 这些断言使「缩进层级与顺序正确」在 Node 中可验证——树组件无法在本环境验证渲染。
 */

test('flattenTree 按深度优先顺序展开，并给出层级', () => {
  const nodes = buildJsonTree({ a: { b: 1 }, c: 2 }, createNodeBudget())
  const rows = flattenTree(nodes)
  assert.deepEqual(rows.map(r => [r.depth, r.label.replace(/\s+/g, ' ')]), [
    [0, '$'],
    [1, '$.a'],
    [2, '$.a.b = 1'],
    [1, '$.c = 2']
  ])
})

test('flattenTree 保留可点选标记：XML 属性/文本不可选', () => {
  const nodes = [
    { id: 1, depthLabel: 'x', label: '/r', emitPath: '/r', children: [
      { id: 2, label: '@id', emitPath: "/r/@id", selectable: false, isLeaf: true },
      { id: 3, label: 'name', emitPath: '/r/name', children: [] }
    ] }
  ]
  const rows = flattenTree(nodes)
  assert.equal(rows.find(r => r.label === '@id').selectable, false)
  assert.equal(rows.find(r => r.label === 'name').selectable, true)
})

test('flattenTree 标注数组节点', () => {
  const nodes = buildJsonTree([{ id: 1 }], createNodeBudget())
  const rows = flattenTree(nodes)
  assert.equal(rows[0].isArray, true)
  assert.equal(rows[0].emitPath, '$[*]')
})

test('flattenTree 空输入返回空数组', () => {
  assert.deepEqual(flattenTree([]), [])
  assert.deepEqual(flattenTree(null), [])
})

test('flattenTree 每行 id 唯一（渲染 key 依赖唯一性）', () => {
  const nodes = buildJsonTree({ a: { b: [1, 2] }, c: { d: { e: 3 } } }, createNodeBudget())
  const rows = flattenTree(nodes)
  assert.ok(rows.length > 3)
  assert.equal(new Set(rows.map(r => r.id)).size, rows.length)
})
