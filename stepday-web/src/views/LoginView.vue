<script setup lang="ts">
import { shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { api, setCredentials, clearCredentials } from '@/api/client'

const router = useRouter()
const user = shallowRef('')
const password = shallowRef('')
const error = shallowRef<string | null>(null)

async function submit() {
  setCredentials(user.value, password.value)
  try {
    await api('/me')
    await router.replace('/')
  } catch {
    clearCredentials()
    error.value = 'Nieprawidłowy login lub hasło'
  }
}
</script>

<template>
  <main class="screen login">
    <div class="brand display">Stepday</div>
    <h1 class="title display">Zaloguj się</h1>
    <form class="form" @submit.prevent="submit">
      <label for="login-user" class="section-label">Login</label>
      <input id="login-user" v-model="user" class="field" autocomplete="username" autocapitalize="off" required />
      <label for="login-pass" class="section-label">Hasło</label>
      <input id="login-pass" v-model="password" class="field" type="password" autocomplete="current-password" required />
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <button type="submit" class="btn-primary">Zaloguj</button>
    </form>
  </main>
</template>

<style scoped>
.login { gap: 16px; padding-top: 24px; }
.brand { font-size: 15px; font-weight: 700; color: var(--accent-ink); }
.title { margin: 0; font-size: 32px; }
.form { display: flex; flex-direction: column; gap: 8px; }
.field { height: 52px; border-radius: 14px; border: 1px solid var(--border); background: var(--surface); padding: 0 14px; font-size: 16px; margin-bottom: 8px; }
.error { color: var(--miss); font-size: 14px; margin: 0; }
</style>
