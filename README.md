# Student — учёт студентов (Spring Boot + PostgreSQL + JWT)

Бэкенд: Spring Boot, JPA, PostgreSQL, JWT-авторизация, три роли.
Фронтенд: обычные HTML/JS/CSS в `src/main/resources/static`.

## Как запустить

1. В PostgreSQL создать пустую базу `students_bd`.
   Подключение, которое ждёт проект (`src/main/resources/application.yml`):
   пользователь `postgres`, пароль `1111`, адрес `localhost:5432`.
   Схема `students_schema` и таблицы создаются сами при первом запуске.
   Если в схеме что то есть, то перед запуском можно выполнить команду `DROP SCHEMA students_schema CASCADE;` в pgAdmin
2. Запустить класс с `@SpringBootApplication`.
3. Открыть в браузере http://localhost:8080 .

## Готовые аккаунты

Создаются автоматически при первом запуске.

| Email | Пароль | Роль | Что можно |
|---|---|---|---|
| admin@example.com | admin | ADMIN | Всё: студенты (просмотр, добавление, изменение, удаление) и пользователи |
| student@example.com | student | STUDENT | Только просмотр списка студентов |

После входа обе роли попадают на список студентов. Администратору дополнительно доступна кнопка «Пользователи».

## API

Все запросы, кроме входа, требуют заголовок `Authorization: Bearer <token>`. Токен действует 1 час.

| Метод | Адрес | Кому | Что делает |
|---|---|---|---|
| POST | `/api/v1/auth/login` | все | Вход: `{"email","password"}` → `{"token","roles",...}` |
| GET | `/api/v1/student` | ADMIN, STUDENT | Список студентов |
| GET | `/api/v1/student/{id}` | ADMIN, STUDENT | Один студент |
| POST | `/api/v1/student/save` | ADMIN | Добавить: `{"name","surname","phone_number","email"}` |
| PUT | `/api/v1/student/{id}` | ADMIN | Изменить телефон и email: `{"phoneNumber","email"}` |
| DELETE | `/api/v1/student/{id}` | ADMIN | Удалить |
| GET | `/api/v1/users` | ADMIN | Список пользователей |
| POST | `/api/v1/users/save` | ADMIN | Создать: `{"email","password","roles"}` |
| PUT | `/api/v1/users/{id}` | ADMIN | Сменить роли: `{"email","roles"}` |
| DELETE | `/api/v1/users/{id}` | ADMIN | Удалить |

## Тесты и покрытие

Тесты лежат в `src/test/java` (JUnit 5 и MockMvc). Они запускают приложение целиком и работают с Postgres. Postgres при запуске тестов должен быть запущен.
Проверяются сервисы, мапперы, JWT, фильтр, обработчик ошибок и все ручки API (`/api/v1/auth`, `/api/v1/student`, `/api/v1/users`) с проверкой прав ролей.

Запуск с проверкой покрытия : ``` sh mvnw verify ```

Отчёт о покрытии открывается в: `target/site/jacoco/index.html`.

## Особенности

- Бэкенд сам не проверяет имя, фамилию, телефон и email — эти проверки сделаны на страницах.
- У студента можно изменить только телефон и email (имя и фамилия при изменении закрыты).
- У пользователя можно изменить только роли (email и пароль — нет).

