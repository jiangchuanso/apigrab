<template>
  <div class="api-config-edit">
    <el-card>
      <div slot="header" class="rc-card-header">
        <span class="rc-card-header__title">{{ isEdit ? '编辑接口配置' : '新增接口配置' }}</span>
        <div class="rc-card-header__actions">
          <el-button size="small" @click="$router.push('/api-config/list')">返回列表</el-button>
        </div>
      </div>

      <el-form ref="form" :model="form" :rules="rules" label-width="120px" size="small">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="接口名称" prop="name">
              <!-- 与列 rc_api_config.name VARCHAR(100) 对齐，避免超长导致存库报错（design C10） -->
              <el-input v-model="form.name" maxlength="100" show-word-limit placeholder="例如：用户订单数据接口" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="请求方法" prop="method">
              <el-select v-model="form.method" style="width: 100%;">
                <el-option label="GET" value="GET" />
                <el-option label="POST" value="POST" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="接口 URL" prop="url">
          <el-input
          v-model="form.url"
          placeholder="https://api.example.com/v1/orders"
          @blur="onUrlBlur"
          @keyup.enter="onUrlBlur"
        />
        </el-form-item>

        <!-- Headers -->
        <el-form-item label="请求头">
          <key-value-editor
            v-model="form.headers"
            key-placeholder="Header 名"
            value-placeholder="Header 值"
            :presets="headerPresets"
            @preset-exists="onHeaderPresetExists"
          />
        </el-form-item>

        <!-- Query Params -->
        <el-form-item label="查询参数">
          <key-value-editor v-model="form.queryParams" key-placeholder="参数名" value-placeholder="参数值" />
          <span class="el-form-item-tip" style="color: #909399; font-size: 12px;">
            若 URL 中已带 ?a=b&c=d，离开输入框后会自动拆入此处并从 URL 中移除
          </span>
        </el-form-item>

        <!-- Body -->
        <template v-if="form.method !== 'GET'">
          <el-form-item label="请求体类型">
            <el-select v-model="form.bodyType" style="width: 200px;">
              <el-option label="NONE" value="NONE" />
              <el-option label="JSON" value="JSON" />
              <el-option label="FORM" value="FORM" />
              <el-option label="XML" value="XML" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="form.bodyType === 'FORM'" label="Body 内容">
            <key-value-editor
              v-model="form.formParams"
              key-placeholder="参数名"
              value-placeholder="参数值"
            />
            <span class="el-form-item-tip" style="color: #909399; font-size: 12px;">
              表单参数将以 a=b&amp;c=d 编码保存；值中的 &amp;、= 等字符会自动编码，也支持 ${参数名} 占位（如 pageNo = ${pageNo}）
            </span>
          </el-form-item>
          <el-form-item v-else-if="form.bodyType !== 'NONE'" label="Body 内容" :error="bodyContentError">
            <el-input
              v-model="form.bodyContent"
              type="textarea"
              :rows="5"
              placeholder="请求体内容"
              @blur="revalidateBody"
            />
          </el-form-item>
        </template>

        <!-- 超时配置：单位秒；留空/非正值时后端按默认 10/60 生效（design D6） -->
        <el-form-item label="连接超时" prop="connectTimeout">
          <el-input-number v-model="form.connectTimeout" :min="1" :step="1" controls-position="right" style="width: 160px;" />
          <span class="el-form-item-tip" style="color: #909399; font-size: 12px; margin-left: 8px;">秒</span>
        </el-form-item>
        <el-form-item label="读取超时" prop="readTimeout">
          <el-input-number v-model="form.readTimeout" :min="1" :step="1" controls-position="right" style="width: 160px;" />
          <span class="el-form-item-tip" style="color: #909399; font-size: 12px; margin-left: 8px;">秒</span>
        </el-form-item>

        <!-- 认证配置：引用共享的认证配置（空=无认证） -->
        <el-divider content-position="left">认证配置</el-divider>
        <el-form-item label="认证配置">
          <el-select v-model="form.authConfigId" clearable placeholder="无认证" style="width: 320px;">
            <el-option
              v-for="item in authConfigOptions"
              :key="item.id"
              :label="`${item.name}（${typeLabel(item.authType)}）`"
              :value="item.id"
            />
          </el-select>
          <span class="el-form-item-tip" style="color: #909399; font-size: 12px;">
            不选即「无认证」；认证在「认证配置」菜单统一维护，可被多个接口共享
          </span>
        </el-form-item>

        <!-- 分页配置 -->
        <el-divider content-position="left">分页配置</el-divider>
        <el-form-item label="启用分页">
          <el-switch v-model="form.pageEnabled" :active-value="1" :inactive-value="0" />
        </el-form-item>

        <template v-if="form.pageEnabled">
          <el-form-item label="分页模式">
              <el-select v-model="pageConfig.mode" style="width: 200px;">
                <el-option label="页码翻页" value="PAGE_NUMBER" />
                <el-option label="偏移翻页" value="OFFSET" />
                <el-option label="游标翻页" value="CURSOR" />
              </el-select>
          </el-form-item>
          <el-form-item label="参数位置">
            <el-select v-model="pageConfig.paramLocation" style="width: 200px;">
              <el-option label="查询参数 (query)" value="QUERY" />
              <el-option label="请求体 (body)" value="BODY" />
              <el-option label="路径参数 (path)" value="PATH" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="pageConfig.paramLocation === 'BODY'">
            <span class="el-form-item-tip" style="color: #909399; font-size: 12px;">
              请求体分页沿用下方当前模式的参数名作为请求体键名，无需单独配置；请求体支持 ${参数名} 占位替换，
              如 {"pageNo": "${pageNo}", "size": "${size}"}。未使用占位符时替换同名键，不存在则新增
            </span>
          </el-form-item>
          <el-form-item v-if="pageConfig.paramLocation === 'QUERY'">
            <span class="el-form-item-tip" style="color: #909399; font-size: 12px;">
              查询参数中已有同名参数时按当前页取值替换（URL 查询串与查询参数表单同时存在时只保留一份），不会产生重复参数
            </span>
          </el-form-item>
          <el-form-item v-if="pageConfig.paramLocation === 'PATH'">
            <span class="el-form-item-tip" style="color: #909399; font-size: 12px;">
              路径分页在 URL 中用 ${参数名} 占位，如 /list/${page}/${size}；未占位的参数将按查询参数追加
            </span>
          </el-form-item>

          <template v-if="pageConfig.mode === 'PAGE_NUMBER'">
            <el-form-item label="页号参数">
              <el-input v-model="pageConfig.pageParam" placeholder="page" style="width: 200px;" />
            </el-form-item>
            <el-form-item label="每页条数参数">
              <el-input v-model="pageConfig.sizeParam" placeholder="size" style="width: 200px;" />
            </el-form-item>
          </template>

          <template v-else-if="pageConfig.mode === 'OFFSET'">
            <el-form-item label="偏移参数">
              <el-input v-model="pageConfig.offsetParam" placeholder="offset" style="width: 200px;" />
            </el-form-item>
            <el-form-item label="上限参数">
              <el-input v-model="pageConfig.limitParam" placeholder="limit" style="width: 200px;" />
              <span class="el-form-item-tip" style="color: #909399; font-size: 12px;">
                上限参数的名称；每次取多少条由下方「每页条数」决定
              </span>
            </el-form-item>
          </template>

          <template v-else-if="pageConfig.mode === 'CURSOR'">
            <el-form-item label="游标参数">
              <el-input v-model="pageConfig.cursorParam" placeholder="cursor" style="width: 200px;" />
            </el-form-item>
            <el-form-item label="下一页游标路径">
              <el-input v-model="pageConfig.cursorPath" placeholder="$.next_cursor" style="width: 200px;" />
              <span class="el-form-item-tip" style="color: #909399; font-size: 12px;">
                从响应中提取下一个游标所用的 JSON 路径
              </span>
            </el-form-item>
            <el-form-item label="初始游标">
              <el-input v-model="pageConfig.initialCursor" placeholder="留空表示从首次请求开始" style="width: 200px;" />
            </el-form-item>
          </template>

          <template v-if="pageConfig.mode !== 'CURSOR'">
            <el-form-item label="每页条数">
              <el-input-number v-model="pageConfig.pageSize" :min="1" :max="10000" />
            </el-form-item>
            <el-form-item label="总页数">
              <el-input-number v-model="pageConfig.totalPages" :min="0" :max="100000" />
              <span class="el-form-item-tip" style="color: #909399; font-size: 12px;">>0 时按总页数结束</span>
            </el-form-item>
            <el-form-item label="按批大小停止">
              <el-switch v-model="pageConfig.stopByBatchSize" />
              <span class="el-form-item-tip" style="color: #909399; font-size: 12px;">开启后：本页条数 < 每页条数即结束</span>
            </el-form-item>
          </template>

          <el-form-item label="请求间隔(ms)">
            <el-input-number v-model="pageConfig.requestIntervalMs" :min="0" :step="100" />
          </el-form-item>
          <el-form-item label="最大页数">
            <el-input-number v-model="pageConfig.maxPages" :min="1" :max="999999" />
          </el-form-item>
        </template>

        <!-- 数据转换 -->
        <el-divider content-position="left">数据转换</el-divider>
        <groovy-script-editor
          v-model="form.convertScript"
          :api-config-id="form.id"
          :config-transform-type="form.transformType"
          :config-convert-script="form.convertScript"
          @transform-type-change="(val) => form.transformType = val"
        />

        <!-- 响应配置：试跑位于本区首位；响应参数在试跑成功前不可编辑 -->
        <el-divider content-position="left">响应配置</el-divider>
        <el-form-item>
          <el-button type="success" @click="handleTest" :loading="testing">
            {{ testing ? '请求中…' : '发起连接测试' }}
          </el-button>
          <span class="el-form-item-tip" style="color: #909399; font-size: 12px;">
            依据上方请求配置发起一次真实请求，然后在此配置响应参数
          </span>
        </el-form-item>

        <el-alert
          v-if="!responseConfigUnlocked"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 12px;"
        >
          <template slot="title">
            响应参数需试跑<strong>成功</strong>后开放。{{ gatingHint }}
          </template>
        </el-alert>

        <!-- 分页测试提示：启用分页时，本次试跑已按首页分页参数发起真实请求（design D5） -->
        <el-alert
          v-if="testResult && testResult.pageApplied"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 12px;"
        >
          <template slot="title">
            本次已按分页配置以<strong>首页</strong>参数发起真实请求：{{ appliedPageParamsLabel }}
          </template>
        </el-alert>

        <!-- 试跑结果面板：锁定状态下也展示，用户需要先看到响应才能判断该配什么 -->
        <el-form-item v-if="testResult">
          <api-response-panel
            :result="testResult"
            :detection="detectionSummary"
            :resp-type="form.respType"
            :selected-path="form.dataPath"
            :testing="testing"
            @select-path="onSelectPath"
            @retest="onTest"
          />
        </el-form-item>

        <el-form-item label="响应类型">
          <el-select v-model="form.respType" :disabled="!responseConfigUnlocked" style="width: 200px;">
            <el-option label="JSON" value="JSON" />
            <el-option label="XML" value="XML" />
          </el-select>
          <el-tag
            v-if="autoFilledFields.indexOf('响应类型') >= 0"
            type="success"
            size="mini"
            style="margin-left: 8px;"
          >已按响应头自动填充</el-tag>
          <span class="el-form-item-tip" style="color: #909399; font-size: 12px;">
            在线测试成功后会依据响应头自动填充，仍可手动改回
          </span>
        </el-form-item>

        <el-form-item label="数据提取路径">
          <el-input v-model="form.dataPath" :disabled="!responseConfigUnlocked" :placeholder="dataPathPlaceholder" />
          <span class="el-form-item-tip" style="color: #909399; font-size: 12px;">
            {{ dataPathHint }}
          </span>
        </el-form-item>

        <el-form-item label="多行响应">
          <el-switch
            v-model="form.enableMultilines"
            :disabled="!responseConfigUnlocked"
            :active-value="1"
            :inactive-value="0"
          />
          <el-tag
            v-if="autoFilledFields.indexOf('多行响应') >= 0"
            type="success"
            size="mini"
            style="margin-left: 8px;"
          >已按响应头自动填充</el-tag>
          <span class="el-form-item-tip" style="color: #909399; font-size: 12px;">开启后按行解析，每行一条 JSON/XML 记录</span>
        </el-form-item>

        <!-- 详情补全：列表→详情两跳采集（1:1 拍平） -->
        <el-divider content-position="left">详情补全</el-divider>
        <el-form-item label="启用详情补全">
          <el-switch v-model="form.detailEnabled" :active-value="1" :inactive-value="0" />
          <span class="el-form-item-tip" style="color: #909399; font-size: 12px;">
            按列表记录的键值逐条调用详情接口，并把详情字段并入记录（同名以详情为准），再写入目标表
          </span>
        </el-form-item>

        <template v-if="form.detailEnabled">
          <el-form-item label="列表键值来源">
            <el-input v-model="detailConfig.keyField" placeholder="留空/#this 取记录自身；或字段名 / $.id" style="width: 320px;" />
            <span class="el-form-item-tip" style="color: #909399; font-size: 12px;">
              列表返回编号/字符串数组时留空；对象数组填字段名（如 id）
            </span>
          </el-form-item>
          <el-form-item label="键值占位符">
            <el-input v-model="detailConfig.keyPlaceholder" placeholder="id" style="width: 200px;" />
            <span class="el-form-item-tip" style="color: #909399; font-size: 12px;">
              详情请求 URL/请求头/查询/请求体里用 ${id} 引用该键值
            </span>
          </el-form-item>
          <el-form-item label="详情请求 URL">
            <el-input v-model="detailConfig.url" placeholder="https://api.example.com/detail/${id}" />
          </el-form-item>
          <el-form-item label="请求方法">
            <el-select v-model="detailConfig.method" style="width: 200px;">
              <el-option label="GET" value="GET" />
              <el-option label="POST" value="POST" />
            </el-select>
          </el-form-item>
          <el-form-item label="请求头">
            <key-value-editor v-model="detailConfig.headers" key-placeholder="Header 名" value-placeholder="Header 值" />
          </el-form-item>
          <el-form-item label="查询参数">
            <key-value-editor v-model="detailConfig.queryParams" key-placeholder="参数名" value-placeholder="参数值" />
          </el-form-item>
          <template v-if="detailConfig.method !== 'GET'">
            <el-form-item label="请求体类型">
              <el-select v-model="detailConfig.bodyType" style="width: 200px;">
                <el-option label="NONE" value="NONE" />
                <el-option label="JSON" value="JSON" />
                <el-option label="FORM" value="FORM" />
                <el-option label="XML" value="XML" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="detailConfig.bodyType === 'FORM'" label="Body 内容">
              <key-value-editor v-model="detailConfig.formParams" key-placeholder="参数名" value-placeholder="参数值" />
              <span class="el-form-item-tip" style="color: #909399; font-size: 12px;">
                值中可用 ${id} 引用列表键值（id 为上方「键值占位符」配置的名称）；正文按表单编码保存，占位符编码后仍可识别
              </span>
            </el-form-item>
            <el-form-item v-else-if="detailConfig.bodyType !== 'NONE'" label="Body 内容">
              <el-input v-model="detailConfig.bodyContent" type="textarea" :rows="4" placeholder="请求体内容，可用 ${id}" />
            </el-form-item>
          </template>
          <el-form-item label="详情响应类型">
            <el-select v-model="detailConfig.respType" style="width: 200px;">
              <el-option label="JSON" value="JSON" />
              <el-option label="XML" value="XML" />
            </el-select>
          </el-form-item>
          <el-form-item label="详情数据路径">
            <el-input v-model="detailConfig.itemPath" placeholder="$.data（JSON 用 JSONPath；XML 用 XPath）" />
            <span class="el-form-item-tip" style="color: #909399; font-size: 12px;">
              从详情响应中取出详情对象的路径；留空取整个响应
            </span>
          </el-form-item>
          <el-form-item label="并发数">
            <el-input-number v-model="detailConfig.concurrency" :min="1" :max="32" />
            <span class="el-form-item-tip" style="color: #909399; font-size: 12px;">对详情接口的并发请求上限，默认 4</span>
          </el-form-item>

          <el-form-item label="补全试跑">
            <el-input v-model="detailSampleKey" placeholder="用于试跑的样例键值（如某个 id）" style="width: 240px; margin-right: 8px;" />
            <el-button type="success" :loading="detailTesting" @click="onTestDetail">发起补全试跑</el-button>
            <span class="el-form-item-tip" style="color: #909399; font-size: 12px;">
              以样例键值发起一次真实详情请求，展示详情内容与合并结果
            </span>
          </el-form-item>

          <template v-if="detailTestResult">
            <el-alert v-if="!detailTestResult.success" type="warning" :closable="false" show-icon style="margin-bottom: 12px;">
              <template slot="title">补全试跑失败：{{ detailTestResult.error }}</template>
            </el-alert>
            <el-form-item v-if="detailTestResult.detail" label="详情响应">
              <json-preview :data="detailTestResult.detail" />
            </el-form-item>
            <el-form-item v-if="detailTestResult.merged" label="合并结果">
              <json-preview :data="detailTestResult.merged" />
              <span class="el-form-item-tip" style="color: #909399; font-size: 12px;">
                _key 为本次试跑使用的键值；详情字段并入后，同名以详情为准
              </span>
            </el-form-item>
          </template>
        </template>

        <!-- 备注 -->
        <el-divider content-position="left">其他</el-divider>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息" />
        </el-form-item>

        <!-- 操作按钮：试跑已上移至响应配置之前，此处仅保留保存与取消 -->
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
          <el-button @click="$router.push('/api-config/list')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { getConfigDetail, createConfig, updateConfig, testConfig, testConfigTransient, testDetailConfig, testDetailConfigTransient } from '@/api/config'
