<script setup>
// 閱讀導引：管理頁：維護活動、上傳圖片與建立場次，並載入銷售統計和售票設定。
import { adminApi, resolveActivityImageUrl } from '@/services/api'
import { connectWebSocket, showNotification } from '@/services/websocket'
import AdminDashboard from './AdminDashboard.vue'
import AdminSalesSettings from './AdminSalesSettings.vue'
import ResponsiveRecordList from './ResponsiveRecordList.vue'
import { onBeforeUnmount } from 'vue'

const activeTab = ref('dashboard')
const dialogVisible = ref(false)
const dialogVisibleSessions = ref(false)
const dialogVisibleSessionsStatus = ref(false)
const dialogMode = ref('')
const keyword = ref('')
const activities = ref([])
const activityImageUrls = ref({})
const sessions = ref([])
const orders = ref([])
const loading = ref(false)
const error = ref('')
const updatedAt = ref('')
const loaded = ref(false)
onMounted(() => connectWebSocket())

loadDashboard()
executeFirst()
/** 並行讀取場次及訂單，兩份資料皆有效才替換統計內容；更新失敗時保留上次成功資料並顯示提示。 */
async function loadDashboard() {
  if (loading.value) return
  loading.value = true
  error.value = ''
  try {
    const [sessionResponse, orderResponse] = await Promise.all([
      adminApi({ method: 'get', url: '/selectAllSessions' }),
      adminApi({ method: 'get', url: '/selectAllticket' }),
    ])
    if (!Array.isArray(sessionResponse.data) || !Array.isArray(orderResponse.data)) {
      throw new Error('Invalid dashboard response')
    }
    sessions.value = sessionResponse.data
    orders.value = orderResponse.data
    loaded.value = true
    updatedAt.value = new Intl.DateTimeFormat('zh-TW', {
      timeZone: 'Asia/Taipei', dateStyle: 'short', timeStyle: 'medium',
    }).format(new Date())
  } catch {
    error.value = loaded.value ? '更新失敗，目前顯示上次成功載入的資料，請重新整理。' : '無法載入銷售資料，請重新整理。'
  } finally {
    loading.value = false
  }
}

/** 載入管理用活動清單，將回傳圖片路徑轉成可供圖片元素使用的網址。 */
async function executeFirst() {
  const response_selectAllActivities = await adminApi({
    method: 'get',
    url: '/selectAllActivities',
  });
  activities.value = response_selectAllActivities.data
  activityImageUrls.value = Object.fromEntries(
    activities.value.map(activity => [activity.id, resolveActivityImageUrl(activity.image_url)])
  )
}

const activityForm = reactive(
  {
    id: '',
    name: '',
    category: '',
    venue: '',
    price: 0,
    description: '',
    column: '',
    row: 10,
  }
)
const seatRowCountByCategory = {
  MUSIC_CONCERT: 50,
  STAGE_PLAY: 100,
  SPECIAL_EXHIBITION: 150,
}
/** 將排號轉成英文字母標籤，支援超過 Z 的排號。 */
const seatRowLabel = (index) => {
  let number = index
  let label = ''
  while (number > 0) {
    number--
    label = String.fromCharCode(65 + number % 26) + label
    number = Math.floor(number / 26)
  }
  return label
}
const seatRangeOptions = computed(() => {
  const rowCount = seatRowCountByCategory[activityForm.category] ?? 0
  return Array.from({ length: rowCount }, (_, index) => {
    const end = seatRowLabel(index + 1)
    return { label: `A～${end} 排（${index + 1} 排）`, value: end }
  })
})
/** 活動類別切換時調整座位排數選項，避免保留不適用的排數。 */
const onActivityCategoryChange = () => {
  activityForm.column = ''
  activityFormNotOk.value.column = ''
}
const activityFormNotOk = ref(
  {
    image: '',
    name: '',
    category: '',
    venue: '',
    price: '',
    column: '',
    row: '',
  }
)
/** 將表單預設的 yyyy/MM/dd 字串轉為 yyyy-MM-dd，月與日補兩位。 */
const formatDate = (date) => {
  const [year, month, day] = date.split('/')
  return `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`
}
/** 預設演出日為今天起 30 天後的第一個星期六，開賣日為明天，演出與開賣時間皆為 09:00。 */
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
const imageFile = ref(null)
const imagePreview = ref('')
const imageUpload = ref(null)
let imageLoadVersion = 0

