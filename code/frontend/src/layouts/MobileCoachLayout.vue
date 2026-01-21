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
  background: #f5f5f5;
  position: relative;
}

@media (min-width: 481px) {
  .mobile-layout {
    box-shadow: 0 0 20px rgba(0, 0, 0, 0.1);
  }
}
</style>
