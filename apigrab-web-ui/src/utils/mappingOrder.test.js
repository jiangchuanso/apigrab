import { test } from 'node:test'
import assert from 'node:assert/strict'
import { withSequentialSortOrder, moveRow } from './mappingOrder.js'

/**
 * 映射行排序（design C8 / P2）。
 *
 * 顺序由 `sort_order` 持久化并在读取时 `ORDER BY sort_order ASC`。重排后必须写回
 * 从 0 起的连续序号；且**不得改动入参**——面板的行是 Vue 里的响应式对象，
 * 就地改写会让界面与后端在各处出现不一致。
 */

test('withSequentialSortOrder 写回从 0 起的连续序号', () => {
  const rows = [{ id: 1 }, { id: 2 }, { id: 3 }]
  const out = withSequentialSortOrder(rows)
  assert.deepEqual(out.map(r => r.sortOrder), [0, 1, 2])
})

test('withSequentialSortOrder 不修改入参', () => {
  const rows = [{ id: 1, sortOrder: 7 }, { id: 2, sortOrder: 3 }]
  withSequentialSortOrder(rows)
  assert.equal(rows[0].sortOrder, 7)
  assert.equal(rows[1].sortOrder, 3)
  assert.equal(rows.length, 2)
})

test('withSequentialSortOrder 保留其他字段', () => {
  const rows = [{ targetField: 'a', targetLength: 64, isPrimaryKey: true }]
  const [out] = withSequentialSortOrder(rows)
  assert.equal(out.targetField, 'a')
  assert.equal(out.targetLength, 64)
  assert.equal(out.isPrimaryKey, true)
  assert.equal(out.sortOrder, 0)
})

test('withSequentialSortOrder 空输入', () => {
  assert.deepEqual(withSequentialSortOrder([]), [])
  assert.deepEqual(withSequentialSortOrder(null), [])
})

test('moveRow 向下移动一位', () => {
  const rows = [{ v: 'a' }, { v: 'b' }, { v: 'c' }]
  assert.deepEqual(moveRow(rows, 0, 1).map(r => r.v), ['b', 'a', 'c'])
})

test('moveRow 向上移动一位', () => {
  const rows = [{ v: 'a' }, { v: 'b' }, { v: 'c' }]
  assert.deepEqual(moveRow(rows, 2, 0).map(r => r.v), ['c', 'a', 'b'])
})

test('moveRow 越界或原地不动时返回等值副本', () => {
  const rows = [{ v: 'a' }, { v: 'b' }]
  assert.deepEqual(moveRow(rows, 0, 0).map(r => r.v), ['a', 'b'])
  assert.deepEqual(moveRow(rows, -1, 1).map(r => r.v), ['a', 'b'])
  assert.deepEqual(moveRow(rows, 0, 5).map(r => r.v), ['a', 'b'])
  assert.deepEqual(moveRow(rows, 5, 0).map(r => r.v), ['a', 'b'])
})

test('moveRow 不修改入参', () => {
  const rows = [{ v: 'a' }, { v: 'b' }]
  moveRow(rows, 0, 1)
  assert.deepEqual(rows.map(r => r.v), ['a', 'b'])
})
