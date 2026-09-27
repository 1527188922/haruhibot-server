/**
 * JM漫画列表的列定义。
 *
 * 为什么单独抽出来：主记录 tab 与收藏夹 tab 用的是同一份列表/瀑布流组件
 * （jm-album-view.vue），列集合也必须共用一份，否则两边迟早会漂移，
 * 出现"收藏夹 tab 的信息没有主记录 tab 全"的问题。
 */

/**
 * 主记录 tab 的列（沿用原有顺序与标签，不要随意调整，避免打乱用户习惯）
 */
const BASE_COLUMN_OPTIONS = [
  { key: 'selection', label: 'selection' },
  { key: 'action', label: '操作' },
  { key: 'index', label: '序号' },
  { key: 'id', label: 'JM ID' },
  { key: 'cover', label: '封面' },
  { key: 'collected', label: '收藏' },
  { key: 'name', label: '名称' },
  { key: 'author', label: '作者' },
  { key: 'tags', label: '标签' },
  { key: 'interactionStats', label: '互动统计' },
  { key: 'imageStats', label: '图片统计' },
  { key: 'zip', label: 'ZIP' },
  { key: 'pdf', label: 'PDF' },
  { key: 'createTime', label: '下载时间' },
  { key: 'chapterCount', label: '章节数' },
  { key: 'chapterList', label: '列表章节' },
  { key: 'works', label: '作品' },
  { key: 'actors', label: '角色' },
  { key: 'description', label: '描述' },
  { key: 'albumFolderName', label: '文件夹' },
  { key: 'relatedList', label: '相关列表' },
  { key: 'addTime', label: 'JM发布时间' },
  { key: 'raw', label: 'raw' }
];

export const ALBUM_COLUMN_OPTIONS = BASE_COLUMN_OPTIONS;

/**
 * 收藏夹 tab 的列：与主记录完全一致，另外多两列收藏夹专属信息
 * （所属收藏夹、加入当前收藏夹的时间）
 */
export const FAVORITE_COLUMN_OPTIONS = [
  ...BASE_COLUMN_OPTIONS.slice(0, 6),
  { key: 'favorites', label: '所属收藏夹' },
  ...BASE_COLUMN_OPTIONS.slice(6, 13),
  { key: 'favoriteAddTime', label: '加入时间' },
  ...BASE_COLUMN_OPTIONS.slice(13)
];

/**
 * 默认显示的列。收藏夹 tab 在主记录的基础上默认多显示"所属收藏夹"
 */
export const DEFAULT_ALBUM_COLUMNS = [
  'selection', 'action', 'index', 'id', 'cover', 'collected',
  'name', 'author', 'tags', 'zip', 'pdf', 'createTime', 'imageStats', 'interactionStats'
];

export const DEFAULT_FAVORITE_COLUMNS = [
  ...DEFAULT_ALBUM_COLUMNS,
  'favorites', 'favoriteAddTime'
];