import { getAuthConfigList } from '@/api/authConfig'
import { extractQuery, mergeParams } from '@/utils/urlQuery'
import { isResponseConfigUnlocked } from '@/utils/responseGating'
import { serializeFormParams, parseFormParams } from '@/utils/formBody'
import { validateBodyContent } from '@/utils/bodyContentValidation'
import ApiResponsePanel from '@/components/ApiResponsePanel'
import GroovyScriptEditor from '@/components/GroovyScriptEditor'
import KeyValueEditor from '@/components/KeyValueEditor'
import JsonPreview from '@/components/JsonPreview'

/**
 * 分页配置的完整默认集合。
 * 5 个参数名的默认值与后端 PageCollector 的兜底值逐字一致：
 *   offsetParam/limitParam/cursorParam -> str(pc.getXxxParam(), "offset"/"limit"/"cursor")
 *   cursorPath                         -> str(pc.getCursorPath(), "$.next_cursor")
 *   initialCursor                      -> Optional.ofNullable(pc.getInitialCursor()).orElse("")
 * 抽为常量以保证 data() 与 loadDetail 共用同一份定义；加载已存配置时合并到本常量之上，
 * 使全部字段存在且响应（否则 Vue 2 下新增属性非响应，v-model 写入不生效）。
 */
const DEFAULT_PAGE_CONFIG = {
  mode: 'PAGE_NUMBER',
  paramLocation: 'QUERY',
  pageParam: 'page',
  sizeParam: 'size',
  offsetParam: 'offset',
  limitParam: 'limit',
  cursorParam: 'cursor',
  cursorPath: '$.next_cursor',
  pageSize: 50,
  totalPages: 0,
  stopByBatchSize: true,
  requestIntervalMs: 0,
  maxPages: 99999,
  initialCursor: ''
}

