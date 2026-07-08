<template>
  <!-- 主容器 -->
  <div class="home-container">
    <!-- 数据概览标题部分 -->
    <div class="select_time">
      <div class="static_top left">
        <i></i><span>总体概况</span><!-- 图标和标题 -->
      </div>
    </div>

    <!-- 信息卡片区域 -->
    <div class="overview-section">
      <!-- 循环渲染信息卡片组件 -->
      <!-- 分别为：唯一key -->
      <!-- 传递卡片a部分数据 -->
      <!-- 传递卡片b部分数据 -->
      <info-card
          v-for="(card, index) in infoCards"
          :key="index"
          :a="card.a"
          :b="card.b"
      />
    </div>

    <!-- 图表展示区域 -->
    <!-- 统计分析图 -->
    <div class="chart-section">
      <div class="chart-group left-group">

        <line-chart
            title="历年省课题统计"
            chart-id="chart1"
            :option="projectOption"
            icon="project1"
        />
        <line-chart
            title="历年省社科奖统计"
            chart-id="chart2"
            :option="awardOption"
            icon="project2"
        />
      </div>

      <!-- 中间词云分析区域 -->
      <div class="div_any_child div_height">
        <div class="div_any_title any_title_width"><img src="../assets/img/title_3.png">省课题词云分析 </div>
        <div id="wordcloud-container" class="p_chart" style="width: 100%; height: 100%;"></div>
      </div>

      <!-- 右侧图表组 -->
      <div class="chart-group right-group">
        <!-- 历年国家社科奖统计图表 -->
        <line-chart
          title="历年国家社科奖统计"
          chart-id="chart3"
          :option="nationalAwardOption"
          icon="project4"
        />
        
        <!-- 历年发表社科论文统计图表 -->
        <line-chart
          title="历年发表社科论文统计"
          chart-id="chart4"
          :option="option"
          icon="project5"
        />
      </div> <!-- 修正：闭合右侧图表组 -->
    </div> <!-- 修正：闭合图表展示区域 -->

    <!-- 分析表格 -->
    <div class="table-section">
      <table-section
          v-for="(table, index) in topLists"
          :key="index"
          :index="index"
          :title="table.title"
          :columns="table.columns"
          :data="table.data"
      />
    </div>
  </div> <!-- 修正：闭合主容器 -->
</template>

<script>
import { defineComponent, ref, onMounted ,computed, watch, onUnmounted} from 'vue'
import axios from 'axios'
import InfoCard from '@/components/InfoCard.vue'// 导入信息卡片组件
import LineChart from '@/components/Charts/LineChart.vue'
import TableSection from '@/components/TableSection.vue'
import * as echarts from 'echarts'; // 引入 echarts
import 'echarts-wordcloud'; // 引入 echarts-wordcloud 扩展

/***
 定义了一个名为 HomeD 的 Vue 组件，并注册了三个子组件：InfoCard、LineChart 和 TableSection。这些子组件将在父组件的模板中被使用。
 ***/
