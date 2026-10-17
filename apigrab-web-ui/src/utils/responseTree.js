// 显式写扩展名：Node 原生 ESM 不做扩展名补全（打包器行为宽松，但本模块需被测试直接导入）
import { appendSegment, appendFlatten } from './jsonPath.js'

/**
 * 响应结构树构建。
 *
 * ## 为什么根级返回**数组**而不是单个节点
 *
 * `el-tree` 的 `data` 属性类型是 `Array`，内部直接调用 `data.forEach(...)`。
 * 此前把单个根节点对象直接交给它，导致 `forEach is not a function`，**整棵树从不渲染**，
 * 表现为「无法点击节点配置路径」。该缺陷在构建与单元测试中都不可见（属运行期属性类型不匹配）。
 *
 * 因此本模块所有对外函数**一律返回数组**，并在测试中由第一条断言钉死。
 *
 * ## 与 JSONPath 规则的一致性
 *
 * 路径拼接复用 `jsonPath.js`（唯一实现）：标识符键走点号，其余走括号写法；
 * 数组节点补展平标记。自行拼串会重蹈「XML 属性产出 `$.@id` 而静默取空值」的覆辙。
 */

export const MAX_TREE_NODES = 2000

/**
 * 把节点树展平为带层级的行数组，供**纯 HTML** 渲染。
 *
 * 为什么不用树组件：`el-tree` 的渲染在本项目的验证环境（无浏览器、无 jsdom）里
 * 完全不可断言，且其 data/props/node-key/插槽语义多次成为故障来源。展平后只需
 * `v-for` + 缩进即可渲染，层级与顺序可以在 Node 中被断言。
 */
export function flattenTree(nodes, baseDepth = 0) {
  const rows = []
  const walk = (list, depth) => {
    ;(list || []).forEach(node => {
      rows.push({
        id: node.id,
        depth,
        label: node.label,
        emitPath: node.emitPath,
        isArray: !!node.isArray,
        selectable: node.selectable !== false,
        hasChildren: !!(node.children && node.children.length)
      })
      if (node.children && node.children.length) {
        walk(node.children, depth + 1)
      }
    })
  }
  walk(nodes, baseDepth)
  return rows
}

/** 节点预算：`left` 为剩余可生成节点数，`seq` 为自增 id（`el-tree` 的 node-key 要求唯一） */
export function createNodeBudget(max = MAX_TREE_NODES) {
  return { left: max, seq: 0 }
}

function take(budget) {
  if (budget.left <= 0) return null
  budget.left -= 1
  // id 从 1 起：0 为假值，作为 el-tree 的 node-key 值时容易被各处的真值判断漏掉
  budget.seq += 1
  return budget.seq
}

/**
 * 由 JSON 值构建节点数组。
 *
 * @returns {Array<Object>} 恒为数组（即使响应根是对象或标量）
 */
export function buildJsonTree(value, budget = createNodeBudget()) {
  const node = buildJsonNode(value, '$', budget)
  return node ? [node] : []
}

function buildJsonNode(value, path, budget) {
  const id = take(budget)
  if (id === null) return null

  if (Array.isArray(value)) {
    const node = {
      id,
      path,
      emitPath: appendFlatten(path),
      isArray: true,
      label: `${path}  [${value.length} 项]`,
      children: []
    }
    if (value.length === 0) {
      node.label = `${path}  []`
      return node
    }
    // 同构假设：只展开首个元素作结构样例
    const child = buildJsonNode(value[0], `${path}[0]`, budget)
    if (child) node.children.push(child)
    return node
  }

  if (value !== null && typeof value === 'object') {
    const node = { id, path, emitPath: path, label: path, children: [] }
    Object.keys(value).forEach(key => {
      const child = buildJsonNode(value[key], appendSegment(path, key), budget)
      if (child) node.children.push(child)
    })
    return node
  }

  const shown = value === null ? 'null' : String(value)
  return { id, path, emitPath: path, isLeaf: true, label: `${path} = ${shown}` }
}

/**
 * 由 XML 根元素构建节点数组。
 *
 * 属性与文本节点标记为 `selectable: false`：解析侧只接受**元素节点**作为一条记录，
 * 让用户选它们会配出取不到记录的路径。
 *
 * @returns {Array<Object>} 恒为数组
 */
export function buildXmlTree(rootElement, budget = createNodeBudget()) {
  const node = buildXmlNode(rootElement, '', budget)
  return node ? [node] : []
}

function buildXmlNode(element, parentPath, budget) {
  const id = take(budget)
  if (id === null || !element) return null

  const name = element.nodeName
  const path = parentPath ? `${parentPath}/${name}` : `/${name}`
  const node = { id, path, emitPath: path, label: `${path}  <${name}>`, children: [] }

  for (let i = 0; i < element.attributes.length; i++) {
    const attr = element.attributes[i]
    node.children.push({
      id: take(budget),
      path,
      emitPath: `${path}/@${attr.name}`,
      selectable: false,
      isLeaf: true,
      label: `  @${attr.name} = ${attr.value}`
    })
  }

  let text = ''
  for (let i = 0; i < element.childNodes.length; i++) {
    const child = element.childNodes[i]
    if (child.nodeType === 3 || child.nodeType === 4) {
      text += child.nodeValue
    }
  }
  if (text.trim()) {
    node.children.push({
      id: take(budget),
      path,
      emitPath: `${path}/text()`,
      selectable: false,
      isLeaf: true,
      label: `  #text = ${text.trim()}`
    })
  }

  for (let i = 0; i < element.children.length; i++) {
    const childNode = buildXmlNode(element.children[i], path, budget)
    if (childNode) node.children.push(childNode)
  }
  return node
}
