<script setup>
import { computed, toRefs } from 'vue'

const props = defineProps({
  sessions: { type: Array, required: true },
  orders: { type: Array, required: true },
  loading: { type: Boolean, required: true },
  error: { type: String, required: true },
  updatedAt: { type: String, required: true },
  loaded: { type: Boolean, required: true },
})
const emit = defineEmits(['refresh'])
const { sessions, orders } = toRefs(props)
const number = (value) => Number(value ?? 0)
const integerFormat = new Intl.NumberFormat('zh-TW')
const moneyFormat = new Intl.NumberFormat('zh-TW', {
  style: 'currency', currency: 'TWD', maximumFractionDigits: 2,
})
const formatNumber = (value) => integerFormat.format(value)
const formatMoney = (value) => moneyFormat.format(value)
const totals = computed(() => sessions.value.reduce((total, session) => ({
  capacity: total.capacity + number(session.capacity),
  sold: total.sold + number(session.sold),
  reserved: total.reserved + number(session.reserved),
}), { capacity: 0, sold: 0, reserved: 0 }))
const revenue = computed(
  () => orders.value.reduce((total, order) =>
    total + (order.status === 'PAID' ? number(order.payprice) : 0), 0)
)
const rate = (sold, capacity) => capacity > 0 ? `${(sold / capacity * 100).toFixed(1)}%` : '—'
const statusDefinitions = [
  { value: 'PENDING_PAYMENT', label: '待付款', type: 'warning' },
  { value: 'PAID', label: '已付款', type: 'success' },
  { value: 'CANCELLED', label: '已取消', type: 'info' },
  { value: 'EXPIRED', label: '已逾期', type: 'danger' },
  { value: 'REFUNDED', label: '已退款', type: 'info' },
]
const statusCounts = computed(() => {
  const counts = orders.value.reduce((result, order) => {
    result[order.status] = (result[order.status] ?? 0) + 1
    return result
  }, {})
  const known = statusDefinitions.map((status) => ({ ...status, count: counts[status.value] ?? 0 }))
  const knownTotal = known.reduce((total, status) => total + status.count, 0)
  const other = orders.value.length - knownTotal
  return other ? [...known, { value: 'OTHER', label: '其他狀態', type: 'info', count: other }] : known
})
const rows = computed(() => sessions.value.map((session) => {
  const capacity = number(session.capacity)
  const sold = number(session.sold)
  const reserved = number(session.reserved)
  return { ...session, capacity, sold, reserved, available: capacity - sold - reserved }
}))

</script>

<template>
  <section v-loading="loading" class="dashboard" aria-label="銷售儀表板">
    <div class="dashboard-toolbar">
      <div>
        <h2>銷售概況</h2>
        <p>所有活動與場次的累計銷售資料</p>
      </div>
      <div class="refresh-controls">
        <span v-if="updatedAt">更新時間：{{ updatedAt }}（台北）</span>
        <el-button :loading="loading" @click="emit('refresh')">重新整理</el-button>
      </div>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />
    <template v-if="loaded">
      <div class="metric-grid">
        <el-card shadow="never">
          <div class="metric-label">累計營收</div>
          <strong>{{ formatMoney(revenue) }}</strong>
          <p>已付款訂單的實付金額</p>
        </el-card>
        <el-card shadow="never">
          <div class="metric-label">整體售票率</div>
          <strong>{{ rate(totals.sold, totals.capacity) }}</strong>
          <p>已售 {{ formatNumber(totals.sold) }} ／容量 {{ formatNumber(totals.capacity) }} 張</p>
        </el-card>
        <el-card shadow="never">
          <div class="metric-label">可售庫存</div>
          <strong>{{ formatNumber(totals.capacity - totals.sold - totals.reserved) }} 張</strong>
          <p>保留中 {{ formatNumber(totals.reserved) }} 張</p>
        </el-card>
        <el-card shadow="never">
          <div class="metric-label">待付款訂單</div>
          <strong>{{ formatNumber(statusCounts[0].count) }} 筆</strong>
          <p>全部訂單 {{ formatNumber(orders.length) }} 筆</p>
        </el-card>
      </div>
      <el-card shadow="never">
        <template #header>
          <h3>訂單狀態</h3>
        </template>
        <div class="status-grid">
          <div v-for="status in statusCounts" :key="status.value" class="status-item">
            <el-tag :type="status.type">{{ status.label }}</el-tag>
            <strong>{{ formatNumber(status.count) }} 筆</strong>
          </div>
        </div>
      </el-card>
      <el-card shadow="never">
        <template #header>
          <h3>場次銷售與庫存</h3>
        </template>
        <el-table :data="rows" stripe row-key="id" empty-text="目前沒有場次資料">
          <el-table-column prop="name" label="活動" min-width="180" />
          <el-table-column prop="id" label="場次編號" min-width="180" />
          <el-table-column label="演出時間" min-width="190">
            <template #default="{ row }">{{ row.date }} {{ row.time }}</template>
          </el-table-column>
          <el-table-column prop="capacity" label="容量" min-width="90" align="right" />
          <el-table-column prop="sold" label="已售" min-width="90" align="right" />
          <el-table-column prop="reserved" label="保留中" min-width="90" align="right" />
          <el-table-column prop="available" label="可售" min-width="90" align="right" />
          <el-table-column label="售票率" min-width="180">
            <template #default="{ row }">
              <el-progress v-if="row.capacity > 0"
                :percentage="Math.min(100, Math.max(0, row.sold / row.capacity * 100))"
                :format="() => rate(row.sold, row.capacity)" />
              <span v-else>—</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </section>
</template>

<style scoped>
.dashboard {
  display: grid;
  gap: 20px;
  min-height: 180px;
}

.dashboard-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

h2,
h3 {
  margin: 0;
}

h2 {
  font-size: 22px;
}

h3 {
  font-size: 17px;
}

p {
  margin: 10px 0 0;
  color: #606266;
  font-size: 13px;
}

.refresh-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.refresh-controls span {
  color: #606266;
  font-size: 13px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.metric-label {
  margin-bottom: 14px;
  color: #606266;
}

.metric-grid strong {
  font-size: 26px;
  overflow-wrap: anywhere;
}

.status-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20px 36px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

@media (max-width: 1100px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 600px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
