import VueJsonPrettyImport from 'vue-json-pretty'

/**
 * 第三方 JSON 展示组件的**唯一引入点**。
 *
 * ## 为什么必须集中在这里
 *
 * `vue-json-pretty` 是只提供 `main`（CommonJS）的包，`module.exports` 形如
 * `{ default: Component, __esModule: true }`，**没有**名为 `VueJsonPretty` 的命名导出。
 *
 * 上一轮曾写成命名导入 `import { VueJsonPretty } from 'vue-json-pretty'`，绑定恒为
 * `undefined`，组件静默不渲染，响应格式化视图全部空白——而 `npm run build` 不报错，
 * 因为打包器不校验 CommonJS 的缺失命名导入。
 *
 * 把引入收敛到本模块后，`jsonDisplay.test.js` 才能断言到**真实的引入点**；
 * 组件若直接从第三方包导入，这层守护就形同虚设。
 *
 * ## 为什么要显式解包
 *
 * 同一个默认导入在不同环境下解析结果不同：
 *   - 打包器（webpack，本项目的构建方式）识别 `__esModule`，默认导入直接得到组件。
 *   - Node 原生 ESM 不做该解包，默认导入得到整个 `module.exports` 对象。
 *
 * 两者之一若被写死，另一端就会拿到没有渲染能力的对象。这里显式判断、按需取 `.default`，
 * 使两种环境都得到真实组件——测试环境（Node）与构建环境（打包器）因此能断言同一件事。
 *
 * 展示组件请一律从这里导入：
 *   import JsonDisplay from '@/utils/jsonDisplay'
 */
const VueJsonPretty =
  VueJsonPrettyImport && typeof VueJsonPrettyImport.render === 'function'
    ? VueJsonPrettyImport
    : VueJsonPrettyImport && VueJsonPrettyImport.default

export default VueJsonPretty
