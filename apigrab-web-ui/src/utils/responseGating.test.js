import { test } from 'node:test'
import assert from 'node:assert/strict'
import { isResponseConfigUnlocked } from './responseGating.js'

/**
 * 响应配置解锁规则（design C2）。
 *
 * ## 为什么这条规则需要专门断言
 *
 * 后端 `success` 只有在「取到响应 **且** 按当前响应类型解析成功」时才为真。响应类型本身
 * 又是被锁定的参数之一——如果「试跑失败就保持锁定」无例外，则响应类型选错的接口永远
 * 无法改正格式，形成死锁（非默认格式接口完全无法配置）。
 *
 * 因此规则必须包含「判定不出格式时解锁」这一出口，而它又依赖 `detectedRespType` 的
 * `null` / `undefined` 语义。混用会让出口失效，症状只在特定接口上显现、极难发现。
 */

test('未试跑时锁定', () => {
  assert.equal(isResponseConfigUnlocked({ hasTested: false }), false)
  assert.equal(isResponseConfigUnlocked({ hasTested: false, alreadyUnlocked: false }), false)
})

test('试跑成功后解锁', () => {
  assert.equal(isResponseConfigUnlocked({ hasTested: true, success: true, detectedRespType: null }), true)
  assert.equal(isResponseConfigUnlocked({ hasTested: true, success: true, detectedRespType: 'JSON' }), true)
})

test('试跑失败但未能判定格式时解锁（死锁出口）', () => {
  // 判定为 null：试过但响应头无法对应到已知格式
  assert.equal(isResponseConfigUnlocked({ hasTested: true, success: false, detectedRespType: null }), true)
  // 根本拿不到响应（如网络失败）：三键缺失，同样属「无法判定」
  assert.equal(isResponseConfigUnlocked({ hasTested: true, success: false, detectedRespType: undefined }), true)
})

test('试跑失败但判定出格式时保持锁定', () => {
  // 此时已知正确格式，应由自动填充纠正并引导重新试跑，而不是让用户按错误格式继续配
  assert.equal(isResponseConfigUnlocked({ hasTested: true, success: false, detectedRespType: 'XML' }), false)
  assert.equal(isResponseConfigUnlocked({ hasTested: true, success: false, detectedRespType: 'JSON' }), false)
})

test('已解锁后不再因后续失败重新锁上', () => {
  assert.equal(
    isResponseConfigUnlocked({ hasTested: true, success: false, detectedRespType: 'XML', alreadyUnlocked: true }),
    true,
    '解锁后重锁会打断用户正在进行的输入'
  )
  assert.equal(isResponseConfigUnlocked({ alreadyUnlocked: true }), true)
})

test('缺少状态对象时安全返回锁定', () => {
  assert.equal(isResponseConfigUnlocked(null), false)
  assert.equal(isResponseConfigUnlocked(undefined), false)
  assert.equal(isResponseConfigUnlocked({}), false)
})
