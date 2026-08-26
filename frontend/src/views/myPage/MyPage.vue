<script setup>
import { ref } from 'vue'

import ChangePasswordModal from '@/components/member/ChangePasswordModal.vue'
import MemberProfileDetails from '@/components/member/MemberProfileDetails.vue'
import MemberProfileSummary from '@/components/member/MemberProfileSummary.vue'
import WithdrawAccountModal from '@/components/member/WithdrawAccountModal.vue'
import { useMemberProfile } from '@/composables/useMemberProfile'
import DefaultLayout from '@/layouts/DefaultLayout.vue'

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
        <MemberProfileSummary :profile="profile" @updated="updateLoadedProfile" />
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
}

@media (max-width: 760px) {
  .profile-page {
    padding-top: 38px;
  }

  .profile-content {
    grid-template-columns: 1fr;
  }
}
</style>
