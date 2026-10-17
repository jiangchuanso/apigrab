<template>
  <div class="datasource-form">
        <el-form-item label="数据源名称" prop="name">
          <!-- 与列 rc_datasource_config.name VARCHAR(100) 对齐，避免超长导致存库报错（design C10） -->
          <el-input v-model="form.name" maxlength="100" show-word-limit placeholder="例如：生产 MySQL 库" />
        </el-form-item>

    <el-form-item label="数据库类型" prop="dbType">
      <el-select v-model="form.dbType" @change="onDbTypeChange" style="width: 200px;">
        <el-option label="MySQL" value="MYSQL" />
        <el-option label="Oracle" value="ORACLE" />
        <el-option label="SQL Server" value="SQLSERVER" />
        <el-option label="PostgreSQL" value="POSTGRESQL" />
      </el-select>
    </el-form-item>

    <template v-if="form.dbType">
      <el-form-item label="JDBC URL" prop="jdbcUrl">
        <el-input v-model="form.jdbcUrl" :placeholder="urlPlaceholder" type="textarea" :rows="2" />
      </el-form-item>

      <el-form-item label="驱动类" prop="driverClass">
        <el-input v-model="form.driverClass" :placeholder="driverPlaceholder" disabled />
      </el-form-item>

      <el-form-item v-if="form.dbType === 'ORACLE'" label="数据库版本" prop="dbVersion">
        <el-input v-model="form.dbVersion" placeholder="如：19c (用于驱动管理)" />
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
const DB_CONFIG = {
  MYSQL: {
    driver: 'com.mysql.cj.jdbc.Driver',
    url: 'jdbc:mysql://localhost:3306/your_database?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai'
  },
  ORACLE: {
    driver: 'oracle.jdbc.OracleDriver',
    url: 'jdbc:oracle:thin:@localhost:1521:your_sid'
  },
  SQLSERVER: {
    driver: 'com.microsoft.sqlserver.jdbc.SQLServerDriver',
    url: 'jdbc:sqlserver://localhost:1433;databaseName=your_database'
  },
  POSTGRESQL: {
    driver: 'org.postgresql.Driver',
    url: 'jdbc:postgresql://localhost:5432/your_database'
  }
}

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
  computed: {
    form: {
      get() { return this.value },
      set(val) { this.$emit('input', val) }
    },
    urlPlaceholder() {
      const cfg = DB_CONFIG[this.form.dbType]
      return cfg ? cfg.url : ''
    },
    driverPlaceholder() {
      const cfg = DB_CONFIG[this.form.dbType]
      return cfg ? cfg.driver : ''
    }
  },
  methods: {
    onDbTypeChange() {
      const cfg = DB_CONFIG[this.form.dbType]
      if (cfg) {
        this.form.driverClass = cfg.driver
        this.form.jdbcUrl = cfg.url
      }
      this.$emit('db-type-change', this.form.dbType)
    }
  }
}
</script>
