<script setup>
import { bookingApi } from '@/services/api'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  maxSelection: {
    type: Number,
    default: 1
  },
  unavailableSeats: {
    type: Set,
    default: () => new Set()
  },
  activityId: {
    type: String,
    default: () => ''
  }
})

const emit = defineEmits(['update:modelValue'])

const seats = ref([])
const seatsPerRow = ref(0)
const loading = ref(false)
const loadError = ref(false)
const selectedRow = ref('')
const seatScroll = ref(null)

const seatRows = computed(() => {
  const grouped = new Map()
  for (const seat of seats.value) {
    const row = seat.row.trim()
    if (!grouped.has(row)) grouped.set(row, [])
    grouped.get(row).push(seat)
  }
  return [...grouped].map(([label, rowSeats]) => ({ label, seats: rowSeats }))
})
const availableCount = computed(() => seats.value.filter((seat) => !isUnavailable(seat.id)).length)

watch(() => props.activityId, loadSeats, { immediate: true })
async function loadSeats(activityId) {
  seats.value = []
  seatsPerRow.value = 0
  selectedRow.value = ''
  loadError.value = false
  if (!activityId) return
  loading.value = true
  try {
    const response = await bookingApi.get('/selectOnlySeats', {
      params: { activity_id: activityId }
    })
    if (props.activityId !== activityId) return
    seats.value = response.data
    seatsPerRow.value = Number(response.data[0]?.seats_per_row ?? 0)
  } catch {
    if (props.activityId === activityId) loadError.value = true
  } finally {
    if (props.activityId === activityId) loading.value = false
  }
}

const isSelected = (seatId) => props.modelValue.includes(seatId)
const isUnavailable = (seatId) => props.unavailableSeats.has(seatId)
const jumpToRow = () => {
  const row = seatScroll.value?.querySelector(`[data-row="${selectedRow.value}"]`)
  row?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

const selectSeat = (seatId) => {
  if (isUnavailable(seatId)) return

  if (isSelected(seatId)) {
    emit('update:modelValue', props.modelValue.filter((id) => id !== seatId))
    return
  }

  const nextSeats = props.maxSelection === 1
    ? [seatId]
    : [...props.modelValue, seatId].slice(0, props.maxSelection)
  emit('update:modelValue', nextSeats)
}
</script>

<template>
  <div class="seat-map" aria-label="座位選擇區">
    <div class="map-header">
      <div>
        <div class="map-title">選擇你的座位</div>
        <p v-if="seatRows.length" class="map-subtitle">
          {{ seatRows.length }} 排 · 每排 {{ seatsPerRow }} 席 · 尚有 {{ availableCount }} 席可選
        </p>
      </div>
      <label v-if="seatRows.length" class="row-jump">
        <span>快速找排</span>
        <select v-model="selectedRow" aria-label="快速找排" @change="jumpToRow">
          <option value="" disabled>選擇排別</option>
          <option v-for="row in seatRows" :key="row.label" :value="row.label">{{ row.label }} 排</option>
        </select>
      </label>
    </div>

    <div class="stage"><span>前方</span></div>

    <div v-if="loading" class="map-message" role="status">正在載入座位…</div>
    <div v-else-if="loadError" class="map-message" role="alert">
      無法載入座位 <button type="button" class="retry-button" @click="loadSeats(activityId)">重試</button>
    </div>
    <div v-else-if="!seatRows.length" class="map-message">目前沒有可選座位</div>
    <div v-else ref="seatScroll" class="seat-scroll" role="region" aria-label="座位排列" tabindex="0">
      <div v-for="row in seatRows" :key="row.label" class="seat-row" :data-row="row.label">
        <span class="row-label">{{ row.label }} 排</span>
        <div class="row-seats" :style="{ gridTemplateColumns: `repeat(${seatsPerRow}, 42px)` }">
          <button v-for="seat in row.seats" :key="seat.id" type="button" class="seat" :class="{
            selected: isSelected(seat.id),
            unavailable: isUnavailable(seat.id)
          }" :disabled="isUnavailable(seat.id)"
            :aria-label="`${row.label} 排 ${seat.number} 號${isUnavailable(seat.id) ? '，不可選' : ''}`"
            :aria-pressed="isSelected(seat.id)" @click="selectSeat(seat.id)">
            {{ seat.number }}
          </button>
        </div>
        <span class="row-label row-label--end" aria-hidden="true">{{ row.label }}</span>
      </div>
    </div>

    <div class="map-footer">
      <div class="legend" aria-label="座位狀態圖例">
        <span><i class="legend-dot available" />可選</span>
        <span><i class="legend-dot selected" />已選</span>
        <span><i class="legend-dot unavailable" />不可選</span>
      </div>
      <strong v-if="modelValue.length" class="selection-summary">已選 {{ modelValue.join('、') }}</strong>
    </div>
  </div>
