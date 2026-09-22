<script setup>
import { useRoute } from 'vue-router'
import { bookingApi } from '@/services/api'
import SeatMap from '@/components/booking/SeatMap.vue'


const route = useRoute()
const nextStep_disabled = ref(false)
const selectedActivityName = computed(() => {
  let activity_id = route.query.activity_id
  let activity_name = route.query.activity_name
  if (activity_id === undefined) {
    nextStep_disabled.value = true
  } else {
    nextStep_disabled.value = false
    activityId.value = route.query.activity_id
    selectOnlyActivities()
  }
  return activity_name
})
const selectOnlyActivities = async () => {
  const response_selectOnlyActivities = await bookingApi({
    method: 'get',
    url: '/selectOnlyActivities',
    params: {
      activity_id: route.query.activity_id
    },
  });
  dates.value = response_selectOnlyActivities.data
}
const handleDateChange = async () => {
  const response_selectOnlySession = await bookingApi({
    method: 'get',
    url: '/selectOnlySession',
    params: {
      date: selectedDate.value,
      activity_id: route.query.activity_id
    },
  });
  sessions.value = response_selectOnlySession.data
  const responsePrice = await bookingApi({
    method: 'get',
    url: '/selectOnlyActivitiesPrice',
    params: {
      activity_id: route.query.activity_id
    },
  });
  selectedPrice.value = responsePrice.data.price
}

const selectOnlyUnavailableSeats = async () => {
  const response = await bookingApi({
    method: 'get',
    url: '/selectOnlyUnavailableSeats',
    params: {
      activity_id: route.query.activity_id,
      selected_date: selectedDate.value,
      selected_session: selectedSession.value,
    },
  });
  unavailableSeats.value = new Set(response.data)
}

const step = ref(0)
const ticketDialogVisible = ref(false)
const activityId = ref()
const sessionId = ref()
const selectedDate = ref()
const selectedSession = ref()
const selectedPrice = ref(0)
const selectedSeats = ref([])
const unavailableSeats = ref(new Set())

const bookingSteps = ['選日期', '選場次', '建立訂單']
const dates = ref([])
const sessions = ref([])

const ticketForm = reactive(
  {
    name: '',
    date: '',
    status: 'PENDING_PAYMENT',
    price: 0,
    seat: ''
  }
)

const tickets = ref([])

const nextStep = () => {
  if (step.value < bookingSteps.length - 1) {
    step.value += 1
  }
  if (step.value === 1) {
    if (selectedDate.value === undefined) {
      step.value = 0
    } else {
      handleDateChange()
    }
  } else if (step.value === 2) {
    if (selectedSession.value === undefined) {
      step.value = 1
    } else {
      selectOnlyUnavailableSeats()
    }
  }
}

const previousStep = () => {
  if (step.value > 0) {
    step.value -= 1
  }
}

const pendingBooking = ref(null)
const submittingBooking = ref(false)
const createOrder = async () => {
  if (submittingBooking.value) return
  if (selectedSeats.value.length != 1) {
    ElMessage({
      type: 'error',
      message: `${'請選擇座位'}`,
    })
    return
  }
  Object.assign(
    ticketForm,
    {
      activity_id: route.query.activity_id,
      name: selectedActivityName.value,
      date: selectedDate.value,
      time: selectedSession.value,
      price: selectedPrice.value,
      status: 'PENDING_PAYMENT',
      seat: selectedSeats.value[0]
    }
  )
  const fingerprint = JSON.stringify([ticketForm.session_id, ticketForm.activity_id, ticketForm.seat])
  if (pendingBooking.value?.fingerprint !== fingerprint) {
    pendingBooking.value = { fingerprint, key: crypto.randomUUID() }
  }
  submittingBooking.value = true
  try {
    const response = await bookingApi({
      method: 'post',
      url: '/saveTicket',
      headers: {
        // 冪等鍵（Idempotency Key）
        'Idempotency-Key': pendingBooking.value.key
      },
      data: ticketForm,
    });
    ticketDialogVisible.value = false
    step.value = 0
    tickets.value = response.data.data
    pendingBooking.value = null
  } catch (error) {
    ticketDialogVisible.value = true
    if (error.response?.status === 409) {
      await selectOnlyUnavailableSeats()
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

      <el-card shadow="never" class="content-card">
        <template #header>
          <div class="card-title">
            <span>{{ bookingSteps[step] }}</span>
            <el-tag type="primary" effect="light">{{ selectedActivityName }}</el-tag>
          </div>
        </template>

        <section v-if="step === 0">
          <el-text type="info">請選擇想參加的演出日期。</el-text>
          <el-radio-group v-model="selectedDate" class="selection-list">
            <el-radio v-for="date in dates" :key="date.value" :label="date.value" border>
              <strong>{{ date.label }}</strong>
              <span>{{ selectedActivityName }} 場次</span>
            </el-radio>
          </el-radio-group>
        </section>

        <section v-else-if="step === 1">
          <el-text type="info">{{ selectedDate }} 尚有以下可售場次。</el-text>
          <el-radio-group v-model="selectedSession" class="selection-list">
            <el-radio v-for="session in sessions" :key="session.value" :label="session.value" border>
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
            <el-descriptions-item label="票價">NT$ {{ selectedPrice }}</el-descriptions-item>
            <el-descriptions-item label="座位">
              {{ selectedSeats.length ? selectedSeats.join('、') : '尚未選擇' }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="seat-section">
            <div class="seat-section__heading">
              <div>
                <h3>選擇座位</h3>
                <el-text type="info">目前可選擇一個座位；既有訂單送出流程維持不變。</el-text>
              </div>
              <el-tag v-if="selectedSeats.length" type="success" effect="light">
                已選 {{ selectedSeats[0] }}
              </el-tag>
            </div>
            <SeatMap v-model="selectedSeats" :max-selection="1" :unavailable-seats="unavailableSeats"
              :activity-id="activityId" />
          </div>
        </section>

        <div class="form-actions">
          <el-button :disabled="step === 0" @click="previousStep">上一步</el-button>
          <el-button :disabled="nextStep_disabled" v-if="step < bookingSteps.length - 1" type="primary"
            @click="nextStep">下一步</el-button>
          <el-button v-else type="primary" @click="ticketDialogVisible = true">建立訂單</el-button>
        </div>
      </el-card>
    </el-main>
  </el-container>

  <el-dialog v-model="ticketDialogVisible" title="確認建立訂單" width="min(520px, 92vw)">
    <el-alert title="建立訂單後，請於期限內完成付款。" type="warning" :closable="false" show-icon />
    <p v-if="selectedSeats.length" class="confirm-seat">座位：{{ selectedSeats.join('、') }}</p>
    <p class="confirm-seat">NT$ {{ selectedPrice }}</p>
    <template #footer>
      <el-button @click="ticketDialogVisible = false">返回修改</el-button>
      <el-button type="primary" @click="createOrder">確認建立</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.booking-page {
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
