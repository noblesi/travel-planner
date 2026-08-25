import { onBeforeUnmount, ref } from 'vue'

import { readLocalStorage, writeLocalStorage } from '@/utils/browserStorage'

const PANEL_WIDTH_STORAGE_KEY = 'planEditorPanelWidth'
const DEFAULT_PANEL_WIDTH = 430
const MIN_PANEL_WIDTH = 340
const MAX_PANEL_WIDTH = 560
const KEYBOARD_RESIZE_STEP = 20

export function clampPlanEditorPanelWidth(width) {
  return Math.min(MAX_PANEL_WIDTH, Math.max(MIN_PANEL_WIDTH, width))
}

export function resolveStoredPlanEditorPanelWidth(storedValue) {
  if (storedValue === null || String(storedValue).trim() === '') return DEFAULT_PANEL_WIDTH

  const width = Number(storedValue)
  if (!Number.isFinite(width)) return DEFAULT_PANEL_WIDTH
  return clampPlanEditorPanelWidth(width)
}

export function usePlanEditorPanelResize() {
  const schedulePanelWidth = ref(
    resolveStoredPlanEditorPanelWidth(readLocalStorage(PANEL_WIDTH_STORAGE_KEY)),
  )
  const resizingPanel = ref(false)

  function resizePanelTo(clientX) {
    schedulePanelWidth.value = clampPlanEditorPanelWidth(clientX)
  }

  function handlePanelResize(event) {
    if (resizingPanel.value) resizePanelTo(event.clientX)
  }

  function stopPanelResize() {
    if (!resizingPanel.value) return

    resizingPanel.value = false
    writeLocalStorage(PANEL_WIDTH_STORAGE_KEY, String(schedulePanelWidth.value))
    document.removeEventListener('pointermove', handlePanelResize)
    document.removeEventListener('pointerup', stopPanelResize)
  }

  function startPanelResize(event) {
    resizingPanel.value = true
    resizePanelTo(event.clientX)
    document.addEventListener('pointermove', handlePanelResize)
    document.addEventListener('pointerup', stopPanelResize, { once: true })
  }

  function adjustPanelWidth(event) {
    if (!['ArrowLeft', 'ArrowRight'].includes(event.key)) return

    event.preventDefault()
    const direction = event.key === 'ArrowRight' ? 1 : -1
    schedulePanelWidth.value = clampPlanEditorPanelWidth(
      schedulePanelWidth.value + direction * KEYBOARD_RESIZE_STEP,
    )
    writeLocalStorage(PANEL_WIDTH_STORAGE_KEY, String(schedulePanelWidth.value))
  }

  onBeforeUnmount(() => {
    resizingPanel.value = false
    document.removeEventListener('pointermove', handlePanelResize)
    document.removeEventListener('pointerup', stopPanelResize)
  })

  return {
    schedulePanelWidth,
    startPanelResize,
    adjustPanelWidth,
  }
}
