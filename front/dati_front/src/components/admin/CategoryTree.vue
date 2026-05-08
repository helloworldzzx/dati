<script setup>
import { Delete as DeleteIcon, Edit, SwitchButton } from '@element-plus/icons-vue'

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

const emit = defineEmits(['toggle', 'edit', 'disable', 'delete'])

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
          <el-button size="small" :icon="Edit" @click="emit('edit', node)">编辑</el-button>
          <el-button size="small" type="danger" plain :icon="SwitchButton" @click="emit('disable', node)">
            禁用
          </el-button>
          <el-button size="small" type="danger" :icon="DeleteIcon" @click="emit('delete', node)">
            删除
          </el-button>
        </div>
      </div>

      <div v-if="hasChildren(node) && isExpanded(expandedIds, node)" class="category-tree-children">
        <CategoryTree
          :nodes="node.children"
          :expanded-ids="expandedIds"
          @toggle="emit('toggle', $event)"
          @edit="emit('edit', $event)"
          @disable="emit('disable', $event)"
          @delete="emit('delete', $event)"
        />
      </div>
    </div>
  </div>
</template>
