(() => {
  // このJSは「勤怠状況一覧（S0105）」専用
  const container = document.getElementById("statusTable");
  if (!container) return;

  const keywordInput = document.getElementById("keywordInput");
  const statusFilter = document.getElementById("statusFilter");
  const locationFilter = document.getElementById("locationFilter");

  const resultCount = document.getElementById("resultCount");
  const resultEmpty = document.getElementById("resultEmpty");
  const noResultRow = document.getElementById("noResultRow");

  if (!keywordInput || !statusFilter || !locationFilter || !resultCount || !resultEmpty) {
    console.warn("[status_list.js] フィルター要素が見つかりません。HTMLのidを確認してください。");
    return;
  }

  /**
   * normalize（正規化）
   * 意味：比較しやすい形に文字列を整えること
   * - 前後の空白を除去
   * - 全角スペースを半角スペースに寄せる
   * - 連続スペースを1つにする
   * - 小文字化して比較を安定させる
   */
  function normalize(text) {
    return (text ?? "")
      .trim()
      .replace(/\u3000/g, " ")
      .replace(/\s+/g, " ")
      .toLowerCase();
  }

  // ★行は基本変化しないので、最初に一回だけ取得して使い回す
  const rows = Array.from(container.querySelectorAll("tbody tr"))
    .filter((row) => row.dataset.name);

  function applyFilters() {
    const keyword = normalize(keywordInput.value);
    const statusCode = statusFilter.value;       // "ALL" / "WORKING" / "OFF"
    const locationCode = locationFilter.value;   // "ALL" / "OFFICE" / "HOME" / ...

    let visibleCount = 0;

    rows.forEach((row) => {
      const rowName = normalize(row.dataset.name);
      const rowStatus = row.dataset.status || "";       // 念のため保険
      const rowLocation = row.dataset.location || "";   // 念のため保険

      const matchesKeyword = keyword === "" || rowName.includes(keyword);
      const matchesStatus = statusCode === "ALL" || rowStatus === statusCode;
      const matchesLocation = locationCode === "ALL" || rowLocation === locationCode;

      const isVisible = matchesKeyword && matchesStatus && matchesLocation;

      // ★hiddenの方が戻し方が安定
      row.hidden = !isVisible;
      if (isVisible) visibleCount++;
    });

    resultCount.textContent = `${visibleCount}件表示中`;
   
     if (noResultRow) {
      noResultRow.hidden = (visibleCount !== 0);
  }

   if (resultEmpty) {
      resultEmpty.style.display = "none";
    }
  }

  keywordInput.addEventListener("input", applyFilters);
  statusFilter.addEventListener("change", applyFilters);
  locationFilter.addEventListener("change", applyFilters);

  applyFilters();

   container.addEventListener("click", (e) => {
    // 操作系クリックは無視
    if (e.target.closest("a, button, form")) return;

    const el = e.target.closest(".js-ellipsis");
    if (!el) return;

    el.classList.toggle("is-expanded");
  });

})();
