<script setup>
import { adminApi } from '@/services/api'

const activeTab = ref('activities')
const dialogVisible = ref(false)
const dialogVisibleSessions = ref(false)
const dialogVisibleSessionsStatus = ref(false)
const dialogMode = ref('')
const keyword = ref('')
const activities = ref([])
const sessions = ref([])
const orders = ref([])

executeFirst()
async function executeFirst() {
  const response_selectAllActivities = await adminApi({
    method: 'get',
    url: '/selectAllActivities',
  });
  activities.value = response_selectAllActivities.data
  const response_selectAllSessions = await adminApi({
    method: 'get',
    url: '/selectAllSessions',
  });
  sessions.value = response_selectAllSessions.data
  const response_selectAllticket = await adminApi({
    method: 'get',
    url: '/selectAllticket',
  });
  orders.value = response_selectAllticket.data
}

const activityForm = reactive(
  {
    id: '',
    name: '',
    category: '',
    venue: '',
    price: 0,
    seat: '',
    description: '',
    column: '',
    row: '',
  }
)
const activityFormNotOk = ref(
  {
    name: '',
    category: '',
    venue: '',
    price: '',
    seat: '',
    column: '',
    row: '',
  }
)
const formatDate = (date) => {
  const [year, month, day] = date.split('/')
  return `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`
}
const getday = () => {
  const now = new Date()
  const date = new Date(now)
  date.setDate(date.getDate() + 30)
  // 往後找星期六
  const daysUntilSaturday = (6 - date.getDay() + 7) % 7
  date.setDate(date.getDate() + daysUntilSaturday)

  const tomorrow = new Date(now)
  tomorrow.setDate(tomorrow.getDate() + 1)

  return {
    date: `${date.getFullYear()}/${String(date.getMonth() + 1).padStart(2, '0')}/${String(date.getDate()).padStart(2, '0')}`,
    time: `09:00`,
    salesdate: `${tomorrow.getFullYear()}/${String(tomorrow.getMonth() + 1).padStart(2, '0')}/${String(tomorrow.getDate()).padStart(2, '0')}`,
    salestime: `09:00`
  }
}
const datetime = ref(getday())
const sessionForm = reactive(
  {
    id: '',
    activity_id: '',
    date: formatDate(datetime.value.date),
    time: datetime.value.time,
    salesdate: formatDate(datetime.value.salesdate),
    salestime: datetime.value.salestime,
    status: 'COMING_SOON',
  }
)
const sessionFormNotOk = ref(
  {
    activity_id: '',
    date: '',
    time: '',
    salesdate: '',
    salestime: '',
    status: ''
  }
)

const filteredActivities =
  computed(() =>
    activities.value.filter((item) =>
      !keyword.value || `${item.id}${item.name}${item.venue}`.toLowerCase().includes(keyword.value.toLowerCase())
    )
  )
