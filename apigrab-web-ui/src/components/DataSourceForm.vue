<template>
  <div class="datasource-form">
        <el-form-item label="数据源名称" prop="name">
          <!-- 与列 rc_datasource_config.name VARCHAR(100) 对齐，避免超长导致存库报错（design C10） -->
          <el-input v-model="form.name" maxlength="100" show-word-limit placeholder="例如：生产 MySQL 库" />
        </el-form-item>

    <el-form-item label="数据库类型" prop="dbType">
      <el-select v-model="form.dbType" @change="onDbTypeChange" filterable style="width: 260px;">
        <el-option v-for="t in dbTypes" :key="t.value" :label="t.label" :value="t.value" />
      </el-select>
    </el-form-item>

    <template v-if="form.dbType">
      <el-form-item label="JDBC URL" prop="jdbcUrl">
        <el-input v-model="form.jdbcUrl" :placeholder="urlPlaceholder" type="textarea" :rows="2" />
      </el-form-item>

      <el-form-item label="驱动类" prop="driverClass">
        <el-input v-model="form.driverClass" :placeholder="driverPlaceholder" />
      </el-form-item>

      <el-form-item label="外部驱动" prop="driverPath">
        <div class="ds-driver-row">
          <el-select v-model="selectedDriverPath" placeholder="选择已放置的驱动包" clearable
                     style="width: 280px;" @change="onDriverPackageChange">
            <el-option v-for="p in availableDrivers" :key="p.version"
                       :label="driverPackageLabel(p)" :value="p.driverPath" />
          </el-select>
          <el-button size="mini" @click="loadMetadata(true)">刷新驱动</el-button>
        </div>
        <el-input v-model="form.driverPath" class="ds-driver-path"
                  placeholder="驱动 jar 或其所在目录，多个用逗号分隔；留空则使用应用内置驱动" />
        <div v-if="currentType && currentType.externalDriver" class="ds-tip">
          该数据库驱动未随应用 classpath 打包，请选择上方驱动包，或填写 drivers 目录中对应 jar 的路径。
        </div>
      </el-form-item>

      <el-form-item v-if="showDbVersion" label="数据库版本" prop="dbVersion">
        <el-input v-model="form.dbVersion" placeholder="如：19c / DM8（选填，仅作备注）" />
      </el-form-item>

      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="数据库用户名" />
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" placeholder="数据库密码" show-password />
      </el-form-item>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="最大连接数" prop="poolMaxSize">
            <el-input-number v-model="form.poolMaxSize" :min="1" :max="200" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="最小空闲" prop="poolMinIdle">
            <el-input-number v-model="form.poolMinIdle" :min="0" :max="100" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息" />
      </el-form-item>
    </template>
  </div>
</template>

<script>
import { getDbTypes, getDriverPackages, refreshDriverPackages } from '@/api/datasource'

