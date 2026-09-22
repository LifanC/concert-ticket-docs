<script setup>
import { useRouter } from 'vue-router'
import { activityApi, bookingApi } from '@/services/api'
import { toFindCookie } from '@/components/componentsJs/cookie.js'

const router = useRouter()
const keyword = ref('')
const selectedCategory = ref('全部')
const selectedStatus = ref('全部')
const myTicketsVisible = ref(false)
const paypriceDialogVisible = ref(false)
const tickets = ref([])
const favoriteActivityIds = ref(new Set())
const showFavoritesOnly = ref(false)
const activities = ref([])
const activityImageUrls = ref({})
let imageRequestsCancelled = false

async function loadActivityImages() {
  const ids = [...new Set(activities.value.map((activity) => activity.id))]
  const images = await Promise.all(ids.map(async (id) => {
    try {
      const response = await activityApi.get(`/activityImage/${encodeURIComponent(id)}`, {
        responseType: 'blob',
      })
      return [id, response.data]
    } catch (error) {
      if (error.response?.status !== 404) {
        console.error(`無法載入活動 ${id} 的圖片`, error)
      }
      return [id, null]
    }
  }))
  if (imageRequestsCancelled) return

  const nextUrls = {}
  for (const [id, blob] of images) {
    if (blob) nextUrls[id] = URL.createObjectURL(blob)
  }
  Object.values(activityImageUrls.value).forEach((url) => URL.revokeObjectURL(url))
  activityImageUrls.value = nextUrls
}

onUnmounted(() => {
  imageRequestsCancelled = true
  Object.values(activityImageUrls.value).forEach((url) => URL.revokeObjectURL(url))
})

executeFirst()
async function executeFirst() {
  const response = await activityApi.get('/selectAllActivities')
  activities.value = response.data
  void loadActivityImages()
  if (toFindCookie('accessToken')) {
    const favoriteResponse = await activityApi.get(
      '/selectOnlyFavoriteActivities'
    )

    favoriteActivityIds.value = new Set(
      favoriteResponse.data.map(item =>
        `${item.activity_id}`
      )
    )
  }
}

const categories = [
  { value: '全部', label: '全部' },
  { value: 'MUSIC_CONCERT', label: '音樂演唱會' },
  { value: 'STAGE_PLAY', label: '舞台劇' },
  { value: 'SPECIAL_EXHIBITION', label: '展覽特展' },
]
const statuses = [
  { value: '全部', label: '全部' },
  { value: 'COMING_SOON', label: '即將開賣' },
  { value: 'TICKETS_ARE_ON_SALE', label: '售票中' },
  { value: 'ENDED', label: '已結束' },
]
const categoryMap = {
  MUSIC_CONCERT: '音樂演唱會',
  STAGE_PLAY: '舞台劇',
  SPECIAL_EXHIBITION: '展覽特展',
}

const filteredActivities = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  return activities.value.filter((activity) => {
    const matchesKeyword = !normalizedKeyword || [activity.name, activity.venue, activity.id].some((value) => {
      return String(value ?? '').toLowerCase().includes(normalizedKeyword)
    })
    const matchesCategory = selectedCategory.value === '全部' || activity.category === selectedCategory.value
    const matchesStatus = selectedStatus.value === '全部' || activity.status === selectedStatus.value
    const matchesFavorite = !showFavoritesOnly.value || favoriteActivityIds.value.has(getFavoriteKey(activity))
    return matchesKeyword && matchesCategory && matchesStatus && matchesFavorite
  })
})

