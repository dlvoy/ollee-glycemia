// Ollee glycemia watchface for the Forge beta (https://github.com/olleewatch/forge_emulator).
//
// Main  = the 6-char tag the app also sends as the screensaver text (e.g. "120 -5")
// Day   = minutes since the measurement (0-39), minutes since the link was lost, or "--"
// LAP   = the app is feeding us live data, PM = the reading is outdated
// A status word flashes on Main when the link is lost, the app went quiet or the reading is old.
// L (top left) lights the watch for LIGHT_MS.
//
// Frames (BLE write, printable ASCII, max 20 bytes), see docs/watchface/PROTOCOL.md:
//   G<ageSeconds|->,<tag>   live state
//   P,<label>               the user paused/stopped the watch in the app
//   Q                       handshake query, answered with the notification H1,gly,<FACE_VERSION>
//
// Espruino rules: hooks are top-level function declarations, no top-level calls (no hoisting),
// every hook catches its own errors (an uncaught error disables the face).

var subscribe = E_BLE_RX + E_BLE_CONNECT + E_BLE_DISCONNECT + E_CLOCK_MINUTE + E_L_SHORT;

var OUTDATED_MS = 630000;   // 10.5 min, same as the app's STALE boundary
var LINK_TIMEOUT_MS = 660000; // 11 min without any frame: the app keeps sending at least every 5
var FLASH_MS = 2000;
var LIGHT_MS = 3000;
var DAY_MAX = 39;           // the first Day digit can only show 1-3
var FACE_ID = 'gly';
var FACE_VERSION = 3;       // keep watchface/VERSIONS.md in sync

var text = '--- --';
var measuredAt = 0;         // watch-clock ms of the reading, 0 = unknown
var lastRxAt = 0;           // watch-clock ms of the last frame, 0 = none yet
var lostAt = 0;             // watch-clock ms of the last disconnect, 0 = connected
var paused = false;
var flashId = -1;
var lightId = -1;
var lastMode = '';

function nowMs() {
  return new Date().getTime();
}

// Returns {k, age, text} or null for anything that is not a valid frame
function parseFrame(data) {
  if (typeof data !== 'string' || data.length < 3) return null;
  var k = data.charAt(0);
  if (k === 'P') {
    if (data.charAt(1) !== ',') return null;
    return { k: 'P', age: null, text: data.substring(2) };
  }
  if (k !== 'G') return null;
  var c = data.indexOf(',');
  if (c < 2 || c >= data.length - 1) return null;
  var a = data.substring(1, c);
  if (a === '-') return { k: 'G', age: null, text: data.substring(c + 1) };
  if (a.length > 7) return null;
  for (var i = 0; i < a.length; i++) {
    var d = a.charCodeAt(i);
    if (d < 48 || d > 57) return null;
  }
  return { k: 'G', age: parseInt(a, 10), text: data.substring(c + 1) };
}

// P paused, L no BLE link, A link up but the app is silent, D no measurement, O outdated, V live
function modeAt(now) {
  if (paused) return 'P';
  if (!Ollee.connected()) return 'L';
  if (lastRxAt === 0 || now - lastRxAt >= LINK_TIMEOUT_MS) return 'A';
  if (measuredAt === 0) return 'D';
  if (now - measuredAt >= OUTDATED_MS) return 'O';
  return 'V';
}

function dayText(since, now) {
  if (since === 0) return '--';
  var m = Math.floor((now - since) / 60000);
  if (m < 0) m = 0;
  return m <= DAY_MAX ? String(m) : '--';
}

function flashWord(mode) {
  if (mode === 'L') return 'no bLE';
  if (mode === 'A') return 'no APP';
  if (mode === 'O') return ' old  ';
  return '';
}

// Handshake reply: tells the app this face is installed. Firmware without Ollee.send skips it.
function hello() {
  if (typeof Ollee.send === 'function') Ollee.send('H1,' + FACE_ID + ',' + FACE_VERSION);
}

function cancelFlash() {
  if (flashId !== -1) Ollee.clearTimeout(flashId);
  flashId = -1;
}

function endFlash() {
  try {
    flashId = -1;
    if (Ollee.active()) Ollee.drawMain(text);
  } catch (e) {}
}

function cancelLight() {
  if (lightId !== -1) Ollee.clearTimeout(lightId);
  lightId = -1;
  Ollee.led_off();
}

function lightOff() {
  try {
    lightId = -1;
    Ollee.led_off();
  } catch (e) {}
}

// trigger: 'enter', 'minute' (always flash a status word) or 'event' (flash only on a mode change)
function render(trigger) {
  if (!Ollee.active()) return;
  var now = nowMs();
  var mode = modeAt(now);
  var day = '--';
  if (mode === 'L') day = dayText(lostAt, now);
  else if (mode === 'A') day = dayText(lastRxAt, now);
  else if (mode === 'O' || mode === 'V') day = dayText(measuredAt, now);

  cancelFlash();
  Ollee.drawMain(text);
  Ollee.drawDay(day);
  Ollee.drawLap(mode === 'V' || mode === 'O' || mode === 'D');
  Ollee.drawPM(mode === 'O');

  var word = flashWord(mode);
  if (word !== '' && (trigger !== 'event' || mode !== lastMode)) {
    var id = Ollee.setTimeout(endFlash, FLASH_MS);
    if (id !== -1) {
      flashId = id;
      Ollee.drawMain(word);
    }
  }
  lastMode = mode;
}

function init() {
  try {
    Ollee.subscribe(E_BLE_RX + E_BLE_CONNECT + E_BLE_DISCONNECT);
    lostAt = Ollee.connected() ? 0 : nowMs();
    if (lostAt === 0) hello();
  } catch (e) {}
}

function enter() {
  try {
    Ollee.clear();
    render('enter');
  } catch (e) {}
}

function exit() {
  try {
    cancelFlash();
    cancelLight();
    lastMode = '';
  } catch (e) {}
}

function ble_rx(data) {
  try {
    if (typeof data === 'string' && data.charAt(0) === 'Q') {
      hello();
      return;
    }
    var f = parseFrame(data);
    if (f === null) return;
    var now = nowMs();
    lastRxAt = now;
    text = f.text;
    if (f.k === 'P') {
      paused = true;
      measuredAt = 0;
    } else {
      paused = false;
      measuredAt = f.age === null ? 0 : now - f.age * 1000;
    }
    render('event');
  } catch (e) {}
}

function ble_connect() {
  try {
    lostAt = 0;
    render('event');
  } catch (e) {}
}

function ble_disconnect() {
  try {
    lostAt = nowMs();
    render('event');
  } catch (e) {}
}

function clock_minute() {
  try {
    render('minute');
  } catch (e) {}
}

// Restarts the light on every press; with no free timer slot the LED stays off
function l_short() {
  try {
    if (lightId !== -1) Ollee.clearTimeout(lightId);
    Ollee.led_on();
    lightId = Ollee.setTimeout(lightOff, LIGHT_MS);
    if (lightId === -1) Ollee.led_off();
  } catch (e) {}
}