const openAdd = () => {
  Object.assign(
    activityForm,
    {
      id: '',
      name: '',
      category: 'MUSIC_CONCERT',
      venue: '',
      price: 1280,
      description: ''
    }
  )
  dialogMode.value = '新增活動'
  dialogVisible.value = true
}
const openEdit = (activity) => {
  Object.assign(activityForm, activity)
  dialogMode.value = '修改活動'
  dialogVisible.value = true
}
const saveActivity = async () => {
  activityFormNotOk.value = {
    name: '',
    venue: ''
  }
  if (!activityForm.name || !activityForm.venue || !activityForm.column || !activityForm.row) {
    return
  }
  try {
    const response = await adminApi({
      method: 'post',
      url: '/saveActivity',
      data: activityForm,
    });
    activities.value = response.data.data
    dialogVisible.value = false
  } catch (error) {
    let data = error.response.data.data[1]?.error ?? {}
    activityFormNotOk.value = {
      name: data.name ?? '',
      venue: data.venue ?? '',
    }
    dialogVisible.value = true
  }
}
const deleteActivity = async (activity) => {
  Object.assign(
    activityForm,
    {
      id: activity.id
    }
  )
  const response = await adminApi({
    method: 'delete',
    url: '/deleteActivity',
    data: activityForm,
  });
  activities.value = response.data.data
}
const createSession = async () => {
  sessionFormNotOk.value = {
    activity_id: '',
    date: '',
    time: '',
    status: ''
  }
  if (
    !sessionForm.activity_id ||
    !sessionForm.date || !sessionForm.time ||
    !sessionForm.salesdate || !sessionForm.salestime
  ) {
    return
  }
  try {
    const response = await adminApi({
      method: 'post',
      url: '/createSession',
      data: sessionForm,
    });
    sessions.value = response.data.data
    ElMessage({
      type: 'success',
      message: `${'成功'}`,
    })
  } catch (error) {
    let data = error.response.data.data[1]?.error ?? {}
    sessionFormNotOk.value = {
      activity_id: data.activity_id ?? '',
      date: data.date ?? '',
      time: data.time ?? '',
    }
  }
}
const delayedDays = ref(7)
const delayedDaysDate = ref('')
const openSessionsEdit = (sessions) => {
  Object.assign(sessionForm, sessions)
  delayedDaysDate.value = getDaysSaveSessions(sessionForm.date, delayedDays.value)
  dialogVisibleSessions.value = true
}
const delayed = (days) => {
  delayedDaysDate.value = getDaysSaveSessions(sessionForm.date, days)
};
const saveSessions = () => {
  let dateSession = getDaysSaveSessions(sessionForm.date, delayedDays.value)
  Object.assign(
    sessionForm,
    {
      date: dateSession
    }
  )
  createSession()
  dialogVisibleSessions.value = false
}
const getDaysSaveSessions = (saveDate, days) => {
  const [year, month, day] = saveDate.split("-").map(Number);
  const date = new Date(year, month - 1, day);
  date.setDate(date.getDate() + days)
  return `${date.getFullYear()}` + "-" +
    `${String(date.getMonth() + 1).padStart(2, '0')}` + "-" +
    `${String(date.getDate()).padStart(2, '0')}`
}
const sessionStatusForm = reactive(
  {
    id: '',
    activity_id: '',
    date: '',
    time: '',
    status: '',
  }
)
const openSessionsStatusEdit = (sessions) => {
    Object.assign(sessionStatusForm, sessions)
    dialogVisibleSessionsStatus.value = true
}
const saveSessionsStatus = async () => {
  const response = await adminApi({
    method: 'post',
    url: '/createSession',
    data: sessionStatusForm,
  });
  sessions.value = response.data.data
  ElMessage({
    type: 'success',
    message: `${'成功'}`,
  })
  dialogVisibleSessionsStatus.value = false
}
const statusMap = {
  COMING_SOON: '即將開賣',
  TICKETS_ARE_ON_SALE: '售票中',
  SOLD_OUT: '已售完',
  ENDED: '已結束',
  PENDING_PAYMENT: '等待付款',
  PAID: '已付款',
  CANCELLED: '取消',
  EXPIRED: '超過付款期限',
  REFUNDED: '已退款',
}
const statusType = (status) => (
  {
    'COMING_SOON': 'warning',
    'TICKETS_ARE_ON_SALE': 'success',
    'SOLD_OUT': 'error',
    'ENDED': 'info',
    'PENDING_PAYMENT': 'success',
    'PAID': 'success',
    'CANCELLED': 'info',
    'EXPIRED': 'warning',
    'REFUNDED': 'info',
  }[status] || 'info'
)
</script>

