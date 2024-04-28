.open ../database.sqlite
PRAGMA foreign_keys = ON;

BEGIN TRANSACTION;
.read accounts.sql

-- главная таблица
CREATE TABLE locations (
   id INTEGER PRIMARY KEY
,  account REFERENCES accounts(id)

   -- main location information
,  latitude REAL
,  longitude REAL
,  utc_time DATETIME
   -- addition info
,  fix INTEGER -- Position Fix Indicator
,  satellites INTEGER -- Satellites Used

   -- service information
,  received DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX locations_account_received_index
          ON locations(account,received);

-- todo: настройки
--.read database/options.sql
COMMIT;

.shell ol setup.lisp
