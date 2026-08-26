<template>
    <DefaultLayout>
    <div name="myPageContainer" class="myPage-container">
        <div name="profileWrapDiv" class="profile-wrap">
            <div name="profileImageDiv" class="profile-div">
                <!-- 왼쪽 프로필 카드리뉴얼 -->
            <div name="profileImageDiv" class="profile-div">
                <div class="profile-img-container">
                    <img :src="imageURL" class="img-size"/>
                    <img :src="PansleImg" @click="changeProfileImage" alt="프로필 이미지 변경" class="profile-change-btn"/>
                    <input type="file" ref="fileInput" style="display: none;" accept="image/*" @change="handleFileChange" />
                </div>

                <div class="nickname-container">
                    <input type="text" ref='nickNameInput'
                        v-model="userInfo.nickName" 
                        :readonly="isReadOnly" 
                        @blur="handleNicknameSubmit" 
                        @change="handleNicknameSubmit"
                        @keyup.enter="$event.target.blur()"
                        class="nickName-text"/>
                    <img :src="PansleImg" @click="changeNickName" class="nickName-change-btn" alt="닉네임 변경"/>
                </div>

                <a class="member-draw-btn" @click="drawMember">회원 탈퇴</a>
            </div>
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
                        <div v-if="!isEditMode" class="value-text">{{ userInfo.genderCode }}</div>
                        <select v-else v-model="userInfo.genderCode" class="info-input select-box">
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

    <div v-if="customConfirm.isOpen" class="modal-overlay" @click="closeConfirm">
        <div class="alert-content" @click.stop>
            <p class="alert-message">{{ customConfirm.message }}</p>
            <div class="confirm-button-group">
                <button class="modal-cancel-btn" @click="closeConfirm">취소</button>
                <button class="alert-confirm-btn" @click="handleConfirm">확인</button>
            </div>
        </div>
    </div>

    </DefaultLayout>
</template>

<script setup>
import { ref } from 'vue'

import DefaultLayout from '@/layouts/DefaultLayout.vue'
import PansleImg from '@/assets/myPageImage/pencil.webp'
import DefaultImg from '@/assets/myPageImage/default_profile.webp'

import { getMemberInfo } from '@/api/member'
import { postModifyMemberInfo } from '@/api/member'
import { getModifyNickname } from '@/api/member'
import { postModifyProfileImage } from '@/api/member'
import { getDeleteAccount } from '@/api/member'
import { postModifyPassword } from '@/api/member'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const authStore = useAuthStore()

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
    genderCode: '',
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
        imageURL.value = member.profileImageUrl
        console.log(imageURL.value)
    }

    userInfo.value = {
        name: member.memberName,
        nickName: member.nickname,
        birthDate: member.birthDate.split('T')[0],
        genderCode: memberGender,
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


const drawMember = () => {
    showConfirm("회원 탈퇴를 진행합니다. 정말 탈퇴 하시겠습니까?", () => {
        getDeleteAccount().then((response) => {
            showAlert("성공적으로 회원탈퇴가 완료 되었습니다.")
            authStore.logout()
            router.push({ name: 'home' })
        }).catch((error) => {
            showAlert("회원 탈퇴 중 문제가 발생하였습니다. 조금 뒤 다시 시도 해주세요.")
        })
    })
}

const customConfirm = ref({
    isOpen: false,
    message: '',
    onConfirm: null
})

const showConfirm = (msg, onConfirmCallback) => {
    customConfirm.value.message = msg
    customConfirm.value.onConfirm = onConfirmCallback
    customConfirm.value.isOpen = true
}

const closeConfirm = () => {
    customConfirm.value.isOpen = false
    customConfirm.value.message = ''
    customConfirm.value.onConfirm = null
}

const handleConfirm = () => {
    if (typeof customConfirm.value.onConfirm === 'function') {
        customConfirm.value.onConfirm()
    }
    closeConfirm()
}

// const isWithdrawalModalOpen = ref(false)
// const { profile, status, errorMessage, loadProfile, updateLoadedProfile } = useMemberProfile()
</script>

<style scoped>
/* 페이지 메인 레이아웃 */
.myPage-container {
    width: 100%;
    min-height: calc(100vh - 120px);
    padding: 40px 20px 80px;
    background-color: #fdf8f5;
    box-sizing: border-box;
}

.profile-wrap {
    display: flex;
    justify-content: center;
    align-items: flex-start;
    gap: 30px;
    max-width: 1100px;
    margin: 0 auto;
}

/* 프로필 카드 영역 (좌측) */
.profile-div {
    width: 280px;
    background-color: #c2410c;
    border-radius: 20px;
    padding: 35px 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    box-shadow: 0 10px 25px rgba(194, 65, 12, 0.2);
    box-sizing: border-box;
}

.profile-img-container {
    position: relative;
    width: 150px;
    height: 150px;
    margin-bottom: 20px;
}

.img-size {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    object-fit: cover;
    border: 3px solid #ffffff;
}

.profile-change-btn {
    position: absolute;
    bottom: 5px;
    right: 5px;
    width: 32px;
    height: 32px;
    background-color: rgba(0, 0, 0, 0.6);
    padding: 6px;
    border-radius: 50%;
    cursor: pointer;
    box-sizing: border-box;
    transition: transform 0.2s;
}

.profile-change-btn:hover {
    transform: scale(1.1);
}

.nickname-container {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    margin-bottom: 30px;
    width: 100%;
}

.nickName-text {
    width: 150px;
    background: transparent;
    border: none;
    color: #ffffff;
    font-size: 18px;
    font-weight: 700;
    text-align: center;
    outline: none;
    padding: 4px 0;
}

.nickName-text:not([readonly]) {
    border-bottom: 2px solid #ffffff;
}

.nickName-change-btn {
    width: 20px;
    height: 20px;
    cursor: pointer;
    filter: brightness(0) invert(1);
}

.member-draw-btn {
    color: #fca5a5;
    font-size: 13px;
    text-decoration: underline;
    cursor: pointer;
    margin-top: 10px;
}

.member-draw-btn:hover {
    color: #ffffff;
}

/* 정보 표시 영역 (우측) */
.myInfo-wrap {
    flex: 1;
    max-width: 750px;
    background-color: #ffffff;
    border-radius: 20px;
    padding: 35px 40px;
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.05);
    border: 1px solid #f1f5f9;
    box-sizing: border-box;
}

