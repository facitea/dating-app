<template>
  <section class="simple-page">
    <div class="simple-shell">
      <div class="simple-card">
        <p class="eyebrow">Home</p>
        <h1>메인 홈</h1>
        <p class="description">
          오늘의 인연, 채팅, 프로필로 이동하기 전에 홈에서 앱 전체 흐름과 심사 상태를 확인할 수 있습니다.
        </p>
      </div>

      <div class="simple-card support-card">
        <p class="eyebrow">Realtime Support</p>
        <h2>실시간 문의 도우미</h2>
        <p class="description">
          오른쪽 아래 문의 아이콘을 눌러 자주 묻는 질문을 바로 확인해 보세요. 아이콘은 드래그해서 원하는 위치로 옮길 수 있고,
          개인 문의가 필요한 경우 고객센터로 바로 연결됩니다.
        </p>
      </div>
    </div>


  </section>
</template>

<script setup>

</script>

<style scoped>
.simple-page {
  min-height: 100vh;
  padding: max(16px, env(safe-area-inset-top)) 16px 24px;
  display: flex;
  justify-content: center;
}

.simple-shell {
  width: min(100%, 520px);
  margin: auto 0;
  display: grid;
  gap: 16px;
}

.simple-card {
  padding: 32px 22px;
  border-radius: 32px;
  background: rgba(255, 252, 249, 0.9);
  border: 1px solid rgba(239, 208, 193, 0.78);
  box-shadow: 0 24px 48px rgba(109, 57, 41, 0.08);
  text-align: center;
}

.support-card {
  text-align: left;
}

.eyebrow {
  margin: 0 0 10px;
  color: #af5f42;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

h1,
h2 {
  margin: 0;
  color: #31211d;
}

h1 {
  font-size: clamp(2rem, 7vw, 2.5rem);
}

h2 {
  font-size: clamp(1.5rem, 5vw, 1.9rem);
}

.description {
  margin: 14px 0 0;
  color: #6f564d;
  line-height: 1.7;
}
</style>
