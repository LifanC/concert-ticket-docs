<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { bookingApi } from '@/services/api'
import SeatMap from '@/components/booking/SeatMap.vue'

const route = useRoute()
const activityId = computed(() => String(route.query.activity_id ?? ''))
const selectedActivityName = computed(() => String(route.query.activity_name ?? ''))
const nextStep_disabled = computed(() => !activityId.value || loading.value)
const step = ref(0)
const ticketDialogVisible = ref(false)
const selectedDate = ref('')
const selectedSession = ref('')
const selectedSeats = ref([])
const unavailableSeats = ref(new Set())
const dates = ref([])
const sessions = ref([])
const salesSettings = ref(null)
const selectedTicketType = ref('')
const selectedZoneFilter = ref('')
const loading = ref(false)
const loadError = ref('')
const submittingBooking = ref(false)
const pendingBooking = ref(null)
const bookingSteps = ['選日期', '選場次', '建立訂單']
const sessionId = computed(() => sessions.value.find((item) => item.value === selectedSession.value)?.id ?? '')
const selectedZone = computed(() => zoneForSeat(selectedSeats.value[0]))
const selectedType = computed(() => salesSettings.value?.ticketTypes.find((type) => type.id === selectedTicketType.value))
const selectedPrice = computed(() => {
  if (!salesSettings.value) {
    return null
  }
  if (!salesSettings.value.configured) {
    return Number(salesSettings.value.defaultPrice ?? 0)
  }
  return selectedZone.value?.prices?.[selectedTicketType.value] ?? null
})
const canBook = computed(
  () => !loading.value && !loadError.value && !!salesSettings.value
    && selectedSeats.value.length === 1 && selectedPrice.value !== null
    && (!salesSettings.value.configured || (selectedTicketType.value && salesSettings.value.remainingAllowance > 0))
)
const moneyFormat = new Intl.NumberFormat('zh-TW', { style: 'currency', currency: 'TWD' })
const money = (value) => value === null ? '請先選擇票種與座位' : moneyFormat.format(value)
function errorMessage(error, fallback) {
  return error.response?.data?.message ?? error.response?.data?.data?.find((item) => item.remark)?.remark ?? fallback
}
function zoneForSeat(seatId) {
  if (!seatId || !salesSettings.value?.configured) return null
  const row = seatId.split('-')[0]
  const labels = salesSettings.value.rowLabels
  const index = labels.indexOf(row)
  return salesSettings.value.zones.find((zone) => index >= 0
    && index >= labels.indexOf(zone.rowStart) && index <= labels.indexOf(zone.rowEnd))
}
const seatZones = computed(() => {
  const result = {}
  for (const row of salesSettings.value?.rowLabels ?? []) {
    const zone = zoneForSeat(`${row}-01`)
    if (zone) result[row] = zone
  }
  return result
})
const filteredSeats = computed(() => {
  const result = new Set()
  if (!selectedZoneFilter.value) return result
  for (const [row, zone] of Object.entries(seatZones.value)) {
    if (zone.id !== selectedZoneFilter.value) {
      for (let i = 1; i <= salesSettings.value.seatsPerRow; i++) result.add(`${row}-${String(i).padStart(2, '0')}`)
    }
  }
  return result
})
const seatLayout = computed(
  () => !salesSettings.value ? null : salesSettings.value.rowLabels.flatMap(
    (row) =>
      Array.from(
        { length: salesSettings.value.seatsPerRow }, (_, index) => (
          {
            id: `${row}-${String(index + 1).padStart(2, '0')}`, row, number: index + 1, seats_per_row: salesSettings.value.seatsPerRow,
          }
        )
      )
  )
)

watch(activityId, async (id, _, onCleanup) => {
  let active = true
  onCleanup(() => { active = false })
  step.value = 0
  selectedDate.value = ''
  dates.value = []
  loadError.value = ''
  if (!id) return
  loading.value = true
  try {
    const response = await bookingApi.get('/selectOnlyActivities', { params: { activity_id: id } })
    if (active) dates.value = response.data
  } catch (error) {
    if (active) loadError.value = errorMessage(error, '無法載入活動日期。')
  } finally {
    if (active) loading.value = false
  }
}, { immediate: true })
watch(
  selectedDate, () => {
    selectedSession.value = '';
    sessions.value = [];
    selectedSeats.value = [];
    salesSettings.value = null
  }
)
watch(
  selectedSession, () => {
    selectedSeats.value = [];
    salesSettings.value = null;
    selectedTicketType.value = '';
    selectedZoneFilter.value = ''
  }
)
watch(
  selectedZoneFilter, () => {
    selectedSeats.value = []
  }
)

