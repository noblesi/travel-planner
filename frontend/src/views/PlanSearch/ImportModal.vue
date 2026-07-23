<template>
  <div class="overlay" @click.self="handleClose">
    <div class="modal">

      <!-- STEP: 이름 + 날짜 (기존 플랜에 추가 옵션은 삭제되어, 이제 가져오기는 항상 새 플랜 생성으로 진행) -->
      <template v-if="step === 'new'">
        <div class="modal-header">
          <div>
            <div class="modal-title">일정 가져오기</div>
            <div class="modal-sub">새 플랜의 이름과 날짜를 설정해요</div>
          </div>
          <button class="modal-close" @click="handleClose">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">플랜 이름</label>
            <input class="form-input" type="text" v-model="newPlanName" />
          </div>
          <div class="form-group">
            <label class="form-label">여행 날짜</label>
            <div class="date-row">
              <input class="date-input" type="date" v-model="newStartDate" />
              <input class="date-input" type="date" v-model="newEndDate" />
            </div>
          </div>
          <div class="notice-box">
            💡 원본은 {{ plan.days.length }}일 일정이에요. 여행 기간을 더 짧게 설정하면, 넘어가는 날짜의 일정은 담기지 않아요.
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-primary" :disabled="!newPlanName || !newStartDate || !newEndDate" @click="submitNew">플랜
            만들기</button>
        </div>
      </template>

      <!-- SUCCESS -->
      <template v-else-if="step === 'success'">
        <button class="modal-close modal-close--floating" @click="handleClose">✕</button>
        <div class="success-wrap">
          <div class="success-icon">🎉</div>
          <div class="success-title">새 플랜이 만들어졌어요!</div>
          <div class="success-sub">
            {{ newPlanName }}가 저장됐어요.<br />날짜나 장소는 언제든지 수정할 수 있어요.
          </div>
        </div>
        <div class="modal-footer modal-footer--success">
          <button class="btn-primary" @click="handleClose">내 플랜 보러 가기</button>
        </div>
      </template>

    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  plan: { type: Object, required: true },
})
const emit = defineEmits(['close'])

const step = ref('new')
const newPlanName = ref(`${props.plan.title} (복사)`)
const newStartDate = ref('')
const newEndDate = ref('')

function submitNew() {
  // TODO: 백엔드 연동 시 API 호출 (새 플랜 생성)
  step.value = 'success'
}

function handleClose() {
  emit('close')
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, .32);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  font-family: -apple-system, 'Apple SD Gothic Neo', 'Noto Sans KR', sans-serif;
}

.modal {
  background: #fff;
  border-radius: 16px;
  width: 520px;
  max-width: 90%;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, .18);
}

.modal-header {
  padding: 1.25rem 1.5rem 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.modal-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
}

.modal-sub {
  font-size: 12px;
  color: #999;
  margin-top: 3px;
}

.modal-close {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: #f5f5f5;
  color: #888;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* 성공 화면 전용: 문서 흐름에서 빼서 모달 우측 상단에 고정.
   이렇게 해야 success-wrap이 위아래 대칭 패딩만으로 중앙에 균형 있게 배치된다. */
.modal-close--floating {
  position: absolute;
  top: 1.25rem;
  right: 1.5rem;
  z-index: 1;
}

.modal-body {
  padding: 1.25rem 1.5rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-label {
  font-size: 11px;
  color: #888;
  margin-bottom: 5px;
  display: block;
}

.form-input {
  width: 100%;
  padding: 9px 12px;
  border: 1.5px solid #e0e0e0;
  border-radius: 8px;
  font-size: 13px;
  color: #1a1a1a;
  outline: none;
  transition: border-color .15s;
}

.form-input:focus {
  border-color: #D94530;
}

.date-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.date-input {
  width: 100%;
  padding: 9px 12px;
  border: 1.5px solid #e0e0e0;
  border-radius: 8px;
  font-size: 12px;
  color: #1a1a1a;
  outline: none;
  transition: border-color .15s;
}

.date-input:focus {
  border-color: #D94530;
}

.notice-box {
  background: #fdf5f4;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 11px;
  color: #c0614e;
  line-height: 1.5;
}

.modal-footer {
  padding: 0 1.5rem 1.25rem;
  display: flex;
  gap: 8px;
  align-items: center;
}

.modal-footer--success {
  padding: 0 1.5rem 1.75rem;
}

.btn-primary {
  flex: 1;
  padding: 10px;
  border-radius: 22px;
  border: none;
  background: #D94530;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.btn-primary:hover {
  background: #c23d2b;
}

.btn-primary:disabled {
  background: #e0e0e0;
  color: #bbb;
  cursor: not-allowed;
}

/* 위아래 패딩을 대칭으로 맞춰서 아이콘~버튼까지의 전체 블록이 모달 안에서 수직 중앙에 오도록 한다. */
.success-wrap {
  position: relative;
  text-align: center;
  padding: 2.5rem 1.5rem 1.5rem;
}

.success-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #e8f7f2;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.25rem;
  font-size: 24px;
}

.success-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.success-sub {
  font-size: 13px;
  color: #999;
  line-height: 1.65;
}
</style>