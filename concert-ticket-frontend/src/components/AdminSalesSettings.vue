<script setup>
import { computed, ref } from 'vue'
import { adminApi } from '@/services/api'

const props = defineProps({ sessions: { type: Array, default: () => [] } })
const sessionId = ref('')
const drafts = ref({})
const palette = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399']
let nextId = 0
const loading = ref(false)
const saving = ref(false)
const error = ref('')
let loadVersion = 0
const session = computed(() => props.sessions.find((item) => item.id === sessionId.value))
const draft = computed(() => drafts.value[sessionId.value])
const rowLabels = computed(() => draft.value?.rowLabels ?? [])
const seatsPerRow = computed(() => draft.value?.seatsPerRow ?? 0)
const locked = computed(() => draft.value?.locked || saving.value || loading.value)
const moneyFormat = new Intl.NumberFormat('zh-TW', { style: 'currency', currency: 'TWD' })
const money = (value) => moneyFormat.format(value)
function applySettings(id, data) {
  drafts.value[id] = {
    version: data.version,
    configured: data.configured,
    locked: data.locked,
    rowLabels: data.rowLabels,
    seatsPerRow: Number(data.seatsPerRow),
    capacity: Number(data.capacity),
    defaultPrice: Number(data.defaultPrice ?? 0),
    limit: data.maxTicketsPerMember ?? 4,
    zones: data.configured ? data.zones.map((zone) => ({ ...zone, start: zone.rowStart, end: zone.rowEnd, price: Number(zone.price) }))
      : [{ id: ++nextId, name: '全場', color: palette[0], start: data.rowLabels[0] ?? '', end: data.rowLabels.at(-1) ?? '', price: Number(data.defaultPrice ?? 0) }],
    ticketTypes: data.configured ? data.ticketTypes.map((type) => ({ ...type, discountPercent: Number(type.pricePercent) }))
      : [{ id: ++nextId, name: '全票', discountPercent: 100, eligibility: '' }],
  }
}
function errorMessage(cause, fallback) {
  const data = cause.response?.data
  return data?.message ?? data?.data?.find((item) => item.remark)?.remark ?? fallback
}
async function selectSession(force = false) {
  const version = ++loadVersion
  const id = sessionId.value
  error.value = ''
  if (!session.value || (!force && drafts.value[id])) {
    loading.value = false; return
  }
  loading.value = true
  try {
    const response = await adminApi.get(`/sessions/${encodeURIComponent(id)}/sales-settings`)
    if (version === loadVersion) {
      applySettings(id, response.data)
    }
  } catch (cause) {
    if (version === loadVersion) {
      error.value = errorMessage(cause, '無法載入售票設定，請重試。')
    }
  } finally {
    if (version === loadVersion) {
      loading.value = false
    }
  }
}
const rowAssignments = computed(() => rowLabels.value.map((label, index) => ({
  label,
  zones: (draft.value?.zones ?? []).filter((zone) => {
    const start = rowLabels.value.indexOf(zone.start)
    const end = rowLabels.value.indexOf(zone.end)
    return start >= 0 && end >= start && index >= start && index <= end
  }),
})))
const issues = computed(() => {
  if (!draft.value) return []
  const errors = []
  if (!rowLabels.value.length || !Number.isInteger(seatsPerRow.value) || seatsPerRow.value <= 0) {
    errors.push('活動尚無完整座位配置，無法設定分區。')
  }
  if (rowLabels.value.length * seatsPerRow.value !== draft.value.capacity) {
    errors.push('座位配置與場次容量不一致，請先確認配置。')
  }
  if (!Number.isInteger(draft.value.limit) || draft.value.limit < 1 || draft.value.limit > 10000) {
    errors.push('限購張數必須為 1～10000 的整數。')
  }
  if (!draft.value.zones.length) {
    errors.push('至少需要一個分區。')
  }
  for (const zone of draft.value.zones) {
    if (!zone.name.trim()) {
      errors.push('分區名稱不可空白。')
    }
    if (!/^#[0-9a-fA-F]{6}$/.test(zone.color ?? '')) {
      errors.push('請選擇分區顏色。')
    }
    if (!Number.isFinite(zone.price) || zone.price < 0) {
      errors.push('分區價格必須為非負數。')
    }
    if (rowLabels.value.indexOf(zone.start) < 0 || rowLabels.value.indexOf(zone.end) < rowLabels.value.indexOf(zone.start)) {
      errors.push(`${zone.name || '分區'}的排數範圍無效。`)
    }
  }
  for (const [key, label] of [['zones', '分區'], ['ticketTypes', '票種']]) {
    const names = draft.value[key].map((item) => item.name.trim())
    if (new Set(names).size !== names.length) {
      errors.push(`${label}名稱不可重複。`)
    }
  }
  const missing = rowAssignments.value.filter((row) => row.zones.length === 0)
  const overlapping = rowAssignments.value.filter((row) => row.zones.length > 1)
  if (missing.length) {
    errors.push(`未分區：${missing.map((row) => row.label).join('、')} 排。`)
  }
  if (overlapping.length) {
    errors.push(`分區重疊：${overlapping.map((row) => row.label).join('、')} 排。`)
  }
  if (!draft.value.ticketTypes.length) {
    errors.push('至少需要一種票種。')
  }
  for (const type of draft.value.ticketTypes) {
    if (!type.name.trim()) {
      errors.push('票種名稱不可空白。')
    }
    if (!Number.isFinite(type.discountPercent) || type.discountPercent <= 0 || type.discountPercent > 100) {
      errors.push('票種價格比例必須大於 0 且不超過 100%。')
    }
  }
  return [...new Set(errors)]
})
function zoneCount(zone) {
  return rowAssignments.value.filter((row) => row.zones.some((item) => item.id === zone.id)).length * seatsPerRow.value
}
function payload() {
  return {
    version: draft.value.version,
    maxTicketsPerMember: draft.value.limit,
    zones: draft.value.zones.map((zone) => (
      {
        name: zone.name.trim(),
        color: zone.color,
        rowStart: zone.start,
        rowEnd: zone.end,
        price: zone.price
      }
    )
    ),
    ticketTypes: draft.value.ticketTypes.map(
      (type) => (
        {
          name: type.name.trim(),
          pricePercent: type.discountPercent,
          eligibility: (type.eligibility ?? '').trim()
        }
      )
    ),
  }
}
async function saveSettings() {
  if (issues.value.length || locked.value) return
  const id = sessionId.value
  saving.value = true
  error.value = ''
  try {
    const response = await adminApi.put(`/sessions/${encodeURIComponent(id)}/sales-settings`, payload())
    applySettings(id, response.data)
    ElMessage.success('售票設定已儲存')
  } catch (cause) {
    error.value = errorMessage(cause, '儲存失敗，請檢查設定並重試。')
    if (cause.response?.data?.code === 'SALES_SETTINGS_LOCKED') {
      draft.value.locked = true
    }
  } finally {
    saving.value = false
  }
}
function exportDraft() {
  if (issues.value.length || locked.value) return
  const url = URL.createObjectURL(
    new Blob(
      [JSON.stringify(
        { sessionId: sessionId.value, ...payload() }, null, 2
      )], {
      type: 'application/json'
    }
    )
  )
  const link = document.createElement('a')
  link.href = url
  link.download = `sales-settings-${sessionId.value}.json`
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
  ElMessage.success('目前設定已匯出')
}
</script>

