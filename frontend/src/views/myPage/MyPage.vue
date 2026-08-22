<template>
    <DefaultLayout>
    <div name="myPageContainer" class="myPage-container">
        <div name="profileWrapDiv" class="profile-wrap">
            <div name="profileImageDiv" class="profile-div">
                <img :src="imageURL" class="img-size"/>
                <img :src="PansleImg" @click="changeProfileImage" alt="프로필 이미지 변경" class="profile-change-btn"/>
                <input type="file" ref="fileInput" style="display: none;" accept="image/*" @change="handleFileChange" />
                <input type="text" ref='nickNameInput'
                    v-model="userInfo.nickName" 
                    :readonly="isReadOnly" 
                    @blur="handleNicknameSubmit" 
                    @change="handleNicknameSubmit"
                    @keyup.enter="$event.target.blur()"
                    class="nickName-text"/>
                <img :src="PansleImg" @click="changeNickName" class="nickName-change-btn"/>
                <a href="#void" class="member-draw-btn">회원 탈퇴</a>
            </div>
            <div name="myInfoWrap" class="myInfo-wrap">
                <div class="my-info-container">
                    <!-- 타이틀 영역 -->
                    <div class="title-section">
                    <h2 class="title">나의 정보</h2>
                        <div>
                            <button class="password-reword-btn" @click="passwordReword">
                                비밀번호 재설정
                            </button>

                            <button class="action-btn" @click="isEditMode = !isEditMode">
                                {{ isEditMode ? '취소' : '수정하기' }}
                            </button>
                        </div>
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
                        <div v-if="!isEditMode" class="value-text">{{ userInfo.birthDate }}</div>
                        <input v-else v-model="userInfo.birthDate" type="date" class="info-input" />
                        </div>
                    </div>

                    <!-- 4. 이메일 (전체 너비) -->
                    <div class="form-group full-width">
                        <label>이메일 주소</label>
                        <div v-if="!isEditMode" class="value-text">{{ userInfo.email }}</div>
                        <input v-else v-model="userInfo.email" type="email" class="info-input" placeholder="example@email.com" />
                    </div>

                    <div class="form-group full-width">
                        <label>휴대전화 번호</label>
                        <div v-if="!isEditMode" class="value-text">{{ userInfo.phoneNumber }}</div>
                        <input v-else v-model="userInfo.phoneNumber" type="tel" class="info-input" placeholder="010-1234-5678" />
                    </div>
                </div>

                    <!-- 수정 모드일 때만 나타나는 저장 버튼 -->
                    <div v-if="isEditMode" class="button-section">
                    <button class="save-btn" @click="saveInfo">변경사항 저장</button>
                    </div>
                </div>
                <!-- 영역 끝 -->
            </div>
        </div>
    </div>
    </DefaultLayout>
</template>

<script setup>
import { ref } from 'vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import PansleImg from '@/assets/myPageImage/pansle.webp'
import DefaultImg from '@/assets/myPageImage/default_profile.webp'

import { getMemberInfo } from '@/api/member'
import { postModifyMemberInfo } from '@/api/member'
import { getModifyNickname } from '@/api/member'
import { postModifyProfileImage } from '@/api/member'
import { getDeleteAccount } from '@/api/member'
import router from '@/router'


const isReadOnly = ref(true)
const nickNameInput = ref('')
const fileInput = ref('')
const imageURL = ref('')
const originalNickName = ref('')

async function setMember(){
    const result = await getMemberInfo()
    return result
}

const userInfo = ref({
    // name: '홍길동',
    // gender: '남성',
    // birthdate: '1998-04-02',
    // email: 'hong@example.com'
    name: '',
    nickName: '',
    gender: '',
    birthDate: '',
    email: '',
    phoneNumber: ''
})

setMember().then((response)=>{
    // 사용자 정보 데이터 상태 관리
    const member = response.data ? response.data : response
    let memberGender = "N";

   
    switch (member.genderCode) {
        case "M":
            memberGender = "남성"
            break;
        case "F":
            memberGender = "여성"
            break;    
        default:
            memberGender = "선택 안함"
            break;
    }
    
    originalNickName.value = member.nickname
    console.log(member.birthDate + "date")
    userInfo.value = {
        name: member.memberName,
        nickName: member.nickname,
        birthDate: member.birthDate.split('T')[0],
        gender: memberGender,
        email: member.email,
        phoneNumber: member.phoneNumber
    }
    
}).catch((error) => {
    console.log(error + "값이 없으니 돌아간다.")
    router.push({name: 'home'})
})

if(imageURL.value == ''){
    imageURL.value = DefaultImg
}

const handleNicknameSubmit = () => {
    console.log(originalNickName.value + " / " + userInfo.value.nickName)
    // 1. 입력창을 다시 읽기 전용(readonly) 상태로 변경
    isReadOnly.value = true;
    
    // 2. 입력값이 비어있지 않고, 기존 닉네임과 값이 다를 때만 서버로 전송
    if (userInfo.value.nickName !== originalNickName.value) {
        changeNicnameApply();
        
        // 전송 성공 후 기준 닉네임 업데이트 (실제로는 API 통신 성공(.then) 내부에서 갱신하는 것이 더 안전합니다)
        originalNickName.value = userInfo.value.nickName; 
    } else {
        // 값이 비어있거나 변경사항이 없으면 원래 닉네임으로 원복
        userInfo.value.nickName = originalNickName.value;
    }
}

