const WIDE_BREAKPOINT = 1200
const MEDIUM_BREAKPOINT = 992
const SMALL_BREAKPOINT = 768

/**
 * 取当前窗口宽度。优先 innerWidth，回退到 documentElement.clientWidth，最后回退 screen.width，
 * 保证在移动端浏览器、隐身窗口等场景下都能拿到有效值。
 */
function getWindowWidth() {
    if (typeof window === 'undefined') {
        return WIDE_BREAKPOINT
    }
    return window.innerWidth
        || (document.documentElement && document.documentElement.clientWidth)
        || (window.screen && window.screen.width)
        || WIDE_BREAKPOINT
}

function getWindowHeight() {
    if (typeof window === 'undefined') {
        return 0
    }
    return (window.visualViewport && window.visualViewport.height)
        || window.innerHeight
        || (document.documentElement && document.documentElement.clientHeight)
        || 0
}

export default {
    getWindowWidth,
    getWindowHeight,
    getScreen: function () {
        const width = getWindowWidth()
        if (width >= WIDE_BREAKPOINT) {
            return 3; //大屏幕
        } else if (width >= MEDIUM_BREAKPOINT) {
            return 2; //中屏幕
        } else if (width >= SMALL_BREAKPOINT) {
            return 1; //小屏幕
        } else {
            return 0; //超小屏幕
        }
    }
}
