<template>
  <div class="workitem-list">
    <!-- Filter Bar -->
    <div class="card filter-bar">
      <div class="filter-row">
        <div class="form-group" style="margin-bottom: 0; flex: 1; min-width: 160px;">
          <label>关键字</label>
          <input v-model="filters.keyword" placeholder="搜索标题、描述..." @keyup.enter="handleSearch" />
        </div>
        <div class="form-group" style="margin-bottom: 0; width: 140px;">
          <label>类型</label>
          <select v-model="filters.type">
            <option value="">全部</option>
            <option value="STORY">STORY</option>
            <option value="BUG">BUG</option>
            <option value="TASK">TASK</option>
          </select>
        </div>
        <div class="form-group" style="margin-bottom: 0; width: 120px;">
          <label>优先级</label>
          <select v-model="filters.priority">
            <option value="">全部</option>
            <option value="P0">P0</option>
            <option value="P1">P1</option>
            <option value="P2">P2</option>
            <option value="P3">P3</option>
          </select>
        </div>
        <div class="form-group" style="margin-bottom: 0; width: 140px;">
          <label>状态</label>
          <select v-model="filters.status">
            <option value="">全部</option>
            <option value="DRAFT">DRAFT</option>
            <option value="ANALYZING">ANALYZING</option>
            <option value="READY">READY</option>
            <option value="IN_PROGRESS">IN_PROGRESS</option>
            <option value="IN_TESTING">IN_TESTING</option>
            <option value="DONE">DONE</option>
          </select>
        </div>
        <div class="filter-actions" style="display: flex; gap: 8px; align-items: flex-end;">
          <button class="btn btn-primary" @click="handleSearch">搜索</button>
          <button class="btn" @click="handleReset">重置</button>
        </div>
      </div>
    </div>

    <!-- Toolbar -->
    <div class="toolbar" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <h2 style="font-size: 16px; font-weight: 600;">工作项列表</h2>
      <button class="btn btn-primary" @click="showCreateModal = true">创建工作项</button>
    </div>

    <!-- Table -->
    <div class="card" style="padding: 0; overflow-x: auto;">
      <table v-if="records.length > 0" class="data-table">
        <thead>
          <tr>
            <th>编号</th>
            <th>标题</th>
            <th>类型</th>
            <th>优先级</th>
            <th>状态</th>
            <th>负责人</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in records" :key="item.id">
            <td><code style="background: #f0f2f5; padding: 2px 6px; border-radius: 4px; font-size: 12px;">{{ item.code }}</code></td>
            <td class="title-cell">{{ item.title }}</td>
            <td>
              <span class="tag" :class="'tag-' + getTypeTagClass(item.type)">{{ item.type }}</span>
            </td>
            <td>
              <span class="tag" :class="'tag-p' + (item.priority || '').replace('P', '').toLowerCase()">{{ item.priority }}</span>
            </td>
            <td>
              <span class="status-badge" :class="'status-' + item.status">{{ statusLabel(item.status) }}</span>
            </td>
            <td>{{ item.assignee || '-' }}</td>
            <td>{{ formatDate(item.createdAt) }}</td>
            <td>
              <button class="btn btn-sm" @click="goDetail(item.id)">查看详情</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty-state">暂无数据</div>
    </div>

    <!-- Pagination -->
    <div v-if="total > 0" class="pagination">
      <button class="btn btn-sm" :disabled="pageNo <= 1" @click="changePage(pageNo - 1)">上一页</button>
      <span class="page-info">第 {{ pageNo }} / {{ totalPages }} 页，共 {{ total }} 条</span>
      <button class="btn btn-sm" :disabled="pageNo >= totalPages" @click="changePage(pageNo + 1)">下一页</button>
    </div>

    <!-- Create Modal -->
    <div v-if="showCreateModal" class="modal-overlay" @click.self="showCreateModal = false">
      <div class="modal">
        <h2>创建工作项</h2>
        <div class="form-group">
          <label>标题 <span style="color: red;">*</span></label>
          <input v-model="createForm.title" placeholder="请输入标题" maxlength="255" />
        </div>
        <div class="form-group">
          <label>描述</label>
          <textarea v-model="createForm.description" placeholder="请输入描述" maxlength="10000"></textarea>
        </div>
        <div class="form-group">
          <label>类型 <span style="color: red;">*</span></label>
          <select v-model="createForm.type">
            <option value="">请选择类型</option>
            <option value="STORY">STORY</option>
            <option value="BUG">BUG</option>
            <option value="TASK">TASK</option>
          </select>
        </div>
        <div class="form-group">
          <label>优先级</label>
          <select v-model="createForm.priority">
            <option value="P0">P0</option>
            <option value="P1">P1</option>
            <option value="P2">P2</option>
            <option value="P3">P3</option>
          </select>
        </div>
        <div class="form-group">
          <label>负责人</label>
          <input v-model="createForm.assignee" placeholder="请输入负责人" maxlength="64" />
        </div>
        <div class="form-group">
          <label>标签</label>
          <input v-model="createForm.tagsInput" placeholder="多个标签用逗号分隔" />
        </div>
        <div class="modal-actions">
          <button class="btn" @click="showCreateModal = false">取消</button>
          <button class="btn btn-primary" @click="handleCreate" :disabled="creating">确认创建</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getWorkItemList, createWorkItem } from '../api/workitem'