// 后端 /api/datasource-meta/db-types 不可用时的兜底清单；正常情况以后端 DbType 枚举为准。
const FALLBACK_DB_TYPES = [
  { value: 'MYSQL', label: 'MySQL', driverClass: 'com.mysql.cj.jdbc.Driver', urlSample: 'jdbc:mysql://localhost:3306/your_database', externalDriver: false },
  { value: 'ORACLE', label: 'Oracle', driverClass: 'oracle.jdbc.OracleDriver', urlSample: 'jdbc:oracle:thin:@localhost:1521:your_sid', externalDriver: false },
  { value: 'SQLSERVER', label: 'SQL Server', driverClass: 'com.microsoft.sqlserver.jdbc.SQLServerDriver', urlSample: 'jdbc:sqlserver://localhost:1433;databaseName=your_database', externalDriver: false },
  { value: 'POSTGRESQL', label: 'PostgreSQL', driverClass: 'org.postgresql.Driver', urlSample: 'jdbc:postgresql://localhost:5432/your_database', externalDriver: false },
  { value: 'DM', label: '达梦 DM', driverClass: 'dm.jdbc.driver.DmDriver', urlSample: 'jdbc:dm://localhost:5236/your_database', externalDriver: true },
  { value: 'KINGBASE', label: '人大金仓 KingbaseES', driverClass: 'com.kingbase8.Driver', urlSample: 'jdbc:kingbase8://localhost:54321/your_database', externalDriver: true },
  { value: 'OSCAR', label: '神通 Oscar', driverClass: 'com.oscar.Driver', urlSample: 'jdbc:oscar://localhost:2003/OSCRDB', externalDriver: true },
  { value: 'GBASE8A', label: '南大通用 GBase8A', driverClass: 'com.gbase.jdbc.Driver', urlSample: 'jdbc:gbase://localhost:5258/your_database', externalDriver: true },
  { value: 'HIGHGO', label: '瀚高 HighGo', driverClass: 'com.highgo.jdbc.Driver', urlSample: 'jdbc:highgo://localhost:5866/your_database', externalDriver: true },
  { value: 'OPENGAUSS', label: 'openGauss', driverClass: 'org.opengauss.Driver', urlSample: 'jdbc:opengauss://localhost:15432/your_database', externalDriver: true },
  { value: 'OCEANBASE', label: 'OceanBase（MySQL 模式）', driverClass: 'com.oceanbase.jdbc.Driver', urlSample: 'jdbc:oceanbase://localhost:2881/your_database', externalDriver: true },
  { value: 'UXDB', label: '优炫 UXDB', driverClass: 'com.uxsino.uxdb.Driver', urlSample: 'jdbc:uxdb://localhost:52025/your_database', externalDriver: true }
]

export default {
  name: 'DataSourceForm',
  props: {
    value: {
      type: Object,
      default: () => ({
        name: '',
        dbType: '',
        jdbcUrl: '',
        driverClass: '',
        dbVersion: '',
        username: '',
        password: '',
        poolMaxSize: 10,
        poolMinIdle: 5,
        remark: ''
      })
    }
  },
  data() {
    return {
      dbTypes: FALLBACK_DB_TYPES,
      driverPackages: [],
      selectedDriverPath: ''
    }
  },
  computed: {
    form: {
      get() { return this.value },
      set(val) { this.$emit('input', val) }
    },
    currentType() {
      return this.dbTypes.find(t => t.value === this.form.dbType)
    },
    urlPlaceholder() {
      return this.currentType ? this.currentType.urlSample : ''
    },
    driverPlaceholder() {
      return this.currentType ? this.currentType.driverClass : ''
    },
    availableDrivers() {
      return this.driverPackages.filter(p => p.dbType === this.form.dbType)
    },
    showDbVersion() {
      return this.form.dbType === 'ORACLE' || Boolean(this.currentType && this.currentType.externalDriver)
    }
  },
  created() {
    this.loadMetadata()
  },
  methods: {
    async loadMetadata(force) {
      if (force) {
        try { await refreshDriverPackages() } catch { /* 刷新失败仍展示已有清单 */ }
      }
      try {
        const res = await getDbTypes()
        const types = res.data || res
        if (Array.isArray(types) && types.length) this.dbTypes = types
      } catch { /* 保留兜底清单 */ }
      try {
        const res = await getDriverPackages()
        this.driverPackages = res.data || res || []
      } catch { this.driverPackages = [] }
    },
    onDbTypeChange() {
      const cfg = this.currentType
      if (cfg) {
        this.form.driverClass = cfg.driverClass
        this.form.jdbcUrl = cfg.urlSample
      }
      this.selectedDriverPath = ''
      // 国产库自动带上第一个可用驱动包，减少手工填写路径
      if (cfg && cfg.externalDriver && !this.form.driverPath && this.availableDrivers.length) {
        this.form.driverPath = this.availableDrivers[0].driverPath
        this.selectedDriverPath = this.form.driverPath
      }
      this.$emit('db-type-change', this.form.dbType)
    },
    onDriverPackageChange(val) {
      if (val) this.form.driverPath = val
    },
    driverPackageLabel(p) {
      const jars = (p.jarFiles || []).join(', ')
      return `${p.version}${jars ? ' · ' + jars : ''}`
    }
  }
}
</script>

<style scoped>
.ds-driver-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.ds-driver-path {
  margin-top: 8px;
}
.ds-tip {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}
</style>
