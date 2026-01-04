// attendance-clock.js
// 勤怠入力画面の時計をリアルタイム更新する

document.addEventListener("DOMContentLoaded", () => {
  const clockRoot = document.getElementById("attendanceClock");
  if (!clockRoot) return;

  const dateEl = clockRoot.querySelector(".clock-date");
  const timeEl = clockRoot.querySelector(".clock-time");
  if (!dateEl || !timeEl) return;

  // サーバ時刻（ミリ秒）
  const baseServerEpoch = Number(clockRoot.dataset.serverEpoch);
  if (!Number.isFinite(baseServerEpoch)) return;

  // ページ表示開始からの経過時間（ミリ秒）。PCの時刻変更に影響されにくい。
  const startPerf = performance.now();

  // 「M月 d日 (E)」っぽい文字列を作る（例：10月 22日 (水)）
  function formatDateTokyo(epochMillis) {
    const d = new Date(epochMillis);

    const parts = new Intl.DateTimeFormat("ja-JP", {
      timeZone: "Asia/Tokyo",
      month: "numeric",
      day: "numeric",
      weekday: "short", // (水) みたいな感じ
    }).formatToParts(d);

    const map = {};
    for (const p of parts) {
      if (p.type !== "literal") map[p.type] = p.value;
    }

    // weekday は "水" などになることが多いので、( ) を付ける
    return `${map.month}月 ${map.day}日 (${map.weekday})`;
  }

  // 「H:mm」っぽい文字列（例：15:30）
  function formatTimeTokyo(epochMillis) {
    const d = new Date(epochMillis);

    const parts = new Intl.DateTimeFormat("ja-JP", {
      timeZone: "Asia/Tokyo",
      hour: "numeric",
      minute: "2-digit",
      hour12: false,
    }).formatToParts(d);

    const map = {};
    for (const p of parts) {
      if (p.type !== "literal") map[p.type] = p.value;
    }

    return `${map.hour}:${map.minute}`;
  }

  function render() {
    const elapsed = performance.now() - startPerf;
    const nowEpoch = baseServerEpoch + elapsed;

    dateEl.textContent = formatDateTokyo(nowEpoch);
    timeEl.textContent = formatTimeTokyo(nowEpoch);
  }

  // すぐ反映 → その後も更新
  render();

  // 表示は「分」までなので、1秒ごと更新でもOK（分が変わったときに自然に切り替わる）
  setInterval(render, 1000);
});
