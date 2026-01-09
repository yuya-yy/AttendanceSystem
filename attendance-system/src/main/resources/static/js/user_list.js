(() => {
  // このJSは「ユーザー情報一覧（S0106）」専用
  const tableEl = document.getElementById("userTable");
  if (!tableEl) return;

  const keywordInput = document.getElementById("keywordInput");
  const departmentFilter = document.getElementById("departmentFilter");
  const roleFilter = document.getElementById("roleFilter");
  const locationFilter = document.getElementById("locationFilter");

  const resultCount = document.getElementById("resultCount");
  const resultEmpty = document.getElementById("resultEmpty");
  const noResultRow = document.getElementById("noResultRow");

  if (!keywordInput || !departmentFilter || !roleFilter || !locationFilter || !resultCount || !resultEmpty) {
    console.warn("[user_list.js] フィルター用要素が見つかりません。HTMLのidを確認してください。");
    return;
  }

  /**
   * normalize（正規化）
   * 意味：比較しやすい形に文字列を整えること
   */
  function normalize(text) {
    return (text ?? "")
      .trim()
      .replace(/\u3000/g, " ")
      .replace(/\s+/g, " ")
      .toLowerCase();
  }

  // ★行は基本変化しない前提なので、最初に1回だけ取得して使い回す
  const rows = Array.from(tableEl.querySelectorAll("tbody tr"))
    .filter((row) => row.dataset.name);

  // ★さらに軽くするなら、ここで正規化した値を作っておく（毎回normalizeしない）
  const metas = rows.map((row) => ({
    row,
    name: normalize(row.dataset.name),
    departmentId: row.dataset.departmentId || "",
    role: row.dataset.role || "",
    location: row.dataset.location || "",
  }));

  function applyFilters() {
    const keyword = normalize(keywordInput.value);
    const departmentId = departmentFilter.value; // "ALL" or "3"
    const roleCode = roleFilter.value;           // "ALL" / "ADMIN" / "USER"
    const locationCode = locationFilter.value;   // "ALL" / "OFFICE" / "HOME" / ...

    let visibleCount = 0;

    metas.forEach((m) => {
      const matchesKeyword = keyword === "" || m.name.includes(keyword);
      const matchesDepartment = departmentId === "ALL" || m.departmentId === departmentId;
      const matchesRole = roleCode === "ALL" || m.role === roleCode;
      const matchesLocation = locationCode === "ALL" || m.location === locationCode;

      const isVisible = matchesKeyword && matchesDepartment && matchesRole && matchesLocation;

      // ★display操作より安定：hidden
      m.row.hidden = !isVisible;
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
  departmentFilter.addEventListener("change", applyFilters);
  roleFilter.addEventListener("change", applyFilters);
  locationFilter.addEventListener("change", applyFilters);

  applyFilters();

  tableEl.addEventListener("click", (e) => {
  // 操作ボタン（a/button/form）をクリックした時は無視
  if (e.target.closest("a, button, form")) return;

  const el = e.target.closest(".js-ellipsis");
  if (!el) return;

  el.classList.toggle("is-expanded");
});

})();
