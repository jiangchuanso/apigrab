<template>
  <div class="user-profile">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <div slot="header" class="rc-card-header"><span class="rc-card-header__title">个人资料</span></div>
          <el-form ref="profileForm" :model="profileForm" label-width="90px" size="small">
            <el-form-item label="用户名">
              <el-input v-model="profileForm.username" disabled />
            </el-form-item>
            <el-form-item label="角色">
              <el-tag :type="profileForm.role === 'ADMIN' ? 'danger' : 'info'" size="mini">{{ roleText(profileForm.role) }}</el-tag>
            </el-form-item>
            <el-form-item label="真实姓名">
              <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="地址">
              <el-input v-model="profileForm.address" placeholder="请输入地址" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingProfile" @click="handleSaveProfile">保存</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <div slot="header" class="rc-card-header"><span class="rc-card-header__title">修改密码</span></div>
          <el-form ref="pwdForm" :model="pwdForm" :rules="pwdRules" label-width="90px" size="small">
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password autocomplete="current-password" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password autocomplete="new-password" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password autocomplete="new-password" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingPwd" @click="handleChangePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getUserInfo, updateProfile, changePassword } from '@/api/auth'

export default {
  name: 'UserProfile',
  data() {
    const validateConfirm = (rule, value, callback) => {
      if (value !== this.pwdForm.newPassword) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      savingProfile: false,
      savingPwd: false,
      profileForm: {
        username: '',
        role: '',
        realName: '',
        email: '',
        address: ''
      },
      pwdForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      pwdRules: {
        oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, message: '密码至少 6 位', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入新密码', trigger: 'blur' },
          { validator: validateConfirm, trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.loadProfile()
  },
  methods: {
    async loadProfile() {
      const res = await getUserInfo()
      const data = res.data || {}
      this.profileForm = {
        username: data.username || '',
        role: data.role || '',
        realName: data.realName || '',
        email: data.email || '',
        address: data.address || ''
      }
      this.$store.commit('SET_REALNAME', data.realName || '')
    },
    async handleSaveProfile() {
      this.savingProfile = true
      try {
        await updateProfile({
          realName: this.profileForm.realName,
          email: this.profileForm.email,
          address: this.profileForm.address
        })
        this.$message.success('资料已保存')
        this.$store.commit('SET_REALNAME', this.profileForm.realName)
      } finally {
        this.savingProfile = false
      }
    },
    handleChangePassword() {
      this.$refs.pwdForm.validate(async (valid) => {
        if (!valid) return
        this.savingPwd = true
        try {
          await changePassword({
            oldPassword: this.pwdForm.oldPassword,
            newPassword: this.pwdForm.newPassword
          })
          this.$message.success('密码已修改')
          this.pwdForm = { oldPassword: '', newPassword: '', confirmPassword: '' }
          this.$refs.pwdForm.clearValidate()
        } finally {
          this.savingPwd = false
        }
      })
    },
    roleText(role) {
      const map = { ADMIN: '管理员', USER: '普通用户' }
      return map[role] || role || '-'
    }
  }
}
</script>
