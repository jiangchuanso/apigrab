/**
 * 响应配置的解锁规则（design C2）。
 *
 * ## 规则
 *
 * | 状态 | 结果 |
 * | --- | --- |
 * | 本次会话尚未试跑 | 锁定 |
 * | 试跑成功 | 解锁 |
 * | 试跑失败且未能判定格式 | **解锁** |
 * | 试跑失败但判定出格式 | 保持锁定 |
 * | 已解锁（记忆位） | 恒解锁 |
 *
 * ## 为什么「未能判定格式时解锁」是必需的，而不是宽松
 *
 * 后端 `success` 只有在「取到响应 **且** 按当前响应类型解析成功」时才为真，而响应类型本身
 * 也是被锁定的参数。若失败一律锁定，则响应类型选错的接口永远无法改正格式——形成死锁，
 * 非默认格式（如 XML）的接口将完全无法配置。该出口是唯一的解。
 *
 * ## `null` 与 `undefined` 都表示「未能判定」
 *
 * - `null`：试过，但响应头无法对应到已知格式（后端判定三键恒存在，未判定即为 null）。
 * - `undefined`：根本没拿到响应（网络失败等），键缺失。
 *
 * 两者都必须解锁，故用宽松比较 `== null` 一并判定。写成严格比较会漏掉第二种，
 * 使「接口不可达」时同样陷入死锁。
 */

/**
 * @param {{hasTested?: boolean, success?: boolean, detectedRespType?: string|null, alreadyUnlocked?: boolean}|null} state
 * @returns {boolean} true 表示响应配置可编辑
 */
export function isResponseConfigUnlocked(state) {
  if (!state) return false
  // 解锁后保持解锁：后续失败若把它重新锁上，会打断用户正在进行的输入
  if (state.alreadyUnlocked) return true
  if (!state.hasTested) return false
  if (state.success) return true
  // 试跑失败：仅在「未能判定格式」时放行
  return state.detectedRespType == null
}
