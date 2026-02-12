<template>
  <div class="mobile-layout">
    <router-view />
  </div>
</template>

<script setup>
</script>

<style scoped>
.mobile-layout {
  width: 100%;
  min-height: 100vh;
  max-width: 480px;
  margin: 0 auto;
  background: #f3f7ff;
  position: relative;
}

@media (min-width: 481px) {
  .mobile-layout {
    box-shadow: 0 0 20px rgba(74, 112, 255, 0.16);
  }
}
</style>
