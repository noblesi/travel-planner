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
    <!-- 비밀번호 재설정 모달 창 -->
    <div v-if="isPasswordModalOpen" class="modal-overlay" @click="closePasswordModal">
        <div class="modal-content" @click.stop>
            <h3 class="modal-title">비밀번호 재설정</h3>
            
            <div class="modal-form-group">
                <label>현재 비밀번호</label>
                <input type="password" v-model="passwordForm.currentPassword" class="modal-input" placeholder="현재 비밀번호 입력" />
            </div>
            
            <div class="modal-form-group">
                <label>새 비밀번호</label>
                <input type="password" v-model="passwordForm.newPassword" class="modal-input" placeholder="새 비밀번호 입력" />
            </div>
            
            <div class="modal-form-group">
                <label>새 비밀번호 확인</label>
                <input type="password" v-model="passwordForm.confirmPassword" class="modal-input" placeholder="새 비밀번호 다시 입력" @keyup.enter="submitPasswordChange" />
            </div>

            <div class="modal-button-group">
                <button class="modal-cancel-btn" @click="closePasswordModal">취소</button>
                <button class="modal-submit-btn" @click="submitPasswordChange">변경하기</button>
            </div>
        </div>
    </div>

    <div v-if="customAlert.isOpen" class="modal-overlay" @click="closeAlert">
        <div class="alert-content" @click.stop>
            <p class="alert-message">{{ customAlert.message }}</p>
            <button class="alert-confirm-btn" @click="closeAlert">확인</button>
        </div>
    </div>

    </DefaultLayout>
</template>

<script setup>
import { ref } from 'vue'
import DefaultLayout from '@/layouts/DefaultLayout.vue'
import PansleImg from '@/assets/myPageImage/pansle.webp'
import DefaultImg from '@/assets/myPageImage/default_profile.webp'

import { getImageUrl } from '@/utils/image'
import { getMemberInfo } from '@/api/member'
import { postModifyMemberInfo } from '@/api/member'
import { getModifyNickname } from '@/api/member'
import { postModifyProfileImage } from '@/api/member'
import { getDeleteAccount } from '@/api/member'
import { postModifyPassword } from '@/api/member'
import router from '@/router'


const customAlert = ref({
    isOpen: false,
    message: ''
})

const showAlert = (msg) => {
    customAlert.value.message = msg
    customAlert.value.isOpen = true
}

const closeAlert = () => {
    customAlert.value.isOpen = false
    customAlert.value.message = ''
}

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
    name: '',
    nickName: '',
    gender: '',
    birthDate: '',
    email: '',
    phoneNumber: '',
    imageURL: ''
})

setMember().then((response)=>{
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
    console.log(member.profileImageUrl + "/ date")
    if(member.profileImageUrl == '' || member.profileImageUrl == null){
        imageURL.value = DefaultImg
    } else {
        console.log(member.profileImageUrl)
        imageURL.value = getImageUrl(member.profileImageUrl)
        console.log(imageURL.value)
    }

    userInfo.value = {
        name: member.memberName,
        nickName: member.nickname,
        birthDate: member.birthDate.split('T')[0],
        gender: memberGender,
        email: member.email,
        phoneNumber: member.phoneNumber,
        
    }
    
}).catch((error) => {
    console.log(error + "값이 없으니 돌아간다.")
    router.push({name: 'home'})
})



const handleNicknameSubmit = () => {
    console.log(originalNickName.value + " / " + userInfo.value.nickName)
    isReadOnly.value = true;
    
    
    if (userInfo.value.nickName !== originalNickName.value) {
        changeNicnameApply();
        originalNickName.value = userInfo.value.nickName; 
    } else {
        
        userInfo.value.nickName = originalNickName.value;
    }
}

