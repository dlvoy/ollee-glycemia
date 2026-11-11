'use strict';
// Checks our stand-ins against the pinned forge_emulator submodule (tools/forge_emulator), so a
// bump of the pin that changes the Forge API or the emulator page fails here and not in the browser.
// Skipped when the submodule is not checked out: git submodule update --init tools/forge_emulator

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { E, BACKGROUND_BITS, FACE_PATH } = require('./harness');

const REPO = path.join(__dirname, '..', '..');
const EMU = path.join(REPO, 'tools', 'forge_emulator');
const skip = fs.existsSync(path.join(EMU, 'index.html'))
  ? false
  : 'tools/forge_emulator is not checked out (git submodule update --init tools/forge_emulator)';

const read = (...parts) => fs.readFileSync(path.join(...parts), 'utf8');

function intConst(source, name) {
  const m = source.match(new RegExp(`const ${name} = (\\d+);`));
  assert.ok(m, `${name} not found`);
  return Number(m[1]);
}

test('the Bluetooth panel has the elements forge-bridge.js drives', { skip }, () => {
  const html = read(EMU, 'index.html');
  const bridge = read(REPO, 'tools', 'forge-bridge', 'forge-bridge.js');
  const ids = [...bridge.matchAll(/getElementById\('([\w-]+)'\)/g)].map((m) => m[1]);
  assert.deepEqual(ids.sort(), ['ble-connected', 'ble-input', 'ble-send-btn']);
  for (const id of ids) assert.ok(html.includes(`id="${id}"`), `index.html lacks id="${id}"`);
});

test('harness event bits match the emulator', { skip }, () => {
  const ollee = read(EMU, 'js', 'ollee.js');
  const emu = {};
  for (const m of ollee.matchAll(/const (E_[A-Z_]+) = 1 << (\d+);/g)) emu[m[1]] = 1 << Number(m[2]);
  for (const [name, bit] of Object.entries(E)) {
    assert.equal(emu[name], bit, `${name}: harness ${bit}, emulator ${emu[name]}`);
  }

  const bg = ollee.match(/const FORGE_BG_EVENTS = ([^;]+);/);
  assert.ok(bg, 'FORGE_BG_EVENTS not found');
  const bits = bg[1].split('|').map((s) => s.trim()).reduce((acc, name) => {
    assert.ok(name in emu, `unknown bit ${name} in FORGE_BG_EVENTS`);
    return acc | emu[name];
  }, 0);
  assert.equal(BACKGROUND_BITS, bits);
});

test('the face fits the emulator script limit', { skip }, () => {
  const limit = intConst(read(EMU, 'js', 'app.js'), 'FORGE_SCRIPT_BYTE_LIMIT');
  assert.ok(Buffer.byteLength(read(FACE_PATH), 'utf8') <= limit);
});

test('protocol frames fit one emulator BLE write', { skip }, () => {
  const max = intConst(read(EMU, 'js', 'ollee.js'), 'FORGE_BLE_MAX_DATA_LEN');
  const fixtures = JSON.parse(read(__dirname, 'fixtures', 'frames.json'));
  for (const f of fixtures.encode) {
    assert.ok(Buffer.byteLength(f.frame, 'latin1') <= max, `${f.name}: ${f.frame}`);
  }
  // The query is one RX write, the hello one TX notification
  for (const frame of [fixtures.handshake.query, fixtures.handshake.hello]) {
    assert.ok(Buffer.byteLength(frame, 'latin1') <= max, frame);
  }
});