</template>

<style scoped>
.seat-map {
  margin-top: 20px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 18px;
  background: var(--el-bg-color);
  overflow: hidden;
  box-shadow: 0 10px 28px rgba(25, 50, 90, 0.06);
}

.map-header,
.map-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 22px;
}

.map-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.map-subtitle {
  margin: 5px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.row-jump {
  display: flex;
  align-items: center;
  gap: 9px;
  flex-shrink: 0;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.row-jump select {
  min-width: 110px;
  padding: 8px 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
  font: inherit;
}

.stage {
  width: min(440px, 72%);
  margin: 0 auto 16px;
  padding: 9px 0;
  border-radius: 0 0 48px 48px;
  background: linear-gradient(180deg, var(--el-color-primary-light-7), var(--el-color-primary-light-9));
  border: 1px solid var(--el-color-primary-light-5);
  border-top: 0;
  color: var(--el-text-color-regular);
  text-align: center;
  font-size: 12px;
  letter-spacing: 0.18em;
}

.seat-scroll {
  max-height: 440px;
  overflow: auto;
  padding: 10px 20px 20px;
  background: var(--el-fill-color-extra-light);
  border-block: 1px solid var(--el-border-color-extra-light);
}

.seat-row {
  display: grid;
  grid-template-columns: 48px max-content 48px;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-width: max-content;
  margin: 0 auto;
  padding: 5px 0;
}

.seat-row + .seat-row {
  border-top: 1px dashed var(--el-border-color-extra-light);
}

.row-label {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 600;
  text-align: right;
  white-space: nowrap;
}

.row-label--end {
  text-align: left;
}

.row-seats {
  display: grid;
  gap: 8px;
}

.seat {
  width: 42px;
  height: 40px;
  padding: 0;
  border: 1px solid var(--el-color-primary-light-5);
  border-radius: 9px 9px 11px 11px;
  background: var(--el-bg-color);
  color: var(--el-color-primary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: background-color 0.15s ease, border-color 0.15s ease, transform 0.15s ease;
}

.seat:hover:not(:disabled),
.seat:focus-visible {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  transform: translateY(-2px);
}

.seat:focus-visible,
.row-jump select:focus-visible,
.retry-button:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}

.seat.selected {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary);
  color: white;
  box-shadow: 0 3px 8px var(--el-color-primary-light-5);
}

.seat.unavailable {
  border-color: var(--el-border-color);
  background: var(--el-fill-color-dark);
  color: var(--el-text-color-placeholder);
  cursor: not-allowed;
  text-decoration: line-through;
}

.map-footer {
  flex-wrap: wrap;
  min-height: 58px;
}

.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 18px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.legend span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  width: 13px;
  height: 13px;
  border: 1px solid var(--el-color-primary-light-5);
  border-radius: 4px;
  background: var(--el-bg-color);
}

.legend-dot.selected {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary);
}

.legend-dot.unavailable {
  border-color: var(--el-border-color);
  background: var(--el-fill-color-dark);
}

.selection-summary {
  color: var(--el-color-primary);
  font-size: 13px;
}

.map-message {
  padding: 70px 20px;
  color: var(--el-text-color-secondary);
  text-align: center;
  background: var(--el-fill-color-extra-light);
}

.retry-button {
  margin-left: 8px;
  padding: 5px 10px;
  border: 1px solid var(--el-color-primary);
  border-radius: 6px;
  background: var(--el-bg-color);
  color: var(--el-color-primary);
  cursor: pointer;
}

@media (max-width: 600px) {
  .map-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .map-header,
  .map-footer {
    padding: 15px;
  }

  .seat-scroll {
    padding-inline: 12px;
  }
}
</style>
