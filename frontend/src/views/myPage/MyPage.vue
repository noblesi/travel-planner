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
                <a class="member-draw-btn" @click="drawMember">회원 탈퇴</a>
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
import PansleImg from '@/assets/myPageImage/pansle.webp'
import DefaultImg from '@/assets/myPageImage/default_profile.webp'

import { getImageUrl } from '@/utils/image'
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

const isPasswordModalOpen = ref(false)
const isWithdrawalModalOpen = ref(false)
const { profile, status, errorMessage, loadProfile, updateLoadedProfile } = useMemberProfile()
</script>

<template>
  <DefaultLayout>
    <main class="profile-page" aria-labelledby="profile-title">
      <header class="profile-heading">
        <p>MY PAGE</p>
        <h1 id="profile-title">마이페이지</h1>
        <span>가입한 회원정보를 확인하고 계정을 관리할 수 있습니다.</span>
      </header>

      <section v-if="status === 'loading'" class="profile-state" role="status" aria-live="polite">
        <span class="loading-indicator" aria-hidden="true" />
        회원 정보를 불러오고 있습니다.
      </section>

      <section v-else-if="status === 'error'" class="profile-state profile-state--error" role="alert">
        <strong>회원정보를 표시할 수 없습니다.</strong>
        <p>{{ errorMessage }}</p>
        <button type="button" @click="loadProfile">다시 시도</button>
      </section>

      <section v-else class="profile-content" aria-label="내 회원정보">
        <MemberProfileSummary :profile="profile" />
        <MemberProfileDetails
          :profile="profile"
          @updated="updateLoadedProfile"
          @open-password="isPasswordModalOpen = true"
          @open-withdrawal="isWithdrawalModalOpen = true"
        />
      </section>
    </main>

    <ChangePasswordModal :open="isPasswordModalOpen" @close="isPasswordModalOpen = false" />
    <WithdrawAccountModal :open="isWithdrawalModalOpen" @close="isWithdrawalModalOpen = false" />
  </DefaultLayout>
</template>

<style scoped>
.profile-page {
  min-height: calc(100vh - var(--layout-header-height));
  padding: 56px max(var(--layout-gutter), calc((100% - var(--layout-content-width)) / 2)) 88px;
  background:
    radial-gradient(circle at 90% 5%, rgb(249 115 22 / 10%), transparent 28rem),
    var(--color-page);
}

.profile-heading > p {
  margin: 0;
  color: var(--color-brand-accent);
  font-size: 11px;
  font-weight: 850;
  letter-spacing: 0.14em;
}

.profile-heading h1 {
  margin: 8px 0 0;
  color: var(--color-text);
  font-size: clamp(32px, 4vw, 42px);
  letter-spacing: -0.04em;
}

.profile-heading > span {
  display: block;
  margin-top: 10px;
  color: var(--color-text-muted);
}

.profile-content {
  display: grid;
  grid-template-columns: minmax(240px, 300px) minmax(0, 1fr);
  gap: 24px;
  margin-top: 34px;
}

.profile-state {
  display: flex;
  min-height: 300px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 34px;
  padding: 40px;
  border: 1px solid var(--color-border);
  border-radius: 20px;
  background: var(--color-surface);
  box-shadow: 0 16px 45px rgb(15 23 42 / 6%);
  color: var(--color-text-muted);
  text-align: center;
}

.profile-state p {
  margin: 0;
}

.profile-state--error strong {
  color: var(--color-danger);
}

.profile-state button {
  display: inline-flex;
  min-height: 40px;
  align-items: center;
  justify-content: center;
  padding: 0 15px;
  border: 1px solid var(--color-brand-border);
  border-radius: 10px;
  background: var(--color-brand-soft);
  color: var(--color-brand);
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
}

.loading-indicator {
  width: 28px;
  height: 28px;
  border: 3px solid var(--color-brand-border);
  border-top-color: var(--color-brand);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
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

.member-draw-btn:hover{
    cursor: pointer;
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

@media (max-width: 760px) {
  .profile-page {
    padding-top: 38px;
  }

  .profile-content {
    grid-template-columns: 1fr;
  }
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

.confirm-button-group {
    display: flex;
    justify-content: space-between;
    gap: 0.8rem;
    margin-top: 1.5rem;
}

.confirm-button-group .modal-cancel-btn,
.confirm-button-group .alert-confirm-btn {
    flex: 1;
    margin: 0;
}

</style>
