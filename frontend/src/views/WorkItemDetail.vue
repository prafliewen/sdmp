<template>
  <div class="workitem-detail">
    <!-- Back Button -->
    <div style="margin-bottom: 16px;">
      <button class="btn" @click="goBack">&larr; 返回列表</button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="empty-state">加载中...</div>

    <template v-else-if="item">
      <!-- Info Card -->
      <div class="card">
        <div class="detail-header">
          <div>
            <span style="font-size: 13px; color: #888;">{{ item.code }}</span>
            <h2 style="font-size: 18px; margin-top: 4px;">{{ item.title }}</h2>
          </div>
          <span class="status-badge" :class="'status-' + item.status">{{ statusLabel(item.status) }}</span>
        </div>
        <div class="detail-grid">
          <div class="detail-field">
            <label>类型</label>
            <span><span class="tag" :class="'tag-' + getTypeTagClass(item.type)">{{ item.type }}</span></span>
          </div>
          <div class="detail-field">
            <label>优先级</label>
            <span><span class="tag" :class="'tag-p' + (item.priority || '').replace('P', '').toLowerCase()">{{ item.priority }}</span></span>
          </div>
          <div class="detail-field">
            <label>风险等级</label>
            <span>{{ item.riskLevel || '-' }}</span>
          </div>
          <div class="detail-field">
            <label>负责人</label>
            <span>{{ item.assignee || '-' }}</span>
          </div>
          <div class="detail-field">
            <label>报告人</label>
            <span>{{ item.reporter || '-' }}</span>
          </div>
          <div class="detail-field">
            <label>创建时间</label>
            <span>{{ formatDate(item.createdAt) }}</span>
          </div>
          <div class="detail-field">
            <label>更新时间</label>
            <span>{{ formatDate(item.updatedAt) }}</span>
          </div>
          <div class="detail-field" v-if="item.tags && item.tags.length">
            <label>标签</label>
            <span>
              <span v-for="t in item.tags" :key="t" class="tag" style="margin-right: 4px; background: #f0f2f5; color: #666;">{{ t }}</span>
            </span>
          </div>
        </div>
        <div v-if="item.description" style="margin-top: 16px;">
          <label style="font-size: 13px; font-weight: 500; color: #555;">描述</label>
          <p style="margin-top: 4px; white-space: pre-wrap; color: #333; font-size: 14px; line-height: 1.6;">{{ item.description }}</p>
        </div>
        <div v-if="item.acceptanceCriteria && item.acceptanceCriteria.length" style="margin-top: 16px;">
          <label style="font-size: 13px; font-weight: 500; color: #555;">验收标准</label>
          <ul style="margin-top: 4px; padding-left: 20px; font-size: 14px; color: #333;">
            <li v-for="ac in item.acceptanceCriteria" :key="ac">{{ ac }}</li>
          </ul>
        </div>
        <div v-if="item.p0OpenClarifications > 0 || item.totalOpenClarifications > 0" style="margin-top: 12px; font-size: 13px; color: #e65100;">
          P0 未解决: {{ item.p0OpenClarifications }} | 总计未解决: {{ item.totalOpenClarifications }}
        </div>
      </div>

      <!-- Tabs -->
      <div class="card" style="padding: 0;">
        <div class="tab-bar">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            class="tab-btn"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >{{ tab.label }}</button>
        </div>
        <div class="tab-content">
          <!-- Clarification Tab -->
          <div v-if="activeTab === 'clarification'" class="tab-panel">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
              <h3 style="font-size: 15px; font-weight: 600;">澄清问题列表</h3>
              <button class="btn btn-primary btn-sm" @click="showClarifyAdd = !showClarifyAdd">
                {{ showClarifyAdd ? '取消' : '新增澄清问题' }}
              </button>
            </div>

            <!-- Add Clarification Form -->
            <div v-if="showClarifyAdd" class="card" style="background: #f9fafb;">
              <div class="form-group">
                <label>问题描述 <span style="color:red;">*</span></label>
                <textarea v-model="clarifyForm.question" placeholder="请详细描述需要澄清的问题" maxlength="2000"></textarea>
              </div>
              <div class="form-group">
                <label>严重程度</label>
                <select v-model="clarifyForm.severity">
                  <option value="P0">P0</option>
                  <option value="P1">P1</option>
                  <option value="P2">P2</option>
                </select>
              </div>
              <div style="text-align: right;">
                <button class="btn btn-primary btn-sm" @click="handleAddClarification" :disabled="clarifyAdding">
                  提交
                </button>
              </div>
            </div>

            <!-- Clarification List -->
            <div v-if="clarifications.length > 0">
              <div v-for="c in clarifications" :key="c.id" class="clarify-item">
                <div class="clarify-header">
                  <span class="tag" :class="'tag-p' + (c.severity || '').replace('P', '').toLowerCase()">{{ c.severity }}</span>
                  <span class="status-badge" :class="c.status === 'OPEN' ? 'status-IN_PROGRESS' : 'status-DONE'">
                    {{ c.status === 'OPEN' ? '未解决' : '已解决' }}
                  </span>
                  <span style="font-size: 12px; color: #999; margin-left: auto;">{{ c.raisedBy }} · {{ c.createdAt }}</span>
                </div>
                <p class="clarify-question">{{ c.question }}</p>
                <div v-if="c.status === 'RESOLVED' && c.answer" class="clarify-answer">
                  <strong>答复：</strong>{{ c.answer }}
                  <div style="font-size: 12px; color: #999; margin-top: 4px;">
                    由 {{ c.resolvedBy }} 于 {{ c.resolvedAt }} 解决
                  </div>
                </div>
                <div v-if="c.status === 'OPEN'" style="margin-top: 8px; text-align: right;">
                  <button class="btn btn-sm btn-primary" @click="openResolveModal(c)">解决</button>
                </div>
              </div>
            </div>
            <div v-else class="empty-state">暂无澄清问题</div>

            <!-- Pagination for clarifications -->
            <div v-if="clarifyTotal > clarifyPageSize" class="pagination">
              <button class="btn btn-sm" :disabled="clarifyPageNo <= 1" @click="changeClarifyPage(clarifyPageNo - 1)">上一页</button>
              <span class="page-info">第 {{ clarifyPageNo }} / {{ Math.ceil(clarifyTotal / clarifyPageSize) }} 页</span>
              <button class="btn btn-sm" :disabled="clarifyPageNo >= Math.ceil(clarifyTotal / clarifyPageSize)" @click="changeClarifyPage(clarifyPageNo + 1)">下一页</button>
            </div>
          </div>

          <!-- Transition Tab -->
          <div v-if="activeTab === 'transition'" class="tab-panel">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
              <h3 style="font-size: 15px; font-weight: 600;">状态流转</h3>
              <button
                class="btn btn-primary btn-sm"
                @click="showTransitionModal = true"
                :disabled="allowedTransitions.length === 0"
              >状态流转</button>
            </div>
            <p v-if="allowedTransitions.length === 0" style="color: #999; font-size: 13px; margin-bottom: 16px;">
              当前状态 {{ statusLabel(item.status) }} 无可用流转目标
            </p>

            <!-- History Table -->
            <table v-if="transitionHistory.length > 0" class="data-table">
              <thead>
                <tr>
                  <th>源状态</th>
                  <th>目标状态</th>
                  <th>原因</th>
                  <th>操作人</th>
                  <th>时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="h in transitionHistory" :key="h.id">
                  <td><span class="status-badge" :class="'status-' + h.fromStatus">{{ statusLabel(h.fromStatus) }}</span></td>
                  <td><span class="status-badge" :class="'status-' + h.toStatus">{{ statusLabel(h.toStatus) }}</span></td>
                  <td>{{ h.reason || '-' }}</td>
                  <td>{{ h.operator || '-' }}</td>
                  <td>{{ h.createdAt }}</td>
                </tr>
              </tbody>
            </table>
            <div v-else class="empty-state">暂无流转记录</div>

            <div v-if="transitionTotal > transitionPageSize" class="pagination">
              <button class="btn btn-sm" :disabled="transitionPageNo <= 1" @click="changeTransitionPage(transitionPageNo - 1)">上一页</button>
              <span class="page-info">第 {{ transitionPageNo }} / {{ Math.ceil(transitionTotal / transitionPageSize) }} 页</span>
              <button class="btn btn-sm" :disabled="transitionPageNo >= Math.ceil(transitionTotal / transitionPageSize)" @click="changeTransitionPage(transitionPageNo + 1)">下一页</button>
            </div>
          </div>

          <!-- AI Analysis Tab -->
          <div v-if="activeTab === 'ai'" class="tab-panel">
            <!-- Trigger Section -->
            <div class="card" style="background: #f9fafb;">
              <h3 style="font-size: 15px; font-weight: 600; margin-bottom: 12px;">触发 AI 分析</h3>
              <div style="display: flex; gap: 12px; align-items: flex-end;">
                <div class="form-group" style="margin-bottom: 0; width: 200px;">
                  <label>分析类型</label>
                  <select v-model="aiType">
                    <option value="SUMMARY">SUMMARY - 摘要</option>
                    <option value="RISK">RISK - 风险评估</option>
                    <option value="CLARIFICATION">CLARIFICATION - 澄清建议</option>
                  </select>
                </div>
                <button class="btn btn-primary" @click="handleTriggerAi" :disabled="aiTriggering">
                  {{ aiTriggering ? '分析中...' : '触发分析' }}
                </button>
              </div>
            </div>

            <!-- Analysis History -->
            <h3 style="font-size: 15px; font-weight: 600; margin: 20px 0 12px;">分析历史</h3>
            <div v-if="aiAnalyses.length > 0">
              <div v-for="a in aiAnalyses" :key="a.id" class="ai-item" @click="toggleAiExpand(a.id)">
                <div class="ai-item-header">
                  <span class="tag" style="background: #e3f2fd; color: #1565c0; margin-right: 8px;">{{ a.analysisType }}</span>
                  <span style="font-size: 12px; color: #999;">来源: {{ a.source || '-' }}</span>
                  <span style="font-size: 12px; color: #999; margin-left: auto;">{{ a.createdAt }}</span>
                </div>
                <p v-if="a.summary" style="font-size: 14px; color: #333; margin: 8px 0;">{{ a.summary }}</p>
                <div v-if="expandedAi === a.id && a.payload" class="ai-payload">
                  <pre>{{ JSON.stringify(a.payload, null, 2) }}</pre>
                </div>
              </div>
            </div>
            <div v-else class="empty-state">暂无分析记录</div>

            <div v-if="aiTotal > aiPageSize" class="pagination">
              <button class="btn btn-sm" :disabled="aiPageNo <= 1" @click="changeAiPage(aiPageNo - 1)">上一页</button>
              <span class="page-info">第 {{ aiPageNo }} / {{ Math.ceil(aiTotal / aiPageSize) }} 页</span>
              <button class="btn btn-sm" :disabled="aiPageNo >= Math.ceil(aiTotal / aiPageSize)" @click="changeAiPage(aiPageNo + 1)">下一页</button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- Transition Modal -->
    <div v-if="showTransitionModal" class="modal-overlay" @click.self="showTransitionModal = false">
      <div class="modal">
        <h2>状态流转</h2>
        <div class="form-group">
          <label>当前状态</label>
          <input :value="statusLabel(item.status)" disabled />
        </div>
        <div class="form-group">
          <label>目标状态 <span style="color:red;">*</span></label>
          <select v-model="transitionForm.targetStatus">
            <option value="">请选择目标状态</option>
            <option v-for="t in allowedTransitions" :key="t" :value="t">{{ statusLabel(t) }}</option>
          </select>
        </div>
        <div class="form-group">
          <label>流转原因</label>
          <textarea v-model="transitionForm.reason" placeholder="可选填写流转原因" maxlength="500"></textarea>
        </div>
        <div class="modal-actions">
          <button class="btn" @click="showTransitionModal = false">取消</button>
          <button class="btn btn-primary" @click="handleTransition" :disabled="transiting || !transitionForm.targetStatus">
            确认流转
          </button>
        </div>
      </div>
    </div>

    <!-- Resolve Clarification Modal -->
    <div v-if="showResolveModal" class="modal-overlay" @click.self="showResolveModal = false">
      <div class="modal">
        <h2>解决澄清问题</h2>
        <p style="font-size: 14px; color: #555; margin-bottom: 12px; background: #f5f5f5; padding: 10px; border-radius: 6px;">
          {{ resolveTarget?.question }}
        </p>
        <div class="form-group">
          <label>答复 <span style="color:red;">*</span></label>
          <textarea v-model="resolveForm.answer" placeholder="请输入答复内容" maxlength="2000"></textarea>
        </div>
        <div class="modal-actions">
          <button class="btn" @click="showResolveModal = false">取消</button>
          <button class="btn btn-primary" @click="handleResolve" :disabled="resolving || !resolveForm.answer.trim()">
            确认解决
          </button>
        </div>
      </div>
    </div>

    <!-- Not Found -->
    <div v-if="!loading && !item" class="empty-state">工作项不存在</div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getWorkItemDetail } from '../api/workitem'
