<script setup>

</script>
<template>
    <!-- 화면 전체를 채우는 배경 -->
    <div class="login-container" id="loginWrap">
        <!-- 실제 로그인 양식이 들어갈 박스 -->
        <div class="login-box">
            <h1>로그인</h1>
            
            <!-- 테이블 대신 깔끔한 div 레이아웃 추천 -->
            <div class="login-btn-wrap">
                <div role="button" class="divButton">
                    
                    <strong>Google</strong>
                </div>
            </div>
        </div>
    </div>
</template>


<style lang="scss" scoped>
.login-container {
    position: relative;
    background-color: rgb(240, 158, 64);
    height: 100vh;
    width: 100%;
    text-align: center;
    backdrop-filter: blur(10px);
    display: flex;
    align-items: center;
    justify-content: center; /* 가로 가운데 */
}
.login-box {
    position: absolute;
    background-color: #ffffff; // 박스는 하얀색으로 처리하여 가독성 높임
    width: 400px;
    height: 550px;
    padding: 40px;
    border-radius: 12px;       // 모서리를 부드럽게
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15); // 은은한 그림자 효과
    text-align: center;

    h1 {
        margin-bottom: 20px;
        color: #333333;
    }
}
</style>