<template>
  <el-container class="admin-page">
    <el-header class="page-header">
      <div>
        <h1>管理員後台</h1><el-text type="info">管理活動、場次與訂單資料。</el-text>
      </div>
      <el-tag type="warning" effect="light">管理員權限</el-tag>
    </el-header>
    <el-main>
      <el-tabs v-model="activeTab" class="admin-tabs">
        <el-tab-pane label="活動管理" name="activities">
          <el-card shadow="never" class="filter-card">
            <el-form :inline="true" label-position="top" class="filter-form">
              <el-form-item>
                <el-input v-model="keyword" clearable placeholder="搜尋活動名稱、編號或場地" style="max-width: 330px" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="openAdd">新增活動</el-button>
              </el-form-item>
            </el-form>
          </el-card>
          <el-card shadow="never" class="table-card">
            <template #header>
              <div class="card-title">
                <span>活動列表</span>
                <el-text type="info">共 {{ filteredActivities.length }} 個活動</el-text>
              </div>
            </template>
            <el-table :data="filteredActivities" stripe style="width: 100%" empty-text="找不到活動">
              <el-table-column prop="id" label="活動編號" width="140" />
              <el-table-column prop="name" label="活動名稱" min-width="190" />
              <el-table-column prop="venue" label="場地" min-width="160" />
              <el-table-column label="票價" width="110">
                <template #default="scope">NT$ {{ scope.row.price.toLocaleString() }}</template>
              </el-table-column>
              <el-table-column label="操作" width="150" fixed="right">
                <template #default="scope">
                  <el-form-item>
                    <el-button text type="primary" @click="openEdit(scope.row)">修改活動</el-button>
                  </el-form-item>
                  <el-form-item>
                    <el-button text type="danger" @click="deleteActivity(scope.row)">刪除活動</el-button>
                  </el-form-item>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="建立場次" name="sessions">
          <el-card shadow="never" class="form-card">
            <template #header>
              <div class="card-title">
                <span>建立新場次</span>
                <el-text type="info">新增後可進一步設定票種與座位。</el-text>
              </div>
            </template>
            <el-form :model="sessionForm" label-position="top" class="session-form">
              <el-form-item label="活動" required
                :error="sessionFormNotOk.activity_id !== '' ? sessionFormNotOk.activity_id : ''">
                <el-select v-model="sessionForm.activity_id" placeholder="請選擇活動" style="width: 100%">
                  <el-option v-for="activity in activities" :key="activity.id" :label="activity.name"
                    :value="activity.id" />
                </el-select>
              </el-form-item>
              <el-row :gutter="16">
                <el-col :xs="24" :sm="12">
                  <el-form-item label="開演日期" required
                    :error="sessionFormNotOk.date !== '' ? sessionFormNotOk.date : ''">
                    <el-date-picker v-model="sessionForm.date" type="date" value-format="YYYY-MM-DD"
                      style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="12">
                  <el-form-item label="開演時間" required
                    :error="sessionFormNotOk.time !== '' ? sessionFormNotOk.time : ''">
                    <el-time-picker v-model="sessionForm.time" value-format="HH:mm" format="HH:mm"
                      style="width: 100%" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :xs="24" :sm="12">
                  <el-form-item label="開賣日期" required
                    :error="sessionFormNotOk.salesdate !== '' ? sessionFormNotOk.salesdate : ''">
                    <el-date-picker v-model="sessionForm.salesdate" type="date" value-format="YYYY-MM-DD"
                      style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="12">
                  <el-form-item label="開賣時間" required
                    :error="sessionFormNotOk.salestime !== '' ? sessionFormNotOk.salestime : ''">
                    <el-time-picker v-model="sessionForm.salestime" value-format="HH:mm" format="HH:mm"
                      style="width: 100%" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :xs="24" :sm="12">
                  <el-form-item label="售票狀態" :error="sessionFormNotOk.status !== '' ? sessionFormNotOk.status : ''">
                    <el-select v-model="sessionForm.status" style="width: 100%">
                      <el-option label="即將開賣" value="COMING_SOON" />
                      <el-option label="售票中" value="TICKETS_ARE_ON_SALE" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-button type="primary" @click="createSession">建立場次</el-button>
            </el-form>
          </el-card>
          <el-card shadow="never" class="table-card">
            <template #header>
              <div class="card-title">
                <span>已建立場次</span>
              </div>
            </template>
            <el-table :data="sessions" stripe style="width: 100%" empty-text="找不到場次">
              <el-table-column prop="id" label="場次編號" width="140" />
              <el-table-column prop="activity_id" label="活動編號" width="140" />
              <el-table-column prop="name" label="活動" min-width="180" />
              <el-table-column label="狀態" width="110">
                <template #default="scope">
                  <el-tag :type="statusType(scope.row.status)" effect="light">{{ statusMap[scope.row.status] }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="date" label="開演日期" width="130" />
              <el-table-column prop="time" label="開演時間" width="100" />
              <el-table-column prop="salesdate" label="開賣日期" width="130" />
              <el-table-column prop="salestime" label="開賣時間" width="100" />
              <el-table-column prop="capacity" label="座位數" width="100" />
              <el-table-column prop="reserved" label="未付款數量" width="100" />
              <el-table-column prop="sold" label="已售" width="100" />
              <el-table-column label="操作" width="150" fixed="right">
                <template #default="scope">
                  <el-form-item>
                    <el-button text :type="statusType(scope.row.status)" :disabled="scope.row.status === 'TICKETS_ARE_ON_SALE' ||
                      scope.row.status === 'SOLD_OUT' ||
                      scope.row.status === 'ENDED'
                      " @click="openSessionsEdit(scope.row)">活動延後
                    </el-button>
                  </el-form-item>
                  <el-form-item>
                    <el-button text type="primary" @click="openSessionsStatusEdit(scope.row)">更改狀態</el-button>
                  </el-form-item>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="查看訂單" name="orders">
          <el-card shadow="never" class="table-card">
            <template #header>
              <div class="card-title">
                <span>訂單列表</span>
                <el-text type="info">最近訂單</el-text>
              </div>
            </template>
            <el-table :data="orders" stripe style="width: 100%" empty-text="找不到訂單">
              <el-table-column prop="orderno" label="訂單編號" min-width="150" />
              <el-table-column prop="customer" label="會員" width="110" />
              <el-table-column prop="name" label="活動" min-width="180" />
              <el-table-column label="金額" width="120">
                <template #default="scope">NT$ {{ scope.row.price.toLocaleString() }}</template>
              </el-table-column>
              <el-table-column label="狀態" width="150">
                <template #default="scope">
                  <el-tag :type="statusType(scope.row.status)" effect="light">{{ statusMap[scope.row.status] }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </el-main>
  </el-container>

  <el-dialog v-model="dialogVisible" :title="dialogMode" width="min(580px, 92vw)">
    <el-form :model="activityForm" label-position="top">
      <el-form-item label="活動名稱" required :error="activityFormNotOk.name !== '' ? activityFormNotOk.name : ''">
        <el-input v-model="activityForm.name" />
      </el-form-item>
      <el-form-item label="活動類型" :error="activityFormNotOk.category !== '' ? activityFormNotOk.category : ''">
        <el-select v-model="activityForm.category" style="width: 100%">
          <el-option label="音樂演唱會" value="MUSIC_CONCERT" />
          <el-option label="舞台劇" value="STAGE_PLAY" />
          <el-option label="展覽特展" value="SPECIAL_EXHIBITION" />
        </el-select>
      </el-form-item>
      <el-form-item label="活動場地" required :error="activityFormNotOk.venue !== '' ? activityFormNotOk.venue : ''">
        <el-input v-model="activityForm.venue" />
      </el-form-item>
      <el-form-item label="起始票價" :error="activityFormNotOk.price !== '' ? activityFormNotOk.price : ''">
        <el-input-number v-model="activityForm.price" :min="0" />
      </el-form-item>
      <el-form-item label="座位編號" :error="activityFormNotOk.seat !== '' ? activityFormNotOk.seat : ''">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="欄" required :error="sessionFormNotOk.column !== '' ? sessionFormNotOk.column : ''">
              <el-input v-model="activityForm.column" placeholder="ABCDEFG輸入AG" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="列" required :error="sessionFormNotOk.row !== '' ? sessionFormNotOk.row : ''">
              <el-input v-model="activityForm.row" placeholder="輸入1~10" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form-item>
      <el-form-item label="活動說明">
        <el-input v-model="activityForm.description" type="textarea" :autosize="{ minRows: 5, maxRows: 10 }" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="saveActivity">儲存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="dialogVisibleSessions" :title="dialogMode" width="min(620px, 92vw)" class="session-dialog">
    <el-form :model="sessionForm" label-position="top">
      <!-- 提示 -->
      <el-alert title="活動延後" description="請設定活動延後天數，系統將自動計算新的開演日期。" type="warning" :closable="false" show-icon
        class="mb-20" />
      <!-- 活動資訊 -->
      <div class="session-info-card">
        <div class="info-row">
          <span class="info-label">場次編號</span>
          <span class="info-value">{{ sessionForm.id }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">活動編號</span>
          <span class="info-value">{{ sessionForm.activity_id }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">活動名稱</span>
          <span class="info-value">{{ sessionForm.name }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">活動狀態</span>
          <span class="info-value">
            <el-tag :type="statusType(sessionForm.status)" effect="light">
              {{ statusMap[sessionForm.status] }}
            </el-tag>
          </span>
        </div>
      </div>
      <!-- 延期設定 -->
      <div class="delay-section">
        <div class="delay-title">延期設定</div>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <div class="date-box">
              <span class="date-label">原開演日期</span>
              <span class="date-value">{{ sessionForm.date }}</span>
            </div>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="延後天數">
              <el-input-number v-model="delayedDays" :min="7" controls-position="right" style="width: 100%"
                @change="delayed" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <div class="new-date-box">
              <span class="new-date-label">新的開演日期</span>
              <span class="new-date-value">
                {{ delayedDaysDate || '-' }}
              </span>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisibleSessions = false">取消</el-button>
        <el-button type="primary" @click="saveSessions">確認延後</el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog v-model="dialogVisibleSessionsStatus" :title="dialogMode" width="min(620px, 92vw)" class="session-dialog">
    <el-form :model="sessionStatusForm" label-position="top">
      <!-- 提示 -->
      <el-alert title="狀態設定" description="請設定狀態。" type="warning" :closable="false" show-icon class="mb-20" />
      <!-- 活動資訊 -->
      <div class="session-info-card">
        <div class="info-row">
          <span class="info-label">場次編號</span>
          <span class="info-value">{{ sessionStatusForm.id }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">活動編號</span>
          <span class="info-value">{{ sessionStatusForm.activity_id }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">活動名稱</span>
          <span class="info-value">{{ sessionStatusForm.name }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">活動狀態</span>
          <span class="info-value">
            <el-tag :type="statusType(sessionStatusForm.status)" effect="light">
              {{ statusMap[sessionStatusForm.status] }}
            </el-tag>
          </span>
        </div>
      </div>
      <!-- 狀態設定 -->
      <div class="delay-section">
        <div class="delay-title">狀態設定</div>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <el-select v-model="sessionStatusForm.status" style="width: 100%">
              <el-option label="即將開賣" value="COMING_SOON" />
              <el-option label="售票中" value="TICKETS_ARE_ON_SALE" />
              <el-option label="已售完" value="SOLD_OUT" />
              <el-option label="已結束" value="ENDED" />
            </el-select>
          </el-col>
        </el-row>
      </div>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisibleSessionsStatus = false">取消</el-button>
        <el-button type="primary" @click="saveSessionsStatus">確認</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.admin-page {
  min-height: 100%;
  background: #f7f8fa;
}

.page-header {
  height: auto;
  padding: 24px 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-header h1 {
  margin: 0 0 8px;
  font-size: 26px;
}

.admin-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.filter-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.table-card,
.form-card {
  margin-bottom: 20px;
}

.card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.session-form {
  max-width: 620px;
}

.session-info-card {
  padding: 16px 18px;
  margin-bottom: 24px;
  background: var(--el-fill-color-lighter);
  border-radius: 10px;
}

.info-row {
  display: flex;
  align-items: center;
  min-height: 38px;
  gap: 16px;
}

.info-row+.info-row {
  border-top: 1px solid var(--el-border-color-lighter);
}

.info-label {
  width: 90px;
  flex-shrink: 0;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.info-value {
  color: var(--el-text-color-primary);
  font-weight: 500;
  word-break: break-word;
}

.delay-section {
  padding-top: 4px;
}

.delay-title {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.date-box {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 18px;
}

.date-label {
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.date-value {
  height: 32px;
  line-height: 32px;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.new-date-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  margin-top: 4px;
  background: var(--el-color-warning-light-9);
  border: 1px solid var(--el-color-warning-light-7);
  border-radius: 10px;
}

.new-date-label {
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.new-date-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--el-color-warning-dark-2);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.mb-20 {
  margin-bottom: 20px;
}

@media (max-width: 767px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .filter-card {
    align-items: stretch;
    flex-direction: column;
  }

  .filter-card :deep(.el-input) {
    max-width: none !important;
  }

  .session-info-card {
    padding: 12px 14px;
  }

  .info-row {
    align-items: flex-start;
    padding: 8px 0;
  }

  .info-label {
    width: 76px;
  }

  .new-date-box {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }
}
</style>
