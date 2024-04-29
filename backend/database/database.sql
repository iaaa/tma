.open ../database.sqlite
PRAGMA foreign_keys = ON;

BEGIN TRANSACTION;
.read accounts.sql

-- главная таблица
CREATE TABLE locations (
   id INTEGER PRIMARY KEY
,  account REFERENCES accounts(id)

   -- main location information
,  utc_time REAL  -- Time (UTC)
,  latitude REAL  
,  longitude REAL 
   -- additional info
,  course REAL    -- degrees
,  speed REAL     -- knots
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
