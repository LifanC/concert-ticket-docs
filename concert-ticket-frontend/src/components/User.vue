<script setup>
import { useRouter } from 'vue-router'
import { loginApi } from '@/services/api'
import { syncAuth, resetAuth } from '@/services/auth'
import { toFindCookie, addCookie, clearCookie } from "@/components/componentsJs/cookie.js";
import { connectWebSocket, disconnectWebSocket } from "@/services/websocket";

const router = useRouter()
const activeTab = ref('login')
const isLoggedIn = ref(false)
const loginForm = reactive(
  {
    account: '',
    password: '',
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
    account: '',
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
    account: '',
    name: '',
    email: '',
    phone: '',
    password: '',
  }
)

executeFirst()
function executeFirst() {
  isLoggedIn.value = !!toFindCookie('accessToken')
  if (!isLoggedIn.value) {
    clearCookie('accessName')
    clearCookie('accessToken')
    resetAuth()
    disconnectWebSocket()
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
  let isLoggedIn_ = false
  try {
    const response = await loginApi({
      method: 'post',
      url: '/login',
      data: loginForm,
    });
    let data = response.data.data[0] ?? {}
    if (data.judge) {
      activeTab.value = 'login'
      try {
        const response = await loginApi({
          method: 'post',
          url: '/validate',
          data: {},
        });
        let data = response.data.data[0] ?? {}
        if (data.judge) {
          if (data.accessToken) {
            activeTab.value = 'login'
            isLoggedIn_ = true
            addCookie('accessName', data.name)
            addCookie('accessToken', data.accessToken)
            syncAuth()
            Object.assign(
              profileForm,
              {
                name: data.name,
                email: data.email,
                phone: data.phone,
                birthday: data.birthday
              }
            )
            ElMessage({
              type: 'success',
              message: `${'登入成功'}`,
            })
          }
        } else {
          activeTab.value = 'login'
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
      ElMessage({
        type: 'error',
        message: `${'登入錯誤'}`,
      })
    }
  } catch (error) {
    let data = error.response.data.data[1]?.error ?? {}
    loginFormNotOk.value = {
      account: data.account ?? '',
      password: data.password ?? '',
    }
  }
  isLoggedIn.value = isLoggedIn_
  if (!isLoggedIn_) {
    disconnectWebSocket()
  } else {
    connectWebSocket()
    await router.push({ name: 'Activity' })
  }
}

const submitRegister = async () => {
  registerFormNotOk.value = {
    account: '',
    name: '',
    email: '',
    phone: '',
    password: '',
  }
  if (
    !registerForm.account ||
    !registerForm.name ||
    !registerForm.email ||
    !registerForm.password ||
    registerForm.password !== registerForm.confirmPassword
  ) return
  try {
    const response = await loginApi({
      method: 'post',
      url: '/register',
      data: registerForm,
    });
    let data = response.data.data[0] ?? {}
    Object.assign(
      profileForm,
      {
        account: data.account,
        name: data.name,
        email: data.email,
        phone: data.phone,
        birthday: data.birthday
      }
    )
    activeTab.value = 'profile'
    ElMessage({
      type: 'success',
      message: `${'註冊成功'}`,
    })
  } catch (error) {
    const status = error.response?.status
    if (status === 409) {
      ElMessage({
        type: 'error',
        message: `${'註冊失敗'}`,
      })
    } else {
      let data = error.response.data.data[1]?.error ?? {}
      registerFormNotOk.value = {
        account: data.account ?? '',
        name: data.name ?? '',
        email: data.email ?? '',
        phone: data.phone ?? '',
        password: data.password ?? '',
      }
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
    try {
      const response = await loginApi({
        method: 'put',
        url: '/saveProfile',
        data: profileForm
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
    } catch (error) {
      let data = error.response.data.data[1]?.error ?? {}
      profileFormNotOk.value = {
        phone: data.phone ?? '',
        birthday: data.birthday ?? '',
      }
    }
    activeTab.value = 'profile'
  } else {
    activeTab.value = 'login';
  }
  isLoggedIn.value = isLoggedIn_
  if (!isLoggedIn_) {
    disconnectWebSocket()
  }
}
const logout = async () => {
  let isLoggedIn_ = false
  let accessToken = toFindCookie('accessToken')
  if (accessToken) {
    try {
      const response = await loginApi({
        method: 'post',
        url: '/logout',
        data: {}
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
  }
  isLoggedIn.value = isLoggedIn_
  if (!isLoggedIn_) {
    clearCookie('accessName')
    clearCookie('accessToken')
    resetAuth()
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
        <p class="eyebrow">MY ACCOUNT</p>
        <h1>會員中心</h1>
        <el-text type="info">管理帳戶資料，讓訂票與票券服務更順暢。</el-text>
      </div>
      <el-button v-if="isLoggedIn" plain @click="logout">登出</el-button>
    </el-header>
    <el-main class="member-layout">
      <aside class="member-intro">
        <p class="eyebrow">LIVE IN THE MOMENT</p>
        <h2>下一場精彩，<br>從這裡開始。</h2>
        <p class="intro-description">把期待留給現場，<br>把訂票與帳戶管理交給我們。</p>
        <div class="member-benefits">
          <div><span class="benefit-number">01</span><div><h3>探索喜愛的活動</h3><p>找到值得親臨現場的精彩演出。</p></div></div>
          <div><span class="benefit-number">02</span><div><h3>管理電子票券</h3><p>登入後查看屬於你的演出票券。</p></div></div>
          <div><span class="benefit-number">03</span><div><h3>更新聯絡資訊</h3><p>讓訂單與活動通知順利送達。</p></div></div>
        </div>
        <router-link class="explore-link" to="/">探索活動 <span aria-hidden="true">↗</span></router-link>
      </aside>
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
            <p class="switch-text">還沒有帳戶？
              <el-button text type="primary" @click="activeTab = 'register'">立即註冊</el-button>
            </p>
          </el-tab-pane>
          <el-tab-pane label="註冊" name="register">
            <div class="form-heading">
              <h2>建立新帳戶</h2><el-text type="info">請填寫基本資料，完成後即可開始訂票。</el-text>
            </div>
            <el-form :model="registerForm" label-position="top" class="account-form details-form" @submit.prevent="submitRegister">
              <el-form-item label="帳號" required
                :error="registerFormNotOk.account !== '' ? registerFormNotOk.account : ''">
                <el-input v-model="registerForm.account" placeholder="請輸入帳號" />
              </el-form-item>
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
            </div>
            <el-alert v-if="!isLoggedIn" title="請先登入後再修改會員資料。" type="warning" :closable="false" show-icon
              class="login-alert" />
            <el-form :model="profileForm" label-position="top" class="account-form details-form" :disabled="!isLoggedIn"
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
  max-width: 1120px;
  margin: 0 auto;
}

.page-header {
  height: auto;
  padding: 8px 0 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.eyebrow {
  margin: 0 0 12px;
  font-size: 10px;
  letter-spacing: 3px;
  color: #89664d;
}

.page-header h1 {
  margin: 0 0 10px;
  font-size: 30px;
  font-weight: 500;
  letter-spacing: 3px;
}

.page-header .el-text {
  line-height: 1.8;
}

.member-layout {
  display: grid;
  grid-template-columns: minmax(240px, 0.85fr) minmax(0, 1.3fr);
  padding: 0;
  gap: 28px;
  align-items: start;
  overflow: visible;
}

.member-intro {
  padding: 40px 32px;
  border: 1px solid #cfc7be;
  border-radius: 12px;
  background: linear-gradient(145deg, #eee5da, #e1d5c8);
}

.member-intro h2 {
  margin: 22px 0 16px;
  font-family: 'Noto Serif TC', 'PMingLiU', serif;
  font-size: clamp(26px, 3vw, 36px);
  font-weight: 500;
  line-height: 1.6;
  letter-spacing: 2px;
}

.intro-description {
  margin: 0;
  color: #756b64;
  font-size: 14px;
  line-height: 1.9;
}

.member-benefits {
  display: grid;
  gap: 24px;
  padding: 28px 0;
  margin-top: 28px;
  border-top: 1px solid #c7b7a7;
}

.member-benefits > div {
  display: flex;
  align-items: baseline;
  gap: 16px;
}

.benefit-number {
  color: #89664d;
  font-family: Georgia, serif;
  font-size: 13px;
}

.member-benefits h3 {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 500;
}

.member-benefits p {
  margin: 0;
  color: #756b64;
  font-size: 12px;
  line-height: 1.8;
}

.explore-link {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 44px;
  border-top: 1px solid #c7b7a7;
  padding-top: 18px;
  font-size: 13px;
  color: #6d503c;
  text-decoration: none;
}

.explore-link:hover {
  text-decoration: underline;
}

.user-page .account-card {
  min-width: 0;
  border-radius: 12px;
  box-shadow: 0 12px 32px #433a3a08;
}

.account-card :deep(.el-card__body) {
  padding: 28px 32px 32px;
}

.account-tabs :deep(.el-tabs__item) {
  height: 48px;
  font-size: 14px;
}

.account-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}

.form-heading {
  margin: 24px 0 28px;
}

.form-heading h2 {
  margin: 0 0 10px;
  font-size: 22px;
  font-weight: 500;
}

.form-heading .el-text {
  font-size: 13px;
  line-height: 1.8;
}

.account-form :deep(.el-form-item) {
  margin-bottom: 24px;
  min-width: 0;
}

.account-form :deep(.el-form-item__label) {
  font-size: 13px;
  padding-bottom: 8px;
  line-height: 1.5;
}

.account-form :deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 6px;
}

.details-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 20px;
}

.submit-button,
.profile-actions {
  grid-column: 1 / -1;
}

.submit-button {
  width: 100%;
  min-height: 46px;
  margin-top: 8px;
  border-radius: 6px;
  letter-spacing: 2px;
}

.switch-text {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin: 24px 0 0;
}

.login-alert {
  margin: 20px 0 28px;
}

.profile-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.profile-actions .el-button {
  min-height: 44px;
  min-width: 140px;
  border-radius: 6px;
}

@media (max-width: 960px) {
  .member-layout {
    grid-template-columns: minmax(220px, 0.75fr) minmax(0, 1.3fr);
    gap: 20px;
  }

  .member-intro {
    padding: 32px 24px;
  }

  .account-card :deep(.el-card__body) {
    padding: 24px;
  }

  .details-form {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 767px) {
  .page-header {
    padding: 8px 4px 24px;
  }

  .page-header h1 {
    font-size: 26px;
  }

  .member-layout {
    grid-template-columns: minmax(0, 1fr);
    gap: 20px;
    min-width: 0;
  }

  .member-intro {
    padding: 24px;
  }

  .member-intro .eyebrow {
    margin-bottom: 8px;
  }

  .member-intro h2 {
    margin: 8px 0 12px;
    font-size: 26px;
  }

  .member-intro h2 br,
  .intro-description br {
    display: none;
  }

  .member-benefits {
    display: none;
  }

  .explore-link {
    margin-top: 20px;
  }

  .account-card :deep(.el-card__body) {
    padding: 20px;
  }

  .account-tabs :deep(.el-tabs__item) {
    font-size: 13px;
    padding: 0 10px;
  }

  .profile-actions .el-button {
    width: 100%;
  }
}
</style>