async function loadBookingDetails() {
  const id = sessionId.value
  if (!id) throw new Error('Missing session')
  const [unavailable, settings] = await Promise.all([
    bookingApi.get('/selectOnlyUnavailableSeats', {
      params: {
        activity_id: activityId.value, selected_date: selectedDate.value, selected_session: selectedSession.value,
      }
    }),
    bookingApi.get(`/sessions/${encodeURIComponent(id)}/sales-settings`),
  ])
  if (sessionId.value !== id) return
  unavailableSeats.value = new Set(unavailable.data)
  salesSettings.value = settings.data
  if (!settings.data.ticketTypes.some((type) => type.id === selectedTicketType.value)) {
    selectedTicketType.value = settings.data.ticketTypes[0]?.id ?? ''
  }
  selectedSeats.value = selectedSeats.value.filter((seat) => !unavailableSeats.value.has(seat))
}
async function nextStep() {
  if (loading.value) return
  if ((step.value === 0 && !selectedDate.value) || (step.value === 1 && !selectedSession.value)) return
  loading.value = true
  loadError.value = ''
  try {
    if (step.value === 0) {
      const response = await bookingApi.get('/selectOnlySession', { params: { date: selectedDate.value, activity_id: activityId.value } })
      sessions.value = response.data
      step.value = 1
    } else if (step.value === 1) {
      await loadBookingDetails()
      step.value = 2
    }
  } catch (error) {
    loadError.value = errorMessage(error, '無法載入場次售票資料，請重試。')
  } finally {
    loading.value = false
  }
}
function previousStep() { if (step.value > 0 && !loading.value && !submittingBooking.value) { step.value--; loadError.value = '' } }
async function refreshBooking() {
  loading.value = true
  loadError.value = ''
  try {
    await loadBookingDetails()
  } catch (error) {
    loadError.value = errorMessage(error, '無法更新售票資料，請重試。')
  }
  finally { loading.value = false }
}
async function createOrder() {
  if (submittingBooking.value || !canBook.value) return
  const data = {
    activity_id: activityId.value, name: selectedActivityName.value,
    date: selectedDate.value, time: selectedSession.value,
    status: 'PENDING_PAYMENT', seat: selectedSeats.value[0], ticketTypeId: selectedTicketType.value || null,
  }
  const fingerprint = JSON.stringify([sessionId.value, data.activity_id, data.seat, data.ticketTypeId])
  if (pendingBooking.value?.fingerprint !== fingerprint) {
    pendingBooking.value = { fingerprint, key: crypto.randomUUID() }
  }
  submittingBooking.value = true
  loadError.value = ''
  try {
    await bookingApi.post('/saveTicket', data, { headers: { 'Idempotency-Key': pendingBooking.value.key } })
    ticketDialogVisible.value = false
    pendingBooking.value = null
    selectedSeats.value = []
    ElMessage.success('訂單已建立，請至我的票券完成付款')
    await refreshBooking()
  } catch (error) {
    loadError.value = errorMessage(error, '建立訂單失敗，請重試。')
    if ([400, 409].includes(error.response?.status)) {
      pendingBooking.value = null
      try {
        await loadBookingDetails()
      } catch {
        /* Keep the original order error visible. */
      }
    }
  } finally {
    submittingBooking.value = false
  }
}
</script>

