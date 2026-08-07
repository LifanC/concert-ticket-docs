<script setup>
import { useRouter, useRoute } from 'vue-router'
import axios from "axios";
import { toFindCookie, addCookie, clearCookie } from "@/components/componentsJs/cookie";
import { email } from '@vuelidate/validators';
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";

axios.defaults.baseURL = 'http://localhost:8080/api/v1/booking'
axios.defaults.withCredentials = true;

const route = useRoute()
const router = useRouter()
const nextStep_disabled = ref(false)
const selectedActivityName = computed(() => {
  let activity_id = route.query.activity_id
  let activity_sessionid = route.query.activity_sessionid
  let activity_name = route.query.activity_name
  let activity_date = route.query.activity_date
  let activity_time = route.query.activity_time
  if (activity_id === undefined || activity_sessionid === undefined) {
    nextStep_disabled.value = true
  } else {
    nextStep_disabled.value = false
    selectOnlyActivities()
  }
  return activity_name
})
const selectOnlyActivities = async () => {
  try {
    const response = await axios({
      method: 'get',
      url: '/selectOnlyActivities',
      params: {
        activity_name: route.query.activity_name
      }
    });
    selectedDate.value = route.query.activity_date
    handleDateChange(route.query.activity_date)
    dates.value = response.data
  } catch (error) {
    let status = error.response.status ?? {}
    if (status === 403) {
      router.push('/User')
    }
  }
}

executeFirst()
async function executeFirst() {
  let accessToken = toFindCookie('accessToken')
  if (accessToken) {
    // selectAllTicket
    try {
      const response = await axios({
        method: 'get',
        url: '/selectOnlyTicket',
        params: {
          email: toFindCookie('email')
        }
      });
      tickets.value = response.data
    } catch (error) {
      let status = error.response.status ?? {}
      if (status === 403) {
        router.push('/User')
      }
    }
  } else {
    router.push('/User')
  }
}

const handleDateChange = async (datedate) => {
  try {
    const response = await axios({
      method: 'get',
      url: '/selectOnlySession',
      params: {
        date: datedate
      }
    });
    sessions.value = response.data
    selectedSession.value = route.query.activity_time
    const responsePrice = await axios({
      method: 'get',
      url: '/selectOnlyActivitiesPrice',
      params: {
        activity_id: route.query.activity_id
      }
    });
    selectedPrice.value = responsePrice.data.price
  } catch (error) {
    let status = error.response.status ?? {}
    if (status === 403) {
      router.push('/User')
    }
  }
}

const step = ref(0)
const ticketDialogVisible = ref(false)
const myTicketsVisible = ref(false)
const selectedDate = ref()
const selectedSession = ref()
const selectedPrice = ref(0)

const bookingSteps = ['選日期', '選場次', '建立訂單']
const dates = ref([])
const sessions = ref([])

const ticketForm = reactive(
  {
    orderno: '',
    customer: '',
    email: '',
    name: '',
    date: '',
    status: '已成立',
    price: 0
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
  let accessToken = toFindCookie('accessToken')
  if (accessToken) {
    const now = new Date()
    const date =
      now.getFullYear() +
      String(now.getMonth() + 1).padStart(2, '0') +
      String(now.getDate()).padStart(2, '0')
    const orderno = `CT${date}${String(tickets.value.length + 1).padStart(3, '0')}`
    Object.assign(
      ticketForm,
      {
        orderno: orderno,
        email: toFindCookie('email'),
        name: selectedActivityName.value,
        date: selectedDate.value,
        time: selectedSession.value,
        price: selectedPrice
      }
    )
    try {
      const response = await axios({
        method: 'post',
        url: '/saveTicket',
        data: ticketForm,
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      });
      myTicketsVisible.value = true
      ticketDialogVisible.value = false
      tickets.value = response.data.data
    } catch (error) {
      let status = error.response.status ?? {}
      if (status === 403) {
        router.push('/User')
      }
      myTicketsVisible.value = false
      ticketDialogVisible.value = true
    }
  } else {
    ElMessage({
      type: 'error',
      message: `${'尚未登入'}`,
    })
    myTicketsVisible.value = true
    ticketDialogVisible.value = false
  }
}

const cancelOrder = async (ticket) => {
  let accessToken = toFindCookie('accessToken')
  if (accessToken) {
    Object.assign(
      ticketForm,
      {
        orderno: ticket.orderno,
        email: toFindCookie('email'),
        status: '已取消'
      }
    )
    try {
      const response = await axios({
        method: 'put',
        url: '/cancelOrder',
        data: ticketForm,
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      });
      let data = response.data.data[0] ?? {}
      if (data.judge) {
        ticket.status = '已取消'
      }
    } catch (error) {
      let status = error.response.status ?? {}
      if (status === 403) {
        router.push('/User')
      }
      myTicketsVisible.value = false
    }
  } else {
    ElMessage({
      type: 'error',
      message: `${'尚未登入'}`,
    })
  }
}
const statusType = (status) => (
  {
    '已成立': 'success',
    '已取消': 'info'
  }[status] || 'warning'
)

let client;
function connectWebSocket() {
  console.log("開始建立 websocket");
  const socket = new SockJS(
    "http://localhost:8080/api/ws?username=" + encodeURIComponent(toFindCookie('email'))
  );
  client = new Client({
    webSocketFactory: () => socket,
    debug: (msg) => {
      console.log(msg);
    },
    onConnect() {
      client.subscribe(
        "/user/queue/notifications",
        (msg) => {
          const notification = JSON.parse(msg.body);
          console.log(
            "收到通知",
            notification
          );
          ElMessage({
            type: 'info',
            message:
              notification.title +
              "：" +
              notification.content
          });
        }
      );
    },
    onStompError: (frame) => {
      console.error(
        "STOMP ERROR",
        frame
      );
    },

    onWebSocketError: (error) => {
      console.error(
        "WS ERROR",
        error
      );
    }
  });
  client.activate();
}
onMounted(() => {
  connectWebSocket();
});
onUnmounted(() => {
  if (client) {
    client.deactivate();
  }
});
</script>

<template>
  <el-container class="booking-page">
    <el-header class="page-header">
      <div>
        <h1>線上訂票</h1>
        <el-text type="info">完成日期、場次與座位選擇後，即可建立訂單。</el-text>
      </div>
      <el-button plain type="primary" @click="myTicketsVisible = true">查看我的票券</el-button>
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

  <el-dialog v-model="myTicketsVisible" title="我的票券" width="min(820px, 94vw)">
    <el-table :data="tickets" stripe empty-text="目前沒有票券">
      <el-table-column prop="orderno" label="訂單編號" min-width="145" />
      <el-table-column prop="name" label="活動" min-width="150" />
      <el-table-column prop="date" label="場次" min-width="160" />
      <el-table-column label="狀態" width="100">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.status)" effect="light">{{ scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="scope">
          <el-button text type="danger" :disabled="scope.row.status === '已取消'"
            @click="cancelOrder(scope.row)">取消訂單</el-button>
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
