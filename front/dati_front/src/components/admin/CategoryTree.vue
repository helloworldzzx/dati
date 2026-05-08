<script setup>
defineProps({
  nodes: {
    type: Array,
    required: true,
  },
  expandedIds: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['toggle', 'edit', 'disable'])

function hasChildren(node) {
  return Array.isArray(node.children) && node.children.length > 0
}

function isExpanded(expandedIds, node) {
  return expandedIds.has(node.id)
}
</script>

<template>
  <div class="category-tree">
    <div v-for="node in nodes" :key="node.id" class="category-tree-item">
      <div class="category-tree-row">
        <button
          class="tree-toggle"
          type="button"
          :disabled="!hasChildren(node)"
          :aria-label="isExpanded(expandedIds, node) ? '收起分类' : '展开分类'"
          @click="emit('toggle', node)"
        >
          {{ hasChildren(node) ? (isExpanded(expandedIds, node) ? '▾' : '▸') : '' }}
        </button>

        <button class="category-tree-main" type="button" @click="hasChildren(node) && emit('toggle', node)">
          <span class="badge">L{{ node.level }}</span>
          <strong>{{ node.name }}</strong>
          <span :class="['badge', node.status === 'ENABLED' ? 'badge-success' : 'badge-danger']">
            {{ node.status }}
          </span>
        </button>

        <div class="table-actions">
          <button class="btn" type="button" @click="emit('edit', node)">编辑</button>
          <button class="btn btn-danger" type="button" @click="emit('disable', node)">禁用</button>
        </div>
      </div>

      <div v-if="hasChildren(node) && isExpanded(expandedIds, node)" class="category-tree-children">
        <CategoryTree
          :nodes="node.children"
          :expanded-ids="expandedIds"
          @toggle="emit('toggle', $event)"
          @edit="emit('edit', $event)"
          @disable="emit('disable', $event)"
        />
      </div>
    </div>
  </div>
</template>
