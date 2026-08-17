<script setup>
import { useRouter, useRoute } from 'vue-router'
import axios from "axios";
import { toFindCookie, addCookie, clearCookie } from "@/components/componentsJs/cookie";
import { connectWebSocket, disconnectWebSocket } from "@/services/websocket";

axios.defaults.baseURL = 'http://localhost:8080/api/v1/login'
axios.defaults.withCredentials = true;

const route = useRoute()
const router = useRouter()
const activeTab = ref('login')
const isLoggedIn = ref(false)
const loginForm = reactive(
  {
    account: '',
    password: '',
  }
)
const validateForm = reactive(
  {
    account: '',
  }
)
const loginFormNotOk = ref(
  {
    account: '',
    password: '',
  }
)
const registerForm = reactive(
  {
    name: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: '',
  }
)
const profileForm = reactive(
  {
    name: '',
    email: '',
    phone: '',
    birthday: '',
  }
)
const profileFormNotOk = ref(
  {
    phone: '',
    birthday: '',
  }
)
const registerFormNotOk = ref(
  {
    name: '',
    email: '',
    phone: '',
    password: '',
  }
)

executeFirst()
function executeFirst() {
  let isLoggedIn_ = false
  let is = route.query.isLoggedIn
  if (is != undefined) {
    if (is === 'false') {
      isLoggedIn_ = false
    }
  } else {
    let accessToken = toFindCookie('accessToken')
    if (accessToken) {
      isLoggedIn_ = true
    } else {
      isLoggedIn_ = false
    }
  }
  isLoggedIn.value = isLoggedIn_
  if (!isLoggedIn_) {
    clearCookie('accessToken')
    disconnectWebSocket()
  } else {
    connectWebSocket()
  }
}

const submitLogin = async () => {
  loginFormNotOk.value = {
    account: '',
    password: '',
  }
  if (
    !loginForm.account ||
    !loginForm.password
  ) return
  let judge = false
  try {
    const response = await axios({
      method: 'post',
      url: '/login',
      data: loginForm,
    });
    let data = response.data.data[0] ?? {}
    validateForm.account = data.account
    if (data.judge) {
      judge = true
    } else {
      ElMessage({
        type: 'info',
        message: `${'重新登入'}`,
      })
    }
    clearCookie('accessToken')
  } catch (error) {
    let data = error.response.data.data[1]?.error ?? {}
    loginFormNotOk.value = {
      account: data.account ?? '',
      password: data.password ?? '',
    }
  }
  let isLoggedIn_ = false
  if (judge) {
    activeTab.value = 'login'
    try {
      const response = await axios({
        method: 'post',
        url: '/validate',
        data: validateForm,
      });
      let data = response.data.data[0] ?? {}
      if (data.judge) {
        if (data.accessToken) {
          activeTab.value = 'profile'
          isLoggedIn_ = true
          addCookie('accessToken', data.accessToken)
          Object.assign(
            profileForm,
            {
              name: data.name,
              email: data.email,
              phone: data.phone,
              birthday: data.birthday
            }
          )
        }
      } else {
        ElMessage({
          type: 'info',
          message: `${'重新登入'}`,
        })
      }
    } catch (error) {
      let data = error.response.data.data[1]?.error ?? {}
      loginFormNotOk.value = {
        account: data.account ?? '',
        password: data.password ?? '',
      }
    }
  } else {
    activeTab.value = 'login'
    isLoggedIn_ = false
  }
  isLoggedIn.value = isLoggedIn_
  if (!isLoggedIn_) {
    disconnectWebSocket()
  } else {
    connectWebSocket()
  }
}

const submitRegister = async () => {
  registerFormNotOk.value = {
    name: '',
    email: '',
    phone: '',
    password: '',
  }
  if (
    !registerForm.name ||
    !registerForm.email ||
    !registerForm.password ||
    registerForm.password !== registerForm.confirmPassword
  ) return
  try {
    const response = await axios({
      method: 'post',
      url: '/register',
      data: registerForm,
    });
    let data = response.data.data[0] ?? {}
    Object.assign(
      profileForm,
      {
        name: data.name,
        email: data.email,
        phone: data.phone,
        birthday: data.birthday
      }
    )
    activeTab.value = 'login'
    ElMessage({
      type: 'success',
      message: `${'註冊成功'}`,
    })
  } catch (error) {
    let data = error.response.data.data[1]?.error ?? {}
    registerFormNotOk.value = {
      name: data.name ?? '',
      email: data.email ?? '',
      phone: data.phone ?? '',
      password: data.password ?? '',
    }
    activeTab.value = 'register'
  }
  isLoggedIn.value = false
  disconnectWebSocket()
}