<template>
  <section v-loading="loading" class="settings">
    <el-alert title="分區與票種適用所選場次。修改後請按儲存設定；已有保留或售出座位的場次不可修改。" type="info" :closable="false" show-icon />
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-card shadow="never">
      <template #header>
        <h3>場次售票設定</h3>
      </template>
      <el-select v-model="sessionId" :disabled="saving" placeholder="選擇已建立的場次" aria-label="場次售票設定" filterable
        class="session-select" @change="selectSession()">
        <el-option v-for="item in sessions" :key="item.id" :value="item.id"
          :label="`${item.name} · ${item.date} ${item.time} · ${item.id}`" />
      </el-select>
      <el-button v-if="sessionId" :disabled="saving || loading" @click="selectSession(true)">重新載入已儲存設定</el-button>
      <el-empty v-if="!sessions.length" description="請先建立場次" />
      <template v-if="draft">
        <p>容量 {{ draft.capacity }} 張 · {{ rowLabels.length }} 排 · 每排 {{ seatsPerRow }} 席 · {{ draft.configured ? `已儲存版本
          ${draft.version}` : '尚未設定，儲存前沿用活動票價' }}</p>
        <el-alert v-if="draft.locked" title="此場次已有保留或售出座位，暫不開放修改售票配置。" type="warning" :closable="false" />
        <el-form label-position="top" class="limit-form">
          <el-form-item label="每會員每場次限購張數">
            <el-input-number v-model="draft.limit" :min="1" :max="10000" :precision="0" :disabled="locked" />
          </el-form-item>
          <p>已付款與有效待付款票券合併計算；取消及逾期後釋放額度。</p>
        </el-form>
      </template>
    </el-card>
    <template v-if="draft">
      <el-card shadow="never">
        <template #header>
          <div class="heading">
            <h3>分區定價</h3>
            <el-button :disabled="locked" @click="draft.zones.push(
              { id: ++nextId, name: '', color: palette[draft.zones.length % palette.length], start: '', end: '', price: draft.defaultPrice }
            )">新增分區
            </el-button>
          </div>
        </template>
        <div v-for="(zone, index) in draft.zones" :key="zone.id" class="editor-row">
          <el-form label-position="top" class="zone-fields" :disabled="locked">
            <el-form-item label="分區名稱">
              <el-input v-model="zone.name" placeholder="例如 A 區" maxlength="40" /></el-form-item>
            <el-form-item label="顏色">
              <el-color-picker v-model="zone.color" />
            </el-form-item>
            <el-form-item label="起始排">
              <el-select v-model="zone.start">
                <el-option v-for="row in rowLabels" :key="row" :label="`${row} 排`" :value="row" />
              </el-select>
            </el-form-item>
            <el-form-item label="結束排">
              <el-select v-model="zone.end">
                <el-option v-for="row in rowLabels" :key="row" :label="`${row} 排`" :value="row" />
              </el-select>
            </el-form-item>
            <el-form-item label="全票價格（NT$）">
              <el-input-number v-model="zone.price" :min="0" :precision="2" :step="100" />
            </el-form-item>
          </el-form>
          <div class="row-footer">
            <span>{{ zoneCount(zone) }} 席</span>
            <el-button type="danger" text :disabled="locked" @click="draft.zones.splice(index, 1)">刪除分區</el-button>
          </div>
        </div>
      </el-card>
      <el-card shadow="never">
        <template #header>
          <div class="heading">
            <h3>票種</h3>
            <el-button :disabled="locked" @click="draft.ticketTypes.push(
              { id: ++nextId, name: '', discountPercent: 100, eligibility: '' }
            )">新增票種
            </el-button>
          </div>
        </template>
        <p>各票種適用所有分區。價格比例 100% 為原價、80% 為八折，金額四捨五入至小數點後兩位。</p>
        <div v-for="(type, index) in draft.ticketTypes" :key="type.id" class="editor-row">
          <el-form label-position="top" class="type-fields" :disabled="locked">
            <el-form-item label="票種名稱">
              <el-input v-model="type.name" placeholder="例如全票、學生票" maxlength="40" />
            </el-form-item>
            <el-form-item label="價格比例（%）">
              <el-input-number v-model="type.discountPercent" :min="0.01" :max="100" :precision="2" />
            </el-form-item>
            <el-form-item label="資格說明">
              <el-input v-model="type.eligibility" placeholder="例如入場時須出示學生證" maxlength="200" />
            </el-form-item>
          </el-form>
          <el-button type="danger" text :disabled="locked" @click="draft.ticketTypes.splice(index, 1)">刪除票種</el-button>
        </div>
      </el-card>
      <el-card shadow="never">
        <template #header>
          <h3>座位分區預覽</h3>
        </template>
        <div class="stage">舞台／前方</div>
        <div class="seat-preview">
          <div v-for="row in rowAssignments" :key="row.label" class="seat-row">
            <span class="row-label">{{ row.label }} 排</span>
            <span v-for="seat in seatsPerRow" :key="seat" class="seat"
              :style="{ backgroundColor: row.zones.length === 1 ? row.zones[0].color : '#73767a' }"
              :title="`${row.label}-${seat}：${row.zones.length === 1 ? row.zones[0].name : '未分區或重疊'}`">{{ seat }}
            </span>
          </div>
        </div>
        <el-table :data="draft.zones" empty-text="請新增分區">
          <el-table-column prop="name" label="分區" min-width="130" />
          <el-table-column label="席數" min-width="90">
            <template #default="{ row }">{{ zoneCount(row) }}</template>
          </el-table-column>
          <el-table-column v-for="type in draft.ticketTypes" :key="type.id" :label="type.name || '未命名票種'"
            min-width="140">
            <template #default="{ row }">{{ money(Math.round(row.price * type.discountPercent) / 100) }}</template>
          </el-table-column>
        </el-table>
      </el-card>
      <el-alert v-if="issues.length" type="warning" title="請完成以下設定" :closable="false" show-icon>
        <ul>
          <li v-for="issue in issues" :key="issue">{{ issue }}</li>
        </ul>
      </el-alert>
      <div class="heading save-actions">
        <span>修改後須儲存才會套用</span>
        <div>
          <el-button :disabled="locked || issues.length > 0" @click="exportDraft">匯出設定</el-button>
          <el-button type="primary" :loading="saving" :disabled="locked || issues.length > 0"
            @click="saveSettings">儲存設定</el-button>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.settings {
  display: grid;
  gap: 20px;
}

