.open ./database.sqlite
PRAGMA foreign_keys = ON;

BEGIN TRANSACTION;

-- главная таблица
CREATE TABLE locations (
   id TEXT PRIMARY KEY -- Firebase Instance Id
,  room TEXT -- TODO: REFERENCES rooms(id)
-- ,  account REFERENCES accounts(id)

   -- main location information
,  lat REAL, lon REAL, alt REAL
,  dat REAL, tim REAL

   -- additional info
,  speed REAL    -- knots
,  angle REAL    -- degrees
-- ,  quality INTEGER

   -- addition info
,  fix INTEGER -- Position Fix Indicator (1+ ок, 0 fail)
,  satellites_used INTEGER -- Satellites Used
,  hdop REAL -- HDOP

   -- service information
,  received DATETIME DEFAULT CURRENT_TIMESTAMP
);
-- CREATE INDEX locations_account_received_index
--           ON locations(account,received);

-- history


COMMIT;