.title-section {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 20px;
    border-bottom: 2px solid #f1f5f9;
    margin-bottom: 25px;
}

.title {
    font-size: 22px;
    font-weight: 700;
    color: #1f2937;
    margin: 0;
}

.title-btn-group {
    display: flex;
    gap: 10px;
}

.password-reword-btn {
    padding: 8px 16px;
    background-color: #f3f4f6;
    color: #4b5563;
    border: 1px solid #d1d5db;
    border-radius: 8px;
    font-weight: 600;
    font-size: 13px;
    cursor: pointer;
    transition: background-color 0.2s;
}

.password-reword-btn:hover {
    background-color: #e5e7eb;
}

.action-btn {
    padding: 8px 18px;
    background-color: #ea580c;
    color: #ffffff;
    border: none;
    border-radius: 8px;
    font-weight: 600;
    font-size: 13px;
    cursor: pointer;
    transition: background-color 0.2s;
}

.action-btn:hover {
    background-color: #c2410c;
}

/* 폼 필드 스타일 */
.info-content {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.form-group {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.form-group label {
    font-size: 13px;
    font-weight: 600;
    color: #6b7280;
}

.grid-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20px;
}

.value-text {
    font-size: 15px;
    font-weight: 500;
    color: #111827;
    padding: 11px 14px;
    background-color: #f9fafb;
    border-radius: 8px;
    border: 1px solid #f3f4f6;
    min-height: 44px;
    display: flex;
    align-items: center;
    box-sizing: border-box;
}

.info-input {
    height: 44px;
    padding: 0 14px;
    border: 1px solid #d1d5db;
    border-radius: 8px;
    font-size: 14px;
    outline: none;
    transition: border-color 0.2s, box-shadow 0.2s;
    box-sizing: border-box;
}

.info-input:focus {
    border-color: #ea580c;
    box-shadow: 0 0 0 3px rgba(234, 88, 12, 0.15);
}

.select-box {
    background-color: #ffffff;
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
}

.button-section {
    margin-top: 30px;
    display: flex;
    justify-content: flex-end;
}

.save-btn {
    padding: 12px 28px;
    background-color: #ea580c;
    color: #ffffff;
    border: none;
    border-radius: 8px;
    font-size: 15px;
    font-weight: 700;
    cursor: pointer;
    transition: background-color 0.2s;
}

.save-btn:hover {
    background-color: #c2410c;
}

/* 모달 레이아웃 공통 */
.modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 9999;
    backdrop-filter: blur(3px);
}

.modal-content {
    background-color: #ffffff;
    padding: 2.5rem;
    border-radius: 16px;
    width: 400px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.modal-title {
    margin-top: 0;
    margin-bottom: 1.8rem;
    font-size: 1.3rem;
    color: #111827;
    text-align: center;
    font-weight: 700;
}

.modal-form-group {
    margin-bottom: 1.2rem;
    display: flex;
    flex-direction: column;
}

.modal-form-group label {
    font-size: 0.85rem;
    margin-bottom: 0.5rem;
    color: #4b5563;
    font-weight: 600;
}

.modal-input {
    padding: 0.8rem;
    border: 1px solid #d1d5db;
    border-radius: 8px;
    font-size: 0.95rem;
    outline: none;
}

.modal-input:focus {
    border-color: #ea580c;
    box-shadow: 0 0 0 3px rgba(234, 88, 12, 0.15);
}

.modal-button-group {
    display: flex;
    justify-content: space-between;
    margin-top: 2rem;
    gap: 0.8rem;
}

.modal-cancel-btn {
    flex: 1;
    padding: 0.8rem;
    background-color: #f3f4f6;
    color: #4b5563;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 0.95rem;
    font-weight: 600;
}

.modal-submit-btn {
    flex: 1;
    padding: 0.8rem;
    background-color: #ea580c;
    color: #ffffff;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 0.95rem;
    font-weight: 600;
}

/* Alert & Confirm 모달 */
.alert-content {
    background-color: #ffffff;
    padding: 2rem 1.8rem;
    border-radius: 16px;
    width: 320px;
    text-align: center;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.alert-message {
    font-size: 1rem;
    margin-top: 0;
    margin-bottom: 1.5rem;
    color: #1f2937;
    font-weight: 500;
    line-height: 1.4;
}

.alert-confirm-btn {
    padding: 0.8rem;
    background-color: #ea580c;
    color: #ffffff;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 0.95rem;
    font-weight: 700;
    width: 100%;
}

.confirm-button-group {
    display: flex;
    gap: 0.8rem;
}

/* 반응형 모바일 대응 */
@media (max-width: 850px) {
    .profile-wrap {
        flex-direction: column;
        align-items: center;
    }
    .profile-div, .myInfo-wrap {
        width: 100%;
        max-width: 100%;
    }
    .grid-row {
        grid-template-columns: 1fr;
    }
}

</style>