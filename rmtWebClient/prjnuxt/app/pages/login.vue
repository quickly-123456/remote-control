<template>
    <div class="grid min-h-svh lg:grid-cols-2">
      <UContainer class="flex items-center justify-center min-h-screen">
        <UCard class="w-full max-w-md">
          <UForm :state="form" @submit="onSubmit" class="flex flex-col gap-6">
            <div class="flex flex-col items-center gap-2 text-center">
              <h1 class="text-2xl font-bold">Login to your account</h1>
              <p class="text-muted-foreground text-sm text-balance">
                Enter your phone below to login to your account
              </p>
            </div>
            <div class="grid gap-6">
              <div class="grid gap-3">
                <label for="phone" class="text-sm font-medium">phone</label>
                <UInput
                  id="phone"
                  v-model="form.phone"
                  placeholder="12312313"
                  type="text"
                  :disabled="loading"
                  required
                />
              </div>
              <div class="grid gap-3">
                <div class="flex items-center">
                  <label for="password" class="text-sm font-medium">Password</label>
                </div>
                <UInput
                  id="password"
                  v-model="form.password"
                  type="password"
                  :disabled="loading"
                  required
                />
              </div>
              <UButton
                type="submit"
                color="primary"
                class="w-full"
                :disabled="loading"
              >
                {{ loading ? "登录中..." : "Login" }}
              </UButton>
              <div v-if="error" class="text-red-500 text-sm text-center mt-2">
                {{ error }}
              </div>
            </div>
          </UForm>
        </UCard>
      </UContainer>
      </div>
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
      