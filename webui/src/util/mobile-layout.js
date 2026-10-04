// Keep these boundaries aligned with styles/media.scss and util/admin.js.
export function viewportMode(width) {
  return { phone: width < 768, compact: width < 992 }
}

export function paginationForViewport(options, phone) {
  return phone ? { ...options, layout: 'prev, pager, next', pagerCount: 5 } : { ...options }
}

export function countActiveFilters(model) {
  return Object.values(model || {}).filter(value => Array.isArray(value)
    ? value.some(item => item !== '' && item != null)
    : value !== '' && value != null).length
}
