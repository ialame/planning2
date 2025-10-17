<template>
  <div class="groups-navigation">
    <nav class="navigation-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        @click="$emit('tab-change', tab.id)"
        :class="['nav-tab', { active: tab.id === activeTab }]"
      >
        {{ tab.label }}
      </button>
    </nav>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  activeTab?: string;
}>();

defineEmits<{
  (e: 'tab-change', tabId: string): void;
}>();

const tabs = [
  { id: 'list', label: 'Teams List' },
  { id: 'create', label: 'Create Team' }
];
</script>

<style scoped>
.groups-navigation {
  margin-bottom: 1rem;
}

.navigation-tabs {
  display: flex;
  gap: 0.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.nav-tab {
  padding: 0.75rem 1.5rem;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  font-weight: 500;
  color: #6b7280;
  transition: all 0.2s;
}

.nav-tab:hover {
  color: #3b82f6;
}

.nav-tab.active {
  color: #3b82f6;
  border-bottom-color: #3b82f6;
}
</style>
