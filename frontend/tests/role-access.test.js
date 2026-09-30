import test from 'node:test'
import assert from 'node:assert/strict'
import { roleHome, canVisit } from '../src/roleAccess.js'

test('each role has its own workspace and cannot enter another role workspace', () => {
  const homes = { user: '/dashboard', merchant: '/merchant/products', admin: '/admin/dashboard' }
  for (const [role, home] of Object.entries(homes)) {
    assert.equal(roleHome(role), home)
    assert.equal(canVisit(role, home), true)
    for (const other of Object.keys(homes).filter(value => value !== role)) assert.equal(canVisit(role, homes[other]), false)
    assert.equal(canVisit(role, '/profile'), true)
    assert.equal(canVisit(role, '/demo'), true)
  }
  for (const path of ['/pets/1/weights', '/shop/orders', '/ai/assistant', '/community/hospitals']) {
    assert.equal(canVisit('user', path), true)
    assert.equal(canVisit('merchant', path), false)
    assert.equal(canVisit('admin', path), false)
  }
  assert.equal(canVisit(null, '/dashboard'), false)
  assert.equal(canVisit(null, '/demo'), true)
  assert.equal(canVisit('unknown', '/dashboard'), false)
})
