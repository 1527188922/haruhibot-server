import Vue from 'vue';

/**
 * v-dialogDrag 弹窗拖拽
 *
 * 用法：v-dialogDrag 或 :dialog-drag-enabled="boolean"
 *
 * 移动端/触屏说明：
 * - 这里的拖拽只监听 mousedown/mousemove，触屏不响应；而在触摸设备上按下标题栏再滑动
 *   会被浏览器解释为滚动，反而容易把弹窗拖出可视区域且无法拖回。
 * - 因此移动端由调用方传入 false 关闭（本项目统一传 !isMobileView）；
 *   指令内部再兜底判断一次"当前环境是否只有粗指针(触屏)"，避免遗漏处失效。
 */
function isTouchOnlyDevice() {
  if (typeof window === 'undefined' || !window.matchMedia) {
    return false;
  }
  return window.matchMedia('(pointer: coarse)').matches
    && !window.matchMedia('(pointer: fine)').matches;
}

Vue.directive('dialogDrag', {
  bind(el, binding, vnode, oldVnode) {
    // 显式传 false 时完全不启用（移动端走这条分支）
    if (binding.value === false) {
      return;
    }
    // 触屏设备即便没传参数也不启用，避免误拖
    if (binding.value === undefined && isTouchOnlyDevice()) {
      return;
    }

    const dialogHeaderEl = el.querySelector('.el-dialog__header');
    const dragDom = el.querySelector('.el-dialog');
    if (!dialogHeaderEl || !dragDom) {
      return;
    }
    dialogHeaderEl.style.cursor = 'move';

    const sty = dragDom.currentStyle || window.getComputedStyle(dragDom, null);

    dialogHeaderEl.onmousedown = (e) => {
      const disX = e.clientX - dialogHeaderEl.offsetLeft;
      const disY = e.clientY - dialogHeaderEl.offsetTop;

      let styL, styT;

      if (sty.left.includes('%')) {
        styL = +document.body.clientWidth * (+sty.left.replace(/\%/g, '') / 100);
        styT = +document.body.clientHeight * (+sty.top.replace(/\%/g, '') / 100);
      } else {
        styL = +sty.left.replace(/\px/g, '');
        styT = +sty.top.replace(/\px/g, '');
      }

      document.onmousemove = function (e) {
        const l = e.clientX - disX;
        const t = e.clientY - disY;

        dragDom.style.left = `${l + styL}px`;
        dragDom.style.top = `${t + styT}px`;

        //将此时的位置传出去
        //binding.value({x:e.pageX,y:e.pageY})
      };

      document.onmouseup = function (e) {
        document.onmousemove = null;
        document.onmouseup = null;
      }
    }
  }
})

/**
 * v-contextmenu-longpress 长按模拟右键菜单
 *
 * 手机端没有右键，数据库控制台的 SQL 编辑区（@contextmenu）和文件管理的
 * el-tree（@node-contextmenu）在移动端原本完全无法触发。这里把 touch 长按
 * 合成为一次 contextmenu 事件派发出去，让既有的右键逻辑在移动端复用。
 *
 * 用法（加在需要支持的容器上，事件会冒泡给内部的 contextmenu 监听器）：
 *   <div v-contextmenu-longpress>...</div>
 */
Vue.directive('contextmenuLongpress', {
  bind(el, binding) {
    const duration = (binding.value && binding.value.duration) || 500;
    const moveThreshold = (binding.value && binding.value.moveThreshold) || 12;
    let timer = null;
    let startX = 0;
    let startY = 0;

    const clearTimer = () => {
      if (timer) {
        clearTimeout(timer);
        timer = null;
      }
    };

    const onTouchStart = (event) => {
      if (!event.touches || event.touches.length !== 1) {
        clearTimer();
        return;
      }
      const touch = event.touches[0];
      startX = touch.clientX;
      startY = touch.clientY;
      clearTimer();
      timer = setTimeout(() => {
        timer = null;
        const target = touch.target || el;
        // contextmenu 监听器可能挂在元素自身或祖先上，直接派发并让其冒泡
        const contextmenuEvent = new MouseEvent('contextmenu', {
          bubbles: true,
          cancelable: true,
          view: window,
          clientX: startX,
          clientY: startY
        });
        target.dispatchEvent(contextmenuEvent);
      }, duration);
    };

    const onTouchMove = (event) => {
      if (!timer || !event.touches || event.touches.length !== 1) {
        return;
      }
      const touch = event.touches[0];
      // 手指移动超过阈值说明是滚动/滑动，取消长按
      if (Math.abs(touch.clientX - startX) > moveThreshold
        || Math.abs(touch.clientY - startY) > moveThreshold) {
        clearTimer();
      }
    };

    el.__onTouchStartLongpress = onTouchStart;
    el.__onTouchMoveLongpress = onTouchMove;
    el.__clearLongpress = clearTimer;

    el.addEventListener('touchstart', onTouchStart, { passive: true });
    el.addEventListener('touchmove', onTouchMove, { passive: true });
    el.addEventListener('touchend', clearTimer);
    el.addEventListener('touchcancel', clearTimer);
  },
  unbind(el) {
    if (el.__onTouchStartLongpress) {
      el.removeEventListener('touchstart', el.__onTouchStartLongpress);
    }
    if (el.__onTouchMoveLongpress) {
      el.removeEventListener('touchmove', el.__onTouchMoveLongpress);
    }
    if (el.__clearLongpress) {
      el.removeEventListener('touchend', el.__clearLongpress);
      el.removeEventListener('touchcancel', el.__clearLongpress);
    }
    delete el.__onTouchStartLongpress;
    delete el.__onTouchMoveLongpress;
    delete el.__clearLongpress;
  }
})
