import { test } from 'node:test'
import assert from 'node:assert/strict'
import { parseMergeKeys, serializeMergeKeys, findUnmatchedKeys, enforcePrimaryKeyNotNull } from './mergeKeys.js'

/**
 * 逐字段主键 ↔ 任务级 mergeKeys 字符串互转（design C9 / P1）。
 *
 * 主键标记复用既有 `task.merge_keys` 存储，因此这一对互转是「勾选」与「既有持久化格式」
 * 之间的唯一桥梁。转换出错会导致主键静默丢失或凭空多出冲突键——两者都只在真正采集时
 * 才以「重复数据」或「更新错行」的形式暴露。
 */

test('parseMergeKeys 空输入返回空数组', () => {
  assert.deepEqual(parseMergeKeys(null), [])
  assert.deepEqual(parseMergeKeys(undefined), [])
  assert.deepEqual(parseMergeKeys(''), [])
  assert.deepEqual(parseMergeKeys('   '), [])
  assert.deepEqual(parseMergeKeys(',,,'), [])
})

test('parseMergeKeys 按逗号切分并去除空白', () => {
  assert.deepEqual(parseMergeKeys('id'), ['id'])
  assert.deepEqual(parseMergeKeys('id,name'), ['id', 'name'])
  assert.deepEqual(parseMergeKeys(' id , name '), ['id', 'name'])
  assert.deepEqual(parseMergeKeys('id,'), ['id'])
})

test('parseMergeKeys 去重并保持首次出现顺序', () => {
  assert.deepEqual(parseMergeKeys('id,id,name'), ['id', 'name'])
  assert.deepEqual(parseMergeKeys('name,id,name'), ['name', 'id'])
})

test('serializeMergeKeys 取勾选行的目标字段', () => {
  assert.equal(serializeMergeKeys([]), '')
  assert.equal(serializeMergeKeys([{ targetField: 'id', isPrimaryKey: true }]), 'id')
  assert.equal(
    serializeMergeKeys([
      { targetField: 'id', isPrimaryKey: true },
      { targetField: 'name', isPrimaryKey: true }
    ]),
    'id,name'
  )
})

test('serializeMergeKeys 忽略未勾选、缺字段与空白字段名', () => {
  assert.equal(
    serializeMergeKeys([
      { targetField: 'id', isPrimaryKey: true },
      { targetField: 'name', isPrimaryKey: false },
      { targetField: 'other' },
      { targetField: '', isPrimaryKey: true },
      { targetField: '   ', isPrimaryKey: true },
      { isPrimaryKey: true }
    ]),
    'id'
  )
})

test('serializeMergeKeys 处理空输入', () => {
  assert.equal(serializeMergeKeys(null), '')
  assert.equal(serializeMergeKeys(undefined), '')
})

test('findUnmatchedKeys 报出无法对应到任何行的键', () => {
  const rows = [{ targetField: 'id' }, { targetField: 'name' }]
  assert.deepEqual(findUnmatchedKeys(['id'], rows), [])
  assert.deepEqual(findUnmatchedKeys(['id', 'name'], rows), [])
  assert.deepEqual(findUnmatchedKeys(['id', 'nope'], rows), ['nope'])
  assert.deepEqual(findUnmatchedKeys(['nope', 'zzz'], rows), ['nope', 'zzz'])
})

test('findUnmatchedKeys 去除首尾空白后比较，但不做大小写归一', () => {
  const rows = [{ targetField: 'user_id' }]
  assert.deepEqual(findUnmatchedKeys(['  user_id  '], rows), [])
  // 列名大小写是否敏感取决于方言，擅自归一可能匹配到错误的列，故保持精确比较
  assert.deepEqual(findUnmatchedKeys(['USER_ID'], rows), ['USER_ID'])
})

