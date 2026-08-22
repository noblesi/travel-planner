import { computed, ref } from 'vue'

import {
  isScheduleConflict,
  planEditorErrorMessage,
  scheduleSaveErrorMessage,
  shouldRefreshScheduleAfterError,
} from '@/stores/planEditor/errors'

export function createPlanEditorSaveCoordinator({
  applyScheduleResult,
  refreshEditor,
}) {
  const saveStatus = ref('idle')
  const saveMessage = ref('자동 저장 준비')
  const saveErrorMessage = ref('')
  const schedulePendingSaveCount = ref(0)
  const directPendingSaveCount = ref(0)
  const directSaveFailed = ref(false)
  const lastFailedOperation = ref(null)

  let queueTail = Promise.resolve()
  const directSavePromises = new Set()
  let generation = 0

  const pendingSaveCount = computed(
    () => schedulePendingSaveCount.value + directPendingSaveCount.value,
  )
  const isSaving = computed(() => pendingSaveCount.value > 0)
  const hasSaveError = computed(
    () => saveStatus.value === 'error' || saveStatus.value === 'conflict',
  )
  const hasUnsavedChanges = computed(
    () => hasSaveError.value || lastFailedOperation.value != null || directSaveFailed.value,
  )
  const canRetrySave = computed(() => lastFailedOperation.value != null)

  function reset() {
    generation += 1
    queueTail = Promise.resolve()
    directSavePromises.clear()
    lastFailedOperation.value = null
    schedulePendingSaveCount.value = 0
    directPendingSaveCount.value = 0
    directSaveFailed.value = false
    saveStatus.value = 'idle'
    saveMessage.value = '자동 저장 준비'
    saveErrorMessage.value = ''
    return generation
  }

  function isCurrent(operationGeneration) {
    return operationGeneration === generation
  }

  function trackDirectSave(label, operation, { updateSaveState = true } = {}) {
    const operationGeneration = generation
    const previousScheduleFailure = lastFailedOperation.value
      ? {
          status: saveStatus.value,
          message: saveMessage.value,
        }
      : null
    directPendingSaveCount.value += 1
    if (updateSaveState) {
      directSaveFailed.value = false
      saveStatus.value = 'saving'
      saveMessage.value = label
    }

    const task = queueTail.then(async () => {
      if (!isCurrent(operationGeneration)) return null

      try {
        const result = await operation(() => isCurrent(operationGeneration))
        if (!isCurrent(operationGeneration)) return null

        if (updateSaveState) {
          if (previousScheduleFailure && lastFailedOperation.value) {
            saveStatus.value = previousScheduleFailure.status
            saveMessage.value = previousScheduleFailure.message
          } else {
            saveStatus.value = 'saved'
            saveMessage.value = '모든 변경사항이 저장되었습니다.'
          }
        }
        return result
      } catch (error) {
        if (!isCurrent(operationGeneration)) return null

        if (updateSaveState) {
          directSaveFailed.value = true
          if (previousScheduleFailure && lastFailedOperation.value) {
            saveStatus.value = previousScheduleFailure.status
            saveMessage.value = previousScheduleFailure.message
          } else {
            saveStatus.value = 'error'
            saveMessage.value = '변경사항 저장 실패'
            saveErrorMessage.value = planEditorErrorMessage(error)
          }
        }
        throw error
      }
    })
    queueTail = task.catch(() => undefined)
    directSavePromises.add(task)

    return task.finally(() => {
      directSavePromises.delete(task)
      if (isCurrent(operationGeneration)) {
        directPendingSaveCount.value = Math.max(0, directPendingSaveCount.value - 1)
      }
    })
  }

  async function executeScheduleOperation(operation, operationGeneration) {
    if (!isCurrent(operationGeneration)) return null

    saveStatus.value = 'saving'
    saveMessage.value = `자동 저장 중 · ${operation.label}`
    if (!lastFailedOperation.value || lastFailedOperation.value === operation) {
      saveErrorMessage.value = ''
    }

    try {
      const result = await operation.run()
      if (!isCurrent(operationGeneration)) return null

      if (result?.editor) applyScheduleResult(result.editor, operation.preferredDayId)
      if (lastFailedOperation.value === operation) lastFailedOperation.value = null
      saveStatus.value = 'saved'
      saveMessage.value = '모든 변경사항이 자동 저장되었습니다.'
      return result
    } catch (error) {
      if (!isCurrent(operationGeneration)) return null

      const code = error?.response?.data?.code
      let refreshed = false
      if (shouldRefreshScheduleAfterError(code)) {
        try {
          await refreshEditor(
            operation.preferredDayId,
            () => isCurrent(operationGeneration),
          )
          refreshed = true
        } catch {
          refreshed = false
        }
      }

      if (!isCurrent(operationGeneration)) return null

      saveStatus.value = isScheduleConflict(code) ? 'conflict' : 'error'
      saveMessage.value = saveStatus.value === 'conflict' ? '충돌 복구 필요' : '자동 저장 실패'
      saveErrorMessage.value = scheduleSaveErrorMessage(error, refreshed)
      lastFailedOperation.value = code === 'DUPLICATE_OPERATION' ? null : operation
      throw error
    }
  }

  function enqueueScheduleOperation(operation) {
    const operationGeneration = generation
    schedulePendingSaveCount.value += 1
    saveStatus.value = 'saving'
    saveMessage.value = `자동 저장 대기 · ${schedulePendingSaveCount.value}건`

    const task = queueTail.then(() => executeScheduleOperation(operation, operationGeneration))
    queueTail = task.catch(() => undefined)

    return task.finally(() => {
      if (!isCurrent(operationGeneration)) return

      schedulePendingSaveCount.value = Math.max(0, schedulePendingSaveCount.value - 1)
      if (schedulePendingSaveCount.value > 0) {
        saveStatus.value = 'saving'
        saveMessage.value = `자동 저장 대기 · ${schedulePendingSaveCount.value}건`
      } else if (lastFailedOperation.value && saveStatus.value !== 'conflict') {
        saveStatus.value = 'error'
        saveMessage.value = '자동 저장 실패'
      }
    })
  }

  function retryLastSave() {
    if (!lastFailedOperation.value) return Promise.resolve(null)

    const operation = lastFailedOperation.value
    lastFailedOperation.value = null
    return enqueueScheduleOperation(operation)
  }

  function discardFailedSave() {
    lastFailedOperation.value = null
    directSaveFailed.value = false
    saveErrorMessage.value = ''
    saveStatus.value = 'idle'
    saveMessage.value = '자동 저장 준비'
  }

  function clearDirectSaveFailure() {
    if (!directSaveFailed.value) return

    directSaveFailed.value = false
    if (lastFailedOperation.value) return
    saveErrorMessage.value = ''
    saveStatus.value = 'idle'
    saveMessage.value = '자동 저장 준비'
  }

  async function waitForPendingSaves() {
    while (schedulePendingSaveCount.value > 0 || directPendingSaveCount.value > 0) {
      await Promise.allSettled([queueTail, ...directSavePromises])
    }
    return !hasUnsavedChanges.value
  }

  return {
    saveStatus,
    saveMessage,
    saveErrorMessage,
    pendingSaveCount,
    isSaving,
    hasSaveError,
    hasUnsavedChanges,
    canRetrySave,
    reset,
    isCurrent,
    trackDirectSave,
    enqueueScheduleOperation,
    retryLastSave,
    discardFailedSave,
    clearDirectSaveFailure,
    waitForPendingSaves,
  }
}
