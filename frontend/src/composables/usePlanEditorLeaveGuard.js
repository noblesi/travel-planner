import { onBeforeUnmount, onMounted } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate } from 'vue-router'

const UNSAVED_CHANGES_MESSAGE =
  '저장되지 않은 변경사항이 있습니다. 이 화면을 나가면 입력한 내용이 사라질 수 있습니다. 그래도 나갈까요?'

export function usePlanEditorLeaveGuard({
  isSaving,
  hasUnsavedChanges,
  waitForPendingSaves,
}) {
  function handleBeforeUnload(event) {
    if (!isSaving.value && !hasUnsavedChanges.value) return

    event.preventDefault()
    event.returnValue = ''
  }

  async function confirmNavigation() {
    if (isSaving.value) await waitForPendingSaves()
    if (hasUnsavedChanges.value) return window.confirm(UNSAVED_CHANGES_MESSAGE)
    return true
  }

  onBeforeRouteLeave(confirmNavigation)
  onBeforeRouteUpdate(confirmNavigation)

  onMounted(() => window.addEventListener('beforeunload', handleBeforeUnload))
  onBeforeUnmount(() => window.removeEventListener('beforeunload', handleBeforeUnload))
}
