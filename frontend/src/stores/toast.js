import { ref } from 'vue'
import { defineStore } from 'pinia'

const DEFAULT_DURATION = 4000
const VALID_TYPES = new Set(['success', 'error', 'info'])

export const useToastStore = defineStore('toast', () => {
  const toasts = ref([])
  const timers = new Map()
  let nextId = 1

  function dismiss(id) {
    toasts.value = toasts.value.filter((toast) => toast.id !== id)
    const timer = timers.get(id)
    if (timer !== undefined) clearTimeout(timer)
    timers.delete(id)
  }

  function show({
    message,
    type = 'info',
    duration = DEFAULT_DURATION,
    actionLabel = '',
    action = null,
  }) {
    const normalizedMessage = typeof message === 'string' ? message.trim() : ''
    if (!normalizedMessage) return null

    const id = nextId++
    const toast = {
      id,
      message: normalizedMessage,
      type: VALID_TYPES.has(type) ? type : 'info',
    }
    if (typeof action === 'function' && typeof actionLabel === 'string' && actionLabel.trim()) {
      toast.actionLabel = actionLabel.trim()
      toast.action = action
    }
    toasts.value.push(toast)

    if (duration > 0) {
      timers.set(id, setTimeout(() => dismiss(id), duration))
    }

    return id
  }

  function success(message, options = {}) {
    return show({ ...options, message, type: 'success' })
  }

  function error(message, options = {}) {
    return show({ ...options, message, type: 'error' })
  }

  function info(message, options = {}) {
    return show({ ...options, message, type: 'info' })
  }

  function clear() {
    timers.forEach((timer) => clearTimeout(timer))
    timers.clear()
    toasts.value = []
  }

  return { toasts, show, success, error, info, dismiss, clear }
})