const saveProfile = async () => {
  profileFormNotOk.value = {
    phone: '',
    birthday: ''
  }
  if (
    !profileForm.phone ||
    !profileForm.birthday
  ) return
  let isLoggedIn_ = false
  let accessToken = toFindCookie('accessToken')
  if (accessToken) {
    let judge = false
    try {
      const response = await axios({
        method: 'put',
        url: '/saveProfile',
        data: profileForm,
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      });
      let data = response.data.data[0] ?? {}
      if (data.judge) {
        ElMessage({
          type: 'success',
          message: `${'修改會員資料成功'}`,
        })
        isLoggedIn_ = true
      } else {
        ElMessage({
          type: 'info',
          message: `${'修改會員資料失敗'}`,
        })
        isLoggedIn_ = false
      }
    } catch (error) {
      let data = error.response.data.data[1]?.error ?? {}
      profileFormNotOk.value = {
        phone: data.phone ?? '',
        birthday: data.birthday ?? '',
      }
      isLoggedIn_ = false
    }
    activeTab.value = 'profile'
  } else {
    isLoggedIn_ = false
    activeTab.value = 'login';
  }
  isLoggedIn.value = isLoggedIn_
  if (!isLoggedIn_) {
    disconnectWebSocket()
  } else {
    connectWebSocket()
  }
}
const logout = async () => {
  let isLoggedIn_ = false
  let accessToken = toFindCookie('accessToken')
  if (accessToken) {
    try {
      const response = await axios({
        method: 'post',
        url: '/logout',
        data: {},
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      });
      let data = response.data.data[0] ?? {}
      if (data.judge) {
        ElMessage({
          type: 'success',
          message: `${'登出'}`,
        })
        Object.assign(
          profileForm,
          {
            name: '',
            email: '',
            phone: '',
            birthday: ''
          }
        )
        isLoggedIn_ = false
        activeTab.value = 'login'
        loginForm.account = '';
        loginForm.password = '';
      } else {
        isLoggedIn_ = true
      }
    } catch (error) {
      ElMessage({
        type: 'error',
        message: `${'登出'}`,
      })
      isLoggedIn.value = false
      Object.assign(
        profileForm,
        {
          name: '',
          email: '',
          phone: '',
          birthday: ''
        }
      )
    }
  } else {
    isLoggedIn_ = false
  }
  isLoggedIn.value = isLoggedIn_
  if (!isLoggedIn_) {
    clearCookie('accessToken')
    disconnectWebSocket()
  } else {
    connectWebSocket()
  }
}
</script>