import { getClarifications, addClarification, resolveClarification } from '../api/clarification'
import { transitWorkItem, getTransitionHistory } from '../api/transition'
import { triggerAiAnalysis, getAiAnalyses } from '../api/ai'

const route = useRoute()
const router = useRouter()

const id = computed(() => route.params.id)

// Work item state
const item = ref(null)
const loading = ref(true)

// Tab state
const tabs = [
  { key: 'clarification', label: '澄清问题' },
  { key: 'transition', label: '状态流转' },
  { key: 'ai', label: 'AI 分析' }
]
const activeTab = ref('clarification')

// Status transition map
const transitionMap = {
  DRAFT: ['ANALYZING'],
  ANALYZING: ['READY', 'DRAFT'],
  READY: ['IN_PROGRESS', 'ANALYZING'],
  IN_PROGRESS: ['IN_TESTING', 'READY'],
  IN_TESTING: ['DONE', 'IN_PROGRESS'],
  DONE: []
}

const allowedTransitions = computed(() => {
  if (!item.value) return []
  return transitionMap[item.value.status] || []
})

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

function getTypeTagClass(type) {
  if (type === 'STORY') return 'story'
  if (type === 'BUG') return 'bug'
  if (type === 'TASK') return 'task'
  return ''
}

function goBack() {
  router.push({ name: 'WorkItemList' })
}

