/**
 * 容器 logo 映射
 * 用于根据容器镜像名称匹配对应的 logo 图片
 */
export const CONTAINER_LOGOS = {
  // 常用镜像 logo
  'nginx': '/src/assets/logo/nginx.png',
  'moviepilot': '/src/assets/logo/moviepilot.png',
  'httpd': '/src/assets/logo/httpd.png',
  'postgres': '/src/assets/logo/postgres.png',
  'mongo': '/src/assets/logo/mongo.png',
  'node': '/src/assets/logo/node.png',
  'python': '/src/assets/logo/python.png',
  'java': '/src/assets/logo/java.png',
  'php': '/src/assets/logo/php.png',
  'wordpress': '/src/assets/logo/wordpress.png',
  'jenkins': '/src/assets/logo/jenkins.png',
  'gitlab': '/src/assets/logo/gitlab.png',
  'elasticsearch': '/src/assets/logo/elasticsearch.png',
  'kibana': '/src/assets/logo/kibana.png',
  'grafana': '/src/assets/logo/grafana.png',
  'prometheus': '/src/assets/logo/prometheus.png',
  'rabbitmq': '/src/assets/logo/rabbitmq.png',
  'kafka': '/src/assets/logo/kafka.png',
};

/**
 * 获取容器 logo
 * @param imageName 容器镜像名称
 * @param containerId 容器ID，用于判断是否需要显示首字母
 * @param showInitial 需要显示首字母的容器ID集合
 * @returns logo 图片路径，如果没有匹配则返回 null
 */
export const getContainerLogo = (
  imageName: string,
  containerId: string,
  showInitial: Set<string>
): string | null => {
  if (!imageName || showInitial.has(containerId)) return null;
  
  // 转换为小写以进行不区分大小写的匹配
  const name = imageName.toLowerCase();
  
  // 移除版本号（如果有）
  const baseName = name.split(':')[0];
  
  // 移除仓库前缀（如果有）
  const cleanName = baseName.split('/').pop() || '';
  
  // 遍历所有 logo 键
  for (const [key, logo] of Object.entries(CONTAINER_LOGOS)) {
    if (cleanName.includes(key)) {
      return logo;
    }
  }
  
  // 如果没有匹配到，返回 null
  return null;
}; 