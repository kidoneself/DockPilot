<template>
  <t-card title="出入库概览" subtitle="出入库统计" class="dashboard-chart-card" :bordered="false">
    <template #actions>
      <t-button>导出</t-button>
    </template>
    <t-row :gutter="[16, 16]">
      <t-col :xs="12" :xl="6">
        <t-card :bordered="false" class="dashboard-chart-card">
          <template #title>
            <div class="dashboard-chart-title">
              <span>本月入库</span>
              <span class="dashboard-chart-subtitle">较上月</span>
            </div>
          </template>
          <div
            id="stokeContainer"
            class="dashboard-chart-container"
            :style="{ width: '100%', height: `${resizeTime * 326}px` }"
          />
        </t-card>
      </t-col>
      <t-col :xs="12" :xl="6">
        <t-card :bordered="false" class="dashboard-chart-card">
          <template #title>
            <div class="dashboard-chart-title">
              <span>本月出库</span>
              <span class="dashboard-chart-subtitle">较上月</span>
            </div>
          </template>
          <div
            id="outContainer"
            class="dashboard-chart-container"
            :style="{ width: '100%', height: `${resizeTime * 326}px` }"
          />
        </t-card>
      </t-col>
    </t-row>
  </t-card>
</template>

<script lang="ts">
export default {
  name: 'DashboardBase',
};
</script>

<script setup lang="ts">
import { useWindowSize } from '@vueuse/core';
import { LineChart } from 'echarts/charts';
import { GridComponent, TooltipComponent } from 'echarts/components';
import * as echarts from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { computed, nextTick, onMounted, ref, watch } from 'vue';

import { useSettingStore } from '@/store';
import { changeChartsTheme } from '@/utils/color';
import { LAST_7_DAYS } from '@/utils/date';

import { constructInitDataset } from '../index';

echarts.use([LineChart, GridComponent, TooltipComponent, CanvasRenderer]);

const store = useSettingStore();
const resizeTime = ref(1);

const chartColors = computed(() => store.chartColors);

// stokeChart
let stokeContainer: HTMLElement;
let stokeChart: echarts.ECharts;
const renderStokeChart = () => {
  if (!stokeContainer) {
    stokeContainer = document.getElementById('stokeContainer');
  }
  stokeChart = echarts.init(stokeContainer);
  stokeChart.setOption(constructInitDataset({ dateTime: LAST_7_DAYS, ...chartColors.value }));
};

// outChart
let outContainer: HTMLElement;
let outChart: echarts.ECharts;
const renderOutChart = () => {
  if (!outContainer) {
    outContainer = document.getElementById('outContainer');
  }
  outChart = echarts.init(outContainer);
  outChart.setOption(constructInitDataset({ dateTime: LAST_7_DAYS, ...chartColors.value }));
};

const renderCharts = () => {
  renderStokeChart();
  renderOutChart();
};

// chartSize update
const updateContainer = () => {
  if (document.documentElement.clientWidth >= 1400 && document.documentElement.clientWidth < 1920) {
    resizeTime.value = Number((document.documentElement.clientWidth / 2080).toFixed(2));
  } else if (document.documentElement.clientWidth < 1080) {
    resizeTime.value = Number((document.documentElement.clientWidth / 1080).toFixed(2));
  } else {
    resizeTime.value = 1;
  }

  stokeChart.resize({
    width: stokeContainer.clientWidth,
    height: resizeTime.value * 326,
  });
  outChart.resize({
    width: outContainer.clientWidth,
    height: resizeTime.value * 326,
  });
};

onMounted(() => {
  renderCharts();
  nextTick(() => {
    updateContainer();
  });
});

const { width, height } = useWindowSize();
watch([width, height], () => {
  updateContainer();
});

watch(
  () => store.brandTheme,
  () => {
    changeChartsTheme([stokeChart, outChart]);
  },
);

watch(
  () => store.mode,
  () => {
    [stokeChart, outChart].forEach((item) => {
      item.dispose();
    });

    renderCharts();
  },
);

const onCurrencyChange = (checkedValues: string[]) => {
  stokeChart.setOption(constructInitDataset({ dateTime: checkedValues, ...chartColors.value }));
  outChart.setOption(constructInitDataset({ dateTime: checkedValues, ...chartColors.value }));
};
</script>

<style lang="less" scoped>
.dashboard-chart-card {
  padding: var(--td-comp-paddingTB-xxl) var(--td-comp-paddingLR-xxl);

  :deep(.t-card__header) {
    padding: 0;
  }

  :deep(.t-card__body) {
    padding: 0;
    margin-top: var(--td-comp-margin-xxl);
  }

  :deep(.t-card__title) {
    font: var(--td-font-title-large);
    font-weight: 400;
  }
}

.dashboard-chart-title {
  display: flex;
  justify-content: space-between;
  align-items: center;

  &-subtitle {
    color: var(--td-text-color-secondary);
    font-size: var(--td-font-size-body-small);
  }
}
</style>