<template>
  <el-container class="user-page">
    <el-header class="page-header">
      <div>
        <h1>會員中心</h1>
        <el-text type="info">管理帳戶資料，讓訂票與票券服務更順暢。</el-text>
      </div>
      <el-tag :type="isLoggedIn ? 'success' : 'info'" effect="light">{{ isLoggedIn ? `已登入・${profileForm.name}` : '尚未登入'
      }}</el-tag>
    </el-header>
    <el-main>
      <el-card shadow="never" class="account-card">
        <el-tabs v-model="activeTab" class="account-tabs" stretch>
          <el-tab-pane label="登入" name="login">
            <div class="form-heading">
              <h2>登入會員帳戶</h2>
              <el-text type="info">登入後可管理訂單與電子票券。</el-text>
            </div>
            <el-form :model="loginForm" label-position="top" class="account-form" @submit.prevent="submitLogin">
              <el-form-item label="電子信箱或帳號" required
                :error="loginFormNotOk.account !== '' ? loginFormNotOk.account : ''">
                <el-input v-model="loginForm.account" placeholder="wang@example.com" autocomplete="username" />
              </el-form-item>
              <el-form-item label="密碼" required :error="loginFormNotOk.password !== '' ? loginFormNotOk.password : ''">
                <el-input v-model="loginForm.password" type="password" show-password placeholder="請輸入密碼"
                  autocomplete="current-password" />
              </el-form-item>
              <el-button type="primary" class="submit-button" native-type="submit">登入</el-button>
            </el-form>
            <p class="switch-text">還沒有帳戶？<el-button text type="primary" @click="activeTab = 'register'">立即註冊</el-button>
            </p>
          </el-tab-pane>
          <el-tab-pane label="註冊" name="register">
            <div class="form-heading">
              <h2>建立新帳戶</h2><el-text type="info">請填寫基本資料，完成後即可開始訂票。</el-text>
            </div>
            <el-form :model="registerForm" label-position="top" class="account-form" @submit.prevent="submitRegister">
              <el-form-item label="姓名" required :error="registerFormNotOk.name !== '' ? registerFormNotOk.name : ''">
                <el-input v-model="registerForm.name" placeholder="請輸入姓名" />
              </el-form-item>
              <el-form-item label="電子信箱" required
                :error="registerFormNotOk.email !== '' ? registerFormNotOk.email : ''">
                <el-input v-model="registerForm.email" placeholder="wang@example.com" />
              </el-form-item>
              <el-form-item label="手機號碼" :error="registerFormNotOk.phone !== '' ? registerFormNotOk.phone : ''">
                <el-input v-model="registerForm.phone" placeholder="0912345678" />
              </el-form-item>
              <el-form-item label="密碼" required
                :error="registerFormNotOk.password !== '' ? registerFormNotOk.password : ''">
                <el-input v-model="registerForm.password" type="password" show-password placeholder="至少 8 個字元"
                  autocomplete="new-password" />
              </el-form-item>
              <el-form-item label="確認密碼" required
                :error="registerForm.confirmPassword && registerForm.password !== registerForm.confirmPassword ? '兩次輸入的密碼不一致' : ''">
                <el-input v-model="registerForm.confirmPassword" type="password" show-password placeholder="再次輸入密碼"
                  autocomplete="new-password" />
              </el-form-item>
              <el-button type="primary" class="submit-button" native-type="submit">建立帳戶</el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="修改會員資料" name="profile">
            <div class="form-heading">
              <div>
                <h2>會員資料</h2>
                <el-text type="info">保持聯絡資訊正確，以接收訂單與活動通知。</el-text>
              </div>
              <el-tag v-if="isLoggedIn" type="success" effect="light">已登入</el-tag>
            </div>
            <el-alert v-if="!isLoggedIn" title="請先登入後再修改會員資料。" type="warning" :closable="false" show-icon
              class="login-alert" />
            <el-form :model="profileForm" label-position="top" class="account-form" :disabled="!isLoggedIn"
              @submit.prevent="saveProfile">
              <el-form-item label="姓名">
                <el-input v-model="profileForm.name" :disabled="true" />
              </el-form-item>
              <el-form-item label="電子信箱">
                <el-input v-model="profileForm.email" :disabled="true" />
              </el-form-item>
              <el-form-item label="手機號碼" :error="profileFormNotOk.phone !== '' ? profileFormNotOk.phone : ''">
                <el-input v-model="profileForm.phone" />
              </el-form-item>
              <el-form-item label="生日" :error="profileFormNotOk.birthday !== '' ? profileFormNotOk.birthday : ''">
                <el-date-picker v-model="profileForm.birthday" type="date" value-format="YYYY-MM-DD"
                  style="width: 100%" />
              </el-form-item>
              <div class="profile-actions">
                <el-button v-if="isLoggedIn" type="danger" plain @click="logout">登出</el-button>
                <el-button type="primary" native-type="submit" :disabled="!isLoggedIn">儲存修改</el-button>
              </div>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </el-main>
  </el-container>
</template>

<style scoped>
.user-page {
  min-height: 100%;
  background: #f7f8fa;
}

.page-header {
  height: auto;
  padding: 24px 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-header h1 {
  margin: 0 0 8px;
  font-size: 26px;
}

.account-card {
  max-width: 720px;
}

.account-tabs {
  padding: 0 8px;
}

.form-heading {
  margin: 26px 0 20px;
}

.form-heading h2 {
  margin: 0 0 7px;
  font-size: 20px;
}

.account-form {
  max-width: 460px;
}

.submit-button {
  width: 100%;
  margin-top: 6px;
}

.switch-text {
  color: var(--el-text-color-secondary);
  margin: 18px 0 12px;
}

.login-alert {
  margin: 20px 0;
}

.profile-actions {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-top: 8px;
}

@media (max-width: 767px) {
  .page-header {
    align-items: flex-start;
    gap: 12px;
    flex-direction: column;
  }

  .account-card {
    margin: 0 -8px;
  }

  .account-tabs {
    padding: 0;
  }
}
</style>
