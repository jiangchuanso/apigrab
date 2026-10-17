<template>
  <el-dialog
    title="关于"
    :visible.sync="dialogVisible"
    width="480px"
    custom-class="about-dialog">
    <div class="about">
      <div class="about__brand">
        <span class="about__mark" aria-hidden="true">AG</span>
        <div class="about__brand-text">
          <div class="about__name">ApiGrab</div>
          <div class="about__subtitle">接口数据采集入库系统</div>
        </div>
      </div>

      <p class="about__intro">
        ApiGrab 是一套配置化的接口数据采集入库平台。无需编写代码，即可对接各类 REST 接口，
        完成分页拉取、字段映射与多数据库写入，并提供任务调度、执行监控与失败告警能力。
      </p>

      <dl class="about__meta">
        <div class="about__meta-row">
          <dt class="about__meta-label">项目地址</dt>
          <dd class="about__meta-value">
            <a
              class="about__link"
              href="https://gitee.com/inrgihc/apigrab"
              target="_blank"
              rel="noopener noreferrer">
              https://gitee.com/inrgihc/apigrab
            </a>
          </dd>
        </div>
        <div v-if="displayVersion" class="about__meta-row">
          <dt class="about__meta-label">版本号</dt>
          <dd class="about__meta-value about__version">{{ displayVersion }}</dd>
        </div>
      </dl>
    </div>

    <div slot="footer">
      <el-button type="primary" @click="dialogVisible = false">关闭</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getVersion } from '@/api/version'
import { formatVersion, extractVersion } from '@/utils/version'

export default {
  name: 'AboutDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:visible'],
  data() {
    return {
      version: ''
    }
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible
      },
      set(value) {
        this.$emit('update:visible', value)
      }
    },
    displayVersion() {
      return formatVersion(this.version)
    }
  },
  watch: {
    visible(value) {
      if (value) {
        this.loadVersion()
      }
    }
  },
  methods: {
    async loadVersion() {
      try {
        const res = await getVersion()
        this.version = extractVersion(res)
      } catch (e) {
        this.version = ''
      }
    }
  }
}
</script>

<style scoped>
.about {
  color: var(--rc-text);
}

.about__brand {
  display: flex;
  align-items: center;
  gap: var(--rc-space-3);
  margin-bottom: var(--rc-space-4);
}

.about__mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 40px;
  height: 40px;
  border-radius: var(--rc-radius);
  background: linear-gradient(135deg, var(--rc-primary), var(--rc-primary-strong));
  color: #fff;
  font-size: var(--rc-text-base);
  font-weight: 700;
  letter-spacing: 0.5px;
}

.about__name {
  font-size: var(--rc-text-lg);
  font-weight: 600;
  line-height: 1.3;
}

.about__subtitle {
  font-size: var(--rc-text-sm);
  color: var(--rc-text-muted);
}

.about__intro {
  margin: 0 0 var(--rc-space-4);
  font-size: var(--rc-text-base);
  color: var(--rc-text-secondary);
  line-height: 1.6;
}

.about__meta {
  margin: 0;
}

.about__meta-row {
  display: flex;
  align-items: baseline;
  gap: var(--rc-space-3);
  padding: var(--rc-space-2) 0;
  border-top: 1px solid var(--rc-border);
}

.about__meta-label {
  flex: 0 0 auto;
  width: 68px;
  margin: 0;
  color: var(--rc-text-muted);
  font-size: var(--rc-text-sm);
}

.about__meta-value {
  margin: 0;
  min-width: 0;
}

.about__link {
  color: var(--rc-primary);
  word-break: break-all;
}

.about__link:hover {
  color: var(--rc-primary-hover);
}

.about__version {
  font-family: var(--rc-font-mono);
  font-size: var(--rc-text-sm);
}
</style>
