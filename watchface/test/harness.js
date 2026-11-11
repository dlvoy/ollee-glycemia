'use strict';
// Minimal stand-in for the Forge runtime: runs a watchface script in a vm context with only
// firmware-like globals, a fake Ollee object and a controllable clock.
// Mirrors the rules in forge_emulator docs/api/README.md (draw calls ignored off screen, 4 shared
// timer slots returning -1 when full, hooks found by top-level name, subscribe bits).

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const E = {
  E_A_SHORT: 1 << 0, E_A_LONG: 1 << 1, E_L_SHORT: 1 << 2, E_L_LONG: 1 << 3,
  E_TILT: 1 << 4, E_BUTTONS: 1 << 5, E_TAP: 1 << 6, E_BLE_RX: 1 << 7,
  E_CLOCK_MINUTE: 1 << 8, E_CLOCK_HOUR: 1 << 9, E_BLE_CONNECT: 1 << 10,
  E_BLE_DISCONNECT: 1 << 11, E_CLOCK_SECOND: 1 << 12,
};
const BACKGROUND_BITS = E.E_BLE_RX | E.E_CLOCK_MINUTE | E.E_CLOCK_HOUR | E.E_BLE_CONNECT | E.E_BLE_DISCONNECT;
const HOOK_BIT = {
  ble_rx: E.E_BLE_RX, clock_minute: E.E_CLOCK_MINUTE, ble_connect: E.E_BLE_CONNECT,
  ble_disconnect: E.E_BLE_DISCONNECT, l_short: E.E_L_SHORT,
};
const MINUTE = 60000;
// Aligned to a minute boundary so clock_minute ticks land on whole multiples of 60000
const T0 = 28333333 * MINUTE;
const FACE_PATH = path.join(__dirname, '..', 'glycemia.js');

function createHarness(opts = {}) {
  const scriptPath = opts.scriptPath || FACE_PATH;
  const clock = { now: opts.start === undefined ? T0 : opts.start };
  const state = { active: false, connected: opts.connected !== false, background: 0, disabled: null };
  const screen = { main: '      ', day: '  ', lap: false, pm: false };
  const led = { on: false };
  const timers = new Array(opts.timerSlots === undefined ? 4 : opts.timerSlots).fill(null);
  const calls = [];
  const sent = [];
  let ctx;

  const rec = (name, fn) => (...args) => { calls.push(name); return fn(...args); };
  const draw = (field, width) => (t) => {
    if (!state.active) return;
    screen[field] = String(t).slice(0, width).padStart(width, ' ');
  };
  const ollee = {
    active: rec('active', () => state.active),
    connected: rec('connected', () => state.connected),
    clear: rec('clear', () => {
      if (!state.active) return;
      screen.main = '      '; screen.day = '  '; screen.lap = false; screen.pm = false;
    }),
    drawMain: rec('drawMain', draw('main', 6)),
    drawDay: rec('drawDay', draw('day', 2)),
    drawLap: rec('drawLap', (on) => { if (state.active) screen.lap = !!on; }),
    drawPM: rec('drawPM', (on) => { if (state.active) screen.pm = !!on; }),
    drawHR: rec('drawHR', () => {}),
    drawColon: rec('drawColon', () => {}),
    beep: rec('beep', () => {}),
    led_on: rec('led_on', () => { led.on = true; }),
    led_off: rec('led_off', () => { led.on = false; }),
    subscribe: rec('subscribe', (bits) => {
      if (bits & ~BACKGROUND_BITS) return false;
      state.background |= bits;
      return true;
    }),
    unsubscribe: rec('unsubscribe', (bits) => { state.background &= ~bits; }),
    // Like the emulator: false when no phone is connected, the data is empty or over 256 bytes
    send: rec('send', (data) => {
      const s = String(data);
      if (!state.connected || s.length === 0 || s.length > 256) return false;
      sent.push(s);
      return true;
    }),
    setTimeout: rec('setTimeout', (fn, ms) => {
      const id = timers.findIndex((t) => t === null);
      if (id === -1) return -1;
      timers[id] = { fn, due: clock.now + ms };
      return id;
    }),
    clearTimeout: rec('clearTimeout', (id) => {
      if (id === undefined || id === null) timers.fill(null);
      else if (timers[id]) timers[id] = null;
    }),
  };

  // Only what the firmware offers; no Map/Set/Promise/setTimeout/require.
  const sandbox = { Ollee: ollee, Math, String, Number, parseInt, parseFloat, isNaN, JSON, Object, Array, ...E };
  function FakeDate() {}
  FakeDate.prototype.getTime = () => clock.now;
  sandbox.Date = FakeDate;
  ctx = vm.createContext(sandbox);

  function guarded(fn) {
    try { return fn(); } catch (e) { state.disabled = e; return undefined; }
  }

  function hook(name) {
    const fn = ctx[name];
    return typeof fn === 'function' ? fn : null;
  }

  function deliver(name, ...args) {
    if (state.disabled) return false;
    const fn = hook(name);
    if (!fn) return false;
    const bit = HOOK_BIT[name];
    const onScreen = state.active && (vm.runInContext('typeof subscribe === "number" ? subscribe : 0', ctx) & bit);
    if (!onScreen && !(state.background & bit)) return false;
    guarded(() => fn(...args));
    return true;
  }

  const h = {
    ollee, ctx, clock, state, screen, led, calls, sent, E, T0, MINUTE,
    load() {
      const src = fs.readFileSync(scriptPath, 'utf8');
      h.source = src;
      guarded(() => vm.runInContext(src, ctx, { filename: 'glycemia.js' }));
    },
    init() { guarded(() => hook('init') && hook('init')()); },
    enter() {
      state.active = true;
      guarded(() => hook('enter') && hook('enter')());
    },
    exit() {
      guarded(() => hook('exit') && hook('exit')());
      state.active = false;
      timers.fill(null);
    },
    rx(data) { return deliver('ble_rx', data); },
    // A button hook such as 'l_short'; reaches the face only on screen (no background bit)
    press(name) { return deliver(name); },
    setConnected(on) {
      state.connected = on;
      return deliver(on ? 'ble_connect' : 'ble_disconnect');
    },
    // The emulator's "Minute tick" button: fires the event without moving the clock
    fireMinute() { return deliver('clock_minute'); },
    // Moves the clock, firing due timers and RTC minute ticks in order
    advance(ms) {
      const target = clock.now + ms;
      for (;;) {
        const nextMinute = (Math.floor(clock.now / MINUTE) + 1) * MINUTE;
        let next = Math.min(target, nextMinute);
        let timerIdx = -1;
        timers.forEach((t, i) => {
          if (t && t.due <= next && (timerIdx === -1 || t.due < timers[timerIdx].due)) timerIdx = i;
        });
        if (timerIdx !== -1) next = Math.max(clock.now, timers[timerIdx].due);
        clock.now = next;
        if (timerIdx !== -1) {
          const t = timers[timerIdx];
          timers[timerIdx] = null;
          if (state.active) guarded(() => (typeof t.fn === 'function' ? t.fn() : null));
          continue;
        }
        if (clock.now === nextMinute) h.fireMinute();
        if (clock.now >= target) break;
      }
    },
    pendingTimers() { return timers.filter(Boolean).length; },
    snapshot() { return { ...screen }; },
  };
  return h;
}

// A harness with the face loaded, initialised and on screen
function openFace(opts = {}) {
  const h = createHarness(opts);
  h.load();
  h.init();
  if (opts.onScreen !== false) h.enter();
  return h;
}

module.exports = { createHarness, openFace, FACE_PATH, MINUTE, T0, E, BACKGROUND_BITS };