<template>
  <el-container class="booking-page">
    <el-header class="page-header">
      <div>
        <h1>線上訂票</h1>
        <el-text type="info">完成日期、場次與座位選擇後，即可建立訂單。</el-text>
      </div>
    </el-header>

    <el-main>
      <el-card shadow="never" class="process-card">
        <el-steps :active="step" finish-status="success" align-center>
          <el-step v-for="item in bookingSteps" :key="item" :title="item" />
        </el-steps>
      </el-card>

      <el-card v-loading="loading" shadow="never" class="content-card">
        <template #header>
          <div class="card-title">
            <span>{{ bookingSteps[step] }}</span>
            <el-tag type="primary" effect="light">{{ selectedActivityName }}</el-tag>
          </div>
        </template>

        <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />
        <section v-if="step === 0">
          <el-text type="info">請選擇想參加的演出日期。</el-text>
          <el-radio-group v-model="selectedDate" class="selection-list">
            <el-radio v-for="date in dates" :key="date.value" :value="date.value" border>
              <strong>{{ date.label }}</strong>
              <span>{{ selectedActivityName }} 場次</span>
            </el-radio>
          </el-radio-group>
        </section>

        <section v-else-if="step === 1">
          <el-text type="info">{{ selectedDate }} 尚有以下可售場次。</el-text>
          <el-radio-group v-model="selectedSession" class="selection-list">
            <el-radio v-for="session in sessions" :key="session.id" :value="session.value"
              :disabled="session.available <= 0" border>
              <strong>{{ session.label }}</strong>
              <span>剩餘 {{ session.available }} 張</span>
            </el-radio>
          </el-radio-group>
        </section>

        <section v-else class="order-summary">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="編號">{{ sessionId }}</el-descriptions-item>
            <el-descriptions-item label="活動">{{ selectedActivityName }}</el-descriptions-item>
            <el-descriptions-item label="日期">{{ selectedDate }}</el-descriptions-item>
            <el-descriptions-item label="場次">{{ selectedSession }}</el-descriptions-item>
            <el-descriptions-item label="票價">{{ money(selectedPrice) }}</el-descriptions-item>
            <el-descriptions-item v-if="selectedZone" label="分區">{{ selectedZone.name }}</el-descriptions-item>
            <el-descriptions-item v-if="selectedType" label="票種">{{ selectedType.name }}</el-descriptions-item>
            <el-descriptions-item label="座位">
              {{ selectedSeats.length ? selectedSeats.join('、') : '尚未選擇' }}
            </el-descriptions-item>
          </el-descriptions>

          <div v-if="salesSettings?.configured" class="sales-options">
            <el-alert :type="salesSettings.remainingAllowance > 0 ? 'info' : 'warning'" :closable="false"
              :title="`每會員限購 ${salesSettings.maxTicketsPerMember} 張，已購／有效待付款 ${salesSettings.memberTicketQuantity} 張，還可購買 ${salesSettings.remainingAllowance} 張。`" />
            <el-form label-position="top">
              <el-form-item label="票種">
                <el-select v-model="selectedTicketType" aria-label="票種">
                  <el-option v-for="type in salesSettings.ticketTypes" :key="type.id" :value="type.id"
                    :label="type.name" />
                </el-select>
              </el-form-item>
              <el-text v-if="selectedType?.eligibility" type="warning">{{ selectedType.eligibility }}</el-text>
              <el-form-item label="分區">
                <el-select v-model="selectedZoneFilter" aria-label="分區" placeholder="所有分區">
                  <el-option label="所有分區" value="" />
                  <el-option v-for="zone in salesSettings.zones" :key="zone.id" :value="zone.id"
                    :label="`${zone.name} · ${money(zone.prices[selectedTicketType] ?? null)} · 剩餘 ${zone.available} 席`" />
                </el-select>
              </el-form-item>
            </el-form>
          </div>

          <div class="seat-section">
            <div class="seat-section__heading">
              <div>
                <h3>選擇座位</h3>
                <el-text type="info">每次訂單可選一個座位；選位後顯示實際票價。</el-text>
              </div>
              <el-tag v-if="selectedSeats.length" type="success" effect="light">
                已選 {{ selectedSeats[0] }}
              </el-tag>
            </div>
            <SeatMap v-model="selectedSeats" :max-selection="1" :unavailable-seats="unavailableSeats"
              :activity-id="activityId" :seat-layout="seatLayout" :row-zones="seatZones"
              :filtered-seats="filteredSeats" />
            <el-button :disabled="loading || submittingBooking" @click="refreshBooking">更新剩餘座位與額度</el-button>
          </div>
        </section>

        <div class="form-actions">
          <el-button :disabled="step === 0 || loading || submittingBooking" @click="previousStep">上一步</el-button>
          <el-button :disabled="nextStep_disabled" v-if="step < bookingSteps.length - 1" type="primary"
            @click="nextStep">下一步</el-button>
          <el-button v-else type="primary" :disabled="!canBook || submittingBooking"
            @click="ticketDialogVisible = true">建立訂單</el-button>
        </div>
      </el-card>
    </el-main>
  </el-container>

  <el-dialog v-model="ticketDialogVisible" title="確認建立訂單" width="min(520px, 92vw)">
    <el-alert title="建立訂單後，請於期限內完成付款。" type="warning" :closable="false" show-icon />
    <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" />
    <p v-if="selectedSeats.length" class="confirm-seat">座位：{{ selectedSeats.join('、') }}</p>
    <p v-if="selectedZone" class="confirm-seat">{{ selectedZone.name }} · {{ selectedType?.name }}</p>
    <p class="confirm-seat">{{ money(selectedPrice) }}</p>
    <template #footer>
      <el-button :disabled="submittingBooking" @click="ticketDialogVisible = false">返回修改</el-button>
      <el-button type="primary" :loading="submittingBooking" :disabled="!canBook" @click="createOrder">確認建立</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.sales-options {
  display: grid;
  gap: 16px;
  margin-top: 20px;
}