// 기존에 작성하신 API 호출 로직
const changeNicnameApply = () => {
    console.log("보내기 전 변경할 내 닉네임 : " + userInfo.value.nickName + " / " + originalNickName.value)
    getModifyNickname(userInfo.value.nickName).then((response)=>{
        console.log("보낸 후 변경할 내 닉네임 : " + userInfo.value.nickName)
        alert(response + " 성공했습니다.");
        originalNickName.value = userInfo.value.nickName; // 성공 시 기준값 업데이트
    }).catch((error) => {
        alert(error + "닉네임 변경에 실패했습니다.");
        userInfo.value.nickName = originalNickName.value; // 실패 시 원래 값으로 복구
    });
}

const changeNickName = () => {
    isReadOnly.value = false
    nickNameInput.value.focus()
}

const changeProfileImage = () => {
    fileInput.value.click()
}

const passwordReword = () => {
    alert("비밀번호 재설정")
}

////////////////////////////////////////////////////////////////
const handleFileChange = (event) => {
  const file = event.target.files[0]
  
  if (file) {
    // 선택한 이미지 파일을 화면에 미리보기 위해 URL을 생성
    imageURL.value = URL.createObjectURL(file)
    
    // 백엔드 서버로 전송할 때는 이 'file' 객체를 FormData에 담아 보낸다.
    console.log('선택된 파일 객체:', file)
  }
}

// 수정 모드 상태 토글
const isEditMode = ref(false)


// 저장 함수
const saveInfo = () => {
    let genderCodeStr = 'N'

    if (userInfo.value.gender === '남성') {
        genderCodeStr = 'M'
    } else if (userInfo.value.gender === '여성') {
        genderCodeStr = 'F'
    }

    const requestData = {
        memberName: userInfo.value.name,
        email: userInfo.value.email,
        genderCode: genderCodeStr,
        birthDate: userInfo.value.birthDate === '' ? null : userInfo.value.birthDate, 
        phoneNumber: userInfo.value.phoneNumber === '' ? null : userInfo.value.phoneNumber
    }

    postModifyMemberInfo(requestData).then((response) => {
        alert('정보가 성공적으로 수정되었습니다.');
        isEditMode.value = false;
    }).catch((error) => {
        alert('정보 수정에 실패했습니다.');
        console.error(error);
    });

}



</script>

<style lang="scss" scoped>

/* 폰트 지정 및 컨테이너 내부 여백 설정 */
.my-info-container {
  font-family: 'Pretendard', 'Noto Sans KR', sans-serif;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  color: #ffffff;
  padding: 50px;
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

// 비밀번호 재설정 버트
.password-reword-btn {
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #ffffff;
  padding: 0.5rem 1rem;
  margin-right: 10px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s ease;
}
.password-reword-btn:hover {
  background: rgba(255, 255, 255, 0.3);
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
  background-color: rgba(247, 145, 62, 0.945);
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
  background-color: rgba(247, 145, 62, 0.945);
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

///////////////////////////////////////////////////////
.myInfo-border {
    text-align: center;
    border-radius: 25px;
    border: 1px solid #e9822e;
}

.member-name-label{
    position: absolute;
    font-size: 25px;
    width: 130px;
    top: 60px;
    left: 40px;
    color: #f1d7c2;
}

.member-name-title{
    position: absolute;
    font-size: 35px;
    top: 53px;
    left: 190px;
    width: 250px;
    
    background-color: #cd7652;
}


.title-text{
    font-size: 35px;
    color: #f1d7c2;
    border-bottom: 3px solid #f1d7c2;
    position: absolute;
    top: 10px;
    left: 45px;
}

.title-text:hover{
    cursor: default;
}

.myInfo-wrap{

    position: absolute;
    top: 130px;
    left : 700px;
    width: 900px;
    height: 600px;
    border-radius: 20px;

    background-color: #c2410c;
}

.myInfo-div{
    grid-template-columns: 300px 300px 200px;
    grid-template-rows: 200px 200px 100px ;
    position: absolute;
    top: 75px;
    left: 50px;
    width: 800px;
    height: 500px;
    border-radius: 50px;

    background-color: #cd7652;
}

.member-draw-btn{
    color: #a89a9a;
    position: absolute;
    font-size: 12px;
    top: 255px;
    left: 100px;
   
}

.myPage-container{
    // position: relative;
    //background-color: #804c77;
    width: 100%;
    min-height: 80vh;
}

.img-size{
    position: absolute;
    border-radius: 500px;
    width: 170px;
    height: 170px;
}

.profile-div{
    position: absolute;
    top: 130px;
    left: 400px;
    width: 250px;
    height: 600px;
    padding-top: 2%;
    padding-left: 2%;
    border-radius: 20px;
    background-color: #c2410c;
}
.profile-change-btn{
    position: absolute;
    background-color: #979595;
    opacity: 0.5;
    border-radius: 8px;
    
    top: 170px;
    left: 175px;
    width: 23px;
    height: 23px;
}

.profile-change-btn:hover{
    cursor: pointer;    
}

.nickName-change-btn{
    position: absolute;
    top: 220px;
    left: 183px;
    width: 23px;
    height: 23px;
}

.nickName-change-btn:hover{
    cursor: pointer;    
}

.nickName-text{
    position: absolute;
    top: 220px;
    left: 39px;
    width: 170px;
    text-align: center;
    border: 0px;
    text-underline-offset: inherit;
    background-color: #c2410c;    
}
.nickName-text:focus{
    outline: none;
    border-bottom: 1px solid;
}



</style>
