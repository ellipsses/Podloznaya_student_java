// Страница лежит в static/, поэтому открывается с того же адреса, что и API -
// относительные пути работают, CORS настраивать не нужно.
const API = '/api/v1/student';

// Кто вошёл (auth.js кладёт это в sessionStorage при входе). Нет данных - отправляем на страницу входа.
const auth = getAuth();
if (!auth) location.replace('login.html');
const roles = auth ? roleNames(auth.roles) : [];
const canView = roles.includes('ADMIN') || roles.includes('STUDENT');  // видеть студентов
const canWrite = roles.includes('ADMIN');                              // добавлять, менять, удалять
const isAdmin = roles.includes('ADMIN');                               // управлять пользователями

const form = document.getElementById('form');
const formError = document.getElementById('form-error');
const submitBtn = document.getElementById('submit');
const listEl = document.getElementById('list');
const stateEl = document.getElementById('state');
const countEl = document.getElementById('count');
const searchEl = document.getElementById('search');
const allEl = document.getElementById('all');
const detailsEl = document.getElementById('details');
const detailsBody = document.getElementById('details-body');
const toastEl = document.getElementById('toast');
const formTitle = document.getElementById('form-title');
const cancelBtn = document.getElementById('cancel');
const editBtn = document.getElementById('edit');
const deleteBtn = document.getElementById('delete');
const editHint = document.getElementById('edit-hint');

let students = [];     // [{id, name, surname}] - формат StudentResponseForList
let selectedId = null;
let current = null;    // полные данные выбранного студента (id, name, surname, phone, email)
let editingId = null;  // если не null - форма работает в режиме редактирования

// ---------- работа с API ----------

// Сам запрос (request) с токеном и разбором ошибок лежит в auth.js.
// Бэкенд называет поле телефона по-разному: при добавлении ждёт phone_number,
// а в ответе отдаёт phoneNumber. Внутри страницы везде используем phone.

const fetchAll = async () => (await request(API)).response || [];
const fetchOne = async (id) => {
  const s = await request(`${API}/${id}`);
  return { ...s, phone: s.phoneNumber };
};
const createStudent = (body) => request(`${API}/save`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    name: body.name,
    surname: body.surname,
    phone_number: body.phone,
    email: body.email,
  }),
});
// Сервер меняет только телефон и email, причём пустое (null) значение не трогает,
// поэтому для очистки поля отправляем пустую строку
const updateStudent = (id, body) => request(`${API}/${id}`, {
  method: 'PUT',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ phoneNumber: body.phone ?? '', email: body.email ?? '' }),
});
const deleteStudent = (id) => request(`${API}/${id}`, { method: 'DELETE' });

// ---------- отрисовка ----------

function initials(s) {
  return ((s.name || '')[0] || '') + ((s.surname || '')[0] || '');
}

// Затухание у нижней строки показывается, только если список можно прокрутить дальше
function updateScrollHint() {
  const overflow = listEl.scrollHeight > listEl.clientHeight + 1;
  listEl.classList.toggle('scrollable', overflow);
  listEl.classList.toggle('at-end', listEl.scrollTop + listEl.clientHeight >= listEl.scrollHeight - 1);
}

function renderList() {
  const q = searchEl.value.trim().toLowerCase();
  const shown = students.filter(s =>
    `${s.name} ${s.surname}`.toLowerCase().includes(q));

  countEl.textContent = students.length ? `всего: ${students.length}` : '';
  listEl.replaceChildren();

  for (const s of shown) {
    const li = document.createElement('li');
    li.tabIndex = 0;
    li.className = s.id === selectedId ? 'active' : '';

    const av = document.createElement('span');
    av.className = 'avatar';
    av.textContent = initials(s).toUpperCase();

    const name = document.createElement('span');
    name.textContent = `${s.surname} ${s.name}`;

    const id = document.createElement('span');
    id.className = 'id';
    id.textContent = `#${s.id}`;

    li.append(av, name, id);
    li.addEventListener('click', () => toggle(s.id));
    li.addEventListener('keydown', e => {
      if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); toggle(s.id); }
    });
    listEl.append(li);
  }

  updateScrollHint();

  if (!students.length) {
    stateEl.textContent = canWrite ? 'Пока никого нет. Добавьте первого студента слева.' : 'Пока никого нет.';
  }
  else if (!shown.length) stateEl.textContent = 'Никого не нашлось.';
  else stateEl.textContent = '';
}

function renderDetails(s) {
  detailsBody.replaceChildren();

  const title = document.createElement('div');
  title.className = 'details-name';
  title.textContent = `${s.name} ${s.surname}`;

  const dl = document.createElement('dl');
  const rows = [['ID', s.id]];
  // phone/email есть в ответе только если бэкенд отдаёт StudentFullResponse
  if ('phone' in s) rows.push(['Телефон', s.phone || '—']);
  if ('email' in s) rows.push(['Email', s.email || '—']);
  for (const [k, v] of rows) {
    const dt = document.createElement('dt'); dt.textContent = k;
    const dd = document.createElement('dd'); dd.textContent = v;
    dl.append(dt, dd);
  }

  detailsBody.append(title, dl);
  detailsEl.hidden = false;
}

