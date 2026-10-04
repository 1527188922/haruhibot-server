# WebUI mobile adaptation implementation plan

**Goal:** Implement the approved incremental mobile layout while reusing existing data and actions.
**Architecture:** Phone below 768px, compact navigation below 992px. Shared filter, pagination and record-card components; Element tables retain internal horizontal scrolling.
**Tech stack:** Vue 2 / Element UI / SCSS; no new dependencies.
**Spec:** Approved analysis in this chat (2026-10-04).

- [x] Add pure viewport, pagination and filter-count helpers with boundary and nonmutation tests.
- [x] Align mobile mixin and navigation, update viewport height changes, use phone form labels and scoped responsive styles. Remove global table minimum width; retain each table column's own width.
- [x] Add reusable collapsible query form and compact pagination; stack date range panels on phones and cap select/autocomplete overlays.
- [x] Add reusable phone record cards for groups, members, friends, subscriptions, chat and logs, preserving detail fields and actions. Keep subscription table selection synchronized.
- [x] Add visible file menu entry and touch target improvements.
- [x] Compile templates, compare lint with HEAD, run helper tests and production build. Exercise actual components with browser fixture data at 320/375/390/430/768/992px; record limitations.

No backend/API changes, no new UI framework, no deployment.

Validation: helper tests 3/3; browser fixture checks at 320/375/390/430/768/992px passed for group/friend/chat/subscription/log, plus member cards, filter reset, row action, selection synchronization, date panel, label offset, long group popup and long file menu. Final production build passed. No new source lint findings versus HEAD; repository baseline lint and bundle-size warnings remain. Independent read-only review found a file-menu flex overflow, reproduced and fixed with browser regression coverage.

Browser tests use real Vue/Element components with isolated fixture data and no backend requests. iOS Safari, WeChat browser, live backend integration, and real software-keyboard behavior have not been verified.

Run helper checks: node --test webui/tests/mobile-layout.test.cjs
Run browser checks: set PLAYWRIGHT_MODULE to an installed playwright module path, then node webui/tests/mobile-ui.cjs (uses installed Chrome; BROWSER_CHANNEL may select another installed channel).
