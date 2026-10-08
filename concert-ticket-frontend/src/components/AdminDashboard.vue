<script setup>
import { computed, toRefs, ref, watch, onBeforeUnmount } from 'vue'
import { Chart, BarController, BarElement, CategoryScale, LinearScale, DoughnutController, ArcElement, Tooltip, Legend } from 'chart.js'

Chart.register(BarController, BarElement, CategoryScale, LinearScale, DoughnutController, ArcElement, Tooltip, Legend)

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
const ticketLegend = [
  { label: '已售', color: '#409eff' },
  { label: '保留中', color: '#e6a23c' },
  { label: '可售', color: '#dce5ef' },
]
const statusColors = ['#e6a23c', '#67c23a', '#909399', '#f56c6c', '#9b72cf', '#409eff']
const chartStatuses = computed(() => statusCounts.value.map((status, index) => ({
  ...status, color: statusColors[index],
  percentage: orders.value.length ? status.count / orders.value.length * 100 : 0,
})))
const sessionCanvas = ref(null)
const statusCanvas = ref(null)
let sessionChart = null
let statusChart = null
const sessionChartHeight = computed(() => Math.max(260, rows.value.length * 58 + 80))
const sessionChartData = computed(() => ({
  labels: rows.value.map(row => `${row.name || '未命名活動'} · ${row.id}`),
  datasets: ['sold', 'reserved', 'available'].map((key, index) => ({
    label: ticketLegend[index].label,
    backgroundColor: ticketLegend[index].color,
    data: rows.value.map(row => Math.max(0, row[key])),
  })),
}))
const statusChartData = computed(() => ({
  labels: chartStatuses.value.map(status => status.label),
  datasets: [{ data: chartStatuses.value.map(status => status.count), backgroundColor: statusColors }],
}))
watch([sessionCanvas, sessionChartData], ([canvas, data]) => {
  if (!canvas) {
    sessionChart?.destroy()
    sessionChart = null
    return
  }
  if (sessionChart) {
    sessionChart.data = data
    sessionChart.update()
    return
  }
  sessionChart = new Chart(canvas, {
    type: 'bar', data,
    options: {
      responsive: true, maintainAspectRatio: false, indexAxis: 'y',
      scales: {
        x: { stacked: true, beginAtZero: true, ticks: { precision: 0 }, title: { display: true, text: '票數（張）' } },
        y: {
          stacked: true, ticks: {
            callback(value) {
              const label = this.getLabelForValue(value)
              return label.length > 22 ? `${label.slice(0, 22)}…` : label
            }
          }
        },
      },
      plugins: {
        legend: { position: 'top' },
        tooltip: {
          callbacks: {
            title: (items) => {
              const row = rows.value[items[0]?.dataIndex]
              return row ? `${row.name || '未命名活動'} · ${row.id}\n${row.date || ''} ${row.time || ''}` : ''
            },
            label: (context) => `${context.dataset.label}：${formatNumber(context.parsed.x)} 張`,
          }
        },
      },
    },
  })
}, { flush: 'post' })
watch([statusCanvas, statusChartData], ([canvas, data]) => {
  if (!canvas) {
    statusChart?.destroy()
    statusChart = null
    return
  }
  if (statusChart) {
    statusChart.data = data
    statusChart.update()
    return
  }
  statusChart = new Chart(canvas, {
    type: 'doughnut', data,
    options: {
      responsive: true, maintainAspectRatio: false, cutout: '65%',
      plugins: {
        legend: { position: 'bottom' },
        tooltip: {
          callbacks: {
            label: (context) => {
              const percentage = orders.value.length ? context.parsed / orders.value.length * 100 : 0
              return `${context.label}：${formatNumber(context.parsed)} 筆（${percentage.toFixed(1)}%）`
            }
          }
        },
      },
    },
  })
}, { flush: 'post' })
onBeforeUnmount(() => {
  sessionChart?.destroy()
  statusChart?.destroy()
})

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
      <div class="chart-grid">
        <el-card shadow="never">
          <template #header>
            <h3>各場次票況</h3>
          </template>
          <p>滑鼠移至長條查看票數，點擊圖例可顯示或隱藏票況。</p>
          <div v-if="rows.length" class="session-chart">
            <div class="canvas-container" :style="{ height: `${sessionChartHeight}px` }">
              <canvas ref="sessionCanvas" role="img" aria-label="各場次已售、保留中與可售票數比較；詳細數值請見下方場次銷售與庫存表格。" />
            </div>
          </div>
          <el-empty v-else description="目前沒有場次資料" :image-size="70" />
        </el-card>
        <el-card shadow="never">
          <template #header>
            <h3>訂單狀態</h3>
          </template>
          <div class="status-chart">
            <div v-if="orders.length" class="canvas-container status-canvas">
              <canvas ref="statusCanvas" role="img"
                :aria-label="`訂單狀態分布：${chartStatuses.map(status => `${status.label} ${status.count} 筆`).join('，')}`" />
            </div>
            <el-empty v-else description="目前沒有訂單資料" :image-size="70" />
            <div class="status-grid">
              <div v-for="status in chartStatuses" :key="status.value" class="status-item">
                <i class="legend-dot" :style="{ backgroundColor: status.color }" />
                <el-tag :type="status.type">{{ status.label }}</el-tag>
                <strong>{{ formatNumber(status.count) }} 筆</strong>
                <span>{{ status.percentage.toFixed(1) }}%</span>
              </div>
            </div>
          </div>
          <p v-if="!orders.length">目前沒有訂單資料</p>
        </el-card>
      </div>
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
  display: grid;
  gap: 14px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.chart-grid {
  display: grid;
  grid-template-columns: minmax(0, 3fr) minmax(0, 2fr);
  gap: 16px;
}

.chart-grid>* {
  min-width: 0;
}

.legend-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.session-chart {
  margin-top: 16px;
  max-height: 420px;
  overflow-y: auto;
}

.canvas-container {
  position: relative;
  width: 100%;
  min-width: 0;
}

.status-canvas {
  height: 280px;
}

.status-chart {
  display: grid;
  gap: 20px;
}

.status-item>span {
  font-size: 12px;
  color: #606266;
}

@media (max-width: 1100px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }

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
