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
})

defineEmits(['move-up', 'move-down', 'move-time-slot', 'remove'])
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