const resetFilters = () => {
  keyword.value = ''
  selectedCategory.value = '全部'
  selectedStatus.value = '全部'
  showFavoritesOnly.value = false
}
const toggleFavorite = async (activity) => {
  if (!toFindCookie('accessToken')) {
    ElMessage({
      type: 'info',
      message: '請先登入後再收藏活動'
    })
    await router.push({
      name: 'User',
      query: {
        redirect: router.currentRoute.value.fullPath
      }
    })
    return
  }

  const favoriteKey = getFavoriteKey(activity)
  const next = new Set(favoriteActivityIds.value)
  if (next.has(favoriteKey)) {
    await activityApi.delete('/deleteFavoriteActivity', {
      data: {
        activity_id: activity.id,
        session_id: activity.sessionid
      }
    })
    next.delete(favoriteKey)
    ElMessage({ type: 'success', message: '已取消收藏' })
  } else {
    await activityApi.post('/saveFavoriteActivity', {
      activity_id: activity.id,
      session_id: activity.sessionid
    })
    next.add(favoriteKey)
    ElMessage({ type: 'success', message: '已加入收藏' })
  }
  favoriteActivityIds.value = next
}
const getFavoriteKey = (activity) => {
  return `${activity.id}_${activity.sessionid}`
}
const isFavorite = (activity) => {
  return favoriteActivityIds.value.has(getFavoriteKey(activity))
}
const goBooking = (activity) => {
  if (activity.status === 'TICKETS_ARE_ON_SALE') {
    router.push(
      {
        path: '/booking',
        query: {
          activity_id: activity.id,
          activity_name: activity.name
        }
      }
    )
  } else {
    ElMessage({
      type: messageTypeMap[activity.status] || 'info',
      message: `活動${statusMap[activity.status]}`,
    })
  }
}
const ticketForm = reactive(
  {
    name: '',
    date: '',
    status: 'PENDING_PAYMENT',
    price: 0,
    seat: ''
  }
)
const cancelOrder = async (ticket) => {
  Object.assign(
    ticketForm,
    {
      orderno: ticket.orderno,
      session_id: ticket.session_id,
      status: 'PENDING_PAYMENT'
    }
  )
  const response = await bookingApi({
    method: 'put',
    url: '/cancelOrder',
    data: ticketForm,
  });
  let data = response.data.data[0] ?? {}
  if (data.judge) {
    ticket.status = 'CANCELLED'
  }
}
const paypricedataForm = reactive(
  {
    orderno: '',
    session_id: '',
    activity_id: '',
    date: '',
    time: '',
    salesdate: '',
    salestime: '',
  }
)
const payprice = async (payprice) => {
  Object.assign(
    paypricedataForm,
    {
      session_id: payprice.session_id,
      activity_id: payprice.activity_id,
      status: payprice.status,
      date: payprice.date,
      time: payprice.time
    }
  )
  try {
    const response = await bookingApi({
      method: 'post',
      url: '/sessionSalesDate',
      data: paypricedataForm,
    });
    Object.assign(
      paypricedataForm,
      {
        orderno: payprice.orderno,
        salesdate: response.data.salesdate,
        salestime: response.data.salestime
      }
    )
    paypriceDialogVisible.value = true
  } catch (error) {
    paypriceDialogVisible.value = false
  }
}
const dopayprice = async () => {
  const response = await bookingApi({
    method: 'put',
    url: '/dopayprice',
    data: paypricedataForm,
  });
  paypriceDialogVisible.value = false
  let data = response.data.data[0] ?? {}
  if (!data.judge) {
    ElMessage({
      type: 'error',
      message: `${'付款失敗'}`,
    })
  }
}
const messageTypeMap = {
  COMING_SOON: 'warning',
  TICKETS_ARE_ON_SALE: 'success',
  SOLD_OUT: 'error',
  ENDED: 'info',
}
const statusMap = {
  COMING_SOON: '即將開賣',
  TICKETS_ARE_ON_SALE: '售票中',
  SOLD_OUT: '已售完',
  ENDED: '已結束',
}
const ticketsMap = {
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
    'CANCELLED': 'error',
    'EXPIRED': 'warning',
    'REFUNDED': 'info'
  }[status] || 'info'
)
const myTicketsVisibleDialog = async () => {
  myTicketsVisible.value = true
  // selectAllTicket
  const response = await bookingApi({
    method: 'get',
    params: {},
    url: '/selectOnlyTicket',
  });
  tickets.value = response.data
}
</script>

