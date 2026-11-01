// Paste this into the DevTools console of the forge_emulator page.
//
// Polls the app's debug relay (adb forward tcp:8765 tcp:8765) once a second and replays what a
// watch would receive into the emulator, through the emulator's own Bluetooth panel:
//   - every frame is typed into the text box and sent with the Send button, so the emulator's
//     20-byte check, "phone connected" check and BLE log all apply
//   - the relay's link state is mirrored onto the "phone connected" checkbox, which fires
//     ble_connect() / ble_disconnect() in the watchface
//
// Stop with forgeBridge.stop(). Use another relay address with window.FORGE_RELAY = 'http://...'
// before pasting.
(function () {
  if (window.forgeBridge) window.forgeBridge.stop();

  var base = window.FORGE_RELAY || 'http://127.0.0.1:8765';
  var input = document.getElementById('ble-input');
  var sendBtn = document.getElementById('ble-send-btn');
  var linkBox = document.getElementById('ble-connected');
  if (!input || !sendBtn || !linkBox) {
    console.error('[forge-bridge] Bluetooth panel elements not found (#ble-input, #ble-send-btn, #ble-connected). Is this the forge_emulator page?');
    return;
  }

  var after = 0;
  var timer = null;
  var busy = false;
  var reachable = null;
  var stats = { frames: 0, polls: 0, link: null };

  function setLink(on) {
    if (linkBox.checked === on) return;
    linkBox.checked = on;
    linkBox.dispatchEvent(new Event('change', { bubbles: true }));
    console.log('[forge-bridge] phone connected ->', on);
  }

  function sendFrame(data) {
    input.value = data;
    input.dispatchEvent(new Event('input', { bubbles: true }));
    sendBtn.click();
    stats.frames++;
    console.log('[forge-bridge] frame:', JSON.stringify(data));
  }

  function poll() {
    if (busy) return;
    busy = true;
    stats.polls++;
    fetch(base + '/frames?after=' + after, { cache: 'no-store' })
      .then(function (r) { return r.json(); })
      .then(function (json) {
        if (reachable !== true) console.log('[forge-bridge] relay reachable at', base);
        reachable = true;
        if (json.seq < after) after = 0; // the app restarted and its counter began again
        // Link up first so frames are accepted; link down last so frames sent before the drop still arrive
        if (json.link) setLink(true);
        json.frames.forEach(function (f) {
          if (f.seq > after) { sendFrame(f.data); after = f.seq; }
        });
        if (!json.link) setLink(false);
        stats.link = json.link;
      })
      .catch(function (e) {
        if (reachable !== false) {
          console.warn('[forge-bridge] relay not reachable at ' + base + ' (' + e + '). Is "adb forward tcp:8765 tcp:8765" set and the relay on?');
        }
        reachable = false;
      })
      .then(function () { busy = false; });
  }

  timer = setInterval(poll, 1000);
  poll();
  window.forgeBridge = {
    stop: function () { clearInterval(timer); console.log('[forge-bridge] stopped'); },
    status: function () { return { base: base, after: after, reachable: reachable, frames: stats.frames, polls: stats.polls, link: stats.link }; }
  };
  console.log('[forge-bridge] started, polling ' + base + ' (forgeBridge.stop() to end)');
})();
