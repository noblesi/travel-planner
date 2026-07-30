<template>
  <div class="nickname-wrapper">
    <!-- 1. 읽기 모드 (텍스트 + 연필 아이콘) -->
    <div v-if="!isEditing" class="read-container">
      <span class="nickname-text">{{ nickname }}</span>
      <button @click="startEdit" class="edit-icon-btn" aria-label="닉네임 수정">
        <!-- Lucide-react나 FontAwesome의 연필 아이콘 SVG 대체 가능 -->
        <svg xmlns="http://w3.org" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="pencil-icon"><path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/></svg>
      </button>
    </div>

    <!-- 2. 편집 모드 (인라인 인풋창 + 미니 버튼) -->
    <div v-else class="edit-container">
      <input 
        v-model="newNickname" 
        ref="nicknameInput"
        type="text"
        class="inline-input"
        placeholder="닉네임 입력"
        @keyup.enter="saveNickname"
        @keyup.esc="cancelEdit"
      />
      <div class="inline-btn-group">
        <button @click="saveNickname" class="inline-btn save">확인</button>
        <button @click="cancelEdit" class="inline-btn cancel">취소</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 전체 정렬을 중앙으로 배치 */
.nickname-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 40px; /* 인풋창이 켜져도 높이가 유지되도록 설정 */
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

/* 1. 읽기 모드 스타일 */
.read-container {
  display: flex;
  align-items: center;
  gap: 6px;
}

.nickname-text {
  font-size: 18px;
  color: #1e293b;
  font-weight: 500;
  line-height: 1.2;
}

.edit-icon-btn {
  background: none;
  border: none;
  padding: 4px;
  cursor: pointer;
  color: #94a3b8; /* 은은한 회색 아이콘 */
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s ease;
}

/* 마우스를 올렸을 때만 아이콘이 선명해짐 */
.edit-icon-btn:hover {
  color: #475569;
  background-color: #f1f5f9;
}

/* 2. 편집 모드 스타일 */
.edit-container {
  display: flex;
  align-items: center;
  gap: 8px;
  animation: fadeIn 0.15s ease-in-out;
}

.inline-input {
  font-size: 16px;
  font-weight: 500;
  color: #1e293b;
  padding: 6px 12px;
  width: 180px;
  border: 1.5px solid #cbd5e1;
  border-radius: 6px;
  outline: none;
  background-color: #ffffff;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.inline-input:focus {
  border-color: #64748b; /* 너무 튀지 않는 차분한 다크그레이 */
  box-shadow: 0 0 0 3px rgba(100, 116, 139, 0.1);
}

/* 저장/취소 미니 버튼 */
.inline-btn-group {
  display: flex;
  gap: 4px;
}

.inline-btn {
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 500;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  transition: background-color 0.2s;
}

.inline-btn.save {
  background-color: #334155;
  color: #ffffff;
}
.inline-btn.save:hover {
  background-color: #1e293b;
}

.inline-btn.cancel {
  background-color: #f1f5f9;
  color: #64748b;
}
.inline-btn.cancel:hover {
  background-color: #e2e8f0;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-2px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
