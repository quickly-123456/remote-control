<template>
  <UContainer class="flex items-center justify-center min-h-screen">
    <UCard class="w-full max-w-md">
      <template #header>
        <h2 class="text-2xl font-bold text-center">登录</h2>
      </template>
      <UForm :state="form" @submit="onSubmit" class="space-y-4">
        <UFormField label="手机号" name="phone" required>
          <UInput
            v-model="form.phone"
            placeholder="请输入手机号"
            type="text"
            :disabled="loading"
            required
          />
        </UFormField>
        <UFormField label="密码" name="password" required>
          <UInput
            v-model="form.password"
            placeholder="请输入密码"
            type="password"
            :disabled="loading"
            required
          />
        </UFormField>
        <UButton
          type="submit"
          color="primary"
          block
          :loading="loading"
        >
          登录
        </UButton>
        <UAlert v-if="error" color="error" variant="soft" class="mt-2">
          {{ error }}
        </UAlert>
      </UForm>
    </UCard>
  </UContainer>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const form = ref({
  phone: '',
  password: ''
})
const loading = ref(false)
const error = ref('')

const onSubmit = async () => {
  error.value = ''
  loading.value = true
  try {
    // 调用后端API
    const res = await $fetch('http://185.128.227.222:5558/api/login', {
      method: 'POST',
      body: {
        phone: form.value.phone,
        password: form.value.password
      }
    }) as { result: number; message?: string }
    if (res.result === 0) {
      // 登录成功，跳转首页或其他页面
      navigateTo('/')
    } else {
      error.value = res.message || '登录失败'
    }
  } catch (e: any) {
    error.value = e?.data?.message || e.message || '网络错误'
  } finally {
    loading.value = false
  }
}
</script>
  