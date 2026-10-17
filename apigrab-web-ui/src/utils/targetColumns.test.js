import test from 'node:test'
import assert from 'node:assert/strict'
import {
  columnNameOf,
  columnNamesOf,
  findColumnByName,
  isTargetFieldValid,
  isPrimaryKeyColumn,
  primaryKeyColumnsOf,
  unmappedPrimaryKeyColumns,
  typeAndLengthForColumn
} from './targetColumns.js'

test('columnNameOf 兼容对象/字符串/空值', () => {
  assert.equal(columnNameOf({ name: 'user_id' }), 'user_id')
  assert.equal(columnNameOf('user_id'), 'user_id')
  assert.equal(columnNameOf({}), '')
  assert.equal(columnNameOf(null), '')
})

test('columnNamesOf 过滤空名', () => {
  const cols = [{ name: 'a' }, 'b', { name: '' }, null, { name: 'c' }]
  assert.deepEqual(columnNamesOf(cols), ['a', 'b', 'c'])
})

test('findColumnByName 忽略大小写与首尾空白', () => {
  const cols = [{ name: 'Order_ID' }, { name: 'amount' }]
  assert.equal(findColumnByName(cols, 'order_id').name, 'Order_ID')
  assert.equal(findColumnByName(cols, '  AMOUNT ').name, 'amount')
})

test('findColumnByName 未命中/空名返回 null', () => {
  const cols = [{ name: 'amount' }]
  assert.equal(findColumnByName(cols, 'missing'), null)
  assert.equal(findColumnByName(cols, ''), null)
  assert.equal(findColumnByName(cols, null), null)
  assert.equal(findColumnByName([], 'amount'), null)
})

test('isTargetFieldValid 仅当属于列集合时为真', () => {
  const cols = [{ name: 'amount' }]
  assert.equal(isTargetFieldValid(cols, 'AMOUNT'), true)
  assert.equal(isTargetFieldValid(cols, 'other'), false)
  assert.equal(isTargetFieldValid(cols, ''), false)
})

test('isPrimaryKeyColumn 兼容 true 与字符串标记', () => {
  assert.equal(isPrimaryKeyColumn({ name: 'id', primaryKey: true }), true)
  assert.equal(isPrimaryKeyColumn({ name: 'id', primaryKey: 'true' }), true)
  assert.equal(isPrimaryKeyColumn({ name: 'id', primaryKey: ' TRUE ' }), true)
  assert.equal(isPrimaryKeyColumn({ name: 'id', primaryKey: 'True' }), true)
  assert.equal(isPrimaryKeyColumn({ name: 'id', primaryKey: false }), false)
  assert.equal(isPrimaryKeyColumn({ name: 'id', primaryKey: 'false' }), false)
})

test('isPrimaryKeyColumn 缺失/非法输入安全返回 false', () => {
  assert.equal(isPrimaryKeyColumn({ name: 'id' }), false)
  assert.equal(isPrimaryKeyColumn({ name: 'id', primaryKey: null }), false)
  assert.equal(isPrimaryKeyColumn({ name: 'id', primaryKey: 1 }), false)
  assert.equal(isPrimaryKeyColumn('id'), false)
  assert.equal(isPrimaryKeyColumn(null), false)
})

test('primaryKeyColumnsOf 仅返回主键列名且顺序稳定', () => {
  const cols = [
    { name: 'Order_ID', primaryKey: 'true' },
    { name: 'amount', primaryKey: 'false' },
    { name: 'tenant_id', primaryKey: true },
    { name: '', primaryKey: 'true' }
  ]
  assert.deepEqual(primaryKeyColumnsOf(cols), ['Order_ID', 'tenant_id'])
})

test('primaryKeyColumnsOf 无主键/空输入返回空数组', () => {
  assert.deepEqual(primaryKeyColumnsOf([{ name: 'a' }, { name: 'b' }]), [])
  assert.deepEqual(primaryKeyColumnsOf([]), [])
  assert.deepEqual(primaryKeyColumnsOf(null), [])
})

test('unmappedPrimaryKeyColumns 主键已映射时返回空', () => {
  const cols = [{ name: 'id', primaryKey: 'true' }, { name: 'name', primaryKey: 'false' }]
  assert.deepEqual(unmappedPrimaryKeyColumns(cols, ['id', 'name']), [])
  // 大小写/空白不敏感
  assert.deepEqual(unmappedPrimaryKeyColumns(cols, ['  ID  ', 'name']), [])
})

test('unmappedPrimaryKeyColumns 主键未映射时返回该主键', () => {
  const cols = [{ name: 'id', primaryKey: 'true' }, { name: 'name', primaryKey: 'false' }]
  assert.deepEqual(unmappedPrimaryKeyColumns(cols, ['name']), ['id'])
})

test('unmappedPrimaryKeyColumns 尚未映射任何列时返回空（避免选表误报）', () => {
  const cols = [{ name: 'id', primaryKey: 'true' }]
  assert.deepEqual(unmappedPrimaryKeyColumns(cols, []), [])
  assert.deepEqual(unmappedPrimaryKeyColumns(cols, ['', '   ']), [])
  assert.deepEqual(unmappedPrimaryKeyColumns(cols, null), [])
})

test('unmappedPrimaryKeyColumns 无主键表返回空', () => {
  const cols = [{ name: 'a' }, { name: 'b' }]
  assert.deepEqual(unmappedPrimaryKeyColumns(cols, ['a']), [])
})

test('typeAndLengthForColumn VARCHAR 取列长度', () => {
  assert.deepEqual(typeAndLengthForColumn({ type: 'VARCHAR', size: '64' }), { targetType: 'VARCHAR', targetLength: 64 })
})

test('typeAndLengthForColumn VARCHAR 缺长度回退默认 255', () => {
  assert.deepEqual(typeAndLengthForColumn({ type: 'VARCHAR' }), { targetType: 'VARCHAR', targetLength: 255 })
  assert.deepEqual(typeAndLengthForColumn({ type: 'VARCHAR', size: '0' }), { targetType: 'VARCHAR', targetLength: 255 })
})

test('typeAndLengthForColumn 非 VARCHAR 长度为空', () => {
  assert.deepEqual(typeAndLengthForColumn({ type: 'INT' }), { targetType: 'BIGINT', targetLength: null })
})

test('typeAndLengthForColumn 时间类型优先于 DATE', () => {
  // 'DATETIME'.includes('DATE') 为真，必须先判 TIMESTAMP/DATETIME
  assert.equal(typeAndLengthForColumn({ type: 'DATETIME' }).targetType, 'TIMESTAMP')
  assert.equal(typeAndLengthForColumn({ type: 'TIMESTAMP' }).targetType, 'TIMESTAMP')
  assert.equal(typeAndLengthForColumn({ type: 'DATE' }).targetType, 'DATE')
})

test('typeAndLengthForColumn 布尔/小数/缺类型', () => {
  assert.equal(typeAndLengthForColumn({ type: 'TINYINT(1)' }).targetType, 'BIGINT')
  assert.equal(typeAndLengthForColumn({ type: 'BOOLEAN' }).targetType, 'BOOLEAN')
  assert.equal(typeAndLengthForColumn({ type: 'DECIMAL' }).targetType, 'DECIMAL')
  // 缺类型按 toTargetType('') -> VARCHAR
  assert.deepEqual(typeAndLengthForColumn({}), { targetType: 'VARCHAR', targetLength: 255 })
  assert.deepEqual(typeAndLengthForColumn(null), { targetType: 'VARCHAR', targetLength: 255 })
})
