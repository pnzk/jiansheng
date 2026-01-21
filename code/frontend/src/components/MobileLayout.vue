<template>
  <div class="mobile-container">
    <div class="mobile-frame">
      <div class="mobile-notch"></div>
      <div class="mobile-screen">
        <slot></slot>
      </div>
      <div class="mobile-home-indicator"></div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.mobile-container {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 20px;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.mobile-frame {
  width: 375px;
  min-height: 812px;
  background: #1a1a1a;
  border-radius: 40px;
  padding: 12px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
  position: relative;
}

.mobile-notch {
  width: 150px;
  height: 30px;
  background: #1a1a1a;
  border-radius: 0 0 20px 20px;
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
}

.mobile-screen {
  width: 100%;
  height: 788px;
  background: #f5f5f5;
  border-radius: 30px;
  overflow-y: auto;
  overflow-x: hidden;
}

.mobile-screen::-webkit-scrollbar {
  width: 0;
}

.mobile-home-indicator {
  width: 134px;
  height: 5px;
  background: #fff;
  border-radius: 3px;
  position: absolute;
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
}
</style>
