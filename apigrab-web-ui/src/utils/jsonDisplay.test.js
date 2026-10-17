import { test } from 'node:test'
import assert from 'node:assert/strict'
import DefaultDisplay from './jsonDisplay.js'
import * as vendorModule from 'vue-json-pretty'

/**
 * 依赖形态守卫。
 *
 * 背景：本模块引入的第三方 JSON 展示组件**只有默认导出**。上一轮曾写成命名导入
 * `import { VueJsonPretty } from 'vue-json-pretty'`，绑定恒为 undefined，组件从不渲染，
 * 响应格式化视图全部空白——而 `npm run build` 不报错（打包器不校验 CommonJS 的缺失命名导入）。
 *
 * 因此这两条断言不是形式：正向断言直接捕获该缺陷，反向断言把「为何必须用默认导入」
 * 固化在代码里。这是设计约束 C12 的落点。
 */

test('默认导入解析为具备渲染能力的组件，而非 undefined', () => {
  assert.ok(DefaultDisplay, '默认导入不得为 undefined——命名导入会得到 undefined 并导致格式化视图空白')
  assert.equal(typeof DefaultDisplay, 'object')
  assert.equal(
    typeof DefaultDisplay.render,
    'function',
    '导入结果必须是一个可渲染的 Vue 组件'
  )
})

test('该包不提供命名导出 VueJsonPretty，故必须使用默认导入', () => {
  // 这条断言是「有意的警报」：若该包未来新增此命名导出，本断言会失败并提示复核引入方式，
  // 而不是让一段已经过时的注释继续误导后人。
  assert.equal(
    vendorModule.VueJsonPretty,
    undefined,
    '若此处不再为 undefined，说明该包新增了命名导出，请复核 jsonDisplay.js 的引入方式并更新本断言与注释'
  )
})