// Clarification state
const clarifications = ref([])
const showClarifyAdd = ref(false)
const clarifyAdding = ref(false)
const clarifyPageNo = ref(1)
const clarifyPageSize = ref(20)
const clarifyTotal = ref(0)
const clarifyForm = reactive({
  question: '',
  severity: 'P2'
})

const showResolveModal = ref(false)
const resolveTarget = ref(null)
const resolving = ref(false)
const resolveForm = reactive({ answer: '' })

async function fetchClarifications() {
  try {
    const data = await getClarifications(id.value, {
      pageNo: clarifyPageNo.value,
      pageSize: clarifyPageSize.value
    })
    clarifications.value = data.records || []
    clarifyTotal.value = data.total || 0
  } catch {
    // handled by interceptor
  }
}

function changeClarifyPage(p) {
  clarifyPageNo.value = p
  fetchClarifications()
}

async function handleAddClarification() {
  if (!clarifyForm.question.trim()) return
  clarifyAdding.value = true
  try {
    await addClarification(id.value, {
      question: clarifyForm.question,
      severity: clarifyForm.severity
    })
    clarifyForm.question = ''
    clarifyForm.severity = 'P2'
    showClarifyAdd.value = false
    clarifyPageNo.value = 1
    fetchClarifications()
  } finally {
    clarifyAdding.value = false
  }
}

