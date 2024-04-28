-- account идентифицирует пользователя,
--    один пользователь - один аккаунт
CREATE TABLE accounts (
   id PRIMARY KEY

,  login TEXT UNIQUE
,  password TEXT -- здесь хранятся хеши, не пароли!
,  name TEXT  -- имя пользователя

,  enabled INTEGER DEFAULT 0 -- по-умолчанию аккаунт не активирован
,  creation_time DATETIME DEFAULT CURRENT_TIMESTAMP

,  session TEXT UNIQUE -- сеансовый ключ
,  remote_address TEXT -- адрес, с которого зашли в аккаунт (todo: завести отдельную табличку, и позволить множественные заходы с разных адресов?)
,  login_time DATETIME DEFAULT CURRENT_TIMESTAMP -- время последнего логина
);
