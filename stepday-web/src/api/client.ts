// Klient API: Basic auth (tryb jednego użytkownika, docs/decisions.md A2).

const BASE = '/api'
const AUTH_KEY = 'stepday.auth'

export class ApiError extends Error {
  readonly status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

function readAuth(): string | null {
  try {
    return localStorage.getItem(AUTH_KEY)
  } catch {
    return null
  }
}

export function setCredentials(user: string, password: string): void {
  try {
    localStorage.setItem(AUTH_KEY, btoa(unescape(encodeURIComponent(`${user}:${password}`))))
  } catch {
    /* tryb prywatny: logowanie tylko na tę sesję */
  }
  sessionAuth = btoa(unescape(encodeURIComponent(`${user}:${password}`)))
}

export function clearCredentials(): void {
  sessionAuth = null
  try {
    localStorage.removeItem(AUTH_KEY)
  } catch {
    /* brak dostępu do storage */
  }
}

let sessionAuth: string | null = readAuth()

export function hasCredentials(): boolean {
  return sessionAuth !== null
}

let onUnauthorized: () => void = () => {}
export function setUnauthorizedHandler(handler: () => void): void {
  onUnauthorized = handler
}

export async function api<T>(path: string, init: { method?: string; body?: unknown } = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (sessionAuth) headers.Authorization = `Basic ${sessionAuth}`
  if (init.body !== undefined) headers['Content-Type'] = 'application/json'
  const res = await fetch(BASE + path, {
    method: init.method ?? 'GET',
    headers,
    body: init.body === undefined ? undefined : JSON.stringify(init.body),
  })
  if (res.status === 401) {
    onUnauthorized()
    throw new ApiError(401, 'Zaloguj się ponownie')
  }
  if (!res.ok) {
    let detail = `Błąd ${res.status}`
    try {
      const problem = await res.json()
      if (problem?.detail) detail = problem.detail
    } catch {
      /* odpowiedź bez treści */
    }
    throw new ApiError(res.status, detail)
  }
  return res.status === 204 ? (undefined as T) : ((await res.json()) as T)
}
