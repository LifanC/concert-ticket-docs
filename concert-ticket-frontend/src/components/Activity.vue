<script setup>
import { useRouter } from 'vue-router'
import { activityApi, bookingApi } from '@/services/api'
import { toFindCookie } from '@/components/componentsJs/cookie.js'

const router = useRouter()
const canUseFavorites = ref(hasUserAuthority())
const showFavoriteControls = computed(() => canUseFavorites.value || !toFindCookie('accessToken'))
function hasUserAuthority() {
  const token = toFindCookie('accessToken')
  if (!token) return false
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const claims = JSON.parse(atob(payload.padEnd(Math.ceil(payload.length / 4) * 4, '=')))
    return Array.isArray(claims.authorities) && claims.authorities.includes('USER_ITEM_IMPLEMENT')
  } catch {
    return false
  }
}
const keyword = ref('')
const selectedCategory = ref('全部')
const selectedStatus = ref('全部')
const myTicketsVisible = ref(false)
const paypriceDialogVisible = ref(false)
const tickets = ref([])
const favoriteActivityIds = ref(new Set())
const pendingFavoriteIds = ref(new Set())
const showFavoritesOnly = ref(false)
const activities = ref([])
const activityImageUrls = ref({})
const activityImageStatus = ref({})
let imageRequestsCancelled = false
let activityImageLoadVersion = 0

async function loadActivityImages() {
  if (imageRequestsCancelled) return
  const version = ++activityImageLoadVersion
  const ids = [...new Set(activities.value.map((activity) => activity.id))]
  Object.values(activityImageUrls.value).forEach(url => URL.revokeObjectURL(url))
  activityImageUrls.value = {}
  activityImageStatus.value = Object.fromEntries(ids.map(id => [id, 'loading']))
  const isCurrent = () => !imageRequestsCancelled && version === activityImageLoadVersion
  await Promise.all(ids.map(async (id) => {
    try {
      const response = await activityApi.get(`/activityImage/${encodeURIComponent(id)}`, {
        responseType: 'blob',
      })
      if (!isCurrent()) return
      const oldUrl = activityImageUrls.value[id]
      activityImageUrls.value[id] = URL.createObjectURL(response.data)
      activityImageStatus.value[id] = 'loaded'
      if (oldUrl) URL.revokeObjectURL(oldUrl)
    } catch (error) {
      if (!isCurrent()) return
      activityImageStatus.value[id] = error.response?.status === 404 ? 'missing' : 'error'
      if (error.response?.status !== 404) {
        console.error(`無法載入活動 ${id} 的圖片`, error)
      }
    }
  }))
}

onUnmounted(() => {
  imageRequestsCancelled = true
  Object.values(activityImageUrls.value).forEach((url) => URL.revokeObjectURL(url))
})