<template>
  <el-container class="activity-page">
    <el-header class="page-header">
      <div>
        <h1>活動資訊</h1><el-text type="info">探索最新活動，找到屬於你的精彩體驗。</el-text>
      </div>
      <el-tag type="primary" effect="light">{{ filteredActivities.length }} 個活動</el-tag>
      <el-button plain type="primary" @click="myTicketsVisibleDialog">查看我的票券</el-button>
    </el-header>
    <el-main>
      <el-card shadow="never" class="filter-card">
        <el-form :inline="true" label-position="top" class="filter-form">
          <el-form-item label="搜尋活動">
            <el-input v-model="keyword" clearable placeholder="活動名稱、場地或活動編號" style="width: 280px" />
          </el-form-item>
          <el-form-item label="活動類型">
            <el-select v-model="selectedCategory" style="width: 140px">
              <el-option v-for="category in categories" :key="category.value" :label="category.label"
                :value="category.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="售票狀態">
            <el-select v-model="selectedStatus" style="width: 140px">
              <el-option v-for="status in statuses" :key="status.value" :label="status.label" :value="status.value" />
            </el-select>
          </el-form-item>
          <el-form-item label=" ">
            <el-checkbox v-model="showFavoritesOnly" border>只看收藏</el-checkbox>
          </el-form-item>
          <el-form-item label=" ">
            <el-button plain type="warning" @click="resetFilters">重設篩選</el-button>
          </el-form-item>
        </el-form>
      </el-card>
      <el-card shadow="never" class="activity-card">
        <template #header>
          <div class="card-title">
            <span>活動列表</span><el-text type="info">點選查看活動詳細資訊與售票狀態。</el-text>
          </div>
        </template>
        <el-table :data="filteredActivities" stripe style="width: 100%" empty-text="找不到符合篩選條件的活動">
          <el-table-column label="圖片" width="208">
            <template #default="scope">
              <el-image v-if="activityImageUrls[scope.row.id]" :src="activityImageUrls[scope.row.id]"
                :preview-src-list="[activityImageUrls[scope.row.id]]" :alt="`${scope.row.name}圖片`" fit="contain"
                class="activity-table-image" preview-teleported />
              <span v-else class="activity-table-no-image">無圖片</span>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="活動名稱" min-width="220">
            <template #default="scope">
              <div class="activity-name">
                {{ scope.row.name }}
              </div>
              <el-text size="small" type="info">
                {{ scope.row.id }} · {{ categoryMap[scope.row.category] }}
              </el-text>
            </template>
          </el-table-column>
          <el-table-column prop="venue" label="活動場地" min-width="150" />
          <el-table-column label="狀態" width="120">
            <template #default="scope">
              <el-tag :type="statusType(scope.row.status)" effect="light">{{ statusMap[scope.row.status] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="scope">
              <el-button plain :type="isFavorite(scope.row) ? 'warning' : 'default'" @click="toggleFavorite(scope.row)">
                {{ isFavorite(scope.row) ? '已收藏' : '收藏' }}
              </el-button>
              <el-button type="primary" @click="goBooking(scope.row)">查看詳情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-main>
  </el-container>
  <el-dialog v-model="myTicketsVisible" title="我的票券" width="min(1350px, 94vw)">
    <el-table :data="tickets" stripe empty-text="目前沒有票券">
      <el-table-column prop="orderno" label="訂單編號" min-width="150" />
      <el-table-column prop="session_id" label="編號" min-width="150" />
      <el-table-column prop="activity_id" label="活動編號" min-width="150" />
      <el-table-column prop="seat" label="座位號碼" min-width="100" />
      <el-table-column prop="name" label="活動" min-width="150" />
      <el-table-column prop="date" label="場次" min-width="100" />
      <el-table-column prop="time" label="時間" min-width="100" />
      <el-table-column prop="timename" label="" min-width="70" />
      <el-table-column label="狀態" width="100" fixed="right">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.status)" effect="light">{{ ticketsMap[scope.row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="scope">
          <el-button text type="danger" :disabled="scope.row.status === 'PAID' ||
            scope.row.status === 'CANCELLED' ||
            scope.row.status === 'EXPIRED' ||
            scope.row.status === 'REFUNDED'
            " @click="cancelOrder(scope.row)">取消訂單
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="scope">
          <el-button text :type="statusType(scope.row.status)"
            :disabled="
            scope.row.status === 'PAID' ||
            scope.row.status === 'CANCELLED' ||
            scope.row.status === 'EXPIRED' ||
            scope.row.status === 'REFUNDED'
            "@click="payprice(scope.row)">付款
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
  <el-dialog v-model="paypriceDialogVisible" title="付款" width="min(520px, 92vw)">
    <el-alert title="請完成付款。" type="warning" :closable="false" show-icon />
    <p class="confirm-seat">
      {{ paypricedataForm.orderno }}
    </p>
    <p class="confirm-seat">
      請於開賣後 {{ paypricedataForm.salesdate }} {{ paypricedataForm.salestime }}
      ～
      開演前 {{ paypricedataForm.date }} {{ paypricedataForm.time }} 完成付款
    </p>
    <template #footer>
      <el-button @click="paypriceDialogVisible = false">返回</el-button>
      <el-button type="primary" @click="dopayprice">付款</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.activity-page {
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

.filter-card,
.activity-card {
  margin-bottom: 20px;
}

.filter-form {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.filter-form :deep(.el-form-item) {
  margin-right: 0;
  margin-bottom: 0;
}

.card-title,
.detail-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.activity-name {
  margin-bottom: 4px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.activity-table-image {
  display: block;
  width: 180px;
  height: 102px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}

.activity-table-no-image {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.detail-image {
  display: block;
  width: 100%;
  height: 280px;
  margin-bottom: 20px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
}

.detail-heading {
  margin-bottom: 20px;
}

.detail-heading h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.detail-list :deep(.el-descriptions__label) {
  width: 130px;
}

@media (max-width: 767px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .filter-form {
    display: grid;
    gap: 0;
  }

  .filter-form :deep(.el-form-item),
  .filter-form :deep(.el-input),
  .filter-form :deep(.el-select) {
    width: 100% !important;
  }

  .card-title,
  .detail-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .detail-list :deep(.el-descriptions__label) {
    width: 100px;
  }
}
</style>
