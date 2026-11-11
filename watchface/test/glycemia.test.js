'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { createHarness, openFace, FACE_PATH, MINUTE } = require('./harness');

const fixtures = JSON.parse(fs.readFileSync(path.join(__dirname, 'fixtures', 'frames.json'), 'utf8'));
const FLASH_MS = 2000;

test('script fits the 8158 byte limit', () => {
  assert.ok(Buffer.byteLength(fs.readFileSync(FACE_PATH, 'utf8'), 'utf8') <= 8158);
});

test('script sticks to what Espruino has', () => {
  const src = fs.readFileSync(FACE_PATH, 'utf8').replace(/\/\/.*$/gm, '');
  assert.doesNotMatch(src, /=>|`|\blet\b|\bconst\b|\bclass\b/);
  assert.doesNotMatch(src, /\b(Map|Set|Promise|Symbol|require|setInterval)\b/);
  assert.doesNotMatch(src, /(^|[^.\w])setTimeout\(/m);
});

test('loading the script makes no Ollee calls', () => {
  const h = createHarness();
  h.load();
  assert.deepEqual(h.calls, []);
  assert.equal(h.state.disabled, null);
});

test('init subscribes to background BLE events', () => {
  const h = createHarness();
  h.load();
  h.init();
  const { E } = h;
  assert.equal(h.state.background, E.E_BLE_RX | E.E_BLE_CONNECT | E.E_BLE_DISCONNECT);
});

test('fixtures: parseFrame agrees with the shared decode vectors', () => {
  const h = createHarness();
  h.load();
  for (const c of fixtures.decode) {
    const got = h.ctx.parseFrame(c.frame);
    if (!c.valid) {
      assert.equal(got, null, c.name);
      continue;
    }
    assert.notEqual(got, null, c.name);
    assert.equal(got.k, c.kind, c.name);
    assert.equal(got.text, c.text, c.name);
    if (c.kind === 'G') assert.equal(got.age, c.ageSec, c.name);
  }
});

test('G frame sets Main to the tag, Day to the age and LAP on', () => {
  const h = openFace();
  h.rx('G83,120 -5');
  assert.deepEqual(h.snapshot(), { main: '120 -5', day: ' 1', lap: true, pm: false });
  assert.equal(h.pendingTimers(), 0);
});

test('Day advances on minute ticks with no new data', () => {
  const h = openFace();
  h.rx('G83,120 -5');
  assert.equal(h.screen.day, ' 1');
  h.advance(MINUTE);
  assert.equal(h.screen.day, ' 2');
  h.advance(3 * MINUTE); // 83 s + 4 min = 323 s
  assert.equal(h.screen.day, ' 5');
  assert.equal(h.screen.main, '120 -5');
});

test('Day shows -- from 40 minutes and 39 at most', () => {
  const h = openFace();
  h.rx('G2340,120 -5'); // 39 min
  assert.equal(h.screen.day, '39');
  h.rx('G2400,120 -5'); // 40 min
  assert.equal(h.screen.day, '--');
});

test('unknown age shows -- without outdated markers or a flash', () => {
  const h = openFace();
  h.rx('G-,--- --');
  assert.deepEqual(h.snapshot(), { main: '--- --', day: '--', lap: true, pm: false });
  assert.equal(h.pendingTimers(), 0);
});

test('outdated reading: PM on, flash, then the tag again', () => {
  const h = openFace();
  h.rx('G700,120 -5');
  assert.equal(h.screen.pm, true);
  assert.equal(h.screen.lap, true);
  assert.equal(h.screen.main, ' old  ');
  assert.equal(h.screen.day, '11');
  h.advance(FLASH_MS);
  assert.equal(h.screen.main, '120 -5');
  assert.equal(h.pendingTimers(), 0);
});

test('a live reading turns outdated at 10.5 minutes while keepalives arrive', () => {
  const h = openFace();
  h.rx('G0,120 -5');
  h.advance(5 * MINUTE);
  h.rx('G300,120 -5');
  h.advance(5 * MINUTE);
  h.rx('G600,120 -5');
  assert.equal(h.screen.pm, false);
  h.advance(MINUTE); // 660 s old
  assert.equal(h.screen.pm, true);
  assert.equal(h.screen.main, ' old  ');
  h.advance(FLASH_MS);
  assert.equal(h.screen.main, '120 -5');
});

test('lost BLE link: flash, LAP off, Day counts from the disconnect', () => {
  const h = openFace();
  h.rx('G0,120 -5');
  h.advance(3 * MINUTE);
  h.setConnected(false);
  assert.equal(h.screen.main, 'no bLE');
  assert.equal(h.screen.lap, false);
  assert.equal(h.screen.pm, false);
  assert.equal(h.screen.day, ' 0');
  h.advance(FLASH_MS);
  assert.equal(h.screen.main, '120 -5');
  h.advance(2 * MINUTE - FLASH_MS); // lands exactly on the minute tick, two minutes after the disconnect
  assert.equal(h.screen.day, ' 2');
  assert.equal(h.screen.main, 'no bLE', 'a minute tick flashes the status word again');
});

test('link comes back: live display returns', () => {
  const h = openFace();
  h.rx('G0,120 -5');
  h.advance(3 * MINUTE);
  h.setConnected(false);
  h.advance(MINUTE);
  h.setConnected(true);
  h.rx('G10,118  2');
  assert.deepEqual(h.snapshot(), { main: '118  2', day: ' 0', lap: true, pm: false });
});

test('app silent for 11 minutes: no APP, Day counts from the last frame', () => {
  const h = openFace();
  h.rx('G0,120 -5');
  h.advance(10 * MINUTE);
  assert.equal(h.screen.lap, true, 'still live at 10 min');
  h.advance(MINUTE);
  assert.equal(h.screen.main, 'no APP');
  assert.equal(h.screen.lap, false);
  assert.equal(h.screen.day, '11');
});

test('no frame received since start: no APP and Day --', () => {
  const h = openFace();
  assert.equal(h.screen.main, 'no APP');
  assert.equal(h.screen.day, '--');
  assert.equal(h.screen.lap, false);
  h.advance(FLASH_MS);
  assert.equal(h.screen.main, '--- --');
});

test('paused watch: label on Main, Day --, never a lost-link flash', () => {
  const h = openFace();
  h.rx('P, pause');
  assert.deepEqual(h.snapshot(), { main: ' pause', day: '--', lap: false, pm: false });
  assert.equal(h.pendingTimers(), 0);
  h.setConnected(false); // the app drops the link after sending the label
  assert.equal(h.screen.main, ' pause');
  h.advance(5 * MINUTE);
  assert.equal(h.screen.main, ' pause');
  assert.equal(h.pendingTimers(), 0);
  h.setConnected(true);
  h.rx('G5,121  1');
  assert.deepEqual(h.snapshot(), { main: '121  1', day: ' 0', lap: true, pm: false });
});

test('frames received while another face is on screen show up on enter', () => {
  const h = openFace({ onScreen: false });
  assert.equal(h.rx('G83,120 -5'), true, 'delivered in the background');
  assert.equal(h.fireMinute(), false, 'minute ticks are not delivered off screen');
  assert.equal(h.screen.main, '      ');
  h.enter();
  assert.equal(h.screen.main, '120 -5');
  assert.equal(h.screen.day, ' 1');
  assert.equal(h.screen.lap, true);
});

test('a disconnect while off screen is seen on enter', () => {
  const h = openFace({ onScreen: false });
  h.rx('G0,120 -5');
  h.advance(2 * MINUTE);
  h.setConnected(false);
  h.advance(3 * MINUTE);
  h.enter();
  assert.equal(h.screen.day, ' 3');
  assert.equal(h.screen.main, 'no bLE');
});

test('malformed frames never throw and change nothing', () => {
  const h = openFace();
  h.rx('G83,120 -5');
  const before = h.snapshot();
  for (const bad of ['', 'G', 'Gx,', 'G5,', 'Z1', 'P', 'x'.repeat(20), 'G-5,abc', 'G99999999,abc', '\u0000ÿ']) {
    h.rx(bad);
  }
  assert.equal(h.state.disabled, null);
  assert.deepEqual(h.snapshot(), before);
});

test('a watch clock set backwards clamps Day to 0', () => {
  const h = openFace();
  h.rx('G600,120 -5');
  h.clock.now -= 15 * MINUTE; // the reading is now 5 minutes "in the future"
  h.fireMinute();
  assert.equal(h.screen.day, ' 0');
  assert.equal(h.state.disabled, null);
});

test('no free timer slot: no flash, no error', () => {
  const h = openFace({ timerSlots: 0 });
  assert.equal(h.state.disabled, null);
  assert.equal(h.screen.main, '--- --');
  h.setConnected(false);
  assert.equal(h.screen.main, '--- --');
  assert.equal(h.state.disabled, null);
});

test('exit clears the flash timer and enter shows it again', () => {
  const h = openFace();
  assert.equal(h.pendingTimers(), 1);
  h.exit();
  assert.equal(h.pendingTimers(), 0);
  h.enter();
  assert.equal(h.screen.main, 'no APP');
  assert.equal(h.pendingTimers(), 1);
});

test('LIVE state uses no timers at all', () => {
  const h = openFace();
  h.rx('G30,120 -5');
  h.advance(4 * MINUTE);
  assert.equal(h.pendingTimers(), 0);
});

test('a new frame during a flash replaces it immediately', () => {
  const h = openFace();
  h.rx('G700,120 -5'); // outdated flash running
  assert.equal(h.screen.main, ' old  ');
  h.rx('G10,119  1');
  assert.equal(h.screen.main, '119  1');
  assert.equal(h.pendingTimers(), 0);
});

// Handshake (spec 16)

test('Q is answered with the hello from the shared fixtures', () => {
  const h = openFace();
  h.sent.length = 0; // init already said hello
  assert.equal(h.rx(fixtures.handshake.query), true);
  assert.deepEqual(h.sent, [fixtures.handshake.hello]);
  assert.equal(h.state.disabled, null);
});

test('Q is answered while another face is on screen', () => {
  const h = openFace({ onScreen: false });
  h.sent.length = 0;
  assert.equal(h.rx('Q'), true);
  assert.deepEqual(h.sent, [fixtures.handshake.hello]);
});

test('Q leaves the tag, Day and mode alone', () => {
  const h = openFace();
  h.rx('G60,120 -5');
  const before = h.snapshot();
  h.advance(10.5 * MINUTE); // NO_APP would be 11 min after the last G frame
  h.rx('Q');
  assert.equal(h.screen.main, before.main);
  h.advance(0.5 * MINUTE + 1);
  assert.equal(h.screen.lap, false, 'Q must not count as app traffic');
});

test('init says hello only when a phone is connected', () => {
  const on = createHarness();
  on.load();
  on.init();
  assert.deepEqual(on.sent, [fixtures.handshake.hello]);

  const off = createHarness({ connected: false });
  off.load();
  off.init();
  assert.deepEqual(off.sent, []);
  assert.equal(off.state.disabled, null);
});

test('firmware without Ollee.send: no hello, face keeps working', () => {
  const h = createHarness();
  delete h.ollee.send;
  h.load();
  h.init();
  h.enter();
  h.rx('Q');
  h.rx('G30,120 -5');
  assert.equal(h.state.disabled, null);
  assert.equal(h.screen.main, '120 -5');
});

test('the hello carries the face version from the script', () => {
  const h = createHarness();
  h.load();
  assert.equal(fixtures.handshake.hello, `H1,${h.ctx.FACE_ID},${h.ctx.FACE_VERSION}`);
});

const LIGHT_MS = 3000;

test('L lights the watch and the light goes off after 3 s', () => {
  const h = openFace();
  assert.equal(h.press('l_short'), true);
  assert.equal(h.led.on, true);
  h.advance(LIGHT_MS - 1);
  assert.equal(h.led.on, true);
  h.advance(1);
  assert.equal(h.led.on, false);
  assert.equal(h.pendingTimers(), 0);
  assert.equal(h.state.disabled, null);
});

test('a second L press restarts the 3 s', () => {
  const h = openFace();
  h.press('l_short');
  h.advance(2000);
  h.press('l_short');
  assert.equal(h.pendingTimers(), 1);
  h.advance(2000);
  assert.equal(h.led.on, true);
  h.advance(1000);
  assert.equal(h.led.on, false);
});

test('L with no free timer slot leaves the LED off', () => {
  const h = openFace({ timerSlots: 0 });
  h.press('l_short');
  assert.equal(h.led.on, false);
  assert.equal(h.state.disabled, null);
});

test('leaving the face while lit turns the light off', () => {
  const h = openFace();
  h.press('l_short');
  h.exit();
  assert.equal(h.led.on, false);
  assert.equal(h.pendingTimers(), 0);
});

test('L does not reach the face off screen', () => {
  const h = openFace({ onScreen: false });
  assert.equal(h.press('l_short'), false);
  assert.equal(h.led.on, false);
});

test('the light and the status flash run side by side', () => {
  const h = openFace();
  h.rx('G83,120 -5');
  h.setConnected(false);
  assert.equal(h.screen.main, 'no bLE');
  h.press('l_short');
  assert.equal(h.pendingTimers(), 2);
  h.advance(FLASH_MS);
  assert.equal(h.screen.main, '120 -5');
  assert.equal(h.led.on, true);
  h.advance(LIGHT_MS - FLASH_MS);
  assert.equal(h.led.on, false);
});