executeFirst()
async function executeFirst() {
  const response = await activityApi.get('/selectAllActivities')
  if (imageRequestsCancelled) return
  activities.value = response.data
  void loadActivityImages()
  if (canUseFavorites.value) {
    try {
      const favoriteResponse = await activityApi.get('/selectOnlyFavoriteActivities')
      favoriteActivityIds.value = new Set(
        favoriteResponse.data.map(item => String(item.activity_id))
      )
    } catch (error) {
      ElMessage.error('無法載入收藏，請重新整理後再試')
    }
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
  if (!hasUserAuthority()) {
    ElMessage.info('收藏功能僅供一般會員使用')
    return
  }
  if (pendingFavoriteIds.value.has(favoriteKey)) return
  pendingFavoriteIds.value.add(favoriteKey)
  const removing = favoriteActivityIds.value.has(favoriteKey)
  try {
    if (removing) {
      await activityApi.delete('/deleteFavoriteActivity', {
        data: { activity_id: activity.id }
      })
    } else {
      await activityApi.post('/saveFavoriteActivity', { activity_id: activity.id })
    }
    const next = new Set(favoriteActivityIds.value)
    if (removing) next.delete(favoriteKey)
    else next.add(favoriteKey)
    favoriteActivityIds.value = next
    ElMessage.success(removing ? '已取消收藏' : '已加入收藏')
  } catch (error) {
    const data = error.response?.data
    ElMessage.error(data?.message ?? data?.data?.[1]?.error?.activity_id ?? '收藏操作失敗，請稍後再試')
  } finally {
    pendingFavoriteIds.value.delete(favoriteKey)
  }
}
const getFavoriteKey = (activity) => {
  return String(activity.id)
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
  <section class="experience-hero" aria-labelledby="experience-title">
    <div class="hero-copy">
      <p class="eyebrow">MUSIC · THEATRE · EXHIBITION</p>
      <h2 id="experience-title">讓每一次相遇，<br>都成為值得珍藏的時光。</h2>
      <p class="hero-description">一首歌、一場戲、一段心動的瞬間。<br>在這裡，找到屬於你的下一場精彩。</p>
      <a href="#discover" class="discover-link">探索活動 <span aria-hidden="true">↗</span></a>
    </div>
    <div class="hero-art" aria-hidden="true">
      <div class="art-arch">
        <div class="vinyl-record"><span>LIVE<br><i>the moment</i></span></div>
      </div>
      <span class="art-caption">A LITTLE MUSIC. A BEAUTIFUL LIFE.</span>
      <span class="art-star">✳</span>
    </div>
    <span class="hero-edition" aria-hidden="true">THE EXPERIENCE COLLECTION / 01</span>
  </section>
  <el-container class="activity-page">
    <el-header id="discover" class="page-header">
      <div>
        <p class="eyebrow section-eyebrow">DISCOVER YOUR NEXT MOMENT</p>
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
          <el-form-item v-if="showFavoriteControls" label=" ">
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
                class="activity-table-image" preview-teleported>
                <template #error><span class="activity-table-no-image">圖片載入失敗</span></template>
              </el-image>
              <span v-else class="activity-table-no-image" :aria-busy="activityImageStatus[scope.row.id] === 'loading'">
                {{ activityImageStatus[scope.row.id] === 'loading' ? '圖片載入中…'
                  : activityImageStatus[scope.row.id] === 'error' ? '圖片載入失敗' : '無圖片' }}
              </span>
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
              <el-button v-if="showFavoriteControls" plain :type="isFavorite(scope.row) ? 'warning' : 'default'"
                :loading="pendingFavoriteIds.has(getFavoriteKey(scope.row))" @click="toggleFavorite(scope.row)">
                {{ isFavorite(scope.row) ? '已收藏' : '收藏' }}
              </el-button>
              <el-button type="primary" @click="goBooking(scope.row)">查看詳情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-main>
  </el-container>
  <el-dialog v-model="myTicketsVisible" title="我的票券" width="min(1100px, 94vw)">
    <el-empty v-if="tickets.length === 0" description="目前沒有票券" />
    <div v-else class="ticket-grid">
      <el-card v-for="ticket in tickets" :key="ticket.orderno" class="ticket-card" shadow="never">
        <template #header>
          <div class="ticket-header">
            <strong>{{ ticket.name }}</strong>
            <el-tag :type="statusType(ticket.status)" effect="light">{{ ticketsMap[ticket.status] }}</el-tag>
          </div>
        </template>
        <p class="ticket-session">{{ ticket.date }} {{ ticket.time }} {{ ticket.timename }}</p>
        <dl class="ticket-details">
          <div>
            <dt>座位</dt>
            <dd>{{ ticket.seat }}</dd>
          </div>
          <div>
            <dt>分區</dt>
            <dd>{{ ticket.zone_name || '—' }}</dd>
          </div>
          <div>
            <dt>票種</dt>
            <dd>{{ ticket.ticket_type_name || '—' }}</dd>
          </div>
          <div>
            <dt>票價</dt>
            <dd>{{ new Intl.NumberFormat('zh-TW', { style: 'currency', currency: 'TWD' }).format(ticket.price ?? 0) }}
            </dd>
          </div>
        </dl>
        <details class="ticket-extra">
          <summary>詳細資訊</summary>
          <dl class="ticket-details">
            <div><dt>訂單編號</dt><dd>{{ ticket.orderno }}</dd></div>
            <div><dt>場次編號</dt><dd>{{ ticket.session_id }}</dd></div>
            <div><dt>活動編號</dt><dd>{{ ticket.activity_id }}</dd></div>
          </dl>
        </details>
        <div class="ticket-actions">
          <el-button type="danger" plain :disabled="ticket.status !== 'PENDING_PAYMENT'"
            @click="cancelOrder(ticket)">取消訂單</el-button>
          <el-button type="primary" :disabled="ticket.status !== 'PENDING_PAYMENT'"
            @click="payprice(ticket)">付款</el-button>
        </div>
      </el-card>
    </div>
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
.ticket-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  max-height: 65vh;
  overflow-y: auto;
}

.ticket-card {
  min-width: 0;
}

.ticket-extra {
  margin-top: 16px;
  border-top: 1px solid var(--el-border-color-light);
  padding-top: 12px;
}

.ticket-extra summary {
  cursor: pointer;
  color: var(--el-color-primary);
}

.ticket-extra[open] summary {
  margin-bottom: 12px;
}

.ticket-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.ticket-header strong {
  overflow-wrap: anywhere;
}

.ticket-header .el-tag {
  flex-shrink: 0;
}

.ticket-session {
  margin: 0 0 16px;
  color: var(--el-text-color-secondary);
}

.ticket-details {
  display: grid;
  gap: 10px;
  margin: 0;
}

.ticket-details>div {
  display: grid;
  grid-template-columns: 80px minmax(0, 1fr);
  gap: 12px;
}

.ticket-details dt {
  color: var(--el-text-color-secondary);
}

.ticket-details dd {
  margin: 0;
  overflow-wrap: anywhere;
}

.ticket-actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}

