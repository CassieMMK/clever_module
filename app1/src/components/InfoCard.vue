<template>
  <div class="info-card" :class="a.color">
    <img :src="require(`@/assets/img/info_${iconMap[a.icon]}.png`)" class="icon">
    <div class="content">
      <div class="title">{{ a.title }}</div>
      <div class="value">{{ a.value }}</div>
    </div>
    <img :src="require(`@/assets/img/info_${iconMap[b.icon]}.png`)" class="icon">
    <div class="content">
      <div class="title">{{ b.title }}</div>
      <div class="value">{{ b.value }}</div>
    </div>
  </div>
</template>

<script>
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'InfoCard',
  props: {
    a: {
      type: Object,
      required: true
    },
    b: {
      type: Object,
      required: true
    }
  },
  setup() {
    const iconMap = {
      project1: 1,
      project2: 2,
      project4: 4,
      project5: 5,
      project6: 6,
      project7: 7,
    }

    const getImageUrl = (name) => {
      return new URL(`../assets/img/${name}`, import.meta.url).href
    }

    return {
      iconMap,
      getImageUrl
    }
  }
})
</script>

<style lang="scss" scoped>
.info-card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #034c6a;
  
  &.red .value { color: #ff4e4e ; }
  &.sky .value { color: #25f3e6 ; }
  &.yellow .value { color: #ffff43; }

  .icon {
    width: 50px;
    margin-right: 15px;
  }

  .title {
    font-size: 13px;
    color: #ffffff;
  }

  .value {
    font-size: 24px;
    font-weight: bold;
    margin-top: 10px;
  }
}
</style>