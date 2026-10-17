import { test } from 'node:test'
import assert from 'node:assert/strict'
import { formatVersion, extractVersion } from './version.js'

test('formatVersion 为纯版本号加上小写 v 前缀', () => {
  assert.equal(formatVersion('1.0.0'), 'v1.0.0')
})

test('formatVersion 去除已有的 v/V 前缀后统一为小写 v，并忽略首尾空格', () => {
  assert.equal(formatVersion('v1.0.0'), 'v1.0.0')
  assert.equal(formatVersion('V2.3.1'), 'v2.3.1')
  assert.equal(formatVersion('  1.0.0  '), 'v1.0.0')
})

test('formatVersion 对空值返回空串（降级，不展示版本行）', () => {
  assert.equal(formatVersion(''), '')
  assert.equal(formatVersion('   '), '')
  assert.equal(formatVersion(null), '')
  assert.equal(formatVersion(undefined), '')
})

test('extractVersion 从统一响应体中取出 data.version', () => {
  assert.equal(
    extractVersion({ code: 200, message: '成功', data: { version: '1.0.0' } }),
    '1.0.0'
  )
})

test('extractVersion 在字段或响应体缺失时返回空串（降级）', () => {
  assert.equal(extractVersion({}), '')
  assert.equal(extractVersion({ data: {} }), '')
  assert.equal(extractVersion({ data: null }), '')
  assert.equal(extractVersion({ data: { version: null } }), '')
  assert.equal(extractVersion(null), '')
  assert.equal(extractVersion(undefined), '')
})
