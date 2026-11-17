/**
 * QQ 头像地址统一生成。
 * 其他页面需要群/用户头像时直接引用这里，避免重复拼接 URL。
 */

export function userAvatarUrl(userId) {
  return userId ? 'https://q1.qlogo.cn/g?b=qq&nk=' + userId + '&s=100' : '';
}

export function groupAvatarUrl(groupId) {
  return groupId ? 'https://p.qlogo.cn/gh/' + groupId + '/' + groupId + '/100' : '';
}

export function sessionAvatarUrl(scene, targetId) {
  return scene === 'group' ? groupAvatarUrl(targetId) : userAvatarUrl(targetId);
}
