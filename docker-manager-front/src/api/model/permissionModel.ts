import { defineComponent } from 'vue';

import { RouteMeta } from '@/types/interface';

export interface MenuListResult {
  code: number;
  data: Array<RouteItem>;
  message: string;
}

export type Component<T = any> =
  | ReturnType<typeof defineComponent>
  | (() => Promise<typeof import('*.vue')>)
  | (() => Promise<T>);

export interface RouteItem {
  path: string;
  name: string;
  component?: Component | string;
  components?: Component;
  redirect?: string;
  meta: RouteMeta;
  children?: Array<RouteItem>;
}
