/*
 * 容器相关常量定义
 * 生成时间：2024-06-09 21:10:00
 */

import { FormRule } from 'tdesign-vue-next';

// 镜像选项
export const IMAGE_OPTIONS = [
    {label: 'nginx', value: 'nginx'},
    {label: 'mysql', value: 'mysql'},
    {label: 'redis', value: 'redis'},
    {label: 'postgres', value: 'postgres'},
];

// 环境类型选项
export const TYPE_OPTIONS = [
    {label: '开发环境', value: 'dev'},
    {label: '测试环境', value: 'test'},
    {label: '生产环境', value: 'prod'},
];

// 网络模式选项
export const NETWORK_OPTIONS = [
    {value: 'bridge', label: '桥接模式', description: '默认网络模式，容器通过桥接网络与外部通信'},
    {value: 'host', label: '主机模式', description: '容器使用主机网络，无需端口映射，性能更好'},
    {value: 'none', label: '禁用网络', description: '容器没有网络连接'},
    {value: 'container', label: '容器模式', description: '与指定容器共享网络命名空间'},
    {value: 'macvlan', label: 'Macvlan模式', description: '分配MAC地址，使容器显示为物理设备'},
    {value: 'ipvlan', label: 'IPvlan模式', description: '共享宿主MAC地址，分配IP地址'},
    {value: 'custom', label: '自定义网络', description: '使用用户创建的自定义网络'},
];

// 重启策略选项
export const RESTART_POLICY_OPTIONS = [
    {value: 'no', label: '不重启', description: '容器退出后不自动重启'},
    {value: 'always', label: '总是重启', description: '容器退出后总是重启，包括手动停止'},
    {value: 'unless-stopped', label: '除非手动停止', description: '容器退出后自动重启，除非手动停止'},
    {value: 'on-failure', label: '失败时重启', description: '容器非正常退出时自动重启'},
];

// 表单校验规则（适用于容器创建/编辑/详情等场景）
export const FORM_RULES: Record<string, FormRule[]> = {
    image: [{ required: true, message: '请选择镜像', type: 'error' }],
    name: [{ required: true, message: '请输入容器名称', type: 'error' }],
    restartPolicy: [{ required: true, message: '请选择重启策略', type: 'error' }],
    networkMode: [{ required: true, message: '请选择网络模式', type: 'error' }],
    ipAddress: [{ required: false, message: '请输入IP地址', type: 'error' }],
    gateway: [{ required: false, message: '请输入网关', type: 'error' }],
    macAddress: [{ required: false, message: '请输入MAC地址', type: 'error' }],
    memoryLimit: [{ required: false, message: '请输入内存限制', type: 'error' }],
    cpuLimit: [{ required: false, message: '请输入CPU限制', type: 'error' }],
    workingDir: [{ required: false, message: '请输入工作目录', type: 'error' }],
    user: [{ required: false, message: '请输入用户', type: 'error' }],
    command: [{ required: false, message: '请输入命令', type: 'error' }],
}; 