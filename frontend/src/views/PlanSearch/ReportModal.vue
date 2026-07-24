<template>
    <div class="overlay" @click.self="handleOverlayClick">
      <div class="modal">

        <template v-if="step === 'form'">
          <div class="modal-title">이 여행 플랜을 신고할게요</div>
          <div class="modal-sub">신고 사유를 선택해주세요</div>

          <div class="reason-list">
            <label v-for="option in reasons" :key="option.value" class="reason-item">
              <input type="radio" name="reason" :value="option.value" v-model="selectedReason" />
              <span>{{ option.label }}</span>
            </label>
          </div>

          <textarea v-model="detail" class="detail-input" placeholder="자세한 내용을 입력해주세요 (선택)" rows="3"></textarea>

          <div class="modal-footer">
            <button class="btn-cancel" @click="$emit('close')">취소</button>
            <button class="btn-submit" :disabled="!selectedReason" @click="submit">신고하기</button>
          </div>
        </template>

        <template v-else-if="step === 'done'">
          <div class="success-wrap">
            <div class="success-icon">✅</div>
            <div class="success-title">신고가 접수됐어요</div>
            <div class="success-sub">
              신고해주셔서 감사해요. 운영팀이 검토 후<br />필요한 조치를 진행할게요.
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-submit" style="flex: 1;" @click="$emit('close')">확인</button>
          </div>
        </template>

      </div>
    </div>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['close', 'submit'])

const reasons = [
  { value: 'INAPPROPRIATE', label: '부적절한 콘텐츠' },
  { value: 'FALSE_INFO', label: '허위/잘못된 정보' },
  { value: 'SPAM', label: '스팸/광고성' },
  { value: 'OTHER', label: '기타' },
]

const selectedReason = ref('')
const detail = ref('')
const step = ref('form')

function submit() {
  if (!selectedReason.value.trim()) return
  // TODO: 백엔드 연동 시 API 호출 (신고 접수) 후 응답 성공하면 step을 'done'으로 전환
  emit('submit', { reason: selectedReason.value, detail: detail.value })
  step.value = 'done'
}

function handleOverlayClick() {
  // 완료 화면에서는 바깥 클릭으로 바로 닫혀도 무방하지만,
  // 작성 중인 폼에서 실수로 닫히는 걸 막고 싶다면 이 부분에서 분기 처리 가능
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
  width: 440px;
  max-width: 90%;
  padding: 1.75rem 2rem 1.5rem;
  box-shadow: 0 20px 60px rgba(0, 0, 0, .18);
}

.modal-title {
  font-size: 17px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 5px;
}

.modal-sub {
  font-size: 13px;
  color: #999;
  margin-bottom: 1.25rem;
}

.reason-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 1.25rem;
}

.reason-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
}

.reason-item input {
  accent-color: #0f766e;
  width: 16px;
  height: 16px;
}

.detail-input {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  font-size: 13px;
  font-family: inherit;
  resize: none;
  outline: none;
  margin-bottom: 1.25rem;
}

.detail-input:focus {
  border-color: #0f766e;
}

.modal-footer {
  display: flex;
  gap: 8px;
}

.btn-cancel {
  flex: 1;
  padding: 11px;
  border-radius: 22px;
  border: 1px solid #e0e0e0;
  background: #fff;
  color: #888;
  font-size: 14px;
  cursor: pointer;
}

.btn-submit {
  flex: 1.4;
  padding: 11px;
  border-radius: 22px;
  border: none;
  background: #0f766e;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.btn-submit:disabled {
  background: #e0e0e0;
  color: #bbb;
  cursor: not-allowed;
}

.success-wrap {
  text-align: center;
  padding: 1rem 0 1.5rem;
}

.success-icon {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: #e8f7f2;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1rem;
  font-size: 22px;
}

.success-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 6px;
}

.success-sub {
  font-size: 13px;
  color: #999;
  line-height: 1.6;
}
</style>