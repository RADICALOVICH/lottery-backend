-- ========================
-- SEED: первый администратор
-- ========================
-- login:    admin
-- password: admin123
--
-- Хэш сгенерирован BCrypt с cost=12.
-- id не указываем — BIGSERIAL сам подставит 1 (первая запись в таблице).
INSERT INTO users (login, password_hash, role)
VALUES ('admin', '$2a$12$C183zqkOOJH4Bc5L.kCN9ecMj6NHwD7/Mhq4XZmJLsvAF2Wpmyy.y', 'ADMIN');
