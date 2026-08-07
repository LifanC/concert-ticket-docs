<script setup>
import {dateConversionYMDhms} from "@/components/componentsJs/ConvertPadding";

const date = ref('')

setInterval(() => {
  date.value = dateConversionYMDhms(true)
}, 1000)

const handleSelect = (url) => {
  location.href = "/" + url
}

</script>

<template>
  <el-container>
    <el-header>
      <el-breadcrumb separator="*">
        <el-breadcrumb-item :to="{ path: '/' }">活動</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/booking' }">訂票</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/user' }">會員</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/admin' }">管理後台</el-breadcrumb-item>
        <el-breadcrumb-item></el-breadcrumb-item>
      </el-breadcrumb>
      <el-text>{{ date }}</el-text>
    </el-header>
    <el-container>
      <el-main>
        <router-view/>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>

</style>
