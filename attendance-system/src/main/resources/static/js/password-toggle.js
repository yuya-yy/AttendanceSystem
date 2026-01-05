/**
 * パスワード表示/非表示切り替え（アイコン版）
 * - data-target の input を探す
 * - input.type を password ⇄ text に切り替える
 * - アイコンを fa-eye ⇄ fa-eye-slash に切り替える
 */
document.addEventListener("DOMContentLoaded", () => {
  const buttons = document.querySelectorAll(".js-password-toggle");

  buttons.forEach((btn) => {
    btn.addEventListener("click", () => {
      const targetId = btn.getAttribute("data-target");
      const input = document.getElementById(targetId);
      if (!input) return;

      // 今が非表示なら→表示にする
      const willShow = input.type === "password";
      input.type = willShow ? "text" : "password";

      // アイコン切り替え（表示中＝eye、非表示中＝eye-slash）
      const icon = btn.querySelector(".js-eye-icon");
      if (icon) {
        icon.classList.remove("fa-eye", "fa-eye-slash");
        icon.classList.add(willShow ? "fa-eye" : "fa-eye-slash");
      }

      // ラベル（ボタンの意味）
      btn.setAttribute("aria-pressed", String(willShow));
      btn.setAttribute("aria-label", willShow ? "パスワードを非表示" : "パスワードを表示");

      input.focus();
    });
  });
});