/** 清除選圖、預覽及圖片欄位錯誤；僅對本頁產生的 blob: URL 呼叫 revokeObjectURL。 */
function clearImageSelection() {
  imageLoadVersion++
  if (imagePreview.value.startsWith('blob:')) {
    URL.revokeObjectURL(imagePreview.value)
  }
  imageFile.value = null
  imagePreview.value = ''
  imageUpload.value?.clearFiles()
  activityFormNotOk.value.image = ''
}
onBeforeUnmount(() => {
  if (imagePreview.value.startsWith('blob:')) URL.revokeObjectURL(imagePreview.value)
})
/** 重設活動表單並開啟新增視窗。 */
const openAdd = () => {
  clearImageSelection()
  Object.assign(
    activityForm,
    {
      id: '',
      name: '',
      category: '',
      venue: '',
      price: 1280,
      description: '',
      column: '',
      row: 10,
    }
  )
  dialogMode.value = '新增活動'
  dialogVisible.value = true
}
/** 將選取活動填入表單並開啟編輯視窗。 */
const openEdit = async (activity) => {
  clearImageSelection()
  const loadVersion = imageLoadVersion
  Object.assign(activityForm, activity)
  activityForm.column = activity.seat_rows?.split(',').at(-1)?.trim() ?? ''
  activityForm.row = activity.row == null ? 10 : Number(activity.row)
  dialogMode.value = '修改活動'
  dialogVisible.value = true
  if (imageLoadVersion === loadVersion && dialogVisible.value) {
    imagePreview.value = activityImageUrls.value[activity.id] || resolveActivityImageUrl(activity.image_url)
  }
}
/** 檢查瀏覽器回報的 JPEG MIME 與 10 MiB 上限，合格才建立預覽 URL；實際內容仍由後端解碼驗證。 */
function onImageChange(uploadFile) {
  const file = uploadFile.raw
  if (!file || file.type !== 'image/jpeg' || file.size > 10 * 1024 * 1024) {
    clearImageSelection()
    activityFormNotOk.value.image = '請選擇小於 10 MB 的 JPG 圖片'
    return
  }
  if (imagePreview.value.startsWith('blob:')) URL.revokeObjectURL(imagePreview.value)
  imageLoadVersion++
  imageFile.value = file
  imagePreview.value = URL.createObjectURL(file)
  activityFormNotOk.value.image = ''
}
/** 檢查必要欄位與類別排數，以 multipart 上傳 activity JSON 及可選 image；成功時合併回應到本地活動清單。 */
const saveActivity = async () => {
  activityFormNotOk.value = {
    image: '',
    name: '',
    category: '',
    venue: '',
    price: '',
    column: '',
    row: '',
  }
  if (!activityForm.category) {
    activityFormNotOk.value.category = '請選擇活動類型'
  }
  if (!seatRangeOptions.value.some((option) => option.value === activityForm.column)) {
    activityFormNotOk.value.column = activityForm.category
      ? '請選擇符合活動類型的座位排別'
      : '請先選擇活動類型'
  }
  if (
    !activityForm.name ||
    !activityForm.category ||
    !activityForm.venue ||
    activityFormNotOk.value.column ||
    !activityForm.row
  ) {
    return
  }
  const formData = new FormData()
  formData.append(
    'activity',
    new Blob(
      [JSON.stringify(activityForm)], { type: 'application/json' }
    )
  )
  if (imageFile.value) formData.append('image', imageFile.value)
  try {
    const response = await adminApi({
      method: 'post',
      url: '/saveActivity',
      data: formData,
    });
    const id = response.data.id
    let activity = activities.value.find(item => item.id === id)
    if (activity) {
      Object.assign(activity, response.data)
    } else {
      activities.value.push(response.data)
      activity = response.data
    }
    const imageUrl = resolveActivityImageUrl(activity.image_url)
    // 圖片替換後路徑可能不變，加入時間參數讓瀏覽器重新取得內容，避免顯示快取舊圖。
    activityImageUrls.value[id] = imageUrl && imageFile.value
      ? `${imageUrl}?v=${Date.now()}` : imageUrl
    dialogVisible.value = false
  } catch (error) {
    const data = error.response?.data?.data?.[1]?.error ?? {}
    activityFormNotOk.value = {
      image: data.image ?? '',
      name: data.name ?? '',
      category: data.category ?? '',
      venue: data.venue ?? '',
      price: data.price ?? '',
      column: data.column ?? '',
      row: data.row ?? '',
    }
    dialogVisible.value = true
  }
}
/** 提交刪除後以回應清單替換活動資料；遇到已有場次的警告時，使用與 WebSocket 共用的通知去重流程呈現。 */
const deleteActivity = async (activity) => {
  try {
    const response = await adminApi({
      method: 'delete',
      url: '/deleteActivity',
      data: { id: activity.id },
    });
    activities.value = response.data.data
    if (activityImageUrls.value[activity.id]) {
      delete activityImageUrls.value[activity.id]
    }
    ElMessage.success('活動已刪除')
  } catch (error) {
    const notification = error.response?.data?.notification
    if (notification) {
      showNotification(notification)
    } else {
      ElMessage.error(error.response?.data?.message ?? '刪除活動失敗，請稍後再試')
    }
  }
}
/** editing 為 false 時以空 id 新增場次，為 true 時保留原 id 更新；成功後以後端場次清單替換本地資料。 */
const createSession = async (editing = false) => {
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
      data: { ...sessionForm, id: editing ? sessionForm.id : '' },
    });
    sessions.value = response.data.data
    ElMessage({
      type: 'success',
      message: `${'成功'}`,
    })
  } catch (error) {
    const data = error.response?.data?.data?.[1]?.error ?? {}
    sessionFormNotOk.value = {
      activity_id: data.activity_id ?? '',
      date: data.date ?? '',
      time: data.time ?? '',
      salesdate: data.salesdate ?? '',
      salestime: data.salestime ?? '',
      status: data.status ?? '',
    }
  }
}
const delayedDays = ref(7)
const delayedDaysDate = ref('')
/** 將選取場次填入編輯狀態並開啟場次編輯視窗。 */
const openSessionsEdit = (sessions) => {
  Object.assign(sessionForm, sessions)
  delayedDaysDate.value = getDaysSaveSessions(sessionForm.date, delayedDays.value)
  dialogVisibleSessions.value = true
}
/** 依指定天數更新延後日期的預覽，供場次日期選擇使用。 */
const delayed = (days) => {
  delayedDaysDate.value = getDaysSaveSessions(sessionForm.date, days)
};
/** 把預覽的延後日期填入表單，啟動 createSession(true) 並立即關閉視窗；此函式未等待儲存完成。 */
const saveSessions = () => {
  let dateSession = getDaysSaveSessions(sessionForm.date, delayedDays.value)
  Object.assign(
    sessionForm,
    {
      date: dateSession
    }
  )
  createSession(true)
  dialogVisibleSessions.value = false
}
/** 以瀏覽器本地日期將 yyyy-MM-dd 加上指定天數，再回傳相同格式；Date 會自動處理跨月與跨年。 */
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
/** 開啟指定場次的狀態編輯視窗。 */
const openSessionsStatusEdit = (sessions) => {
  Object.assign(sessionStatusForm, sessions)
  dialogVisibleSessionsStatus.value = true
}
/** 以場次編號及狀態表單呼叫共用場次儲存端點，成功後替換場次清單並關閉狀態視窗。 */
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
const categoryMap = {
  MUSIC_CONCERT: '音樂演唱會',
  STAGE_PLAY: '舞台劇',
  SPECIAL_EXHIBITION: '展覽特展',
}
/** 將業務狀態對應至 Element Plus 標籤顏色。 */
const statusType = (status) => (
  {
    'COMING_SOON': 'warning',
    'TICKETS_ARE_ON_SALE': 'success',
    'SOLD_OUT': 'error',
    'ENDED': 'info',
    'PENDING_PAYMENT': 'success',
    'PAID': 'success',
    'CANCELLED': 'error',
    'EXPIRED': 'warning',
    'REFUNDED': 'info',
  }[status] || 'info'
)

