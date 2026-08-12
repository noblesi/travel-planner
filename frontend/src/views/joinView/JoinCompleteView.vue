<script setup>
import { RouterLink } from 'vue-router'

</script>

<template>
  <main class="join-complete">
    <section class="completion-card" aria-labelledby="completion-title">
      <div class="completion-icon" aria-hidden="true">✓</div>
      <p class="eyebrow">WELCOME TO WITHTRIP</p>
      <h1 id="completion-title">회원가입이 완료되었습니다.</h1>
      <p class="description">로그인하고 WithTrip에서 새로운 여행 계획을 시작해 보세요.</p>

      <div class="actions">
        <RouterLink class="primary-action" to="/loginView">로그인하기</RouterLink>
        <RouterLink class="secondary-action" to="/">홈으로</RouterLink>
      </div>
    </section>
  </main>
</template>

<style scoped>
.join-complete {
  display: grid;
  min-height: 100vh;
  padding: 32px 20px;
  place-items: center;
  background: var(--color-page);
}

.completion-card {
  width: min(460px, 100%);
  padding: 48px 40px;
  border: 1px solid var(--color-brand-border);
  border-radius: 24px;
  background: var(--color-surface);
  box-shadow: 0 24px 70px rgb(15 23 42 / 10%);
  text-align: center;
}

.completion-icon {
  display: grid;
  width: 64px;
  height: 64px;
  margin: 0 auto 24px;
  border-radius: 50%;
  place-items: center;
  background: var(--color-brand-soft);
  color: var(--color-brand);
  font-size: 30px;
  font-weight: 800;
}

.eyebrow {
  margin: 0 0 12px;
  color: var(--color-brand);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

h1 {
  margin: 0;
  color: var(--color-text);
  font-size: clamp(28px, 6vw, 36px);
  line-height: 1.3;
  letter-spacing: -0.035em;
}

.description {
  margin: 20px 0 0;
  color: var(--color-text-muted);
  line-height: 1.7;
}

.actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 32px;
}

.primary-action,
.secondary-action {
  display: inline-flex;
  min-height: 48px;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  font-weight: 700;
}

.primary-action {
  border: 1px solid var(--color-brand);
  background: var(--color-brand);
  color: var(--color-brand-on);
}

.primary-action:hover {
  border-color: var(--color-brand-hover);
  background: var(--color-brand-hover);
}

.primary-action:focus-visible,
.secondary-action:focus-visible {
  outline: 3px solid var(--color-brand-focus);
  outline-offset: 3px;
}

.secondary-action {
  border: 1px solid var(--color-brand-border);
  background: var(--color-surface);
  color: var(--color-brand);
}

@media (max-width: 480px) {
  .completion-card {
    padding: 40px 24px;
  }

  .actions {
    grid-template-columns: 1fr;
  }
}
</style>