const router = useRouter()

// Filters
const filters = reactive({
  keyword: '',
  type: '',
  priority: '',
  status: ''
})

// List state
const records = ref([])
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

// Create modal state
const showCreateModal = ref(false)
const creating = ref(false)
const createForm = reactive({
  title: '',
  description: '',
  type: '',
  priority: 'P2',
  assignee: '',
  tagsInput: ''
})

function getTypeTagClass(type) {
  if (type === 'STORY') return 'story'
  if (type === 'BUG') return 'bug'
  if (type === 'TASK') return 'task'
  return ''
}

const statusLabelMap = {
  DRAFT: '草稿',
  ANALYZING: '分析中',
  READY: '就绪',
  IN_PROGRESS: '进行中',
  IN_TESTING: '测试中',
  DONE: '已完成'
}

function statusLabel(status) {
  return statusLabelMap[status] || status
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}`
}

async function fetchList() {
  loading.value = true
  try {
    const params = {
      pageNo: pageNo.value,
      pageSize: pageSize.value
    }
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.type) params.type = filters.type
    if (filters.priority) params.priority = filters.priority
    if (filters.status) params.status = filters.status

    const data = await getWorkItemList(params)
    records.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNo.value = 1
  fetchList()
}

function handleReset() {
  filters.keyword = ''
  filters.type = ''
  filters.priority = ''
  filters.status = ''
  pageNo.value = 1
  fetchList()
}

function changePage(p) {
  pageNo.value = p
  fetchList()
}

function goDetail(id) {
  router.push({ name: 'WorkItemDetail', params: { id } })
}

async function handleCreate() {
  if (!createForm.title.trim()) {
    return
  }
  if (!createForm.type) {
    return
  }
  creating.value = true
  try {
    const payload = {
      title: createForm.title.trim(),
      type: createForm.type,
      priority: createForm.priority
    }
    if (createForm.description) payload.description = createForm.description
    if (createForm.assignee) payload.assignee = createForm.assignee
    if (createForm.tagsInput.trim()) {
      payload.tags = createForm.tagsInput.split(',').map(t => t.trim()).filter(Boolean)
    }
    await createWorkItem(payload)
    showCreateModal.value = false
    resetCreateForm()
    fetchList()
  } finally {
    creating.value = false
  }
}

function resetCreateForm() {
  createForm.title = ''
  createForm.description = ''
  createForm.type = ''
  createForm.priority = 'P2'
  createForm.assignee = ''
  createForm.tagsInput = ''
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.filter-bar .filter-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: flex-end;
}

.filter-actions {
  padding-bottom: 2px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th,
.data-table td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
  white-space: nowrap;
}

.data-table th {
  background: #fafafa;
  font-weight: 600;
  color: #555;
  font-size: 13px;
}

.data-table tbody tr:hover {
  background: #f8f9ff;
}

.title-cell {
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tag-story { background: #e8f5e9; color: #2e7d32; }
.tag-bug { background: #fde8e8; color: #c62828; }
.tag-task { background: #e3f2fd; color: #1565c0; }

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 20px;
  font-size: 14px;
}

.page-info {
  color: #666;
}
</style>