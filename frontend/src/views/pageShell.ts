export function renderPageShell(
  title: string,
  subtitle: string,
  username: string,
  leaveLabel: string,
  bodyHtml: string,
  bannersHtml: string,
): string {
  return `
    <section class="page">
      <header class="page-header">
        <div class="page-header__title">
          <p class="eyebrow">${escapeHtml(subtitle)}</p>
          <h1>${escapeHtml(title)}</h1>
        </div>
        <div class="page-header__actions">
          <span class="player-chip">${escapeHtml(username)}</span>
          <button id="page-leave-btn" class="btn btn-ghost leave-btn" type="button">${escapeHtml(leaveLabel)}</button>
        </div>
      </header>
      ${bannersHtml}
      ${bodyHtml}
    </section>
  `;
}

export function bindLeaveButton(root: HTMLElement, onLeave: () => void): void {
  root.querySelector("#page-leave-btn")?.addEventListener("click", onLeave);
}

function escapeHtml(text: string): string {
  return text
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}
