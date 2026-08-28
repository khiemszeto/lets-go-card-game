export function renderLogin(
  root: HTMLElement,
  onEnter: (username: string) => void | Promise<void>,
  error: string | null,
): void {
  root.innerHTML = `
    <section class="login-screen">
      <div class="login-card">
        <p class="eyebrow">Tiến Lên</p>
        <h1>Enter the Lobby</h1>
        <p class="subtitle">Type any name to play. A quick guest account is created behind the scenes.</p>
        <form id="login-form" class="login-form">
          <label>
            Your name
            <input id="username-input" type="text" maxlength="20" placeholder="e.g. Player_Alice" required autofocus />
          </label>
          ${error ? `<p class="error-banner">${escapeHtml(error)}</p>` : ""}
          <button id="login-submit" type="submit" class="btn btn-primary btn-lg">Enter Lobby</button>
        </form>
      </div>
    </section>
  `;

  const form = root.querySelector<HTMLFormElement>("#login-form")!;
  const input = root.querySelector<HTMLInputElement>("#username-input")!;
  const submitBtn = root.querySelector<HTMLButtonElement>("#login-submit")!;

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    void (async () => {
      submitBtn.disabled = true;
      submitBtn.textContent = "Connecting…";
      try {
        await onEnter(input.value.trim());
      } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = "Enter Lobby";
      }
    })();
  });
}

function escapeHtml(text: string): string {
  return text
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}
