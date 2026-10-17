import { test } from 'node:test'
import assert from 'node:assert/strict'
import { serializeFormParams, parseFormParams } from './formBody.js'

test('serializeFormParams 普通键值', () => {
  assert.equal(serializeFormParams({ a: '1', b: '2' }), 'a=1&b=2')
})

test('serializeFormParams 值含 & 时编码为 %26', () => {
  assert.equal(serializeFormParams({ q: 'a&b' }), 'q=a%26b')
})

test('serializeFormParams 空值/空对象', () => {
  assert.equal(serializeFormParams({}), '')
  assert.equal(serializeFormParams(null), '')
  assert.equal(serializeFormParams({ a: null }), 'a=')
})

test('保留字符与中文无损往返', () => {
  const source = { eq: 'x=y', plus: 'x+y', space: 'x y', pct: '100%', cjk: '中文', amp: 'a&b' }
  const encoded = serializeFormParams(source)
  assert.deepEqual(parseFormParams(encoded), source)
})

test('不得双重编码：值里的 & 只编码一次', () => {
  const encoded = serializeFormParams({ q: 'a&b' })
  assert.ok(!encoded.includes('%2526'), `实际: ${encoded}`)
})

test('parseFormParams 空输入返回空对象', () => {
  assert.deepEqual(parseFormParams(''), {})
  assert.deepEqual(parseFormParams(null), {})
})

test('parseFormParams 还原被编码的键与值', () => {
  assert.deepEqual(parseFormParams('a%26b=x%3Dy'), { 'a&b': 'x=y' })
})

test('占位符 ${id} 编码后仍可无损还原（后端据此识别并替换）', () => {
  const encoded = serializeFormParams({ id: '${id}', scope: 'full' })
  assert.equal(encoded, 'id=%24%7Bid%7D&scope=full')
  assert.deepEqual(parseFormParams(encoded), { id: '${id}', scope: 'full' })
})