h3 {
  margin: 0;
  font-size: 17px;
}

p,
.row-footer,
.heading>span {
  color: #606266;
  font-size: 14px;
}

.session-select {
  width: 100%;
}

.heading,
.row-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.limit-form {
  margin-top: 20px;
}

.editor-row {
  padding: 18px 0;
  border-bottom: 1px solid #ebeef5;
}

.zone-fields {
  display: grid;
  grid-template-columns: minmax(140px, 1fr) 70px repeat(2, minmax(100px, 1fr)) 180px;
  gap: 12px;
}

.type-fields {
  display: grid;
  grid-template-columns: 1fr 180px 2fr;
  gap: 12px;
}

.stage {
  text-align: center;
  padding: 12px;
  background: #f0f2f5;
  border-radius: 8px;
  margin-bottom: 18px;
}

.seat-preview {
  max-height: 400px;
  overflow: auto;
  margin-bottom: 20px;
}

.seat-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  width: max-content;
}

.row-label {
  width: 60px;
  flex-shrink: 0;
}

.seat {
  display: inline-flex;
  width: 30px;
  height: 28px;
  align-items: center;
  justify-content: center;
  color: white;
  border-radius: 4px;
  font-size: 12px;
}

@media (max-width: 1000px) {

  .zone-fields,
  .type-fields {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 600px) {
  .settings {
    min-width: 0;
  }

  .settings>* {
    min-width: 0;
  }

  .settings :deep(.el-card__body),
  .settings :deep(.el-card__header) {
    padding: 16px;
  }

  .settings :deep(.el-form-item__content) {
    min-width: 0;
  }

  .settings :deep(.el-input-number),
  .settings :deep(.el-select) {
    width: 100%;
  }

  .settings :deep(.el-button) {
    min-height: 44px;
  }

  .session-select+.el-button {
    margin-top: 12px;
    margin-left: 0;
    width: 100%;
    white-space: normal;
  }

  .heading>div {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    width: 100%;
  }

  .heading>div .el-button {
    flex: 1;
    margin-left: 0;
  }

  .save-actions {
    justify-content: center;
    gap: 16px;
  }

  .save-actions>span {
    width: 100%;
    text-align: center;
  }

  .seat-preview {
    max-width: 100%;
  }

  .zone-fields,
  .type-fields {
    grid-template-columns: 1fr;
  }
}
</style>
