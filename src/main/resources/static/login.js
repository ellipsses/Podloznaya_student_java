// Если уже вошли - сразу дальше
const existing = getAuth();
if (existing) location.replace(landingPage(existing.roles));

const form = document.getElementById('auth-form');
const errorEl = document.getElementById('auth-error');
const submitBtn = document.getElementById('auth-submit');

function showError(message) { errorEl.textContent = message; errorEl.hidden = false; }
function hideError() { errorEl.hidden = true; }

// После входа обе роли (ADMIN и STUDENT) попадают на список студентов
function landingPage() {
  return 'index.html';
}

if (new URLSearchParams(location.search).has('expired')) {
  showError('Сессия истекла, войдите заново.');
}

async function login(email, password) {
  let res;
  try {
    res = await fetch('/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });
  } catch {
    throw new Error('Нет связи с сервером. Запущен ли бэкенд на :8080?');
  }
  // Сервер при неверном пароле отвечает ошибкой без понятного текста (401/403/500),
  // поэтому любой неуспех считаем неверным email или паролем
  if (!res.ok) throw new Error('Неверный email или пароль');
  const data = await res.json();
  setAuth({ token: data.token, email, roles: roleNames(data.roles) });
  location.replace(landingPage(data.roles));
}

form.addEventListener('submit', async e => {
  e.preventDefault();
  const email = form.elements.email.value.trim();
  const password = form.elements.password.value;

  if (!email || !password) return showError('Введите email и пароль.');
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return showError('Введите корректный email.');
  hideError();

  submitBtn.disabled = true;
  try {
    await login(email, password);
  } catch (err) {
    showError(err.message);
    submitBtn.disabled = false;
  }
});