export default defineComponent({
  name: 'HomeD',// 组件名称
  components: {// 注册子组件
    InfoCard,// 数据卡片组件
    LineChart,// 折线图组件
    TableSection// 表格组件
  },

  setup() {
    /***
     获取课题总数接口
     ***/
    const provincetotalTopics = ref("加载中...") // 初始化显示加载状态

    const fetchTotalTopics = async () => {
      try {
        // 1. 确认接口路径与代理配置匹配
        const response = await axios.get('/api/sheke/dataview/provin_topics')
        console.log("接口原始数据:", response.data)
        // 2. 根据接口文档解析嵌套数据结构
        if (response.data.success) {
          provincetotalTopics.value = response.data.data.count.toLocaleString() // 注意.data.data.count
        } else {
          console.error("接口业务异常:", response.data.msg)
          provincetotalTopics.value = "N/A"
        }
      } catch (error) {
        console.error("请求失败:", error)
        provincetotalTopics.value = "加载失败"
      }
    }
    // 在挂载时调用接口
    onMounted(() => {
      fetchTotalTopics()
    })

    /***
     获取国家社会科学将获奖总数接口
     ***/
    const countrytotalTopics = ref("加载中...") // 初始化显示加载状态

    const getCountryTopicsCount = async () => {
      try {
        // 1. 确认接口路径与代理配置匹配
        const response = await axios.get('/api/sheke/dataview/country_topics')
        console.log("接口原始数据:", response.data)
        // 2. 根据接口文档解析嵌套数据结构
        if (response.data.success) {
          countrytotalTopics.value = response.data.data.count.toLocaleString() // 注意.data.data.count
        } else {
          console.error("接口业务异常:", response.data.msg)
          countrytotalTopics.value = "N/A"
        }
      } catch (error) {
        console.error("请求失败:", error)
        countrytotalTopics.value = "加载失败"
      }
    }
    // 在挂载时调用接口
    onMounted(() => {
      getCountryTopicsCount()
    })

    /***
     获取省社会科学获奖总数接口
     ***/
    const socialScienceAweres = ref("加载中...") // 初始化显示加载状态

    const getSocialScienceAwaresCount = async () => {
      try {
        // 1. 确认接口路径与代理配置匹配
        const response = await axios.get('/api/sheke/dataview/social_science_awares')
        console.log("接口原始数据:", response.data)
        // 2. 根据接口文档解析嵌套数据结构
        if (response.data.success) {
          socialScienceAweres.value = response.data.data.count.toLocaleString() // 注意.data.data.count
        } else {
          console.error("接口业务异常:", response.data.msg)
          socialScienceAweres.value = "N/A"
        }
      } catch (error) {
        console.error("请求失败:", error)
        socialScienceAweres.value = "加载失败"
      }
    }
    // 在挂载时调用接口
    onMounted(() => {
      getSocialScienceAwaresCount()
    })

    /***
     获取四川科研团队数量
     ***/
    const socialScienceGroups = ref("加载中...") // 初始化显示加载状态

    const getSocialScienceGroupNums = async () => {
      try {
        // 1. 确认接口路径与代理配置匹配
        const response = await axios.get('/api/sheke/dataview/sheke_teams_num')
        console.log("接口原始数据:", response.data)
        // 2. 根据接口文档解析嵌套数据结构
        if (response.data.success) {
          socialScienceGroups.value = response.data.data.count.toLocaleString() // 注意.data.data.count
        } else {
          console.error("接口业务异常:", response.data.msg)
          socialScienceGroups.value = "N/A"
        }
      } catch (error) {
        console.error("请求失败:", error)
        socialScienceGroups.value = "加载失败"
      }
    }
    // 在挂载时调用接口
    onMounted(() => {
      getSocialScienceGroupNums()
    })

    /***
     获取SSCI和HCI发表数量
     ***/
    const SSCITotalNums = ref("加载中...") // 初始化显示加载状态

    const getSSCITotalNums = async () => {
      try {
        // 1. 确认接口路径与代理配置匹配
        const response = await axios.get('/api/sheke/dataview/SSCI_total_nums')
        console.log("接口原始数据:", response.data)
        // 2. 根据接口文档解析嵌套数据结构
        if (response.data.success) {
          SSCITotalNums.value = response.data.data.count.toLocaleString() // 注意.data.data.count
        } else {
          console.error("接口业务异常:", response.data.msg)
          SSCITotalNums.value = "N/A"
        }
      } catch (error) {
        console.error("请求失败:", error)
        SSCITotalNums.value = "加载失败"
      }
    }
    // 在挂载时调用接口
    onMounted(() => {
      getSSCITotalNums()
    })

    /***
     获取CSCI发表数量
     ***/
    const CSCITotalNums = ref("加载中...") // 初始化显示加载状态

    const getCSCITotalNums = async () => {
      try {
        // 1. 确认接口路径与代理配置匹配
        const response = await axios.get('/api/sheke/dataview/CSCI_total_nums')
        console.log("接口原始数据:", response.data)
        // 2. 根据接口文档解析嵌套数据结构
        if (response.data.success) {
          CSCITotalNums.value = response.data.data.count.toLocaleString() // 注意.data.data.count
        } else {
          console.error("接口业务异常:", response.data.msg)
          CSCITotalNums.value = "N/A"
        }
      } catch (error) {
        console.error("请求失败:", error)
        CSCITotalNums.value = "加载失败"
      }
    }
    // 在挂载时调用接口
    onMounted(() => {
      getCSCITotalNums()
    })

    /***
     用于实现历年省课题统计
     ***/
    const topicStatisticsByYear2 = ref([]); // 用于存储从后端接口返回的数据
    const fetchTopicStatistics2 = async () => {
      try {
        // 请求接口获取历年省课题统计数据
        const response = await axios.get('/api/sheke/dataview/provin_topics_statistic_by_year');
        console.log("历年省课题统计的接口原始数据:", response.data);

        // 确保接口返回成功
        if (response.data.success) {
          topicStatisticsByYear2.value = response.data.data; // 存储数据
          console.log(topicStatisticsByYear2.value.map(item => item.year));

        } else {
          console.error("接口业务异常:", response.data.msg);
        }
      } catch (error) {
        console.error("请求失败:", error);
      }
    };

    // 在组件挂载后调用接口
    onMounted(() => {
      fetchTopicStatistics2();
    });

    /***
     用于实现历年省课题统计
     ***/
    const topicStatisticsByYear = ref([]); // 用于存储从后端接口返回的数据
    const fetchTopicStatistics = async () => {
      try {
        // 请求接口获取历年省课题统计数据
        const response = await axios.get('/api/sheke/dataview/provin_topics_statistic_by_year');
        console.log("历年省课题统计的接口原始数据:", response.data);

        // 确保接口返回成功
        if (response.data.success) {
          topicStatisticsByYear.value = response.data.data; // 存储数据
          console.log(topicStatisticsByYear.value.map(item => item.year));

        } else {
          console.error("接口业务异常:", response.data.msg);
        }
      } catch (error) {
        console.error("请求失败:", error);
      }
    };

    // 在组件挂载后调用接口
    onMounted(() => {
      fetchTopicStatistics();
    });


    /***
     * 获取国社科奖统计接口
     */
    const CountrySocialScienceAwareService = ref([]); // 用于存储从后端接口返回的数据

    const fetchCountrySocialScienceAwareService = async () => {
      try {
        const response = await axios.get("/api/sheke/dataview/national_rewards_statistic"); // 修改为正确的接口路径
        console.log("省社科奖统计接口接口原始数据:", response.data);

        if (response.data.success) {
          CountrySocialScienceAwareService.value = response.data.data; // 存储数据
        } else {
          console.error("接口业务异常:", response.data.msg);
        }
      } catch (error) {
        console.error("请求失败:", error);
      }
    };


    onMounted(() => {
      fetchCountrySocialScienceAwareService(); // 在挂载时调用接口
    });

    // 添加词云数据属性
    const wordCloudData = ref([])

    // 添加 echarts 实例引用
    const echartsInstance = ref(null);

    // 获取词云数据
    const fetchWordCloudData = async (n = 50) => { // 默认获取 Top 50
      try {
        const response = await axios.post('/api/sheke/dataview/word_cloud_by_top_n_keywords_CSSCI', {
          n: n // 发送 Top N 参数
        })
        console.log("词云接口原始数据:", response.data)
        if (response.data.success && response.data.data) {
          // 假设接口返回的数据是 [{ keyword: "词", count: 10 }, ...]
          // 词云组件通常需要 [{ name: "词", value: 10 }, ...]
          wordCloudData.value = response.data.data.map(item => ({
            name: item.keyword,
            value: item.count
          }))
        } else {
          console.error("词云接口业务异常:", response.data.msg)
          wordCloudData.value = [] // 清空数据或设置为错误状态
        }
      } catch (error) {
        console.error("词云请求失败:", error)
        wordCloudData.value = [] // 清空数据或设置为错误状态
      }
    }

    // 在挂载时调用词云接口
    onMounted(() => {
      fetchTotalTopics()
      getCountryTopicsCount()
      getSocialScienceAwaresCount()
      getSocialScienceGroupNums()
      getSSCITotalNums()
      getCSCITotalNums()
      fetchWordCloudData() // 调用词云数据获取函数

      // 初始化 ECharts 实例
      const chartDom = document.getElementById('wordcloud-container');
      if (chartDom) {
        echartsInstance.value = echarts.init(chartDom);
      }
    })

    // 监听 wordCloudData 变化并更新词云图
    watch(wordCloudData, (newData) => {
      if (echartsInstance.value && newData && newData.length > 0) {
        const option = {
          tooltip: {
            show: true
          },
          series: [{
            type: 'wordCloud',
            shape: 'circle', // 词云形状，可选 circle, ellipse, triangle-forward, triangle, star, polygon, diamond
            sizeRange: [12, 60], // 文字大小范围
            rotationRange: [-90, 90], // 文字旋转范围
            rotationStep: 45, // 文字旋转步长
            gridSize: 8, // 词语之间的距离
            drawOutOfBound: false, // 是否允许绘制超出边界
            layoutAnimation: true, // 是否开启动画
            textStyle: {
              color: function () {
                // 随机颜色
                return 'rgb(' + [
                  Math.round(Math.random() * 160),
                  Math.round(Math.random() * 160),
                  Math.round(Math.random() * 160)
                ].join(',') + ')';
              },
              emphasis: {
                shadowBlur: 10,
                shadowColor: '#333'
              }
            }
            ,
            data: newData // 词云数据
          }]
        };
        echartsInstance.value.setOption(option);
      }
    }, {immediate: true}); // 立即执行一次监听，处理初始数据

    // 在组件卸载前销毁 ECharts 实例
    onUnmounted(() => {
      if (echartsInstance.value) {
        echartsInstance.value.dispose();
        echartsInstance.value = null;
      }
    });

    const infoCards = computed(() => [
      {
        a: {
          /***title: '省社科课题总数(项)',
           value: '12,356',
           icon: 'project1',
           color: 'yellow'***/
          title: '省社科课题总数(项)',
          value: provincetotalTopics.value, // 动态绑定响应式数据
          icon: 'project1',
          color: 'yellow'
        },


        b: {
          title: '国家社会科学奖获奖总数(项)',
          value: countrytotalTopics.value,
          icon: 'project2',
          color: 'yellow'
        }
      },
      {
        a: {
          title: '省社会科学奖获奖总数(项)',
          value: socialScienceAweres.value,
          icon: 'project4',
          color: 'sky'
        },
        b: {
          title: '四川科研团队数量(个)',
          value: socialScienceGroups.value,
          icon: 'project5',
          color: 'sky'
        }
      },
      {
        a: {
          title: 'SSCI和HCI发表数量(篇)',
          value: SSCITotalNums.value,
          icon: 'project6',
          color: 'red'
        },
        b: {
          title: 'CSCI发表数量(篇)',
          value: CSCITotalNums.value,
          icon: 'project7',
          color: 'red'
        }
      },
    ])


    // 图表配置

    const projectOption = {
      tooltip: {
        trigger: 'item',
        formatter: "{a} <br/>{b}: {c} ({d}%)"
      },
      legend: {
        orient: 'vertical',
        x: 'right',
        textStyle: {
          color: '#ffffff',
        },
        data: ['2020', '2021', '2022', '2023', '2024']
      },
      color: [
        '#5470C6', // 深蓝
        '#91CC75', // 绿色
        '#FAC858', // 黄色
        '#EE6666', // 红色
        '#73A6FF'  // 浅蓝
      ],
      series: [
        {
          name: '历年省课题统计',
          type: 'pie',
          radius: ['20%', '50%'],
          data: [
            {value: 335, name: '2020'},
            {value: 310, name: '2021'},
            {value: 234, name: '2022'},
            {value: 234, name: '2023'},
            {value: 135, name: '2024'}
          ],
          label: {
            show: true,
            position: 'outside',
            formatter: '{b}: {c}',
            color: 'auto'
          },
          labelLine: {
            show: true,
            length: 10,
            length2: 10
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }
      ]
    }

    const awardOption = computed(() => {
      // 确保数据已加载
      if (!CountrySocialScienceAwareService.value.length) {
        return {
          tooltip: {
            trigger: 'axis',
            axisPointer: {
              type: 'shadow'
            }
          },
          grid: { show: 'true', borderWidth: '0' },
          legend: {
            data: ['2020', '2021', '2022', '2023'],
            textStyle: {
              color: '#ffffff',
            }
          },
          calculable: false,
          xAxis: [
            {
              type: 'value',
              axisLabel: {
                show: true,
                textStyle: {
                  color: '#fff'
                }
              },
              splitLine: {
                lineStyle: {
                  color: ['#f2f2f2'],
                  width: 0,
                  type: 'solid'
                }
              }
            }
          ],
          yAxis: [
            {
              type: 'category',
              data: ['2020', '2021', '2022', '2023'],
              axisLabel: {
                show: true,
                textStyle: {
                  color: '#fff'
                }
              },
              splitLine: {
                lineStyle: {
                  width: 0,
                  type: 'solid'
                }
              }
            }
          ],
          series: []
        };
      }

      // 根据接口返回的数据动态生成图例和系列数据
      const years = CountrySocialScienceAwareService.value.map(item => item.year);  // 提取年份
      const topicCounts = CountrySocialScienceAwareService.value.map(item => item.topicCount);  // 提取课题数

      return {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        grid: { show: 'true', borderWidth: '0' },
        legend: {
          data: years,  // 图例数据，动态生成
          textStyle: {
            color: '#ffffff',
          }
        },
        calculable: false,
        xAxis: [
          {
            type: 'value',
            axisLabel: {
              show: true,
              textStyle: {
                color: '#fff'
              }
            }
            ,
            splitLine: {
              lineStyle: {
                color: ['#f2f2f2'],
                width: 0,
                type: 'solid'
              }
            }
          }
        ],
        yAxis: [
          {
            type: 'category',
            data: years,  // y轴数据是年份
            axisLabel: {
              show: true,
              textStyle: {
                color: '#fff'
              }
            }
            ,
            splitLine: {
              lineStyle: {
                width: 0,
                type: 'solid'
              }
            }
          }
        ],
        series: [
          {
            name: '课题数',
            type: 'bar',
            stack: '总量',
            itemStyle: { normal: { label: { show: true, position: 'insideRight' },color: function(params) {
                  const colors = ['#ff6347', '#ff4500', '#ffa500', '#32cd32', '#00ced1', '#7b68ee', '#d2691e'];  // 颜色数组
                  return colors[params.dataIndex % colors.length];  // 循环使用颜色
                } } },
            data: topicCounts  // 使用从接口获取的课题数数据
          }
        ]
      };
    });


    // 新增国家社科奖统计数据
const nationalAwardStats = ref([]);

// 获取国家社科奖统计数据
const fetchNationalAwardStats = async () => {
  try {
    const response = await axios.get("/api/sheke/dataview/national_award_statistic");
    console.log("国家社科奖统计接口原始数据:", response.data);
    
    if (response.data.success) {
      nationalAwardStats.value = response.data.data;
    } else {
      console.error("接口业务异常:", response.data.msg);
    }
  } catch (error) {
    console.error("请求失败:", error);
  }
};

// 在组件挂载时调用
onMounted(() => {
  fetchNationalAwardStats();
});

// 修改后的历年国家社科奖统计图表配置
const nationalAwardOption = computed(() => {
  // 确保数据已加载
  if (!nationalAwardStats.value.length) {
    return {
      title: {
        text: '历年国家社科奖统计',
        textStyle: { color: '#fff' }
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' }
      },
      grid: { show: true, borderWidth: '0' },
      xAxis: {
        type: 'category',
        data: [],
        axisLabel: { color: '#fff' }
      },
      yAxis: {
        type: 'value',
        axisLabel: { color: '#fff' }
      },
      series: []
    };
  }
  
  // 从接口数据中提取年份和奖项数量
  const years = nationalAwardStats.value.map(item => item.year);
  const awardCounts = nationalAwardStats.value.map(item => item.count);
  
  return {
    title: {
      text: '历年国家社科奖统计',
      textStyle: { color: '#fff' }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const data = params[0];
        return `${data.name}年<br/>国家社科奖: ${data.value}项`;
      }
    },
    grid: { show: true, borderWidth: '0' },
    xAxis: {
      type: 'category',
      data: years,
      axisLabel: { color: '#fff' }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#fff' },
      splitLine: { lineStyle: { color: '#444' } }
    },
    series: [{
      name: '国家社科奖',
      type: 'bar',
      data: awardCounts,
      itemStyle: {
        color: '#409EFF',
        emphasis: { color: '#66B1FF' }
      },
      label: {
        show: true,
        position: 'top',
        color: '#fff'
      }
    }]
  };
});

    const option = {
      grid: {show: 'true', borderWidth: '0'},
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        },
        formatter: function (params) {
          const tar = params[0]
          return tar.name + '<br/>' + tar.seriesName + ' : ' + tar.value
        }
      },
      xAxis: [
        {
          type: 'category',
          splitLine: {show: false},
          data: ['2020', '2021', '2022', '2023', '2024'],
          axisLabel: {
            show: true,
            textStyle: {
              color: '#fff'
            }
          }
        }
      ],
      yAxis: [
        {
          type: 'value',
          splitLine: {show: false},
          axisLabel: {
            show: true,
            textStyle: {
              color: '#fff'
            }
          }
        }
      ],
      series: [
        {
          name: '发表数量',
          type: 'bar',
          stack: '总量',
          itemStyle: {normal: {label: {show: true, position: 'inside'}}},
          data: [900, 1200, 1300, 1200, 1900]
        }
      ]
    }

    // 表格数据
    const topLists = ref([]); // 改为响应式数据

    // 获取省课题前五机关单位数据
    const fetchProvinceTopicTopOrg = async () => {
      try {
        const response = await axios.get('/api/sheke/dataview/province_topic_top_org');
        const responseCSCI = await axios.get('/api/sheke/dataview/CSCI-top-authors');
        const responseSSCI = await axios.get('/api/sheke/dataview/SSCI-top-authors');
        const responseCountry = await axios.get('/api/sheke/dataview/country_topic_top_org');
        if (responseSSCI.data.success){
          const vos = response.data.data;
          const vos1 = responseCSCI.data.data;
          const vos2 = responseSSCI.data.data;
          const vos3 = responseCountry.data.data;
          topLists.value = [
            {
              title: '获国家社科奖前5位',
              columns: [
                { prop: 'rank', label: '排名' },
                { prop: 'unit', label: '获奖单位' },
                { prop: 'count', label: '获奖数(项)' }
              ],
              data: vos3.map((vo, index) => ({
                rank: index + 1,
                unit: vo.organizationName || '未知单位',
                count: vo.topicCountDisplay || 0
              })).slice(0, 5) // 确保只取前5条
            },
            {
              title: '发表SSCI论文前5位',
              columns: [
                { prop: 'rank', label: '排名' },
                { prop: 'unit', label: '获奖人员' },
                { prop: 'count', label: '获奖数(项)' }
              ],
              data: vos2.map((vo, index) => ({
                rank: index + 1,
                unit: vo.name || '未知单位',
                count: vo.paperCountDisplay || 0
              })).slice(0, 5) // 确保只取前5条
            },
            {
              title: '发表CSSCI论文前5位',
              columns: [
                { prop: 'rank', label: '排名' },
                { prop: 'unit', label: '获奖人员' },
                { prop: 'count', label: '获奖数(项)' }
              ],
              data: vos1.map((vo, index) => ({
                rank: index + 1,
                unit: vo.name || '未知单位',
                count: vo.paperCountDisplay || 0
              })).slice(0, 5) // 确保只取前5条
            },
            {
              title: '获省课题前5位',
              columns: [
                { prop: 'rank', label: '排名' },
                { prop: 'unit', label: '获课题单位' },
                { prop: 'count', label: '获课题次数(项)' }
              ],
              // 假设ProvinceTopicTopOrganizationVO包含organizationName和topicCount字段
              data: vos.map((vo, index) => ({
                rank: index + 1,
                unit: vo.organizationName || '未知单位',
                count: vo.topicCountDisplay || 0
              })).slice(0, 5) // 确保只取前5条
            }
          ];
        } else {
          console.error("获取省课题前五数据失败:", response.data.msg);
          setDefaultTopLists();
        }
      } catch (error) {
        console.error("请求省课题前五数据失败:", error);
        setDefaultTopLists();
      }
    };
    // 表格数据
    // 设置默认表格数据
    const setDefaultTopLists = () => {
      topLists.value = [
        {
          title: '获国家社科奖前5位',
          columns: [
            { prop: 'rank', label: '排名' },
            { prop: 'unit', label: '获奖单位' },
            { prop: 'count', label: '获奖数(项)' }
          ],
          data: [
            { rank: 1, unit: '西南财经大学', count: 134 },
            { rank: 1, unit: '西南财经大学', count: 134 },
            { rank: 1, unit: '西南财经大学', count: 134 },
            { rank: 1, unit: '西南财经大学', count: 134 },
            { rank: 1, unit: '西南财经大学', count: 134 },
          ]
        },
        {
          title: '发表SSCI论文前5位',
          columns: [
            { prop: 'rank', label: '排名' },
            { prop: 'unit', label: '获奖人员' },
            { prop: 'count', label: '获奖数(项)' }
          ],
          data: [
            { rank: 1, unit: '西南财经大学', count: 134 },
            { rank: 1, unit: '西南财经大学', count: 134 },
            { rank: 1, unit: '西南财经大学', count: 134 },
            { rank: 1, unit: '西南财经大学', count: 134 },
            { rank: 1, unit: '西南财经大学', count: 134 },
          ]
        },
        {
          title: '发表CSSCI论文前5位',
          columns: [
            { prop: 'rank', label: '排名' },
            { prop: 'unit', label: '获奖人员' },
            { prop: 'count', label: '获奖数(项)' }
          ],
          data: [
            { rank: 1, unit: '西南财经大学', count: 134 },
            { rank: 1, unit: '西南财经大学', count: 134 },
            { rank: 1, unit: '西南财经大学', count: 134 },
            { rank: 1, unit: '西南财经大学', count: 134 },
            { rank: 1, unit: '西南财经大学', count: 134 },
          ]
        },
        {
          title: '获省课题前5位',
          columns: [
            { prop: 'rank', label: '排名' },
            { prop: 'unit', label: '获课题单位' },
            { prop: 'count', label: '获课题次数(项)' }
          ],
          data: [
            { rank: 1, unit: '西南财经大学', count: 134 },
            { rank: 2, unit: '四川大学', count: 120 },
            { rank: 3, unit: '电子科技大学', count: 98 },
            { rank: 4, unit: '西南交通大学', count: 85 },
            { rank: 5, unit: '成都理工大学', count: 76 }
          ]
        }
      ];
    };

// 在onMounted中调用
    onMounted(() => {
      fetchProvinceTopicTopOrg();
      // 其他已有接口调用...
    });

// 在return语句中返回topLists

    return {
      infoCards,
      projectOption,
      awardOption,
      nationalAwardOption,
      option,
      topLists,
      wordCloudData
    }
  }
})
</script>

<style lang="scss" scoped>
.home-container {
  margin: 0 auto;

  .overview-section {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
    margin-bottom: 20px;
  }

  .chart-section {
    display: flex;
    justify-content: space-around;
    margin-bottom: 20px;
    gap: 20px;

    .chart-group {
      width: 42%;

      &.center-group {
        width: 34%;
      }
    }
  }

  .table-section {
    background: #081832;
    height: 300px;
  }
}
</style>