function openResolveModal(c) {
  resolveTarget.value = c
  resolveForm.answer = ''
  showResolveModal.value = true
}

async function handleResolve() {
  if (!resolveForm.answer.trim() || !resolveTarget.value) return
  resolving.value = true
  try {
    await resolveClarification(resolveTarget.value.id, {
      answer: resolveForm.answer
    })
    showResolveModal.value = false
    resolveTarget.value = null
    fetchClarifications()
  } finally {
    resolving.value = false
  }
}

// Transition state
const transitionHistory = ref([])
const showTransitionModal = ref(false)
const transiting = ref(false)
const transitionPageNo = ref(1)
const transitionPageSize = ref(20)
const transitionTotal = ref(0)
const transitionForm = reactive({
  targetStatus: '',
  reason: ''
})

async function fetchTransitionHistory() {
  try {
    const data = await getTransitionHistory(id.value, {
      pageNo: transitionPageNo.value,
      pageSize: transitionPageSize.value
    })
    transitionHistory.value = data.records || []
    transitionTotal.value = data.total || 0
  } catch {
    // handled by interceptor
  }
}

function changeTransitionPage(p) {
  transitionPageNo.value = p
  fetchTransitionHistory()
}

async function handleTransition() {
  if (!transitionForm.targetStatus) return
  transiting.value = true
  try {
    await transitWorkItem(id.value, {
      targetStatus: transitionForm.targetStatus,
      reason: transitionForm.reason || undefined
    })
    showTransitionModal.value = false
    transitionForm.targetStatus = ''
    transitionForm.reason = ''
    // Refresh item detail + transition history
    await fetchDetail()
    transitionPageNo.value = 1
    fetchTransitionHistory()
  } finally {
    transiting.value = false
  }
}