// 同一份欄位設定同時產生桌面表格與手機卡片。
const activityFields = [
  { key: 'image', label: '圖片', role: 'image', width: 208 },
  { key: 'id', label: '活動編號', minWidth: 140 },
  { key: 'name', label: '活動名稱', role: 'title', minWidth: 190 },
  { key: 'category', label: '活動類型', format: row => categoryMap[row.category], minWidth: 110 },
  { key: 'venue', label: '場地', minWidth: 160 },
  { key: 'price', label: '票價', format: row => `NT$ ${Number(row.price ?? 0).toLocaleString()}`, minWidth: 160 },
  { key: 'actions', label: '操作', role: 'actions', width: 230, fixed: 'right' },
]
const sessionFields = [
  { key: 'id', label: '場次編號', minWidth: 140 },
  { key: 'activity_id', label: '活動編號', minWidth: 140 },
  { key: 'name', label: '活動', role: 'title', minWidth: 180 },
  { key: 'status', label: '狀態', role: 'status', width: 110 },
  { key: 'date', label: '開演日期', minWidth: 130 },
  { key: 'time', label: '開演時間', minWidth: 100 },
  { key: 'salesdate', label: '開賣日期', minWidth: 130 },
  { key: 'salestime', label: '開賣時間', minWidth: 100 },
  { key: 'capacity', label: '座位數', minWidth: 100 },
  { key: 'reserved', label: '未付款數量', minWidth: 100 },
  { key: 'sold', label: '已售', minWidth: 100 },
  { key: 'actions', label: '操作', role: 'actions', width: 230, fixed: 'right' },
]
const orderFields = [
  { key: 'orderno', label: '訂單編號', minWidth: 150 },
  { key: 'name', label: '活動', role: 'title', minWidth: 180 },
  { key: 'price', label: '金額', format: row => `NT$ ${Number(row.price ?? 0).toLocaleString()}`, minWidth: 120 },
  { key: 'status', label: '狀態', role: 'status', width: 150 },
]
</script>

