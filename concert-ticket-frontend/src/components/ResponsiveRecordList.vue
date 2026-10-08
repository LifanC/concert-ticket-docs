<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  records: { type: Array, required: true },
  fields: { type: Array, required: true },
  idKey: { type: String, default: 'id' },
  emptyText: { type: String, default: '目前沒有資料' },
})
const mobile = ref(false)
let media
const updateLayout = () => { mobile.value = media.matches }
onMounted(() => {
  media = window.matchMedia('(max-width: 767px)')
  updateLayout()
  media.addEventListener('change', updateLayout)
})
onBeforeUnmount(() => media?.removeEventListener('change', updateLayout))
const cardFields = computed(() => props.fields.filter(field => !['title', 'image', 'status', 'actions'].includes(field.role)))
const value = (field, record) => field.format ? field.format(record) : (record[field.key] ?? '—')
</script>

<template>
  <div v-if="mobile" class="record-list">
    <article v-for="record in records" :key="record[idKey]" class="record-card">
      <template v-for="field in fields.filter(item => item.role === 'image')" :key="field.key">
        <slot :name="field.slot || field.key" :record="record">{{ value(field, record) }}</slot>
      </template>
      <div class="record-heading">
        <template v-for="field in fields.filter(item => ['title', 'status'].includes(item.role))" :key="field.key">
          <h3 v-if="field.role === 'title'">
            <slot :name="field.slot || field.key" :record="record">{{ value(field, record) }}</slot>
          </h3>
          <slot v-else :name="field.slot || field.key" :record="record">{{ value(field, record) }}</slot>
        </template>
      </div>
      <dl>
        <div v-for="field in cardFields" :key="field.key">
          <dt>{{ field.label }}</dt>
          <dd>
            <slot :name="field.slot || field.key" :record="record">{{ value(field, record) }}</slot>
          </dd>
        </div>
      </dl>
      <div v-for="field in fields.filter(item => item.role === 'actions')" :key="field.key" class="record-actions">
        <slot :name="field.slot || field.key" :record="record" />
      </div>
    </article>
    <el-empty v-if="!records.length" :description="emptyText" :image-size="70" />
  </div>
  <el-table v-else :data="records" :row-key="idKey" stripe :empty-text="emptyText" style="width: 100%">
    <el-table-column v-for="field in fields" :key="field.key" :prop="field.key" :label="field.label"
      :width="field.width" :min-width="field.minWidth || 100" :align="field.align" :fixed="field.fixed">
      <template #default="{ row }">
        <div :class="{ 'record-actions': field.role === 'actions' }">
          <slot :name="field.slot || field.key" :record="row">{{ value(field, row) }}</slot>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>

<style scoped>
.record-list {
  display: grid;
  gap: 20px;
}

.record-card {
  min-width: 0;
  border-bottom: 1px solid var(--el-border-color-light);
  padding-bottom: 20px;
}

.record-card:last-child {
  border: 0;
  padding-bottom: 0;
}

.record-heading {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: flex-start;
  margin: 16px 0 12px;
}

h3 {
  flex: 1;
  min-width: 0;
  margin: 0;
  font-size: 18px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

dl {
  display: grid;
  gap: 10px;
  margin: 0;
  font-size: 13px;
}

dl>div {
  display: grid;
  grid-template-columns: 80px minmax(0, 1fr);
  gap: 10px;
}

dt {
  color: var(--el-text-color-secondary);
}

dd {
  margin: 0;
  overflow-wrap: anywhere;
}

.record-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.record-actions :deep(.el-button) {
  margin-left: 0;
}

@media (max-width: 767px) {
  .record-actions {
    margin-top: 16px;
  }

  .record-actions :deep(.el-button) {
    flex: 1;
    min-height: 44px;
  }
}
</style>
