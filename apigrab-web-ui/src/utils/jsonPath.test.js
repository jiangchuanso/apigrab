import { test } from 'node:test'
import assert from 'node:assert/strict'
import { appendSegment, appendFlatten, leafName } from './jsonPath.js'

// 标识符键沿用点号写法以保持可读性
test('appendSegment 点号写法：标识符键', () => {
  assert.equal(appendSegment('$', 'name'), '$.name')
  assert.equal(appendSegment('$', 'id'), '$.id')
  assert.equal(appendSegment('$', '_id'), '$._id')
  assert.equal(appendSegment('$', 'a1'), '$.a1')
  assert.equal(appendSegment('$.data', 'list'), '$.data.list')
})

// 非标识符键必须用括号写法，否则 JSONPath 求值失败
test('appendSegment 括号写法：XML 属性与保留名', () => {
  assert.equal(appendSegment('$', '@id'), "$['@id']")
  assert.equal(appendSegment('$', '#text'), "$['#text']")
  assert.equal(appendSegment('$.row', '@type'), "$.row['@type']")
})

test('appendSegment 括号写法：连字符、空格、数字开头', () => {
  assert.equal(appendSegment('$', 'user-name'), "$['user-name']")
  assert.equal(appendSegment('$', 'user name'), "$['user name']")
  assert.equal(appendSegment('$', '1abc'), "$['1abc']")
  assert.equal(appendSegment('$', ''), "$['']")
})

test('appendSegment 转义键内的引号与反斜杠', () => {
  assert.equal(appendSegment('$', "it's"), "$['it\\'s']")
  assert.equal(appendSegment('$', 'a\\b'), "$['a\\\\b']")
})

test('appendFlatten 为数组节点追加展平标记', () => {
  assert.equal(appendFlatten('$.data.list'), '$.data.list[*]')
  assert.equal(appendFlatten('$'), '$[*]')
  assert.equal(appendFlatten("$['@rows']"), "$['@rows'][*]")
})

// 目标字段名推导：改用括号写法后，按 '.' 切分会得到 data'@id' 之类的垃圾名
test('leafName 取路径末段作为字段名', () => {
  assert.equal(leafName('$.name'), 'name')
  assert.equal(leafName('$.data.name'), 'name')
  assert.equal(leafName("$.data['@id']"), '@id')
  assert.equal(leafName("$['@id']"), '@id')
  assert.equal(leafName("$.data['#text']"), '#text')
  assert.equal(leafName('$.data.list[0].name'), 'name')
  assert.equal(leafName("$.rows[0]['user-name']"), 'user-name')
})

test('leafName 对无末段与根路径不抛异常', () => {
  assert.equal(leafName('$'), '$')
  assert.equal(leafName(''), '')
  assert.equal(leafName(null), '')
})