<template>
  <el-container class="admin-page">
    <el-header class="page-header">
      <div>
        <p class="eyebrow">ADMINISTRATION</p>
        <h1>管理員後台</h1>
        <el-text type="info">管理活動、場次與訂單資料。</el-text>
      </div>
      <el-tag type="warning" effect="light">管理員權限</el-tag>
    </el-header>
    <el-main class="admin-main">
      <el-tabs v-model="activeTab" class="admin-tabs">
        <el-tab-pane label="銷售儀表板" name="dashboard">
          <AdminDashboard v-if="activeTab === 'dashboard'" :sessions="sessions" :orders="orders" :loading="loading"
            :error="error" :updated-at="updatedAt" :loaded="loaded" @refresh="loadDashboard" />
        </el-tab-pane>
        <el-tab-pane label="活動管理" name="activities">
          <el-card shadow="never" class="filter-card">
            <el-form :inline="true" label-position="top" class="filter-form">
              <el-form-item>
                <el-input v-model="keyword" clearable placeholder="搜尋活動名稱、編號或場地" aria-label="搜尋活動" />
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
            <ResponsiveRecordList :records="filteredActivities" empty-text="找不到活動" :fields="activityFields">
              <template #image="{ record }">
                <el-image v-if="activityImageUrls[record.id]" class="record-image" :src="activityImageUrls[record.id]"
                  :preview-src-list="[activityImageUrls[record.id]]" :alt="`${record.name}圖片`" fit="contain"
                  preview-teleported />
                <span v-else class="activity-table-no-image">無圖片</span>
              </template>
              <template #actions="{ record }">
                <el-button plain type="primary" @click="openEdit(record)">修改活動</el-button>
                <el-button plain type="danger" @click="deleteActivity(record)">刪除活動</el-button>
              </template>
            </ResponsiveRecordList>
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
              <el-button type="primary" @click="createSession()">建立場次</el-button>
            </el-form>
          </el-card>
          <el-card shadow="never" class="table-card">
            <template #header>
              <div class="card-title">
                <span>已建立場次</span>
              </div>
            </template>
            <ResponsiveRecordList :records="sessions" empty-text="找不到場次" :fields="sessionFields">
              <template #status="{ record }">
                <el-tag :type="statusType(record.status)">{{ statusMap[record.status] }}</el-tag>
              </template>
              <template #actions="{ record }">
                <el-button plain :type="statusType(record.status)"
                  :disabled="['TICKETS_ARE_ON_SALE', 'SOLD_OUT', 'ENDED'].includes(record.status)"
                  @click="openSessionsEdit(record)">活動延後</el-button>
                <el-button plain type="primary" @click="openSessionsStatusEdit(record)">更改狀態</el-button>
              </template>
            </ResponsiveRecordList>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="分區與限購" name="sales-settings">
          <AdminSalesSettings :sessions="sessions" />
        </el-tab-pane>

        <el-tab-pane label="查看訂單" name="orders">
          <el-card shadow="never" class="table-card">
            <template #header>
              <div class="card-title">
                <span>訂單列表</span>
                <el-text type="info">最近訂單</el-text>
              </div>
            </template>
            <ResponsiveRecordList :records="orders" id-key="orderno" empty-text="找不到訂單" :fields="orderFields">
              <template #status="{ record }">
                <el-tag :type="statusType(record.status)">{{ statusMap[record.status] }}</el-tag>
              </template>
            </ResponsiveRecordList>
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </el-main>
  </el-container>

  <el-dialog v-model="dialogVisible" :title="dialogMode" width="min(580px, 92vw)" @closed="clearImageSelection">
    <el-form :model="activityForm" label-position="top">
      <el-form-item label="活動圖片" :error="activityFormNotOk.image">
        <div class="image-picker">
          <el-upload ref="imageUpload" accept=".jpg,.jpeg" :auto-upload="false" :show-file-list="false"
            :on-change="onImageChange">
            <el-button>選擇 JPG 圖片</el-button>
          </el-upload>
          <div v-if="imagePreview" class="image-preview-wrap">
            <img :src="imagePreview" class="image-preview" alt="活動圖片預覽" />
            <button type="button" class="image-preview-close" aria-label="移除預覽圖片"
              @click="clearImageSelection">×</button>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="活動名稱" required :error="activityFormNotOk.name !== '' ? activityFormNotOk.name : ''">
        <el-input v-model="activityForm.name" />
      </el-form-item>
      <el-form-item label="活動類型" required :error="activityFormNotOk.category !== '' ? activityFormNotOk.category : ''">
        <el-select v-model="activityForm.category" placeholder="請先選擇活動類型" style="width: 100%"
          @change="onActivityCategoryChange">
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
      <el-form-item label="座位編號">
        <el-row :gutter="16" style="width: 100%">
          <el-col :xs="24" :sm="16">
            <el-form-item label="排別範圍" required :error="activityFormNotOk.column">
              <el-select v-model="activityForm.column" :disabled="!activityForm.category"
                :placeholder="activityForm.category ? '請選擇 A～B 等排別' : '請先選擇活動類型'" style="width: 100%">
                <el-option v-for="option in seatRangeOptions" :key="option.value" :label="option.label"
                  :value="option.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="每排座位數" required :error="activityFormNotOk.row">
              <el-input-number v-model="activityForm.row" :min="1" :max="10" style="width: 100%" />
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
  min-width: 0;
}

