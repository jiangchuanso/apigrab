import { test } from 'node:test'
import assert from 'node:assert/strict'
import { extractQuery, mergeParams } from './urlQuery.js'

test('extractQuery 无查询串时原样返回', () => {
  assert.deepEqual(extractQuery(''), { baseUrl: '', params: [] })
  assert.deepEqual(extractQuery('https://api.example.com/v1/orders'), {
    baseUrl: 'https://api.example.com/v1/orders',
    params: []
  })
})

test('extractQuery 拆出参数并剥离查询串', () => {
  assert.deepEqual(extractQuery('https://api.example.com/v1/orders?page=1&size=100'), {
    baseUrl: 'https://api.example.com/v1/orders',
    params: [{ key: 'page', value: '1' }, { key: 'size', value: '100' }]
  })
})

test('extractQuery 支持无 scheme 的相对地址', () => {
  assert.deepEqual(extractQuery('/path?x=1'), { baseUrl: '/path', params: [{ key: 'x', value: '1' }] })
})

// 空值键保留键、值为空串，不丢弃
test('extractQuery 保留空值键', () => {
  assert.deepEqual(extractQuery('?flag'), { baseUrl: '', params: [{ key: 'flag', value: '' }] })
  assert.deepEqual(extractQuery('https://x.com/a?flag'), {
    baseUrl: 'https://x.com/a',
    params: [{ key: 'flag', value: '' }]
  })
})

// qs 会把 a[] 解析成嵌套对象；URLSearchParams 保持扁平
test('extractQuery 对方括号键保持扁平', () => {
  assert.deepEqual(extractQuery('https://x.com/a?a[]=1&b=2'), {
    baseUrl: 'https://x.com/a',
    params: [{ key: 'a[]', value: '1' }, { key: 'b', value: '2' }]
  })
})

test('extractQuery 正确解码百分号转义', () => {
  assert.deepEqual(extractQuery('https://x.com/a?q=%E4%B8%AD%E6%96%87'), {
    baseUrl: 'https://x.com/a',
    params: [{ key: 'q', value: '中文' }]
  })
})

test('extractQuery 重复键各自成行', () => {
  assert.deepEqual(extractQuery('https://x.com/a?a=1&a=2'), {
    baseUrl: 'https://x.com/a',
    params: [{ key: 'a', value: '1' }, { key: 'a', value: '2' }]
  })
})

test('extractQuery 忽略尾随片段标识', () => {
  assert.deepEqual(extractQuery('https://x.com/a?b=1#frag'), {
    baseUrl: 'https://x.com/a',
    params: [{ key: 'b', value: '1' }]
  })
})

test('extractQuery 空查询串被剥离', () => {
  assert.deepEqual(extractQuery('https://x.com/a?'), { baseUrl: 'https://x.com/a', params: [] })
})

// 只补不覆盖：参数表中已有的键一律保留
test('mergeParams 仅补入表中尚不存在的键', () => {
  const existing = [{ key: 'page', value: '5' }]
  const incoming = [{ key: 'page', value: '1' }, { key: 'size', value: '100' }]
  assert.deepEqual(mergeParams(existing, incoming), {
    toAdd: [{ key: 'size', value: '100' }],
    skipped: ['page']
  })
})

test('mergeParams 全部已存在时不新增', () => {
  const existing = [{ key: 'a', value: '1' }]
  assert.deepEqual(mergeParams(existing, [{ key: 'a', value: '1' }]), { toAdd: [], skipped: ['a'] })
})

test('mergeParams 空输入不抛异常', () => {
  assert.deepEqual(mergeParams([], []), { toAdd: [], skipped: [] })
  assert.deepEqual(mergeParams(null, [{ key: 'a', value: '1' }]), {
    toAdd: [{ key: 'a', value: '1' }],
    skipped: []
  })
})