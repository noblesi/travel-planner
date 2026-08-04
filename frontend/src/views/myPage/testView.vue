<template>
  <div class="my-info-container">
    <!-- 타이틀 영역 -->
    <div class="title-section">
      <h2 class="title">나의 정보</h2>
      <button class="action-btn" @click="isEditMode = !isEditMode">
        {{ isEditMode ? '취소' : '수정하기' }}
      </button>
    </div>

    <!-- 정보 표시 / 수정 폼 영역 -->
    <div class="info-content">
      <!-- 1. 이름 (전체 너비) -->
      <div class="form-group full-width">
        <label>이름</label>
        <div v-if="!isEditMode" class="value-text">{{ userInfo.name }}</div>
        <input v-else v-model="userInfo.name" type="text" class="info-input" placeholder="이름을 입력하세요" />
      </div>

      <!-- 2. 성별 & 3. 생년월일 (2열 배치로 공간 절약) -->
      <div class="grid-row">
        <div class="form-group">
          <label>성별</label>
          <div v-if="!isEditMode" class="value-text">{{ userInfo.gender }}</div>
          <select v-else v-model="userInfo.gender" class="info-input select-box">
            <option value="선택 안함">선택 안함</option>
            <option value="남성">남성</option>
            <option value="여성">여성</option>
          </select>
        </div>

        <div class="form-group">
          <label>생년월일</label>
          <div v-if="!isEditMode" class="value-text">{{ userInfo.birthdate }}</div>
          <input v-else v-model="userInfo.birthdate" type="date" class="info-input" />
        </div>
      </div>

      <!-- 4. 이메일 (전체 너비) -->
      <div class="form-group full-width">
        <label>이메일 주소</label>
        <div v-if="!isEditMode" class="value-text">{{ userInfo.email }}</div>
        <input v-else v-model="userInfo.email" type="email" class="info-input" placeholder="example@email.com" />
      </div>
    </div>

    <!-- 수정 모드일 때만 나타나는 저장 버튼 -->
    <div v-if="isEditMode" class="button-section">
      <button class="save-btn" @click="saveInfo">변경사항 저장</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

// 수정 모드 상태 토글
const isEditMode = ref(false)

// 사용자 정보 데이터 상태 관리
const userInfo = ref({
  name: '홍길동',
  gender: '남성',
  birthdate: '1998-04-02',
  email: 'hong@example.com'
})

// 저장 함수
const saveInfo = () => {
  // 백엔드 API 연동이 필요할 경우 이곳에 요청 코드를 작성합니다.
  alert('정보가 성공적으로 수정되었습니다.')
  isEditMode.value = false
}
</script>

<style scoped>
/* 폰트 지정 및 컨테이너 내부 여백 설정 */
.my-info-container {
  font-family: 'Pretendard', 'Noto Sans KR', sans-serif;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  color: #ffffff;
}

/* 상단 타이틀 및 버튼 정렬 */
.title-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid rgba(255, 255, 255, 0.3);
}

.title {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0;
}

/* 상단 우측 수정/취소 버튼 */
.action-btn {
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #ffffff;
  padding: 0.5rem 1rem;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s ease;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 정보 콘텐츠 배치 레이아웃 */
.info-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.grid-row {
  display: flex;
  gap: 1.5rem;
  width: 100%;
}

.grid-row .form-group {
  flex: 1;
}

/* 개별 입력/조회 그룹 스타일 */
.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

/* 라벨 디자인 (연한 브라운 톤) */
.form-group label {
  font-size: 0.9rem;
  font-weight: 600;
  color: #fbe3d6;
  opacity: 0.85;
}

/* 조회 모드일 때 텍스트 박스 */
.value-text {
  background-color: rgba(0, 0, 0, 0.08); /* 기존 박스 톤보다 아주 살짝 어둡게 내려앉는 깊이감 */
  padding: 0.85rem 1.2rem;
  border-radius: 12px;
  font-size: 1.05rem;
  font-weight: 500;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

/* 수정 모드일 때 인풋 박스 디자인 */
.info-input {
  background-color: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.8);
  color: #2b2b2b; /* 텍스트 작성 시 잘 보이도록 어두운 컬러 채택 */
  padding: 0.85rem 1.2rem;
  border-radius: 12px;
  font-size: 1.05rem;
  outline: none;
  box-sizing: border-box;
  width: 100%;
  transition: border-color 0.2s ease;
}

.info-input:focus {
  border-color: #f47a42; /* 포커스 시 포인트 컬러 (기존 UI 주황색 계열 대비) */
  box-shadow: 0 0 0 3px rgba(244, 122, 66, 0.2);
}

/* 셀렉트 박스 스타일 리셋 패딩 */
.select-box {
  appearance: none;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://w3.org' width='24' height='24' viewBox='0 0 24 24' fill='none' stroke='%232b2b2b' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><polyline points='6 9 12 15 18 9'></polyline></svg>");
  background-repeat: no-repeat;
  background-position: right 1rem center;
  background-size: 1.2rem;
}

/* 변경사항 저장 하단 바 버튼 */
.button-section {
  margin-top: 2.5rem;
  display: flex;
  justify-content: flex-end;
}

.save-btn {
  background-color: #ffffff;
  color: #c85329; /* 기존 백그라운드 오렌지/브라운 계열과 대비를 이루는 텍스트 컬러 */
  border: none;
  padding: 0.9rem 2rem;
  border-radius: 12px;
  font-size: 1.05rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s ease, background-color 0.2s ease;
}

.save-btn:hover {
  background-color: #fbe3d6;
  transform: translateY(-1px);
}

.save-btn:active {
  transform: translateY(1px);
}
</style>
