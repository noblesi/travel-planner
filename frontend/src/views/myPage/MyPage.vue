<template>
    <DefaultLayout>
    <div name="myPageContainer" class="myPage-container">
        <div name="profileWrapDiv" class="profile-wrap">
            <div name="profileImageDiv" class="profile-div">
                <img src="" class="img-size"/><br>
                <input type="text" class="nickName-text" value="nickName"/>
            </div>
            <div name="myInfoDiv" class="myInfo-div">

            </div>
        </div>
    </div>
    </DefaultLayout>
</template>

<script setup>
import DefaultLayout from '@/layouts/DefaultLayout.vue'

</script>

<style lang="scss" scoped>


.myPage-container{
    // position: relative;
    background-color: #804c77;
    width: 100%;
    min-height: 80vh;
}

.img-size{
    position: absolute;
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
    background-color: #4c692b;
}

.nickName-text{
    position: absolute;
    top: 220px;
    left: 38px;
    width: 170px;
    text-align: center;
    border: 0px;
    
}

.myInfo-div{
    position: absolute;
    top: 130px;
    left : 700px;
    width: 900px;
    height: 600px;

    background-color: #5af1f7;
}

</style>