.page-header {
  height: auto;
  padding: 8px 0 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.admin-page>.admin-main {
  padding: 0;
  min-width: 0;
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

.admin-page :deep(.el-card) {
  border-radius: 12px;
  box-shadow: 0 8px 24px #433a3a06;
}

.admin-page :deep(.el-card__header) {
  padding: 20px 24px;
  background: #f8f4ee;
}

.admin-page :deep(.el-card__body) {
  padding: 24px;
}

.admin-page :deep(.el-input__wrapper),
.admin-page :deep(.el-select__wrapper) {
  min-height: 42px;
  border-radius: 6px;
}

.admin-tabs :deep(.el-tabs__item) {
  height: 52px;
  font-size: 14px;
  letter-spacing: 1px;
}

.admin-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}

.admin-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-form {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.filter-form :deep(.el-form-item) {
  margin: 0;
}

.filter-form :deep(.el-form-item:first-child) {
  flex: 1;
  max-width: 480px;
  min-width: 0;
}

.filter-form :deep(.el-button) {
  min-height: 42px;
  border-radius: 6px;
}

.table-card,
.form-card {
  margin-bottom: 20px;
}

.card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  font-weight: 500;
}

.session-form {
  max-width: 760px;
  margin: 8px auto;
}

.session-form :deep(.el-form-item) {
  margin-bottom: 24px;
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

.image-picker {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
}

.record-image {
  display: block;
  width: 180px;
  height: 102px;
  object-fit: contain;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}

.activity-table-no-image {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.image-preview-wrap {
  position: relative;
  width: 320px;
  max-width: 100%;
  height: 180px;
}

.image-preview {
  display: block;
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
}

.image-preview-close {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: rgb(0 0 0 / 65%);
  color: #fff;
  font-size: 22px;
  line-height: 28px;
  cursor: pointer;
}

@media (max-width: 767px) {
  .admin-page>.admin-main {
    padding: 12px 0;
    min-width: 0;
  }

  .page-header {
    padding: 8px 4px 24px;
  }

  .page-header h1 {
    font-size: 26px;
  }

  .admin-page :deep(.el-card__header),
  .admin-page :deep(.el-card__body) {
    padding: 16px;
  }

  .admin-page :deep(.el-button) {
    min-height: 44px;
  }

  .admin-tabs :deep(.el-tabs__item) {
    font-size: 13px;
    letter-spacing: 0;
  }

  .admin-page :deep(.record-card) {
    padding: 16px;
    border: 1px solid var(--el-border-color-light);
    border-radius: 10px;
    background: #f8f5f1;
  }

  .record-image {
    width: 100%;
    height: auto;
    aspect-ratio: 16 / 9;
  }

  .admin-tabs :deep(.el-tabs__item) {
    padding-inline: 12px;
  }

  .filter-card {
    display: block;
  }

  .filter-card :deep(.el-card__body) {
    width: 100%;
  }

  .filter-form {
    display: grid;
    gap: 12px;
  }

  .filter-form :deep(.el-form-item) {
    margin: 0;
    width: 100%;
    max-width: none;
  }

  .filter-form :deep(.el-button) {
    width: 100%;
  }

  .session-form :deep(.el-col-12) {
    max-width: 100%;
    flex-basis: 100%;
  }

  .card-title {
    flex-wrap: wrap;
    gap: 8px;
  }

  .table-card :deep(.el-card__body),
  .form-card :deep(.el-card__body) {
    padding: 16px;
  }

  .dialog-footer {
    flex-wrap: wrap;
  }

  .dialog-footer .el-button {
    min-height: 44px;
    margin-left: 0;
  }

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
