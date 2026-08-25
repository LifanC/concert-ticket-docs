<script setup>
import { useRoute } from 'vue-router'
import { bookingApi } from '@/services/api'


const route = useRoute()
const nextStep_disabled = ref(false)
const selectedActivityName = computed(() => {
  let activity_id = route.query.activity_id
  let session_id = route.query.session_id
  let activity_sessionid = route.query.activity_sessionid
  let activity_name = route.query.activity_name
  if (activity_id === undefined || session_id === undefined || activity_sessionid === undefined) {
    nextStep_disabled.value = true
  } else {
    nextStep_disabled.value = false
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
  selectedDate.value = route.query.activity_date
  dates.value = response_selectOnlyActivities.data
  handleDateChange()
}

const handleDateChange = async () => {
  const response_selectOnlySession = await bookingApi({
    method: 'get',
    url: '/selectOnlySession',
    params: {
      date: route.query.activity_date,
      activity_id: route.query.activity_id
    },
  });
  sessionId.value = route.query.session_id
  selectedSession.value = route.query.activity_time
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

const step = ref(0)
const ticketDialogVisible = ref(false)
const myTicketsVisible = ref(false)
const paypriceDialogVisible = ref(false)
const sessionId = ref()
const selectedDate = ref()
const selectedSession = ref()
const selectedPrice = ref(0)

const bookingSteps = ['選日期', '選場次', '建立訂單']
const dates = ref([])
const sessions = ref([])

const ticketForm = reactive(
  {
    customer: '',
    name: '',
    date: '',
    status: 'PENDING_PAYMENT',
    price: 0
  }
)

const paypriceForm = reactive(
  {
    orderno: '',
    name: '',
    date: '',
    time: ''
  }
)

const tickets = ref([])

const nextStep = () => {
  if (step.value < bookingSteps.length - 1) {
    step.value += 1
  }
}

const previousStep = () => {
  if (step.value > 0) {
    step.value -= 1
  }
}

const createOrder = async () => {
  Object.assign(
    ticketForm,
    {
      session_id: route.query.session_id,
      activity_id: route.query.activity_id,
      name: selectedActivityName.value,
      date: selectedDate.value,
      time: selectedSession.value,
      price: selectedPrice,
      status: 'PENDING_PAYMENT'
    }
  )
  try {
    const response = await bookingApi({
      method: 'post',
      url: '/saveTicket',
      data: ticketForm,
    });
    myTicketsVisible.value = true
    ticketDialogVisible.value = false
    tickets.value = response.data.data
  } catch (error) {
    myTicketsVisible.value = false
    ticketDialogVisible.value = true
  }
}

const cancelOrder = async (ticket) => {
  Object.assign(
    ticketForm,
    {
      orderno: ticket.orderno,
      session_id: ticket.session_id,
      status: 'PENDING_PAYMENT'
    }
  )
  try {
    const response = await bookingApi({
      method: 'put',
      url: '/cancelOrder',
      data: ticketForm,
    });
    let data = response.data.data[0] ?? {}
    if (data.judge) {
      ticket.status = 'CANCELLED'
    }
  } catch (error) {
    myTicketsVisible.value = false
  }
}

const paypricedataForm = reactive(
  {
    orderno: '',
    activity: '',
    date: '',
    time: '',
    salesdate: '',
    salestime: '',
  }
)
const payprice = async (payprice) => {
  Object.assign(
    paypriceForm,
    {
      name: payprice.name,
      date: payprice.date,
      time: payprice.time
    }
  )
  try {
    const response = await bookingApi({
      method: 'post',
      url: '/sessionSalesDate',
      data: paypriceForm,
    });
    Object.assign(
      paypricedataForm,
      {
        orderno: payprice.orderno,
        activity: response.data.activity,
        date: response.data.date,
        time: response.data.time,
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
  try {
    const response = await bookingApi({
      method: 'put',
      url: '/dopayprice',
      data: paypricedataForm,
    });
    let data = response.data.data[0] ?? {}
    if (data.judge) {
      paypriceDialogVisible.value = false
      myTicketsVisible.value = false
    }
  } catch (error) {
    paypriceDialogVisible.value = false
    myTicketsVisible.value = false
  }
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
    'PENDING_PAYMENT': 'success',
    'PAID': 'success',
    'CANCELLED': 'warning',
    'EXPIRED': 'warning',
    'REFUNDED': 'info'
  }[status] || 'warning'
)
const myTicketsVisibleDialog = async () => {
  myTicketsVisible.value = true
  // selectAllTicket
  const response = await bookingApi({
    method: 'get',
    url: '/selectOnlyTicket',
    params: {},
  });
  tickets.value = response.data
}
</script>

<template>
  <el-container class="booking-page">
    <el-header class="page-header">
      <div>
        <h1>線上訂票</h1>
        <el-text type="info">完成日期、場次與座位選擇後，即可建立訂單。</el-text>
      </div>
      <el-button plain type="primary" @click="myTicketsVisibleDialog">查看我的票券</el-button>
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
          <el-radio-group v-model="selectedDate" class="selection-list" @change="handleDateChange">
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
          </el-descriptions>
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
    <p class="confirm-seat">NT$ {{ selectedPrice }}</p>
    <template #footer>
      <el-button @click="ticketDialogVisible = false">返回修改</el-button>
      <el-button type="primary" @click="createOrder">確認建立</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="paypriceDialogVisible" title="付款" width="min(520px, 92vw)">
    <el-alert title="請完成付款。" type="warning" :closable="false" show-icon />
    <p class="confirm-seat">
      {{ paypricedataForm.orderno }} 請於期限內 {{ paypricedataForm.salesdate }} {{ paypricedataForm.salestime }} 完成付款
    </p>
    <template #footer>
      <el-button @click="paypriceDialogVisible = false">返回</el-button>
      <el-button type="primary" @click="dopayprice">付款</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="myTicketsVisible" title="我的票券" width="min(1000px, 94vw)">
    <el-table :data="tickets" stripe empty-text="目前沒有票券">
      <el-table-column prop="orderno" label="訂單編號" min-width="145" />
      <el-table-column prop="session_id" label="編號" min-width="100" />
      <el-table-column prop="name" label="活動" min-width="150" />
      <el-table-column prop="date" label="場次" min-width="100" />
      <el-table-column prop="time" label="時間" min-width="100" />
      <el-table-column prop="timename" label="" min-width="70" />
      <el-table-column label="狀態" width="100">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.status)" effect="light">{{ ticketsMap[scope.row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="scope">
          <el-button text type="danger" :disabled="scope.row.status === 'PAID' || scope.row.status === 'CANCELLED'"
            @click="cancelOrder(scope.row)">取消訂單</el-button>
        </template>
      </el-table-column>
      <el-table-column label="" width="100">
        <template #default="scope">
          <el-button text :type="statusType(scope.row.status)"
            :disabled="scope.row.status === 'PAID' || scope.row.status === 'CANCELLED'" @click="payprice(scope.row)">付款
          </el-button>
        </template>
      </el-table-column>
    </el-table>
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
}
</style>
