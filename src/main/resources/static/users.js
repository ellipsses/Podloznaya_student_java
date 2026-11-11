// Страница управления пользователями. Бэкенд пускает сюда только ADMIN (/api/v1/users).
const USERS = '/api/v1/users';

const auth = getAuth();
if (!auth) location.replace('login.html');
const roles = auth ? roleNames(auth.roles) : [];
if (auth && !roles.includes('ADMIN')) location.replace('index.html');

const form = document.getElementById('form');
const formTitle = document.getElementById('form-title');
const formError = document.getElementById('form-error');
const submitBtn = document.getElementById('submit');
const cancelBtn = document.getElementById('cancel');
const passwordRow = document.getElementById('password-row');
const listEl = document.getElementById('list');
const stateEl = document.getElementById('state');
const countEl = document.getElementById('count');
const toastEl = document.getElementById('toast');
const roleBoxes = [...form.querySelectorAll('input[name=role]')];

let users = [];          // [{id, email, roles}]
let editingId = null;    // если не null - форма меняет роли этого пользователя

// ---------- API ----------

const fetchUsers = async () => (await request(USERS)).responses || [];
const createUser = (body) => request(`${USERS}/save`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(body),
});
// Сервер ищет пользователя по email и меняет только роли; ответ у него пустой
const updateRoles = (id, body) => request(`${USERS}/${id}`, {
  method: 'PUT',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(body),
});
const deleteUser = (id) => request(`${USERS}/${id}`, { method: 'DELETE' });

// ---------- отрисовка ----------

let toastTimer;
function toast(message, bad = false) {
  toastEl.textContent = message;
  toastEl.className = 'show' + (bad ? ' bad' : '');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { toastEl.className = ''; }, 3500);
}

const isMe = (u) => u.email.toLowerCase() === auth.email.toLowerCase();

function renderList() {
  countEl.textContent = users.length ? `всего: ${users.length}` : '';
  listEl.replaceChildren();

  for (const u of users) {
    const li = document.createElement('li');

    const av = document.createElement('span');
    av.className = 'avatar';
    av.textContent = (u.email[0] || '?').toUpperCase();

    const info = document.createElement('div');
    info.className = 'u-info';
    const email = document.createElement('div');
    email.className = 'u-email';
    email.textContent = u.email + (isMe(u) ? ' (это вы)' : '');
    const badges = document.createElement('div');
    badges.className = 'badges';
    for (const r of roleNames(u.roles)) {
      const b = document.createElement('span');
      b.className = 'badge';
      b.textContent = ROLE_LABELS[r] || r;
      badges.append(b);
    }
    info.append(email, badges);

    const actions = document.createElement('div');
    actions.className = 'row-actions';
    const edit = document.createElement('button');
    edit.type = 'button';
    edit.className = 'secondary small';
    edit.textContent = 'Роли';
    edit.addEventListener('click', () => startEdit(u));
    actions.append(edit);
    if (!isMe(u)) {            // себя удалять нельзя: можно остаться без администратора
      const del = document.createElement('button');
      del.type = 'button';
      del.className = 'danger small';
      del.textContent = 'Удалить';
      del.addEventListener('click', () => remove(u));
      actions.append(del);
    }

    li.append(av, info, actions);
    listEl.append(li);
  }

  stateEl.textContent = users.length ? '' : 'Пользователей пока нет.';
}

function selectedRoles() {
  return roleBoxes.filter(b => b.checked).map(b => b.value);
}

// ---------- действия ----------

async function reload() {
  try {
    users = await fetchUsers();
    renderList();
  } catch (e) {
    users = [];
    renderList();
    stateEl.textContent = e.message;
  }
}

function startEdit(u) {
  editingId = u.id;
  form.reset();
  formError.hidden = true;
  form.elements.email.value = u.email;
  form.elements.email.readOnly = true;       // email - это ключ, по которому сервер находит пользователя
  passwordRow.hidden = true;                 // пароль сервер при изменении не меняет
  const mine = new Set(roleNames(u.roles));
  for (const b of roleBoxes) b.checked = mine.has(b.value);
  formTitle.textContent = 'Роли пользователя';
  submitBtn.textContent = 'Сохранить роли';
  cancelBtn.hidden = false;
}

function stopEdit() {
  editingId = null;
  form.reset();
  form.elements.email.readOnly = false;
  passwordRow.hidden = false;
  formError.hidden = true;
  for (const f of form.elements) f.classList?.remove('invalid');
  formTitle.textContent = 'Новый пользователь';
  submitBtn.textContent = 'Создать';
  cancelBtn.hidden = true;
}

async function remove(u) {
  if (!confirm(`Удалить пользователя ${u.email}?`)) return;
  try {
    await deleteUser(u.id);
    if (editingId === u.id) stopEdit();
    await reload();
    toast('Пользователь удалён');
  } catch (e) {
    toast(e.message, true);
  }
}

form.addEventListener('submit', async e => {
  e.preventDefault();
  const email = form.elements.email.value.trim();
  const password = form.elements.password.value;
  const chosen = selectedRoles();

  for (const f of form.elements) f.classList?.remove('invalid');
  const problems = [];
  if (editingId === null) {
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      problems.push('корректный email'); form.elements.email.classList.add('invalid');
    } else if (users.some(u => u.email.toLowerCase() === email.toLowerCase())) {
      // сервер на дубликат отвечает ошибкой без понятного текста, поэтому проверяем заранее
      problems.push('другой email: такой пользователь уже есть'); form.elements.email.classList.add('invalid');
    }
    if (password.length < 6) {
      problems.push('пароль не короче 6 символов'); form.elements.password.classList.add('invalid');
    }
  }
  if (!chosen.length) problems.push('хотя бы одну роль');
  if (problems.length) {
    formError.textContent = `Укажите ${problems.join(' и ')}.`;
    formError.hidden = false;
    return;
  }
  // сам себе снять администратора нельзя: некому будет управлять пользователями
  if (editingId !== null && email.toLowerCase() === auth.email.toLowerCase() && !chosen.includes('ADMIN')) {
    formError.textContent = 'Нельзя снять с себя роль администратора.';
    formError.hidden = false;
    return;
  }
  formError.hidden = true;

  submitBtn.disabled = true;
  try {
    if (editingId === null) {
      await createUser({ email, password, roles: chosen });
      form.reset();
      toast('Пользователь создан');
    } else {
      await updateRoles(editingId, { email, roles: chosen });
      stopEdit();
      toast('Роли сохранены');
    }
    await reload();
  } catch (err) {
    toast(editingId === null && err.message === 'Недостаточно прав для этого действия'
      ? 'Не удалось создать пользователя. Возможно, такой email уже есть.' : err.message, true);
  } finally {
    submitBtn.disabled = false;
  }
});

cancelBtn.addEventListener('click', stopEdit);
document.getElementById('logout').addEventListener('click', () => logout());
document.getElementById('to-students').addEventListener('click', () => location.assign('index.html'));

if (auth) {
  document.getElementById('who').textContent = `${auth.email} · ${describeRoles(roles)}`;
  reload();
}
