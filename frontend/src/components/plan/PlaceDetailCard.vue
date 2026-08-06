<script setup>
defineProps({
  place: {
    type: Object,
    required: true,
  },
  addDisabled: {
    type: Boolean,
    default: false,
  },
  existingTimeSlots: {
    type: Array,
    default: () => [],
  },
})

defineEmits(['add'])
</script>

<template>
  <article class="place-detail-card">
    <img v-if="place.imageUrl" :src="place.imageUrl" :alt="`${place.placeName} 이미지`" />
    <div v-else class="place-detail-card__image-empty" aria-hidden="true">⌖</div>

    <div class="place-detail-card__body">
      <span>{{ place.categoryName || '관광지' }}</span>
      <strong>{{ place.placeName }}</strong>
      <p>{{ place.address || '주소 정보가 제공되지 않았습니다.' }}</p>
      <small v-if="place.latitude == null || place.longitude == null">
        지도 좌표가 없는 장소입니다.
      </small>
      <div class="place-detail-card__actions" aria-label="선택 장소 일정 추가">
        <button type="button" :disabled="addDisabled || existingTimeSlots.includes('MORNING')" @click="$emit('add', 'MORNING')">
          {{ existingTimeSlots.includes('MORNING') ? '오전 등록됨' : '오전에 추가' }}
        </button>
        <button type="button" :disabled="addDisabled || existingTimeSlots.includes('AFTERNOON')" @click="$emit('add', 'AFTERNOON')">
          {{ existingTimeSlots.includes('AFTERNOON') ? '오후 등록됨' : '오후에 추가' }}
        </button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.place-detail-card {
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr);
  gap: 12px;
  padding: 12px;
  border: 1px solid #ffd5d1;
  border-radius: 14px;
  background: #fff8f7;
}

.place-detail-card > img,
.place-detail-card__image-empty {
  width: 84px;
  height: 78px;
  border-radius: 10px;
  object-fit: cover;
}

.place-detail-card__image-empty {
  display: grid;
  place-items: center;
  background: #ffe4e1;
  color: #e8443a;
  font-size: 24px;
}

.place-detail-card__body {
  min-width: 0;
}

.place-detail-card__body > span {
  display: block;
  overflow: hidden;
  color: #e8443a;
  font-size: 10px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.place-detail-card__body > strong {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: #1e293b;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.place-detail-card__body > p,
.place-detail-card__body > small {
  display: block;
  margin: 6px 0 0;
  color: #64748b;
  font-size: 11px;
  line-height: 1.4;
}

.place-detail-card__body > small {
  color: #b45309;
}

.place-detail-card__actions {
  display: flex;
  gap: 7px;
  margin-top: 10px;
}

.place-detail-card__actions button {
  min-height: 32px;
  padding: 0 10px;
  border: 0;
  border-radius: 9px;
  background: #ff5a4e;
  color: #fff;
  font-size: 10px;
  font-weight: 800;
  cursor: pointer;
}

.place-detail-card__actions button:last-child {
  background: #475569;
}

.place-detail-card__actions button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}
</style>
