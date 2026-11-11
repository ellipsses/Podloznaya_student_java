// Общее для всех страниц: где хранится вход и как ходить в API.
// Бэкенд выдаёт JWT-токен (POST /api/v1/auth/login), его нужно слать в заголовке
// Authorization: Bearer <токен>. Токен живёт 1 час. Храним в sessionStorage:
// он существует, пока открыта вкладка, и пропадает при выходе.
const AUTH_KEY = 'auth';

const ROLE_LABELS = {
  ADMIN: 'администратор',
  STUDENT: 'студент',
};

function getAuth() {
  try { return JSON.parse(sessionStorage.getItem(AUTH_KEY)); } catch { return null; }
}
function setAuth(auth) {
  try { sessionStorage.setItem(AUTH_KEY, JSON.stringify(auth)); } catch { /* хранилище недоступно */ }
}
function clearAuth() {
  try { sessionStorage.removeItem(AUTH_KEY); } catch { /* хранилище недоступно */ }
}

// Бэкенд в логине отдаёт роли как ROLE_ADMIN, в списке пользователей - как ADMIN. Приводим к ADMIN.
function roleNames(roles) {
  return (roles || []).map(r => String(r).replace(/^ROLE_/, ''));
}
function describeRoles(roles) {
  return roleNames(roles).map(r => ROLE_LABELS[r] || r).join(', ');
}

function logout(expired = false) {
  clearAuth();
  location.replace('login.html' + (expired ? '?expired=1' : ''));
}

// Один общий запрос: подставляет токен и превращает ошибки в понятный Error.
async function request(url, options) {
  const auth = getAuth();
  if (!auth) { logout(); throw new Error('Войдите в систему'); }

  let res;
  try {
    res = await fetch(url, {
      ...options,
      headers: { ...(options && options.headers), Authorization: `Bearer ${auth.token}` },
    });
  } catch {
    throw new Error('Нет связи с сервером. Запущен ли бэкенд на :8080?');
  }
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { /* не JSON */ }

  if (res.status === 401) {            // токен просрочен или недействителен
    logout(true);
    throw new Error('Сессия истекла, войдите заново');
  }
  if (res.status === 403) throw new Error('Недостаточно прав для этого действия');
  if (!res.ok) {
    // GlobalExceptionHandler отдаёт {statusCode, message}
    throw new Error((data && (data.message || data.error)) || `Ошибка сервера (${res.status})`);
  }
  return data;
}