test('findUnmatchedKeys 空输入与空行集', () => {
  assert.deepEqual(findUnmatchedKeys([], [{ targetField: 'id' }]), [])
  assert.deepEqual(findUnmatchedKeys(['id'], []), ['id'])
  assert.deepEqual(findUnmatchedKeys(null, [{ targetField: 'id' }]), [])
  assert.deepEqual(findUnmatchedKeys(['id'], null), ['id'])
})

/**
 * 主键列不允许为空。SQL 中 PRIMARY KEY 隐含 NOT NULL，若界面允许勾主键的同时保留「可空」，
 * 就会出现一个自相矛盾的配置：用户以为该列可空，实际建表后必然非空。
 */
test('enforcePrimaryKeyNotNull 把主键行的可空置为 0', () => {
  const rows = [
    { targetField: 'id', isPrimaryKey: true, nullable: 1 },
    { targetField: 'name', isPrimaryKey: false, nullable: 1 }
  ]
  const out = enforcePrimaryKeyNotNull(rows)
  assert.equal(out[0].nullable, 0, '主键列必须非空')
  assert.equal(out[1].nullable, 1, '非主键列不受影响')
})

test('enforcePrimaryKeyNotNull 保留行的其他字段且不改入参', () => {
  const rows = [{ targetField: 'id', isPrimaryKey: true, nullable: 1, targetLength: 64, convertExpr: 'trim' }]
  const out = enforcePrimaryKeyNotNull(rows)
  assert.equal(out[0].targetLength, 64)
  assert.equal(out[0].convertExpr, 'trim')
  assert.equal(rows[0].nullable, 1, '不得修改入参')
})

test('enforcePrimaryKeyNotNull 处理已是非空、布尔值与应用多次的情形', () => {
  assert.equal(enforcePrimaryKeyNotNull([{ isPrimaryKey: true, nullable: 0 }])[0].nullable, 0)
  // 布尔 false 表示可空，被主键归一为整型 0（契约要求整型，参见 field-mapping-nullable-type）
  assert.equal(enforcePrimaryKeyNotNull([{ isPrimaryKey: true, nullable: true }])[0].nullable, 0)
  const once = enforcePrimaryKeyNotNull([{ isPrimaryKey: true, nullable: 1 }])
  assert.equal(enforcePrimaryKeyNotNull(once)[0].nullable, 0, '重复应用应幂等')
})

test('enforcePrimaryKeyNotNull 空输入安全', () => {
  assert.deepEqual(enforcePrimaryKeyNotNull([]), [])
  assert.deepEqual(enforcePrimaryKeyNotNull(null), [])
})

/**
 * 长文本类型（STRING）不能作为主键：MySQL 拒绝无前缀长度的 TEXT/BLOB 键。
 * 界面会禁用勾选，但接口写入或历史数据仍可能带着「STRING + 主键」到达，必须归一掉。
 */
test('enforcePrimaryKeyNotNull 清除不可作主键类型的勾选', () => {
  const rows = [
    { targetField: 'body', targetType: 'STRING', isPrimaryKey: true, nullable: 1 },
    { targetField: 'id', targetType: 'BIGINT', isPrimaryKey: true, nullable: 1 }
  ]
  const out = enforcePrimaryKeyNotNull(rows)
  assert.equal(out[0].isPrimaryKey, false, 'STRING 不得作为主键')
  assert.equal(out[1].isPrimaryKey, true, '可作主键的类型不受影响')
  // 被取消主键后该列仍按其自身 nullable 语义保留（非主键列允许可空）
  assert.equal(out[0].nullable, 1)
  assert.equal(out[1].nullable, 0, '主键列仍然强制非空')
})

test('enforcePrimaryKeyNotNull 对缺失 targetType 的行保守处理', () => {
  // 类型未知时保留原勾选，交由后端 DDL 兜底，不在前端擅自更改用户配置
  const out = enforcePrimaryKeyNotNull([{ targetField: 'x', isPrimaryKey: true, nullable: 1 }])
  assert.equal(out[0].isPrimaryKey, true)
  assert.equal(out[0].nullable, 0)
})
