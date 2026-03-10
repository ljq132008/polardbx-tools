<template>
  <div id="app">
    <el-container style="height: 100vh">
      <el-aside width="200px">
        <el-menu
          :default-active="$route.path"
          router
          class="el-menu-vertical-demo"
        >
          <div class="logo">
            <h3>Batch Tool</h3>
          </div>
          <el-menu-item index="/">
            <el-icon><List /></el-icon>
            <span>任务列表</span>
          </el-menu-item>
          <el-menu-item index="/batch">
            <el-icon><Console /></el-icon>
            <span>命令执行</span>
          </el-menu-item>
          <el-menu-item index="/rule-groups">
            <el-icon><Folder /></el-icon>
            <span>规则组管理</span>
          </el-menu-item>
          <el-menu-item index="/migration-rules">
            <el-icon><Document /></el-icon>
            <span>迁移规则</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header>
          <div class="header-title">PolarDB-X 数据库批量操作工具</div>
        </el-header>
        <el-main>
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

#app {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.logo {
  padding: 20px;
  background-color: #001529;
  color: white;
  text-align: center;
}

.logo h3 {
  margin: 0;
}

.el-header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-title {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.el-aside {
  background-color: #001529;
}

.el-menu {
  border-right: none;
}

.el-main {
  background-color: #f0f2f5;
  padding: 20px;
}

/* Element Plus 暗色菜单样式 */
.el-menu-vertical-demo {
  background-color: #001529;
}

.el-menu-vertical-demo .el-menu-item {
  color: #fff;
}

.el-menu-vertical-demo .el-menu-item:hover,
.el-menu-vertical-demo .el-menu-item.is-active {
  background-color: #1890ff;
}
</style>
