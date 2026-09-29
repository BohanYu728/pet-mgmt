import test from 'node:test'
import assert from 'node:assert/strict'
import { api, allPets, errorMessage, invalidateCsrf, imageUrl } from '../src/api.js'

test('mutating requests fetch CSRF once and send credentials without an Authorization token', async () => {
  invalidateCsrf()
  const calls = []
  api.defaults.adapter = async config => {
    calls.push({ url: config.url, headers: config.headers.toJSON(), withCredentials: config.withCredentials })
    return { data: config.url === '/auth/csrf' ? { token: 'test-csrf', headerName: 'X-XSRF-TOKEN' } : { message: 'ok' }, status: 200, statusText: 'OK', headers: {}, config }
  }
  await Promise.all([api.post('/pets', { name: '小白' }), api.put('/pets/1', { name: '小黑' })])
  assert.deepEqual(calls.map(c => c.url), ['/auth/csrf', '/pets', '/pets/1'])
  assert.equal(calls[1].headers['X-XSRF-TOKEN'], 'test-csrf')
  assert.equal(calls[2].headers['X-XSRF-TOKEN'], 'test-csrf')
  assert.equal(calls[1].withCredentials, true)
  assert.equal(calls[1].headers.Authorization, undefined)
})

test('a failed CSRF fetch can be retried', async () => {
  invalidateCsrf()
  let attempts = 0
  api.defaults.adapter = async config => {
    if (config.url === '/auth/csrf' && ++attempts === 1) throw new Error('temporary error')
    return { data: { token: 'new-token', headerName: 'X-XSRF-TOKEN' }, status: 200, statusText: 'OK', headers: {}, config }
  }
  await assert.rejects(api.post('/pets', {}), /temporary error/)
  await api.post('/pets', {})
  assert.equal(attempts, 2)
})

test('sequential writes refresh CSRF after a server token rotation', async () => {
  invalidateCsrf()
  let issued = 0
  const sent = []
  api.defaults.adapter = async config => {
    if (config.url === '/auth/csrf') {
      issued++
      return { data: { token: `token-${issued}`, headerName: 'X-XSRF-TOKEN' }, status: 200, statusText: 'OK', headers: {}, config }
    }
    sent.push(config.headers.get('X-XSRF-TOKEN'))
    return { data: { message: 'ok' }, status: 200, statusText: 'OK', headers: {}, config }
  }
  await api.post('/admin/users', { username: 'temp' })
  await api.put('/admin/users/1', { username: 'temp2' })
  assert.deepEqual(sent, ['token-1', 'token-2'])
})

test('pet selector loads all pages for accounts with more than nine pets', async () => {
  api.defaults.adapter = async config => {
    const page = Number(config.params.page)
    return { data: { pets: page === 1 ? Array.from({ length: 9 }, (_, i) => ({ id: i + 1 })) : [{ id: 10 }], pages: 2 }, status: 200, statusText: 'OK', headers: {}, config }
  }
  assert.equal((await allPets()).length, 10)
})

test('API errors expose useful feedback for missing authorization and unavailable service', () => {
  assert.match(errorMessage({ response: { status: 403 } }), /没有权限/)
  assert.match(errorMessage({}), /暂时不可用/)
})

test('backend image paths resolve against the configured API origin', () => {
  assert.equal(imageUrl('uploads/pet.jpg'), 'http://localhost/uploads/pet.jpg')
})
