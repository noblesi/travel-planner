<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  regions: {
    type: Array,
    required: true,
  },
  modelValue: {
    type: String,
    default: '',
  },
  error: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['change', 'update:modelValue'])

const regionPicker = ref(null)
const regionTrigger = ref(null)
const dropdownOpen = ref(false)
const activeIndex = ref(-1)

const selectedRegion = computed(() =>
  props.regions.find((region) => region.regionCode === props.modelValue),
)
const activeRegionId = computed(() =>
  dropdownOpen.value && activeIndex.value >= 0 ? `region-option-${activeIndex.value}` : undefined,
)

function openDropdown(preferLast = false) {
  if (props.regions.length === 0) return

  const selectedIndex = props.regions.findIndex((region) => region.regionCode === props.modelValue)
  activeIndex.value = selectedIndex >= 0 ? selectedIndex : preferLast ? props.regions.length - 1 : 0
  dropdownOpen.value = true
}

function closeDropdown() {
  dropdownOpen.value = false
}

function toggleDropdown() {
  if (dropdownOpen.value) closeDropdown()
  else openDropdown()
}

function moveActiveRegion(offset) {
  if (!dropdownOpen.value) {
    openDropdown(offset < 0)
    return
  }
  activeIndex.value = (activeIndex.value + offset + props.regions.length) % props.regions.length
}

function selectRegion(index) {
  const region = props.regions[index]
  if (!region) return

  emit('update:modelValue', region.regionCode)
  emit('change')
  activeIndex.value = index
  closeDropdown()
  nextTick(() => regionTrigger.value?.focus())
}

function handleKeydown(event) {
  if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
    event.preventDefault()
    moveActiveRegion(event.key === 'ArrowDown' ? 1 : -1)
    return
  }
  if (event.key === 'Home' || event.key === 'End') {
    event.preventDefault()
    if (!dropdownOpen.value) openDropdown()
    activeIndex.value = event.key === 'Home' ? 0 : props.regions.length - 1
    return
  }
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    if (dropdownOpen.value) selectRegion(activeIndex.value)
    else openDropdown()
    return
  }
  if (event.key === 'Escape' && dropdownOpen.value) {
    event.preventDefault()
    closeDropdown()
  }
  if (event.key === 'Tab') closeDropdown()
}

function handleOutsidePointerDown(event) {
  if (!regionPicker.value?.contains(event.target)) closeDropdown()
}

watch(
  () => props.regions,
  () => {
    if (activeIndex.value >= props.regions.length) activeIndex.value = -1
  },
  { deep: true },
)

onMounted(() => document.addEventListener('pointerdown', handleOutsidePointerDown))
onBeforeUnmount(() => document.removeEventListener('pointerdown', handleOutsidePointerDown))
</script>

<template>
  <div class="form-field">
    <label id="regionCode-label" for="regionCode">어디로 떠나시나요?</label>
    <div ref="regionPicker" class="region-picker">
      <button
        id="regionCode"
        ref="regionTrigger"
        class="region-select"
        :class="{ 'region-select--open': dropdownOpen }"
        type="button"
        role="combobox"
        aria-autocomplete="none"
        aria-haspopup="listbox"
        aria-controls="region-options"
        :aria-activedescendant="activeRegionId"
        :aria-describedby="error ? 'regionCode-error' : undefined"
        :aria-expanded="dropdownOpen"
        :aria-invalid="Boolean(error)"
        aria-labelledby="regionCode-label regionCode-value"
        @click="toggleDropdown"
        @keydown="handleKeydown"
      >
        <span
          id="regionCode-value"
          class="region-select__value"
          :class="{ 'region-select__value--placeholder': !selectedRegion }"
        >
          {{ selectedRegion?.regionName || '여행지역을 선택해 주세요' }}
        </span>
        <span class="region-select__chevron" aria-hidden="true" />
      </button>

      <Transition name="region-options">
        <ul
          v-if="dropdownOpen"
          id="region-options"
          class="region-options"
          role="listbox"
          aria-labelledby="regionCode-label"
        >
          <li
            v-for="(region, index) in regions"
            :id="`region-option-${index}`"
            :key="region.regionCode"
            class="region-option"
            :class="{
              'region-option--active': activeIndex === index,
              'region-option--selected': modelValue === region.regionCode,
            }"
            role="option"
            :aria-selected="modelValue === region.regionCode"
            @pointermove="activeIndex = index"
            @mousedown.prevent
            @click="selectRegion(index)"
          >
            <span>{{ region.regionName }}</span>
            <span
              v-if="modelValue === region.regionCode"
              class="region-option__check"
              aria-hidden="true"
            >
              ✓
            </span>
          </li>
        </ul>
      </Transition>
    </div>
    <p v-if="error" id="regionCode-error" class="field-error" role="alert">{{ error }}</p>
  </div>
</template>

<style scoped>
.form-field {
  display: grid;
  gap: 9px;
}
.form-field label {
  color: #1f2937;
  font-size: 15px;
  font-weight: 750;
}
.region-picker {
  position: relative;
}
.region-select {
  display: flex;
  width: 100%;
  min-height: 54px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 16px;
  color: #111827;
  border: 1px solid #d7dce3;
  border-radius: 14px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  outline: none;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease;
}
.region-select:hover:not(:disabled) {
  border-color: #b9c1cd;
}
.region-select:focus-visible,
.region-select--open {
  border-color: #ff5a4e;
  box-shadow: 0 0 0 4px rgb(255 90 78 / 13%);
}
.region-select[aria-invalid='true'] {
  border-color: #dc2626;
}
.region-select__value {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.region-select__value--placeholder {
  color: #94a3b8;
}
.region-select__chevron {
  width: 9px;
  height: 9px;
  flex: 0 0 auto;
  border-right: 2px solid #64748b;
  border-bottom: 2px solid #64748b;
  transform: translateY(-2px) rotate(45deg);
  transition: transform 160ms ease;
}
.region-select--open .region-select__chevron {
  transform: translateY(2px) rotate(225deg);
}
.region-options {
  position: absolute;
  z-index: 30;
  top: calc(100% + 8px);
  right: 0;
  left: 0;
  max-height: 280px;
  margin: 0;
  padding: 8px;
  overflow-y: auto;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 18px 48px rgb(15 23 42 / 16%);
  list-style: none;
  overscroll-behavior: contain;
  scrollbar-color: #cbd5e1 transparent;
  scrollbar-width: thin;
}
.region-option {
  display: flex;
  min-height: 42px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 12px;
  color: #334155;
  border-radius: 10px;
  cursor: pointer;
  transition:
    color 120ms ease,
    background 120ms ease;
}
.region-option--active {
  color: #d83a31;
  background: #fff1f0;
}
.region-option--selected {
  color: #d83a31;
  font-weight: 750;
}
.region-option__check {
  color: #ff5a4e;
  font-size: 16px;
  font-weight: 800;
}
.region-options-enter-active,
.region-options-leave-active {
  transition:
    opacity 140ms ease,
    transform 140ms ease;
  transform-origin: top;
}
.region-options-enter-from,
.region-options-leave-to {
  opacity: 0;
  transform: translateY(-5px) scale(0.99);
}
.field-error {
  margin: 0;
  color: #b91c1c;
  font-size: 13px;
  line-height: 1.5;
}
@media (prefers-reduced-motion: reduce) {
  .region-select,
  .region-select__chevron,
  .region-option,
  .region-options-enter-active,
  .region-options-leave-active {
    transition: none;
  }
}
</style>
