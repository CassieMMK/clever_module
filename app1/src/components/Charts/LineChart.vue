<template>
  <div class="div_any_child">
    <div class="div_any_title">
      <img :src="require(`@/assets/img/title_${iconMap[icon]}.png`)" :alt="title">
      <span>{{ title }}</span>
    </div>
    <div :id="chartId" class="p_chart" ref="chartContainer"></div>
  </div>
</template>

<script>
import { defineComponent, ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts'

export default defineComponent({
  name: 'EChartContainer',
  props: {
    chartId: {
      type: String,
      required: true
    },
    option: {
      type: Object,
      required: true
    },
    title: {
      type: String,
      required: true
    },
    icon: {
      type: String,
      required: true,
      validator: (value) => ['project1', 'project2', 'project4', 'project5'].includes(value)
    }
  },
  setup(props) {
    const chartContainer = ref(null)
    let chartInstance = null

    const iconMap = {
      project1: 1,
      project2: 2,
      project4: 4,
      project5: 5,
    }

    const getImageUrl = (name) => {
      return new URL(`../assets/img/${name}`, import.meta.url).href
    }

    const initChart = () => {
      if (chartInstance) {
        chartInstance.dispose()
      }
      chartInstance = echarts.init(chartContainer.value)
      chartInstance.setOption(props.option)
      
      // Handle window resize
      const resizeHandler = () => {
        chartInstance?.resize()
      }
      window.addEventListener('resize', resizeHandler)
      
      // Cleanup function
      return () => {
        window.removeEventListener('resize', resizeHandler)
        chartInstance?.dispose()
      }
    }

    onMounted(() => {
      initChart()
    })

    watch(() => props.option, (newOption) => {
      if (chartInstance) {
        chartInstance.setOption(newOption)
      }
    }, { deep: true })

    return {
      iconMap,
      chartContainer,
      getImageUrl
    }
  }
})
</script>

<style lang="scss" scoped>




</style>