let toastTimer;
function toast(message, bad = false) {
  toastEl.textContent = message;
  toastEl.className = 'show' + (bad ? ' bad' : '');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { toastEl.className = ''; }, 3500);
}

// ---------- действия ----------

async function select(id) {
  selectedId = id;
  renderList();
  try {
    current = await fetchOne(id);
    renderDetails(current);
  } catch (e) {
    current = null;
    detailsEl.hidden = true;
    toast(e.message, true);
  }
}

// Повторный клик по выбранному студенту снимает выбор
function toggle(id) {
  if (id !== selectedId) return select(id);
  selectedId = null;
  current = null;
  detailsEl.hidden = true;
  renderList();
}

// Режим редактирования: та же форма, но данные уходят в PUT, а не в POST
function startEdit() {
  if (!current) return;
  editingId = current.id;
  form.elements.name.value = current.name || '';
  form.elements.surname.value = current.surname || '';
  form.elements.phone.value = current.phone || '';
  form.elements.email.value = current.email || '';
  // сервер меняет у студента только телефон и email, поэтому имя и фамилию закрываем
  form.elements.name.readOnly = true;
  form.elements.surname.readOnly = true;
  editHint.hidden = false;
  formTitle.textContent = 'Редактирование студента';
  submitBtn.textContent = 'Сохранить';
  cancelBtn.hidden = false;
  form.elements.phone.focus();
}

function stopEdit() {
  editingId = null;
  form.reset();
  for (const f of form.elements) f.classList?.remove('invalid');
  form.elements.name.readOnly = false;
  form.elements.surname.readOnly = false;
  editHint.hidden = true;
  formError.hidden = true;
  formTitle.textContent = 'Новый студент';
  submitBtn.textContent = 'Добавить';
  cancelBtn.hidden = true;
}

async function removeCurrent() {
  if (!current) return;
  if (!confirm(`Удалить студента ${current.name} ${current.surname}?`)) return;
  const id = current.id;
  try {
    await deleteStudent(id);
    if (editingId === id) stopEdit();
    selectedId = null;
    current = null;
    detailsEl.hidden = true;
    await reload();
    toast('Студент удалён');
  } catch (e) {
    toast(e.message, true);
  }
}

async function reload() {
  try {
    students = await fetchAll();
    renderList();
  } catch (e) {
    students = [];
    renderList();
    stateEl.textContent = e.message;
  }
}

form.addEventListener('submit', async e => {
  e.preventDefault();
  const v = Object.fromEntries(new FormData(form));
  const body = {
    name: v.name.trim(),
    surname: v.surname.trim(),
    phone: v.phone.trim() || null,
    email: v.email.trim() || null,
  };

  // сервер имя, фамилию, телефон и email не проверяет, поэтому проверяем здесь
  for (const f of form.elements) f.classList?.remove('invalid');
  const problems = [];
  if (!body.name) { problems.push('имя'); form.elements.name.classList.add('invalid'); }
  if (!body.surname) { problems.push('фамилию'); form.elements.surname.classList.add('invalid'); }
  // телефон: 10-15 цифр, допустимы +, пробелы, дефисы, скобки
  if (body.phone && !/^\+?(?:[\s\-()]*\d){10,15}[\s\-()]*$/.test(body.phone)) {
    problems.push('корректный телефон'); form.elements.phone.classList.add('invalid');
  }
  if (body.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(body.email)) {
    problems.push('корректный email'); form.elements.email.classList.add('invalid');
  }
  if (problems.length) {
    formError.textContent = `Укажите ${problems.join(' и ')}.`;
    formError.hidden = false;
    return;
  }
  formError.hidden = true;

  submitBtn.disabled = true;
  try {
    const wasEditing = editingId !== null;
    const saved = wasEditing
      ? await updateStudent(editingId, body)
      : await createStudent(body);
    stopEdit();
    await reload();
    await select(saved.id);
    toast(wasEditing ? 'Изменения сохранены' : 'Студент добавлен');
  } catch (err) {
    toast(err.message, true);
  } finally {
    submitBtn.disabled = false;
  }
});

listEl.addEventListener('scroll', updateScrollHint);
allEl.addEventListener('toggle', updateScrollHint);

// поиск сам раскрывает плашку «Все студенты», чтобы результат было видно
searchEl.addEventListener('input', () => {
  if (searchEl.value.trim()) allEl.open = true;
  renderList();
});
editBtn.addEventListener('click', startEdit);
cancelBtn.addEventListener('click', stopEdit);
deleteBtn.addEventListener('click', removeCurrent);

// ---------- вход и роли ----------

// STUDENT только смотрит: форму, «Изменить» и «Удалить» прячем (сервер всё равно ответил бы 403)
function applyRole() {
  document.getElementById('layout').classList.toggle('readonly', !canWrite);
  document.getElementById('who').textContent = `${auth.email} · ${describeRoles(roles)}`;
  document.getElementById('to-users').hidden = !isAdmin;
}

document.getElementById('logout').addEventListener('click', () => logout());
document.getElementById('to-users').addEventListener('click', () => location.assign('users.html'));

if (auth) {
  applyRole();
  if (canView) {
    reload();
  } else {
    stateEl.textContent = 'У вашей учётной записи нет доступа к списку студентов.';
  }
}
