<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'

import PlanDateEditor from '@/components/plan/PlanDateEditor.vue'
import PlanMetadataEditor from '@/components/plan/PlanMetadataEditor.vue'
import { usePlanEditorStore } from '@/stores/planEditor'

defineProps({
  canManagePlan: {
    type: Boolean,
    default: true,
  },
})
const emit = defineEmits(['busy-change'])
const editorStore = usePlanEditorStore()
const { plan, isSaving } = storeToRefs(editorStore)

const activeEditor = ref(null)
const metadataBusy = ref(false)
const dateBusy = ref(false)
const busy = computed(() => metadataBusy.value || dateBusy.value)

function openEditor(editor) {
  activeEditor.value = editor
}

function closeEditor(editor) {
  if (activeEditor.value === editor) activeEditor.value = null
}

watch(busy, (value) => emit('busy-change', value), { immediate: true })
onBeforeUnmount(() => emit('busy-change', false))
</script>

<template>
  <div v-if="canManagePlan" class="plan-editor-settings">
    <PlanMetadataEditor
      :plan="plan"
      :open="activeEditor === 'metadata'"
      :disabled="isSaving || dateBusy"
      @request-open="openEditor('metadata')"
      @close="closeEditor('metadata')"
      @busy-change="metadataBusy = $event"
    />
    <PlanDateEditor
      :plan="plan"
      :open="activeEditor === 'dates'"
      :disabled="isSaving || metadataBusy"
      @request-open="openEditor('dates')"
      @close="closeEditor('dates')"
      @busy-change="dateBusy = $event"
    />
  </div>
</template>