const changeNicnameApply = () => {
    console.log("보내기 전 변경할 내 닉네임 : " + userInfo.value.nickName + " / " + originalNickName.value)
    getModifyNickname(userInfo.value.nickName).then((response)=>{
        console.log("보낸 후 변경할 내 닉네임 : " + userInfo.value.nickName)
        showAlert("닉네임 변경에 성공했습니다."); 
        originalNickName.value = userInfo.value.nickName; // 성공 시 기준값 업데이트
    }).catch((error) => {
        showAlert("닉네임 변경에 실패했습니다."); 
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

////////////////////////////////////////////////////////////////
const handleFileChange = (event) => {
  const file = event.target.files[0]
  
  if (file) {
    imageURL.value = URL.createObjectURL(file)
    
    const formData = new FormData()
    formData.append('file', file)

    postModifyProfileImage(formData).then((response) => {
        showAlert("프로필 이미지가 성공적으로 변경되었습니다.");
    }).catch((error) => {
        showAlert("이미지 변경에 실패했습니다.");
        console.error(error);
    })
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
        showAlert('정보가 성공적으로 수정되었습니다.'); 
        isEditMode.value = false;
    }).catch((error) => {
        showAlert('정보 수정에 실패했습니다.'); 
        console.error(error);
    });

}


const isPasswordModalOpen = ref(false)
const passwordForm = ref({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
})


const passwordReword = () => {
    isPasswordModalOpen.value = true 
}


const closePasswordModal = () => {
    isPasswordModalOpen.value = false
    passwordForm.value = {
        currentPassword: '',
        newPassword: '',
        confirmPassword: ''
    }
}


const submitPasswordChange = () => {
    if (!passwordForm.value.currentPassword) {
        showAlert("현재 비밀번호를 입력해주세요."); 
        return;
    }
    if (!passwordForm.value.newPassword) {
        showAlert("새 비밀번호를 입력해주세요."); 
        return;
    }
    if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
        showAlert("새 비밀번호가 일치하지 않습니다. 다시 확인해주세요.");
        return;
    }

    const rewordPass = {
        currentPassword: passwordForm.value.currentPassword,
        rewordPassword: passwordForm.value.newPassword
    }

    console.log("서버로 전송할 비밀번호 데이터:", rewordPass)

    postModifyPassword(rewordPass).then((response) => {
        console.log(response.data + "성공?")
        showAlert("비밀번호가 성공적으로 변경되었습니다.");
        closePasswordModal();
    }).catch((error) => {
        console.log(error + "실패")
        showAlert("현재 비밀번호가 일치하지 않거나 오류가 발생했습니다."); 
    })
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

// 모달 스타일
.modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background-color: rgba(0, 0, 0, 0.6);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 9999;
    backdrop-filter: blur(2px);
}

/* 모달 본체 */
.modal-content {
    background-color: #ffffff;
    padding: 2.5rem;
    border-radius: 16px;
    width: 420px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.25);
    color: #333;
}

.modal-title {
    margin-top: 0;
    margin-bottom: 2rem;
    font-size: 1.4rem;
    color: #2b2b2b;
    text-align: center;
    font-weight: 700;
}

.modal-form-group {
    margin-bottom: 1.2rem;
    display: flex;
    flex-direction: column;
}

.modal-form-group label {
    font-size: 0.9rem;
    margin-bottom: 0.5rem;
    color: #666;
    font-weight: 600;
}

.modal-input {
    padding: 0.85rem;
    border: 1px solid #ddd;
    border-radius: 8px;
    font-size: 1rem;
    outline: none;
    transition: border-color 0.2s;
    background-color: #f9f9f9;
}

.modal-input:focus {
    border-color: #f47a42; /* 기존 UI 주황색 계열 대비 */
    background-color: #ffffff;
    box-shadow: 0 0 0 3px rgba(244, 122, 66, 0.15);
}

.modal-button-group {
    display: flex;
    justify-content: space-between;
    margin-top: 2.5rem;
    gap: 1rem;
}

.modal-cancel-btn {
    flex: 1;
    padding: 0.85rem;
    background-color: #f1f1f1;
    color: #555;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 600;
    transition: background-color 0.2s;
}
.modal-cancel-btn:hover {
    background-color: #e4e4e4;
}

.modal-submit-btn {
    flex: 1;
    padding: 0.85rem;
    background-color: rgba(247, 145, 62, 0.945); /* 기존 프로젝트 컬러 */
    color: #fff;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 600;
    transition: background-color 0.2s;
}
.modal-submit-btn:hover {
    background-color: #cd7652;
}

/* =========================================
   커스텀 Alert 모달 스타일 추가
   ========================================= */

.alert-content {
    background-color: #ffffff;
    padding: 2.5rem 2rem;
    border-radius: 16px;
    width: 320px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.25);
    text-align: center;
    color: #333;
    animation: fadeIn 0.2s ease-out; /* 부드럽게 나타나는 애니메이션 */
}

.alert-message {
    font-size: 1.1rem;
    margin-top: 0;
    margin-bottom: 2rem;
    line-height: 1.5;
    word-break: keep-all; /* 단어 단위로 줄바꿈 되도록 설정 */
    font-weight: 500;
}

.alert-confirm-btn {
    padding: 0.8rem 2.5rem;
    background-color: rgba(247, 145, 62, 0.945); /* 기존 테마 컬러 */
    color: #fff;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 1rem;
    font-weight: 700;
    transition: background-color 0.2s;
    width: 100%; /* 버튼을 꽉 차게 */
}

.alert-confirm-btn:hover {
    background-color: #cd7652;
}

/* 팝업 애니메이션 */
@keyframes fadeIn {
    from {
        opacity: 0;
        transform: translateY(-10px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

</style>