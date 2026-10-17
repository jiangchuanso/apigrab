import { test } from 'node:test'
import assert from 'node:assert/strict'
import { buildMappingsFromSample, buildMappingsFromColumns, lengthForType, CONVERT_EXPR_OPTIONS, TARGET_TYPE_OPTIONS, toTargetType, sanitizeLengthInput, isPrimaryKeyCapable } from './fieldMatch.js'

/**
 * 自动生成映射的初始默认值（design C7 / P3）。
 *
 * 后端 DDL 会真正使用这些值（`AbstractSqlDialectStrategy` 只在 VARCHAR 分支使用长度、
 * nullable 决定 NOT NULL），所以默认值不是装饰：给错会直接体现在建出来的表上。
 *
 * 其中**主键必须默认不勾选**：主键同时驱动建表主键与 Upsert 冲突键，替用户猜测会产生
 * 意料之外的唯一约束与更新行为。
 */

const sample = { id: 1, name: 'alice', amount: 12.5, active: true }

test('按样例生成：字符串类型默认 STRING（长文本、无长度），数值类型留空长度', () => {
  const rows = buildMappingsFromSample(sample)
  const byTarget = Object.fromEntries(rows.map(r => [r.targetField, r]))
  assert.equal(byTarget.name.targetType, 'STRING')
  assert.equal(byTarget.name.targetLength, null, 'STRING 是长文本，不接受长度')
  assert.equal(byTarget.id.targetType, 'BIGINT')
  assert.equal(byTarget.id.targetLength, null, '非字符串类型不应带长度——DDL 中不使用它')
  assert.equal(byTarget.amount.targetType, 'DECIMAL')
  assert.equal(byTarget.amount.targetLength, null)
})

test('按样例生成：默认可空、不勾主键、转换表达式留空', () => {
  const rows = buildMappingsFromSample(sample)
  rows.forEach(row => {
    assert.equal(row.nullable, 1, `${row.targetField} 应默认可空`)
    assert.equal(row.isPrimaryKey, false, `${row.targetField} 不得被自动勾选为主键`)
    assert.equal(row.convertExpr, '')
    assert.equal(row.defaultValue, '')
  })
})

test('按目标列生成：沿用列定义的长度', () => {
  const columns = [
    { name: 'name', type: 'varchar', size: '64' },
    { name: 'id', type: 'bigint' }
  ]
  const rows = buildMappingsFromColumns(sample, columns)
  const byTarget = Object.fromEntries(rows.map(r => [r.targetField, r]))
  assert.equal(byTarget.name.targetLength, 64, '应沿用目标列声明的长度')
  assert.equal(byTarget.id.targetLength, null)
})

test('按目标列生成：列未声明长度时字符串给 255', () => {
  const columns = [{ name: 'name', type: 'varchar' }]
  const rows = buildMappingsFromColumns(sample, columns)
  assert.equal(rows[0].targetLength, 255)
})

test('按目标列生成：同样不自动勾选主键', () => {
  const columns = [{ name: 'id', type: 'bigint' }]
  const rows = buildMappingsFromColumns(sample, columns)
  assert.equal(rows[0].isPrimaryKey, false)
})

test('sortOrder 按生成顺序连续', () => {
  const rows = buildMappingsFromSample(sample)
  assert.deepEqual(rows.map(r => r.sortOrder), rows.map((_, i) => i))
})

test('lengthForType：仅 VARCHAR 有长度默认值', () => {
  assert.equal(lengthForType('VARCHAR'), 255)
  assert.equal(lengthForType('STRING'), null, 'STRING 是长文本，不接受长度')
  assert.equal(lengthForType('INTEGER'), null)
  assert.equal(lengthForType('BIGINT'), null)
  assert.equal(lengthForType('DECIMAL'), null)
  assert.equal(lengthForType('TIMESTAMP'), null)
  assert.equal(lengthForType('BOOLEAN'), null)
  assert.equal(lengthForType(null), null)
})

test('isPrimaryKeyCapable：长文本 STRING 不可作为主键', () => {
  assert.equal(isPrimaryKeyCapable('STRING'), false)
  ;['VARCHAR', 'BIGINT', 'DECIMAL', 'DATE', 'TIMESTAMP', 'BOOLEAN'].forEach(t => {
    assert.equal(isPrimaryKeyCapable(t), true, `${t} 应可作为主键`)
  })
  assert.equal(isPrimaryKeyCapable('UNKNOWN'), false)
})

/**
 * 转换表达式必须是**函数名本身**（`upper`），不能带括号。
 * 后端 `FieldMapper` 用 `hasFunction(convertExpr)` 精确匹配键名，并传入 `expr = null`；
 * 写成 `upper()` 会匹配失败而被静默跳过——表达式看起来配了、实际没生效。
 */
