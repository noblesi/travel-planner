<template>
    <DefaultLayout>
    <div name="myPageContainer" class="myPage-container">
        <div name="profileWrapDiv" class="profile-wrap">
            <div name="profileImageDiv" class="profile-div">
                <img :src="imageURL" class="img-size"/>
                <img :src="PansleImg" @click="changeProfileImage" alt="프로필 이미지 변경" class="profile-change-btn"/>
                <input type="file" ref="fileInput" style="display: none;" accept="image/*" @change="handleFileChange" />
                <input type="text" ref="nickName" :readonly="isReadOnly" @blur="lockInput" class="nickName-text" value="nickName"/>
                <img :src="PansleImg" @click="changeNickName" class="nickName-change-btn"/>
                <a href="#void" class="member-draw-btn">회원 탈퇴</a>
            </div>
            <div name="myInfoWrap" class="myInfo-wrap">
                <Strong class="title-text">나의 정보</Strong>
                <div class="myInfo-div">

                </div>
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

const isReadOnly = ref(true)
const nickName = ref('')
const fileInput = ref('')
const imageURL = ref('')

if(imageURL.value == ''){
    imageURL.value = DefaultImg
}

const lockInput = () => {
    isReadOnly.value=true
}

const changeNickName = () => {
    isReadOnly.value = false
    nickName.value.focus()
}

const changeProfileImage = () => {
    fileInput.value.click()
}

const handleFileChange = (event) => {
  const file = event.target.files[0]
  
  if (file) {
    // 선택한 이미지 파일을 화면에 미리보기 위해 URL을 생성
    imageURL.value = URL.createObjectURL(file)
    
    // 백엔드 서버로 전송할 때는 이 'file' 객체를 FormData에 담아 보낸다.
    console.log('선택된 파일 객체:', file)
  }
}

</script>

<style lang="scss" scoped>
.title-text{
    font-size: 30px;
    color: #545555;
    border-bottom: 3px solid #545555;
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

    background-color: #63a6a1;
}

.myInfo-div{
    position: absolute;
    top: 75px;
    left: 50px;
    width: 800px;
    height: 500px;
    border-radius: 50px;

    background-color: #e3e7e552;
}

.member-draw-btn{
    color: #7c7272;
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
    background-color: #63a6a1;
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
    background-color: #63a6a1;    
}
.nickName-text:focus{
    outline: none;
    border-bottom: 1px solid;
}



</style>
