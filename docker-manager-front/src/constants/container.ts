/*
 * 容器相关常量定义
 * 生成时间：2024-06-09 21:10:00
 */

import { FormRule } from 'tdesign-vue-next';

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
// 权限选项配置
export const VOLUME_PERMISSION_OPTIONS = [
    { label: '只读', value: true },
    { label: '读写', value: false },
];