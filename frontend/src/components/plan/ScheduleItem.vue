<script setup>
const props = defineProps({
  item: {
    type: Object,
    required: true,
  },
  timeSlot: {
    type: String,
    required: true,
  },
  first: {
    type: Boolean,
    default: false,
  },
  last: {
    type: Boolean,
    default: false,
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  selected: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['select', 'drag-start', 'drag-end', 'drop-before', 'move-up', 'move-down', 'move-time-slot', 'remove'])

function startDrag(event) {
  if (props.disabled) return
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('text/plain', String(props.item.scheduleItemId))
  emit('drag-start', props.item)
}
</script>

<template>
  <article
    class="schedule-card"
    :class="{ 'schedule-card--selected': selected }"
    :aria-busy="disabled"
    :draggable="!disabled"
    tabindex="0"
    @click="emit('select', item)"
    @keydown.enter="emit('select', item)"
    @dragstart="startDrag"
    @dragend="emit('drag-end')"
    @dragover.prevent
    @drop.stop.prevent="emit('drop-before', item)"
  >
    <img v-if="item.imageUrl" :src="item.imageUrl" :alt="`${item.placeName} 이미지`" />
    <div v-else class="schedule-card__image-empty" aria-hidden="true">⌖</div>

    <div class="schedule-card__body">
      <span>{{ item.positionNo }}번째 · {{ item.categoryName || '장소' }}</span>
      <strong>{{ item.placeName }}</strong>
      <p v-if="item.address">{{ item.address }}</p>
      <p v-if="item.description" class="schedule-card__description">{{ item.description }}</p>
    </div>

    <div class="schedule-card__actions" :aria-label="`${item.placeName} 일정 편집`">
      <button
        type="button"
        :disabled="disabled || first"
        :aria-label="`${item.placeName} 순서 올리기`"
        @click.stop="$emit('move-up')"
      >
        ↑
      </button>
      <button
        type="button"
        :disabled="disabled || last"
        :aria-label="`${item.placeName} 순서 내리기`"
        @click.stop="$emit('move-down')"
      >
        ↓
      </button>
      <button
        type="button"
        :disabled="disabled"
        @click.stop="$emit('move-time-slot')"
      >
        {{ timeSlot === 'MORNING' ? '오후로' : '오전으로' }}
      </button>
      <button
        class="schedule-card__delete"
        type="button"
        :disabled="disabled"
        :aria-label="`${item.placeName} 일정 삭제`"
        @click.stop="$emit('remove')"
      >
        삭제
      </button>
    </div>
  </article>
</template>

<style scoped>
.schedule-card {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr);
  gap: 10px;
  padding: 10px;
  border: 1px solid #e3e8ef;
  border-radius: 13px;
  background: #fff;
  cursor: grab;
}
.schedule-card:active { cursor: grabbing; }
.schedule-card--selected { border-color: #ff776d; box-shadow: 0 0 0 3px rgb(255 90 78 / 12%); }

.schedule-card > img,
.schedule-card__image-empty {
  width: 58px;
  height: 58px;
  border-radius: 10px;
  object-fit: cover;
}

.schedule-card__image-empty {
  display: grid;
  place-items: center;
  background: #f1f5f9;
  color: #94a3b8;
  font-size: 20px;
}

.schedule-card__body {
  min-width: 0;
}

.schedule-card__body > span {
  color: #ff5a4e;
  font-size: 10px;
  font-weight: 800;
}

.schedule-card__body > strong,
.schedule-card__body > p {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.schedule-card__body > strong {
  margin-top: 4px;
  color: #1e293b;
  font-size: 13px;
}

.schedule-card__body > p {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 10px;
}

.schedule-card__description {
  color: #475569 !important;
}

.schedule-card__actions {
  display: flex;
  grid-column: 1 / -1;
  gap: 6px;
}

.schedule-card__actions button {
  min-height: 30px;
  padding: 0 9px;
  border: 1px solid #dbe2ea;
  border-radius: 8px;
  background: #f8fafc;
  color: #475569;
  font-size: 10px;
  font-weight: 750;
  cursor: pointer;
}

.schedule-card__actions button:disabled {
  cursor: not-allowed;
  opacity: 0.38;
}

.schedule-card__actions .schedule-card__delete {
  margin-left: auto;
  border-color: #fecaca;
  background: #fff1f2;
  color: #b91c1c;
}
</style>
