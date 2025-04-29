<template>
  <t-row :gutter="[16, 16]" class="row-container">
    <t-col :xs="12" :xl="6">
      <t-card :bordered="false" class="dashboard-top-panel-first">
        <template #title>
          <div class="dashboard-top-panel-title">
            <span>总销售额</span>
            <t-tooltip content="指标说明">
              <t-icon name="help-circle" />
            </t-tooltip>
          </div>
        </template>
        <template #actions>
          <t-date-range-picker
            class="card-date-picker-container"
            :default-value="LAST_7_DAYS"
            theme="primary"
            mode="date"
            @change="onMoneyChange"
          />
        </template>
        <div class="dashboard-top-panel-content">
          <div class="dashboard-top-panel-content-left">
            <div class="dashboard-top-panel-content-left-value">¥ 126,560</div>
            <div class="dashboard-top-panel-content-left-label">
              <span>周同比</span>
              <trend :type="'up'" :describe="'12%'" />
            </div>
          </div>
          <div class="dashboard-top-panel-content-right">
            <div id="moneyContainer" class="dashboard-top-panel-chart" />
          </div>
        </div>
      </t-card>
    </t-col>
    <t-col :xs="12" :xl="6">
      <t-card :bordered="false" class="dashboard-top-panel-second">
        <template #title>
          <div class="dashboard-top-panel-title">
            <span>访问量</span>
            <t-tooltip content="指标说明">
              <t-icon name="help-circle" />
            </t-tooltip>
          </div>
        </template>
        <template #actions>
          <t-date-range-picker
            class="card-date-picker-container"
            :default-value="LAST_7_DAYS"
            theme="primary"
            mode="date"
            @change="onRefundChange"
          />
        </template>
        <div class="dashboard-top-panel-content">
          <div class="dashboard-top-panel-content-left">
            <div class="dashboard-top-panel-content-left-value">8,846</div>
            <div class="dashboard-top-panel-content-left-label">
              <span>日访问量</span>
              <trend :type="'down'" :describe="'11%'" />
            </div>
          </div>
          <div class="dashboard-top-panel-content-right">
            <div id="refundContainer" class="dashboard-top-panel-chart" />
          </div>
        </div>
      </t-card>
    </t-col>
    <t-col :xs="12" :xl="6">
      <t-card :bordered="false" class="dashboard-top-panel-third">
        <template #title>
          <div class="dashboard-top-panel-title">
            <span>支付笔数</span>
            <t-tooltip content="指标说明">
              <t-icon name="help-circle" />
            </t-tooltip>
          </div>
        </template>
        <template #actions>
          <t-date-range-picker
            class="card-date-picker-container"
            :default-value="LAST_7_DAYS"
            theme="primary"
            mode="date"
            @change="onOrderChange"
          />
        </template>
        <div class="dashboard-top-panel-content">
          <div class="dashboard-top-panel-content-left">
            <div class="dashboard-top-panel-content-left-value">6,560</div>
            <div class="dashboard-top-panel-content-left-label">
              <span>转化率</span>
              <trend :type="'up'" :describe="'12%'" />
            </div>
          </div>
          <div class="dashboard-top-panel-content-right">
            <div id="orderContainer" class="dashboard-top-panel-chart" />
          </div>
        </div>
      </t-card>
    </t-col>
    <t-col :xs="12" :xl="6">
      <t-card :bordered="false" class="dashboard-top-panel-fourth">
        <template #title>
          <div class="dashboard-top-panel-title">
            <span>运营活动效果</span>
            <t-tooltip content="指标说明">
              <t-icon name="help-circle" />
            </t-tooltip>
          </div>
        </template>
        <template #actions>
          <t-date-range-picker
            class="card-date-picker-container"
            :default-value="LAST_7_DAYS"
            theme="primary"
            mode="date"
            @change="onActivityChange"
          />
        </template>
        <div class="dashboard-top-panel-content">
          <div class="dashboard-top-panel-content-left">
            <div class="dashboard-top-panel-content-left-value">78%</div>
            <div class="dashboard-top-panel-content-left-label">
              <span>周同比</span>
              <trend :type="'up'" :describe="'12%'" />
            </div>
          </div>
          <div class="dashboard-top-panel-content-right">
            <div id="activityContainer" class="dashboard-top-panel-chart" />
          </div>
        </div>
      </t-card>
    </t-col>
  </t-row>
</template>

<script lang="ts">
export default {
  name: 'DashboardBase',
};
</script>

