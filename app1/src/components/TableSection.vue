<template>
  <div class="left div_table_box">
    <div class="div_any_child">
      <div class="div_any_title">
        <img :src="require(`@/assets/img/title_${index+1}.png`)" alt="table icon">
        <span>{{ title }}</span>
      </div>
      <div class="table_p">
        <table>
          <thead>
            <tr>
              <th v-for="(column, index) in columns" :key="index">{{ column.label }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in data" :key="index">
              <td>{{ item.rank }}</td>
              <td>{{ item.unit }}</td>
              <td>{{ item.count }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'DataTable',
  props: {
    title: {
      type: String,
      required: true
    },
    columns: {
      type: Array,
      required: true,
      validator: (value) => {
        return value.every(col => 'label' in col && 'prop' in col)
      }
    },
    data: {
      type: Array,
      required: true,
      validator: (value) => {
        return value.every(item => 'rank' in item && 'unit' in item && 'count' in item)
      }
    },
    index:{
      type:Number,
      required: true,
    }
  },
  setup() {
    const getImageUrl = (name) => {
      return new URL(`../assets/img/${name}`, import.meta.url).href
    }

    return {
      getImageUrl
    }
  }
})
</script>

<style lang="scss" scoped>




</style>