<template><div class="admin-table"><slot /></div></template>

<style scoped>
.admin-table { overflow-x: auto; border: 1px solid var(--admin-border); border-radius: 7px; }
.admin-table :deep(table) { width: 100%; border-collapse: collapse; }
.admin-table :deep(thead) { background: #f4eee9; }
.admin-table :deep(th) { height: 48px; color: #5b6169; font-size: 13px; font-weight: 800; }
.admin-table :deep(td) { height: 54px; padding: 8px 14px; border-bottom: 1px solid var(--admin-border); color: #4a5058; font-size: 13px; text-align: center; }
.admin-table :deep(tbody tr:last-child td) { border-bottom: 0; }
</style>