// AI state
const aiType = ref('SUMMARY')
const aiTriggering = ref(false)
const aiAnalyses = ref([])
const aiPageNo = ref(1)
const aiPageSize = ref(10)
const aiTotal = ref(0)
const expandedAi = ref(null)

async function fetchAiAnalyses() {
  try {
    const data = await getAiAnalyses(id.value, {
      pageNo: aiPageNo.value,
      pageSize: aiPageSize.value
    })
    aiAnalyses.value = data.records || []
    aiTotal.value = data.total || 0
  } catch {
    // handled by interceptor
  }
}

function changeAiPage(p) {
  aiPageNo.value = p
  fetchAiAnalyses()
}

function toggleAiExpand(aid) {
  expandedAi.value = expandedAi.value === aid ? null : aid
}

async function handleTriggerAi() {
  aiTriggering.value = true
  try {
    await triggerAiAnalysis(id.value, { analysisType: aiType.value })
    aiPageNo.value = 1
    fetchAiAnalyses()
  } finally {
    aiTriggering.value = false
  }
}

// Fetch detail
async function fetchDetail() {
  try {
    const data = await getWorkItemDetail(id.value)
    item.value = data
  } catch {
    item.value = null
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDetail()
  fetchClarifications()
  fetchTransitionHistory()
  fetchAiAnalyses()
})
</script>

<style scoped>
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.detail-field {
  font-size: 14px;
}

.detail-field label {
  display: block;
  font-size: 12px;
  color: #999;
  margin-bottom: 2px;
}

.tag-story { background: #e8f5e9; color: #2e7d32; }
.tag-bug { background: #fde8e8; color: #c62828; }
.tag-task { background: #e3f2fd; color: #1565c0; }

.tab-bar {
  display: flex;
  border-bottom: 2px solid #f0f0f0;
}

.tab-btn {
  padding: 12px 24px;
  border: none;
  background: none;
  font-size: 14px;
  cursor: pointer;
  color: #666;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all 0.2s;
}

.tab-btn:hover {
  color: #1a73e8;
}

.tab-btn.active {
  color: #1a73e8;
  border-bottom-color: #1a73e8;
  font-weight: 600;
}

.tab-panel {
  padding: 20px;
}

/* Clarification items */
.clarify-item {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 12px;
}

.clarify-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.clarify-question {
  font-size: 14px;
  color: #333;
  line-height: 1.6;
}

.clarify-answer {
  margin-top: 10px;
  padding: 10px;
  background: #f0fdf4;
  border-radius: 6px;
  font-size: 14px;
  color: #333;
  line-height: 1.6;
}

/* AI items */
.ai-item {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.ai-item:hover {
  background: #f9fafb;
}

.ai-item-header {
  display: flex;
  align-items: center;
}

.ai-payload {
  margin-top: 12px;
  background: #f5f5f5;
  border-radius: 6px;
  padding: 12px;
  overflow-x: auto;
}

.ai-payload pre {
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
}

/* Data table (reused in tabs) */
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th,
.data-table td {
  padding: 10px 14px;
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