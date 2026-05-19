<template>
  <component :is="dashboardComponent" />
</template>

<script setup>
import { computed, defineAsyncComponent, onBeforeUnmount, onMounted, ref } from 'vue'

const MOBILE_BREAKPOINT = 768
const isMobile = ref(false)

const DesktopDashboard = defineAsyncComponent(() => import('@/views/coach/Dashboard.vue'))
const MobileDashboard = defineAsyncComponent(() => import('@/views/coach/MobileDashboard.vue'))

const syncViewport = () => {
  if (typeof window === 'undefined') return
  isMobile.value = window.innerWidth <= MOBILE_BREAKPOINT
}

const dashboardComponent = computed(() => (isMobile.value ? MobileDashboard : DesktopDashboard))

onMounted(() => {
  syncViewport()
  window.addEventListener('resize', syncViewport)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncViewport)
})
</script>