.sales-options .el-select {
  width: min(100%, 480px);
}

.booking-page {
  min-height: 100%;
  background: #f7f8fa;
}

.page-header {
  height: auto;
  padding: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-header h1 {
  margin: 0 0 8px;
  font-size: 26px;
}

.process-card,
.content-card {
  margin-bottom: 20px;
}

.content-card {
  max-width: 860px;
}

.card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.selection-list {
  display: grid;
  gap: 12px;
  width: 100%;
  margin-top: 20px;
}

.selection-list :deep(.el-radio) {
  width: 100%;
  height: auto;
  margin: 0;
  padding: 16px;
}

.selection-list :deep(.el-radio__label) {
  display: flex;
  justify-content: space-between;
  width: 100%;
  padding-left: 10px;
}

.selection-list span {
  color: var(--el-text-color-secondary);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 28px;
}

.confirm-seat {
  margin: 22px 0 0;
  font-weight: 600;
}

.order-summary {
  margin-top: 8px;
}

.seat-section {
  margin-top: 24px;
}

.seat-section__heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.seat-section__heading h3 {
  margin: 0 0 6px;
}

@media (max-width: 767px) {
  .booking-page>.el-main {
    padding: 12px 0;
    min-width: 0;
  }

  .page-header {
    padding: 16px;
  }

  .content-card :deep(.el-card__body),
  .content-card :deep(.el-card__header),
  .process-card :deep(.el-card__body) {
    padding: 16px;
  }

  .card-title {
    flex-wrap: wrap;
    gap: 10px;
  }

  .card-title .el-tag {
    max-width: 100%;
    height: auto;
    white-space: normal;
    line-height: 1.6;
  }

  .selection-list :deep(.el-radio__label) {
    min-width: 0;
    white-space: normal;
    overflow-wrap: anywhere;
  }

  .form-actions .el-button {
    flex: 1;
    margin-left: 0;
    min-height: 44px;
  }

  .order-summary :deep(.el-descriptions__table) {
    table-layout: fixed;
  }

  .order-summary :deep(.el-descriptions__label) {
    width: 64px;
  }

  .order-summary :deep(.el-descriptions__content) {
    overflow-wrap: anywhere;
  }

  .process-card :deep(.el-step__title) {
    font-size: 12px;
    line-height: 1.5;
    margin-top: 8px;
  }

  .seat-section>.el-button {
    width: 100%;
    min-height: 44px;
    margin-top: 12px;
  }

  .page-header {
    align-items: flex-start;
    gap: 14px;
    flex-direction: column;
  }

  .selection-list :deep(.el-radio__label) {
    flex-direction: column;
    gap: 5px;
  }

  .process-card {
    overflow-x: auto;
  }

  .seat-section__heading {
    flex-direction: column;
  }
}
</style>