test('CONVERT_EXPR_OPTIONS 提供的是裸函数名，与后端键名一致', () => {
  const values = CONVERT_EXPR_OPTIONS.map(o => o.value)
  ;['to_string', 'to_int', 'to_decimal', 'to_date', 'trim', 'upper', 'lower'].forEach(name => {
    assert.ok(values.includes(name), `缺少内置函数 ${name}`)
  })
  values.forEach(v => {
    assert.equal(v.includes('('), false, `${v} 不应带括号——后端按函数名精确匹配`)
  })
})

test('CONVERT_EXPR_OPTIONS 不包含无法生效的函数', () => {
  const values = CONVERT_EXPR_OPTIONS.map(o => o.value)
  // default_value 依赖 expr 参数取默认值，而映射引擎固定传 null；且其调用受 value != null 保护，
  // 空值路径根本无法到达——列出它只会让用户配出一个静默无效的表达式
  assert.equal(values.includes('default_value'), false)
})

/**
 * 目标类型集合必须与后端 `TargetType` 完全一致。
 * `target_type` 没有类型转换器，MyBatis 按枚举名解析：前端若出现后端不存在的值，
 * 保存后加载任务详情会因 `No enum constant` 整页失败。
 */
test('TARGET_TYPE_OPTIONS 与后端 TargetType 取值集合一致', () => {
  const values = TARGET_TYPE_OPTIONS.map(o => o.value).sort()
  assert.deepEqual(values, ['BIGINT', 'BOOLEAN', 'DATE', 'DECIMAL', 'STRING', 'TIMESTAMP', 'VARCHAR'])
})

test('TARGET_TYPE_OPTIONS 不再包含已移除的类型', () => {
  const values = TARGET_TYPE_OPTIONS.map(o => o.value)
  ;['INTEGER', 'INT', 'NUMBER', 'DATETIME', 'TEXT'].forEach(removed => {
    assert.equal(values.includes(removed), false, `${removed} 已从后端枚举移除，前端不得再提供`)
  })
})

test('toTargetType 映射到新的类型集合', () => {
  assert.equal(toTargetType('bigint'), 'BIGINT')
  assert.equal(toTargetType('int'), 'BIGINT')
  assert.equal(toTargetType('int unsigned'), 'BIGINT')
  assert.equal(toTargetType('decimal(10,2)'), 'DECIMAL')
  assert.equal(toTargetType('numeric'), 'DECIMAL')
  assert.equal(toTargetType('varchar(64)'), 'VARCHAR')
  assert.equal(toTargetType('text'), 'VARCHAR')
  assert.equal(toTargetType('clob'), 'VARCHAR')
  assert.equal(toTargetType('boolean'), 'BOOLEAN')
  assert.equal(toTargetType('tinyint(1)'), 'BIGINT')
})

test('toTargetType 先判时间戳再判日期（DATETIME 含 DATE 子串）', () => {
  assert.equal(toTargetType('datetime'), 'TIMESTAMP')
  assert.equal(toTargetType('timestamp'), 'TIMESTAMP')
  assert.equal(toTargetType('date'), 'DATE')
})

test('toTargetType 未知类型回退 VARCHAR', () => {
  assert.equal(toTargetType(''), 'VARCHAR')
  assert.equal(toTargetType(null), 'VARCHAR')
  assert.equal(toTargetType('blob'), 'VARCHAR')
})

/**
 * 长度输入只接受有效数字。
 * 后端会把长度拼进 DDL（`VARCHAR(length)`），非法值会生成坏 SQL 或让建表失败，
 * 因此必须在输入阶段就收敛，而不是等提交报错。
 */
test('sanitizeLengthInput 只保留数字', () => {
  assert.equal(sanitizeLengthInput('255'), 255)
  assert.equal(sanitizeLengthInput('12a'), 12)
  assert.equal(sanitizeLengthInput('a12b'), 12)
  assert.equal(sanitizeLengthInput('  100  '), 100)
  assert.equal(sanitizeLengthInput(64), 64)
})

test('sanitizeLengthInput 空输入表示未设置', () => {
  assert.equal(sanitizeLengthInput(''), null)
  assert.equal(sanitizeLengthInput('abc'), null)
  assert.equal(sanitizeLengthInput('   '), null)
  assert.equal(sanitizeLengthInput(null), null)
  assert.equal(sanitizeLengthInput(undefined), null)
})

test('sanitizeLengthInput 收敛到 [1, 65535]', () => {
  assert.equal(sanitizeLengthInput('0'), 1)
  assert.equal(sanitizeLengthInput('1'), 1)
  assert.equal(sanitizeLengthInput('65535'), 65535)
  assert.equal(sanitizeLengthInput('70000'), 65535)
  assert.equal(sanitizeLengthInput('999999999'), 65535)
})

test('sanitizeLengthInput 可自定义上下限', () => {
  assert.equal(sanitizeLengthInput('500', { min: 10, max: 100 }), 100)
  assert.equal(sanitizeLengthInput('5', { min: 10, max: 100 }), 10)
  assert.equal(sanitizeLengthInput('50', { min: 10, max: 100 }), 50)
})