/**
 * 详情补全配置的完整默认集合（对齐后端 DetailConfig）。
 * 抽为常量并在 data()/loadDetail 复用，保证全部键存在——Vue 2 下新增属性非响应，v-model 写入不生效。
 */
const DEFAULT_DETAIL_CONFIG = {
  enabled: 1,
  keyField: '',
  keyPlaceholder: 'id',
  url: '',
  method: 'GET',
  headers: {},
  queryParams: {},
  bodyType: 'NONE',
  bodyContent: '',
  formParams: {},
  respType: 'JSON',
  itemPath: '',
  concurrency: 4
}

export default {
  name: 'ApiConfigEdit',
  components: { ApiResponsePanel, GroovyScriptEditor, KeyValueEditor, JsonPreview },
  data() {
    return {
      isEdit: false,
      saving: false,
      testing: false,
      testResult: null,
      /** JSON/XML 正文格式校验的即时错误（design D10 / B9）；空串表示无错误 */
      bodyContentError: '',
      /** 试跑代次：配置变更时递增，用于丢弃「请求飞行中配置又被改」的过期响应（design C6） */
      testToken: 0,
      /** 本次会话是否已发起过试跑（在请求结束后置位，避免飞行中误判为「已试跑」） */
      hasTested: false,
      /** 解锁记忆位：一旦解锁即保持，避免后续失败把用户正在输入的内容重新锁上（design C2） */
      responseConfigUnlockedEver: false,
      autoFilledFields: [],
      detectionSummary: null,
      form: {
        name: '',
        url: '',
        method: 'GET',
        headers: {},
        queryParams: {},
        bodyType: 'NONE',
        bodyContent: '',
        formParams: {},
        connectTimeout: 10,
        readTimeout: 60,
        authConfigId: null,
        respType: 'JSON',
        dataPath: '',
        enableMultilines: 0,
        pageEnabled: 0,
        pageConfig: '',
        detailEnabled: 0,
        convertScript: '',
        transformType: 'FIELD_MAPPING',
        status: 1,
        remark: ''
      },
      pageConfig: { ...DEFAULT_PAGE_CONFIG },
      detailConfig: { ...DEFAULT_DETAIL_CONFIG },
      /** 补全试跑：样例键值、进行中标志、结果 */
      detailSampleKey: '',
      detailTesting: false,
      detailTestResult: null,
      /** 可选共享认证配置（下拉数据源） */
      authConfigOptions: [],
      /**
       * 常用请求头清单（design P1）。
       * 不含 Authorization——认证头已由认证机制产出，自定义同名头会与之并存两份。
       */
      headerPresets: [
        { label: 'Accept', key: 'Accept', value: 'application/json' },
        { label: 'User-Agent', key: 'User-Agent', value: 'Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0' },
        { label: 'Cache-Control', key: 'Cache-Control', value: 'no-cache' }
      ],
      rules: {
        name: [
          { required: true, message: '请输入接口名称', trigger: 'blur' },
          { max: 100, message: '接口名称不能超过 100 个字符', trigger: 'blur' }
        ],
        url: [{ required: true, message: '请输入接口 URL', trigger: 'blur' }],
        method: [{ required: true, message: '请选择请求方法', trigger: 'change' }],
        connectTimeout: [{ required: true, message: '请输入连接超时', trigger: 'blur' }],
        readTimeout: [{ required: true, message: '请输入读取超时', trigger: 'blur' }]
      }
    }
  },
  computed: {
    // 路径语言由响应类型决定，提示跟随当前选择，避免用户自行推断（design C9）
    dataPathPlaceholder() {
      return this.form.respType === 'XML' ? '/root/row（XML 用 XPath）' : '$.data.list[*]（JSON 用 JSONPath）'
    },
    dataPathHint() {
      if (this.form.respType === 'XML') {
        return '用于取出“记录集合”，每条记录映射为一行；当前响应类型为 XML，此处使用 XPath。留空则取整个文档根作为一条记录。'
      }
      return '用于取出“记录集合”，每条记录映射为一行；当前响应类型为 JSON，此处使用 JSONPath。留空则将整个响应作为一条记录。数组嵌套需用 [*] 展平（如 $.data[*].items[*]）。'
    },
    /**
     * 响应配置是否可编辑（design C2）。
     * 记忆位优先：解锁后保持解锁，避免后续失败重新锁上而打断输入。
     */
    responseConfigUnlocked() {
      if (this.responseConfigUnlockedEver) return true
      return isResponseConfigUnlocked({
        hasTested: this.hasTested,
        success: !!(this.testResult && this.testResult.success === true),
        detectedRespType: this.testResult ? this.testResult.detectedRespType : null,
        alreadyUnlocked: false
      })
    },
    /**
     * 锁定原因说明（design C3）。
     * 若判定出了格式，必须告知格式已被自动纠正、重试即可解锁——否则用户会卡在
     * 「失败→仍锁定」而不知道下一步该做什么，这正是死锁的可见形态。
     */
    gatingHint() {
      if (this.responseConfigUnlocked) return ''
      const detected = this.testResult ? this.testResult.detectedRespType : null
      if (this.hasTested && detected) {
        return `响应头判定为 ${detected}，已自动改用该格式；请重新试跑以解锁。`
      }
      return '请先发起连接测试并成功拿到响应。'
    },
    /** 首页分页参数的可读展示（name=value，空值标注为空） */
    appliedPageParamsLabel() {
      const params = this.testResult && this.testResult.appliedPageParams
      if (!params) return ''
      const entries = Object.entries(params)
      if (!entries.length) return ''
      return entries.map(([name, value]) => `${name}=${value === '' ? '(空)' : value}`).join('、')
    }
  },
  /**
   * 参与请求的配置任一变更即清空旧响应（design C6）。
   * 刻意**不**监听响应类型与多行开关：它们是配置本身，且会被判定结论自动填充，
   * 监听它们会在试跑后立刻把自己刚填充的结果清掉。
   */
  watch: {
    'form.url': 'onRequestConfigChanged',
    'form.method': 'onRequestConfigChanged',
    'form.bodyType': 'onBodyTypeChanged',
    'form.bodyContent': 'onBodyContentChanged',
    'form.formParams': { handler: 'onRequestConfigChanged', deep: true },
    'form.connectTimeout': 'onRequestConfigChanged',
    'form.readTimeout': 'onRequestConfigChanged',
    'form.authConfigId': 'onRequestConfigChanged',
    'form.headers': { handler: 'onRequestConfigChanged', deep: true },
    'form.queryParams': { handler: 'onRequestConfigChanged', deep: true }
  },
  created() {
    this.loadAuthConfigs()
    const id = this.$route.params.id
    if (id) {
      this.isEdit = true
      this.loadDetail(id)
    }
  },
  methods: {
    async loadDetail(id) {
      try {
        const res = await getConfigDetail(id)
        const data = res.data || res
        Object.assign(this.form, data)
        this.form.headers = data.headers || {}
        this.form.queryParams = data.queryParams || {}
        this.form.authConfigId = data.authConfigId != null ? data.authConfigId : null
        // FORM 正文以编码串存储，回显时还原为键值对象（design D2）
        this.form.formParams = data.bodyType === 'FORM' ? parseFormParams(data.bodyContent || '') : {}
        // 超时留空回显默认值，避免「显示空但实际 10/60」的认知错位（design D6）
        this.form.connectTimeout = data.connectTimeout != null ? data.connectTimeout : 10
        this.form.readTimeout = data.readTimeout != null ? data.readTimeout : 60
        if (data.pageConfig) {
          this.pageConfig = { ...DEFAULT_PAGE_CONFIG, ...data.pageConfig }
        }
        if (data.detailConfig) {
          this.detailConfig = { ...DEFAULT_DETAIL_CONFIG, ...data.detailConfig }
          // 详情 FORM 正文同样以编码串存储，回显时必须还原为键值对象；
          // 否则编辑器为空，再次保存会把 bodyContent 覆盖成空串（静默清空详情请求体）
          this.detailConfig.formParams = this.detailConfig.bodyType === 'FORM'
            ? parseFormParams(this.detailConfig.bodyContent || '')
            : {}
          this.form.detailEnabled = data.detailConfig.enabled === 1 ? 1 : 0
        }
      } catch {
        this.$message.error('加载接口配置失败')
      }
    },
    onHeaderPresetExists(info) {
      this.$message.warning(`请求头「${info.label}」已存在，请直接修改其值`)
    },
    async loadAuthConfigs() {
      try {
        const res = await getAuthConfigList({ pageNum: 1, pageSize: 500 })
        this.authConfigOptions = res.data?.list || res.data || []
      } catch {
        this.authConfigOptions = []
      }
    },
    typeLabel(type) {
      const map = {
        BASIC: 'Basic',
        BEARER: 'Bearer',
        OAUTH2: 'OAuth2',
        CUSTOM: '自定义Header',
        PASSWORD_TOKEN: '账号密码换Token'
      }
      return map[type] || type || ''
    },
    /**
     * URL 查询串抽取：拆入查询参数区并从 URL 移除，使参数区成为唯一事实来源
     * （design C4 / D3）。否则同一参数会在最终请求中出现两次。
     * 仅在失焦或回车时触发，不逐键触发，避免边打字边拆解。
     */
    onUrlBlur() {
      const raw = this.form.url
      if (!raw || raw.indexOf('?') < 0) return
      let extracted
      try {
        extracted = extractQuery(raw)
      } catch {
        return // 抽取异常时保留 URL 原样，绝不半拆（design C6）
      }
      if (!extracted || extracted.params.length === 0) {
        if (this.form.url !== extracted.baseUrl) this.form.url = extracted.baseUrl
        return
      }

      const existing = Object.entries(this.form.queryParams || {}).map(([key, value]) => ({ key, value: String(value) }))
      const { toAdd, skipped } = mergeParams(existing, extracted.params)
      if (toAdd.length) {
        const next = { ...(this.form.queryParams || {}) }
        toAdd.forEach(p => { next[p.key] = p.value })
        this.form.queryParams = next
      }
      this.form.url = extracted.baseUrl

      const moved = toAdd.map(p => p.key).join('、')
      this.$message.success(
        skipped.length
          ? `参数已移入查询参数区：${moved}；已存在而跳过：${skipped.join('、')}`
          : `参数已移入查询参数区：${moved}`
      )
    },
    /**
     * 响应树点选节点 → 回填数据提取路径（design D1 / C3）。
     * 必须给出即时反馈：路径输入框位于面板之下，面板较高时会落在屏幕外，
     * 若只静默赋值，用户会以为「点了没反应」。
     */
    onSelectPath(payload) {
      if (!payload) return
      if (payload.path) {
        this.form.dataPath = payload.path
        this.$message.success(`已填入数据提取路径：${payload.path}`)
      } else {
        this.form.dataPath = ''
        this.$message.info('已清除数据提取路径')
      }
    },
    toObject(value) {
      if (value === null || value === undefined || value === '') return null
      if (typeof value === 'object') return value
      try { return JSON.parse(value) } catch { return null }
    },
    buildPayload() {
      const payload = { ...this.form }
      payload.headers = this.toObject(this.form.headers)
      payload.queryParams = this.toObject(this.form.queryParams)
      payload.authConfigId = this.form.authConfigId || null
      payload.pageConfig = this.form.pageEnabled ? this.pageConfig : null
      // 详情补全：始终携带（enabled 反映开关状态），使「关闭」也能覆盖并持久化旧配置
      const detail = { ...this.detailConfig, enabled: this.form.detailEnabled ? 1 : 0 }
      if (detail.bodyType === 'FORM') {
        detail.bodyContent = serializeFormParams(detail.formParams || {})
      }
      delete detail.formParams
      payload.detailConfig = detail
      delete payload.detailEnabled
      // FORM 正文以标准表单编码串落库；键值对象是前端表示，不发给后端（design D1/D2）
      if (this.form.bodyType === 'FORM') {
        payload.bodyContent = serializeFormParams(this.form.formParams)
      }
      delete payload.formParams
      return payload
    },
    async handleSave() {
      const valid = await this.$refs.form.validate()
      if (!valid) return
      this.revalidateBody()
      if (this.bodyContentError) {
        this.$message.error(this.bodyContentError)
        return
      }
      this.saving = true
      try {
        const submitData = this.buildPayload()
        if (this.isEdit) {
          await updateConfig(this.$route.params.id, submitData)
          this.$message.success('更新成功')
        } else {
          await createConfig(submitData)
          this.$message.success('创建成功')
        }
        this.$router.push('/api-config/list')
      } catch (e) {
        // 错误已由 request 拦截器处理
      } finally {
        this.saving = false
      }
    },
    async onTest() {
      // 正文格式非法时不试跑，避免把明显错误的内容发出去（design D10 / B9）
      this.revalidateBody()
      if (this.bodyContentError) {
        this.$message.error(this.bodyContentError)
        return
      }
      // 记录本次试跑实际使用的设置：判定结论不能替换它，用户改回后重新试跑须以改后值为准（design C5）
      const usedRespType = this.form.respType
      const usedMultilines = this.form.enableMultilines
      this.clearTestResult()
      // 捕获本次代次：请求飞行期间配置若又被改动，代次会前进，过期响应将被丢弃
      const token = this.testToken
      this.testing = true
      try {
        const testData = this.buildPayload()
        const id = this.$route.params.id
        const res = id ? await testConfig(id, testData) : await testConfigTransient(testData)
        if (token !== this.testToken) return
        const result = res.data || res
        this.testResult = result
        this.applyDetection(result, usedRespType, usedMultilines)
      } catch {
        // 错误由拦截器处理
      } finally {
        this.testing = false
        // 在请求结束后才置位：飞行中若置位会被判成「已试跑且无判定」而提前解锁
        this.hasTested = true
        this.syncResponseGating()
      }
    },
    /**
     * 刷新解锁记忆位（design C2）。
     * 注意：这里**不得**因未解锁而把记忆位清零——解锁后保持解锁是刻意的。
     */
    syncResponseGating() {
      if (this.responseConfigUnlockedEver) return
      if (this.responseConfigUnlocked) {
        this.responseConfigUnlockedEver = true
      }
    },
    /**
     * 清空试跑产物（design C6）。同时推进代次，使飞行中的请求失效。
     * 不清空响应类型、多行开关与已填的数据提取路径——它们是配置本身，不是试跑结果。
     */
    clearTestResult() {
      this.testToken += 1
      this.testResult = null
      this.detectionSummary = null
      this.autoFilledFields = []
    },
    /** 参与请求的配置任一变更即清空旧响应，避免按过期数据配路径 */
    onRequestConfigChanged() {
      if (this.testResult || this.detectionSummary) {
        this.clearTestResult()
      } else {
        // 无结果可清时仍推进代次，确保飞行中的请求不会把结果写回来
        this.testToken += 1
      }
    },
    /** 校验 JSON/XML 正文格式，结果写入 bodyContentError（design D10 / B9） */
    revalidateBody() {
      this.bodyContentError = validateBodyContent(this.form.bodyType, this.form.bodyContent)
    },
    /** Body 类型变化：切换输入形态，并清掉上一类型的错误提示 */
    onBodyTypeChanged() {
      this.bodyContentError = ''
      this.onRequestConfigChanged()
    },
    /** 正文变化：已报错时即时复检（改对即清除，不逐字打扰），并清空旧试跑结果 */
    onBodyContentChanged() {
      if (this.bodyContentError) {
        this.revalidateBody()
      }
      this.onRequestConfigChanged()
    },
    /**
     * 依据响应头判定结果填充表单（design C4）。
     * 判定不出时保持用户现有设置不动，不写任何默认值（design C3）。
     */
    applyDetection(result, usedRespType, usedMultilines) {
      const detectedType = result ? result.detectedRespType : null
      const detectedMultilines = result ? result.detectedMultilines : null
      const filled = []

      if (detectedType && detectedType !== this.form.respType) {
        this.form.respType = detectedType
        filled.push('响应类型')
      }
      // 多行结论为 null 表示响应头未能判定（例如普通 application/json），此时不得改动开关
      if (detectedMultilines !== null && detectedMultilines !== undefined
        && Number(detectedMultilines) !== Number(this.form.enableMultilines)) {
        this.form.enableMultilines = Number(detectedMultilines)
        filled.push('多行响应')
      }
      this.autoFilledFields = filled

      // 冲突判定用「本次试跑实际使用的值」比较，而非已被自动填充后的当前值
      const conflict = !!detectedType && detectedType !== usedRespType
      this.detectionSummary = {
        contentType: result ? result.contentType : null,
        detectedRespType: detectedType,
        detectedMultilines,
        usedRespType,
        usedMultilines,
        autoFilled: filled,
        conflict,
        success: result ? !!result.success : false
      }

      this.notifyDetection(detectedType, filled, conflict)
    },
    notifyDetection(detectedType, filled, conflict) {
      const summary = this.detectionSummary
      if (!detectedType) {
        const reason = summary && summary.contentType
          ? `（响应头声明为 ${summary.contentType}，无法对应到已有格式）`
          : '（响应未声明内容类型）'
        this.$message.warning(`未能自动识别响应类型${reason}，已保留当前设置，请手动确认`)
        return
      }
      if (filled.length) {
        this.$message.success(`已根据响应头自动填充：${filled.join('、')}`)
        return
      }
      if (conflict) {
        this.$message.warning(`响应头判定为 ${detectedType}，与本次使用的设置不一致，请确认`)
        return
      }
      this.$message.success(`响应头判定为 ${detectedType}，与当前设置一致`)
    },
    handleTest() {
      this.onTest()
    },
    async onTestDetail() {
      const key = String(this.detailSampleKey || '').trim()
      if (!key) {
        this.$message.warning('请输入用于试跑的样例键值')
        return
      }
      this.detailTesting = true
      try {
        const payload = { ...this.buildPayload(), sampleKey: key }
        const id = this.$route.params.id
        const res = id ? await testDetailConfig(id, payload) : await testDetailConfigTransient(payload)
        this.detailTestResult = res.data || res
      } catch {
        // 错误由拦截器处理
      } finally {
        this.detailTesting = false
      }
    }
  }
}
</script>
