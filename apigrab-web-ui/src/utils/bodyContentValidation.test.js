import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  validateJsonBody,
  validateXmlBody,
  validateBodyContent,
  xmlVerdictFromDocument
} from './bodyContentValidation.js'

test('validateJsonBody 合法 JSON 通过', () => {
  assert.equal(validateJsonBody('{"a":1,"b":[1,2]}'), '')
})

test('validateJsonBody 非法 JSON 返回错误', () => {
  assert.notEqual(validateJsonBody('{a:1}'), '')
  assert.notEqual(validateJsonBody('{"a":}'), '')
})

test('validateJsonBody 空正文不报错', () => {
  assert.equal(validateJsonBody(''), '')
  assert.equal(validateJsonBody('   '), '')
  assert.equal(validateJsonBody(null), '')
  assert.equal(validateJsonBody(undefined), '')
})

test('validateXmlBody 空正文不报错', () => {
  assert.equal(validateXmlBody(''), '')
  assert.equal(validateXmlBody(null), '')
})

test('validateBodyContent 仅校验 JSON/XML，其它类型不校验', () => {
  assert.equal(validateBodyContent('NONE', '{bad'), '')
  assert.equal(validateBodyContent('FORM', '{bad'), '')
  assert.equal(validateBodyContent('JSON', '{"a":1}'), '')
  assert.notEqual(validateBodyContent('JSON', '{bad'), '')
})

test('xmlVerdictFromDocument 无 parsererror 视为良构', () => {
  assert.equal(xmlVerdictFromDocument(null), '')
  assert.equal(xmlVerdictFromDocument({ getElementsByTagName: () => [] }), '')
})

test('xmlVerdictFromDocument 含 parsererror 返回错误', () => {
  const doc = { getElementsByTagName: name => (name === 'parsererror' ? [{ textContent: ' mismatched tag' }] : []) }
  const message = xmlVerdictFromDocument(doc)
  assert.notEqual(message, '')
  assert.ok(message.includes('XML 格式不正确'), `实际: ${message}`)
})
