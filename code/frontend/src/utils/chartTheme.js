import * as echarts from 'echarts'

export const CHART_COLORS = [
  '#4a90ff',
  '#6f63ff',
  '#47c7ff',
  '#9a7bff',
  '#35b86b',
  '#ff9f55',
  '#ff6b8a'
]

export const HEATMAP_GRADIENT = ['#eaf1ff', '#b9d1ff', '#6f9dff', '#6f63ff', '#4839d5']

let themeRegistered = false

const gymChartTheme = {
  color: CHART_COLORS,
  backgroundColor: 'transparent',
  textStyle: {
    color: '#586a8f'
  },
  title: {
    textStyle: {
      color: '#2f477d',
      fontWeight: 700
    },
    subtextStyle: {
      color: '#8090b3'
    }
  },
  legend: {
    textStyle: {
      color: '#60739c'
    }
  },
  tooltip: {
    backgroundColor: 'rgba(37, 58, 108, 0.92)',
    borderColor: '#5e84ff',
    textStyle: {
      color: '#fff'
    }
  },
  categoryAxis: {
    axisLine: {
      lineStyle: {
        color: '#d4def5'
      }
    },
    axisTick: {
      show: false
    },
    axisLabel: {
      color: '#6c7fa6'
    },
    splitLine: {
      lineStyle: {
        color: '#edf2ff'
      }
    }
  },
  valueAxis: {
    axisLine: {
      show: false
    },
    axisLabel: {
      color: '#6c7fa6'
    },
    splitLine: {
      lineStyle: {
        color: '#edf2ff'
      }
    }
  }
}

const ensureTheme = () => {
  if (themeRegistered) {
    return
  }
  echarts.registerTheme('gym-blue-violet', gymChartTheme)
  themeRegistered = true
}

export const initChart = (domRef) => {
  if (!domRef) {
    return null
  }

  ensureTheme()
  return echarts.getInstanceByDom(domRef) || echarts.init(domRef, 'gym-blue-violet')
}