<script setup lang="ts">
import { LineChart } from 'echarts/charts';
import { GridComponent, TooltipComponent } from 'echarts/components';
import * as echarts from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import type { DateRangeValue } from 'tdesign-vue-next';
import { computed, nextTick, onMounted, onUnmounted, watch } from 'vue';

import Trend from '@/components/trend/index.vue';
import { useSettingStore } from '@/store';
import { changeChartsTheme } from '@/utils/color';
import { LAST_7_DAYS } from '@/utils/date';

import { constructInitDashboardDataset } from '../index';

echarts.use([LineChart, GridComponent, TooltipComponent, CanvasRenderer]);

let moneyContainer: HTMLElement;
let refundContainer: HTMLElement;
let orderContainer: HTMLElement;
let activityContainer: HTMLElement;
let moneyChart: echarts.ECharts;
let refundChart: echarts.ECharts;
let orderChart: echarts.ECharts;
let activityChart: echarts.ECharts;
const store = useSettingStore();
const chartColors = computed(() => store.chartColors);

const onMoneyChange = (value: DateRangeValue) => {
  moneyChart.setOption(
    constructInitDashboardDataset('line'),
  );
};

const onRefundChange = (value: DateRangeValue) => {
  refundChart.setOption(
    constructInitDashboardDataset('bar'),
  );
};

const onOrderChange = (value: DateRangeValue) => {
  orderChart.setOption(
    constructInitDashboardDataset('line'),
  );
};

const onActivityChange = (value: DateRangeValue) => {
  activityChart.setOption(
    constructInitDashboardDataset('line'),
  );
};

const initChart = () => {
  moneyContainer = document.getElementById('moneyContainer');
  refundContainer = document.getElementById('refundContainer');
  orderContainer = document.getElementById('orderContainer');
  activityContainer = document.getElementById('activityContainer');

  moneyChart = echarts.init(moneyContainer);
  moneyChart.setOption(constructInitDashboardDataset('line'));

  refundChart = echarts.init(refundContainer);
  refundChart.setOption(constructInitDashboardDataset('bar'));

  orderChart = echarts.init(orderContainer);
  orderChart.setOption(constructInitDashboardDataset('line'));

  activityChart = echarts.init(activityContainer);
  activityChart.setOption(constructInitDashboardDataset('line'));
};

const updateContainer = () => {
  moneyChart?.resize({
    width: moneyContainer.clientWidth,
    height: moneyContainer.clientHeight,
  });
  refundChart?.resize({
    width: refundContainer.clientWidth,
    height: refundContainer.clientHeight,
  });
  orderChart?.resize({
    width: orderContainer.clientWidth,
    height: orderContainer.clientHeight,
  });
  activityChart?.resize({
    width: activityContainer.clientWidth,
    height: activityContainer.clientHeight,
  });
};

onMounted(() => {
  nextTick(() => {
    initChart();
  });
  window.addEventListener('resize', updateContainer, false);
});

onUnmounted(() => {
  window.removeEventListener('resize', updateContainer);
});

watch(
  () => store.brandTheme,
  () => {
    changeChartsTheme([moneyChart, refundChart, orderChart, activityChart]);
  },
);
</script>

<style lang="less" scoped>
.row-container {
  padding: var(--td-comp-paddingTB-xxl) var(--td-comp-paddingLR-xxl);
}

.dashboard-top-panel {
  &-first {
    background: linear-gradient(90deg, var(--td-brand-color-1) 0%, var(--td-brand-color) 100%);
  }

  &-second {
    background: linear-gradient(90deg, var(--td-success-color-1) 0%, var(--td-success-color) 100%);
  }

  &-third {
    background: linear-gradient(90deg, var(--td-warning-color-1) 0%, var(--td-warning-color) 100%);
  }

  &-fourth {
    background: linear-gradient(90deg, var(--td-error-color-1) 0%, var(--td-error-color) 100%);
  }

  &-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font: var(--td-font-title-medium);
    font-weight: 500;
    color: var(--td-text-color-primary);
  }

  &-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: var(--td-comp-margin-xxl);

    &-left {
      &-value {
        font: var(--td-font-title-large);
        font-weight: 500;
        color: var(--td-text-color-primary);
      }

      &-label {
        display: flex;
        align-items: center;
        margin-top: var(--td-comp-margin-s);
        color: var(--td-text-color-secondary);
        font-size: var(--td-font-size-body-small);
      }
    }

    &-right {
      flex: 1;
      margin-left: var(--td-comp-margin-xxl);
    }
  }

  &-chart {
    width: 100%;
    height: 100%;
  }
}

.card-date-picker-container {
  margin-left: var(--td-comp-margin-l);
}
</style>
