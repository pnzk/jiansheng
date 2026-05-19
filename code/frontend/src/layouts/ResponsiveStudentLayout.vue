<template>
  <component :is="layoutComponent" />
</template>

<script setup>
import { computed, defineAsyncComponent, onBeforeUnmount, onMounted, ref } from 'vue'

const MOBILE_BREAKPOINT = 768
const isMobile = ref(false)

const DesktopLayout = defineAsyncComponent(() => import('@/layouts/StudentLayout.vue'))
const MobileLayout = defineAsyncComponent(() => import('@/layouts/MobileStudentLayout.vue'))

const syncViewport = () => {
  if (typeof window === 'undefined') return
  isMobile.value = window.innerWidth <= MOBILE_BREAKPOINT
}

const layoutComponent = computed(() => (isMobile.value ? MobileLayout : DesktopLayout))

onMounted(() => {
  syncViewport()
  window.addEventListener('resize', syncViewport)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncViewport)
})
</script>
