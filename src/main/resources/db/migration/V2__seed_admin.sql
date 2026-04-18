-- ========================
-- SEED: первый администратор
-- ========================
-- login:    admin
-- password: admin123
--
-- Хэш сгенерирован BCrypt (at.favre.lib:bcrypt:0.10.2) с cost=12.
-- id не указываем — BIGSERIAL сам подставит 1 (первая запись в таблице).
--
-- ВАЖНО: на проде этого пользователя нужно либо удалить, либо сменить пароль
-- через /api/auth/* ручки после первого логина.
INSERT INTO users (login, password_hash, role)
VALUES ('admin', '$2a$12$C183zqkOOJH4Bc5L.kCN9ecMj6NHwD7/Mhq4XZmJLsvAF2Wpmyy.y', 'ADMIN');
