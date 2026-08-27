<script setup>
const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  maxSelection: {
    type: Number,
    default: 1
  }
})

const emit = defineEmits(['update:modelValue'])

const rows = ['A', 'B', 'C', 'D', 'E', 'F']
const seatsPerRow = 10
const unavailableSeats = new Set(['A-03', 'A-04', 'B-08', 'C-02', 'D-06', 'E-09'])

const seats = computed(() => rows.flatMap((row) => (
  Array.from({ length: seatsPerRow }, (_, index) => ({
    id: `${row}-${String(index + 1).padStart(2, '0')}`,
    row,
    number: index + 1
  }))
)))

const isSelected = (seatId) => props.modelValue.includes(seatId)
const isUnavailable = (seatId) => unavailableSeats.has(seatId)

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
    <div class="stage">舞台</div>

    <div class="seat-grid">
      <button
        v-for="seat in seats"
        :key="seat.id"
        type="button"
        class="seat"
        :class="{
          selected: isSelected(seat.id),
          unavailable: isUnavailable(seat.id)
        }"
        :disabled="isUnavailable(seat.id)"
        :aria-label="`${seat.row} 排 ${seat.number} 號${isUnavailable(seat.id) ? '，不可選' : ''}`"
        :aria-pressed="isSelected(seat.id)"
        @click="selectSeat(seat.id)"
      >
        {{ seat.row }}{{ seat.number }}
      </button>
    </div>

    <div class="legend" aria-label="座位狀態圖例">
      <span><i class="legend-dot available" />可選</span>
      <span><i class="legend-dot selected" />已選</span>
      <span><i class="legend-dot unavailable" />不可選</span>
    </div>
  </div>
</template>

<style scoped>
.seat-map {
  margin-top: 20px;
  padding: 20px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 10px;
  background: var(--el-fill-color-extra-light);
  overflow-x: auto;
}

.stage {
  width: min(420px, 80%);
  margin: 0 auto 28px;
  padding: 8px;
  border-radius: 0 0 50% 50%;
  background: var(--el-color-info-light-7);
  color: var(--el-text-color-regular);
  text-align: center;
  letter-spacing: 0.25em;
}

.seat-grid {
  display: grid;
  grid-template-columns: repeat(10, 42px);
  justify-content: center;
  gap: 9px;
  min-width: 510px;
}

.seat {
  width: 42px;
  height: 38px;
  padding: 0;
  border: 1px solid var(--el-color-primary-light-5);
  border-radius: 7px 7px 10px 10px;
  background: var(--el-bg-color);
  color: var(--el-color-primary);
  cursor: pointer;
  font-size: 12px;
  transition: 0.15s ease;
}

.seat:hover:not(:disabled),
.seat:focus-visible {
  border-color: var(--el-color-primary);
  transform: translateY(-2px);
}

.seat.selected {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary);
  color: white;
}

.seat.unavailable {
  border-color: var(--el-border-color);
  background: var(--el-fill-color-dark);
  color: var(--el-text-color-placeholder);
  cursor: not-allowed;
  text-decoration: line-through;
}

.legend {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 22px;
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
</style>
