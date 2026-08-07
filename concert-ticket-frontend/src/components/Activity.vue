<script setup>
import { useRouter } from 'vue-router'
import axios from "axios";
import { toFindCookie, addCookie, clearCookie } from "@/components/componentsJs/cookie";

axios.defaults.baseURL = 'http://localhost:8080/api/v1/activity'
axios.defaults.withCredentials = true;

const router = useRouter()
const keyword = ref('')
const selectedCategory = ref('全部')
const selectedStatus = ref('全部')
const detailVisible = ref(false)
const currentActivity = ref(null)

executeFirst()
async function executeFirst() {
  // selectAllActivities活動管理
  try {
    const response = await axios({
      method: 'get',
      url: '/selectAllActivities',
    });
    activities.value = response.data
  } catch (error) {
    let status = error.response.status ?? {}
    if (status === 403) {
      router.push('/User')
    }
  }
}

const categories = ['全部', '音樂演唱會', '舞台劇', '展覽特展']
const statuses = ['全部', '即將開賣', '售票中', '已結束']

const activities = ref([])

const filteredActivities = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  return activities.value.filter((activity) => {
    const matchesKeyword = !normalizedKeyword || [activity.name, activity.venue, activity.id].some((value) => {
      value.toLowerCase().includes(normalizedKeyword)
    })
    const matchesCategory = selectedCategory.value === '全部' || activity.category === selectedCategory.value
    const matchesStatus = selectedStatus.value === '全部' || activity.status === selectedStatus.value
    return matchesKeyword && matchesCategory && matchesStatus
  })
})

const resetFilters = () => {
  keyword.value = ''
  selectedCategory.value = '全部'
  selectedStatus.value = '全部'
}
const openDetail = (activity) => {
  currentActivity.value = activity
  detailVisible.value = true
}
const goBooking = () => {
  if (!currentActivity.value) return
  if (currentActivity.value.status != '已結束') {
    detailVisible.value = false
    router.push(
      {
        path: '/booking',
        query: {
          activity_id: currentActivity.value.id,
          activity_sessionid: currentActivity.value.sessionid,
          activity_name: currentActivity.value.name,
          activity_date: currentActivity.value.date,
          activity_time: currentActivity.value.time
        }
      }
    )
  }
}
const statusType = (status) => (
  {
    '即將開賣': 'warning',
    '售票中': 'success',
    '已結束': 'info'
  }[status] || 'info'
)
</script>

<template>
  <el-container class="activity-page">
    <el-header class="page-header">
      <div>
        <h1>活動資訊</h1><el-text type="info">探索最新活動，找到屬於你的精彩體驗。</el-text>
      </div>
      <el-tag type="primary" effect="light">{{ filteredActivities.length }} 個活動</el-tag>
    </el-header>
    <el-main>
      <el-card shadow="never" class="filter-card">
        <el-form :inline="true" label-position="top" class="filter-form">
          <el-form-item label="搜尋活動">
            <el-input v-model="keyword" clearable placeholder="活動名稱、場地或活動編號" style="width: 280px" />
          </el-form-item>
          <el-form-item label="活動類型">
            <el-select v-model="selectedCategory" style="width: 160px">
              <el-option v-for="category in categories" :key="category" :label="category" :value="category" />
            </el-select>
          </el-form-item>
          <el-form-item label="售票狀態">
            <el-select v-model="selectedStatus" style="width: 140px">
              <el-option v-for="status in statuses" :key="status" :label="status" :value="status" />
            </el-select>
          </el-form-item>
          <el-form-item label=" ">
            <el-button plain type="warning" @click="resetFilters">重設篩選</el-button>
          </el-form-item>
        </el-form>
      </el-card>
      <el-card shadow="never" class="activity-card">
        <template #header>
          <div class="card-title">
            <span>活動列表</span><el-text type="info">點選查看活動詳細資訊與售票狀態。</el-text>
          </div>
        </template>
        <el-table :data="filteredActivities" stripe style="width: 100%" empty-text="找不到符合篩選條件的活動">
          <el-table-column prop="name" label="活動名稱" min-width="220">
            <template #default="scope">
              <div class="activity-name">
                {{ scope.row.name }}
              </div>
              <el-text size="small" type="info">{{ scope.row.id }} · {{ scope.row.sessionid }} · {{ scope.row.category }}</el-text>
            </template>
          </el-table-column>
          <el-table-column prop="date" label="活動日期" min-width="190" />
          <el-table-column prop="venue" label="活動場地" min-width="150" />
          <el-table-column prop="price" label="票價" width="120" />
          <el-table-column label="狀態" width="120">
            <template #default="scope">
              <el-tag :type="statusType(scope.row.status)" effect="light">{{ scope.row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="scope">
              <el-button plain type="primary" @click="openDetail(scope.row)">查看詳情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-main>
  </el-container>
  <el-dialog v-model="detailVisible" title="活動詳情" width="min(620px, 92vw)">
    <template v-if="currentActivity">
      <div class="detail-heading">
        <div>
          <h2>{{ currentActivity.name }}</h2>
          <el-text type="info">{{ currentActivity.id }} · {{ currentActivity.sessionid }} · {{ currentActivity.category }}</el-text>
        </div>
        <el-tag :type="statusType(currentActivity.status)" effect="dark">{{ currentActivity.status }}</el-tag>
      </div>
      <el-descriptions :column="1" border class="detail-list">
        <el-descriptions-item label="活動日期">{{ currentActivity.date }} {{ currentActivity.dow }} {{ currentActivity.time }}</el-descriptions-item>
        <el-descriptions-item label="活動場地">{{ currentActivity.venue }}</el-descriptions-item>
        <el-descriptions-item label="開賣時間">{{ currentActivity.sales_start }}</el-descriptions-item>
        <el-descriptions-item label="票價">{{ currentActivity.price }}</el-descriptions-item>
        <el-descriptions-item label="活動說明">{{ currentActivity.description }}</el-descriptions-item>
      </el-descriptions>
    </template>
    <template #footer>
      <el-button @click="detailVisible = false">關閉</el-button>
      <el-button type="primary" @click="goBooking">前往訂票</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.activity-page {
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

.filter-card,
.activity-card {
  margin-bottom: 20px;
}

.filter-form {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.filter-form :deep(.el-form-item) {
  margin-right: 0;
  margin-bottom: 0;
}

.card-title,
.detail-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.activity-name {
  margin-bottom: 4px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.detail-heading {
  margin-bottom: 20px;
}

.detail-heading h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.detail-list :deep(.el-descriptions__label) {
  width: 130px;
}

@media (max-width: 767px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .filter-form {
    display: grid;
    gap: 0;
  }

  .filter-form :deep(.el-form-item),
  .filter-form :deep(.el-input),
  .filter-form :deep(.el-select) {
    width: 100% !important;
  }

  .card-title,
  .detail-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .detail-list :deep(.el-descriptions__label) {
    width: 100px;
  }
}
</style>