.ticket-actions .el-button+.el-button {
  margin-left: 0;
}

@media (max-width: 700px) {
  .ticket-grid {
    grid-template-columns: 1fr;
  }
}

.activity-page {
  min-height: 100%;
  background: transparent;
}

.experience-hero {
  position: relative;
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  min-height: 430px;
  background: #f3eee8;
  overflow: hidden;
  margin-bottom: 32px;
}

.hero-copy {
  padding: 60px 52px 64px;
  position: relative;
  z-index: 1;
}

.eyebrow {
  font-size: 10px;
  letter-spacing: 3px;
  color: #89664d;
  margin: 0 0 24px;
}

.hero-copy h2 {
  font-family: 'Noto Serif TC', 'PMingLiU', serif;
  font-size: clamp(26px, 3vw, 40px);
  font-weight: 500;
  line-height: 1.7;
  letter-spacing: 3px;
  margin: 0 0 20px;
}

.hero-description {
  color: #756b64;
  font-size: 13px;
  line-height: 2.1;
  letter-spacing: 1px;
}

.discover-link {
  display: inline-flex;
  align-items: center;
  gap: 42px;
  text-decoration: none;
  border-bottom: 1px solid #a1795c;
  padding: 14px 0 10px;
  font-size: 13px;
  letter-spacing: 2px;
}

.discover-link:hover {
  color: #89664d;
}

.hero-art {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #d8cec4;
  min-height: 430px;
  overflow: hidden;
}

.art-arch {
  position: relative;
  width: 68%;
  height: 340px;
  border-radius: 180px 180px 0 0;
  background: #b59b85;
  border: 1px solid #b19984;
  transform: translateY(36px);
}

.art-arch::before {
  content: '';
  position: absolute;
  inset: -18px 18px 18px -18px;
  border: 1px solid #a1795c;
  border-radius: inherit;
}

.vinyl-record {
  position: absolute;
  width: 290px;
  max-width: 115%;
  aspect-ratio: 1;
  left: 50%;
  top: 44%;
  transform: translate(-50%, -50%) rotate(-15deg);
  border-radius: 50%;
  background: repeating-radial-gradient(circle, #433a3a 0 2px, #504641 3px 4px);
  box-shadow: 14px 24px 35px #433a3a30;
  display: grid;
  place-items: center;
}

.vinyl-record span {
  display: grid;
  align-content: center;
  text-align: center;
  width: 110px;
  height: 110px;
  border-radius: 50%;
  background: #e5d6c6;
  font: 23px Georgia, serif;
  letter-spacing: 4px;
  color: #433a3a;
}

.vinyl-record i {
  font-size: 12px;
  margin-top: 8px;
  letter-spacing: 0;
}

.art-caption {
  position: absolute;
  bottom: 22px;
  font-size: 8px;
  letter-spacing: 2px;
}

.art-star {
  position: absolute;
  right: 22px;
  top: 20px;
  color: #89664d;
  font-size: 55px;
  font-weight: 300;
}

.hero-edition {
  position: absolute;
  bottom: 20px;
  left: 52px;
  font-size: 8px;
  letter-spacing: 2px;
  color: #756b64;
}

.section-eyebrow {
  margin-bottom: 12px;
}

#discover {
  scroll-margin-top: 24px;
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
  flex-wrap: wrap;
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
  .experience-hero {
    grid-template-columns: 1fr;
  }

  .hero-copy {
    padding: 32px 26px 50px;
  }

  .hero-copy h2 {
    letter-spacing: 1px;
  }

  .hero-art {
    min-height: 300px;
  }

  .art-arch {
    width: 230px;
    height: 270px;
    transform: translateY(30px);
  }

  .vinyl-record {
    width: 230px;
  }

  .hero-edition {
    top: 16px;
    bottom: auto;
    left: auto;
    right: 16px;
    font-size: 7px;
  }

  .hero-copy .eyebrow {
    margin-top: 12px;
    letter-spacing: 2px;
    font-size: 9px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .filter-form {
    display: grid;
    gap: 12px;
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
