<script setup>
import ScheduleItem from '@/components/plan/ScheduleItem.vue'

defineProps({
  items: {
    type: Array,
    default: () => [],
  },
  timeSlot: {
    type: String,
    required: true,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  selectedScheduleItemId: {
    type: [String, Number],
    default: null,
  },
})

defineEmits(['select', 'drag-start', 'drag-end', 'drop-before', 'move-up', 'move-down', 'move-time-slot', 'remove'])
</script>

<template>
  <div class="schedule-list">
    <ScheduleItem
      v-for="(item, index) in items"
      :key="item.scheduleItemId"
      :item="item"
      :time-slot="timeSlot"
      :first="index === 0"
      :last="index === items.length - 1"
      :disabled="disabled"
      :selected="String(item.scheduleItemId) === String(selectedScheduleItemId)"
      @select="$emit('select', item)"
      @drag-start="$emit('drag-start', item)"
      @drag-end="$emit('drag-end')"
      @drop-before="$emit('drop-before', item)"
      @move-up="$emit('move-up', item)"
      @move-down="$emit('move-down', item)"
      @move-time-slot="$emit('move-time-slot', item)"
      @remove="$emit('remove', item)"
    />
  </div>
</template>

<style scoped>
.schedule-list {
  display: grid;
  gap: 8px;
}
</style>
