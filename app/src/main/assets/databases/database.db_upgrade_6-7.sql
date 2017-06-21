CREATE TABLE "transactions" (
	`bank_id`	INTEGER NOT NULL,
	`transaction_date`	INTEGER NOT NULL,
	`account_currency`	TEXT NOT NULL,
	`sms_body`	NUMERIC,
    `sms_id`	INTEGER,
    `icon`	INTEGER,
	`transaction_currency`	TEXT NOT NULL,
	`state_before`	TEXT,
	`state_after`	TEXT,
	`state_difference`	TEXT,
	`commission`	TEXT,
	`extra1`	TEXT,
	`extra2`	TEXT,
	`extra3`	TEXT,
	`extra4`	TEXT,
	`exchange_rate`	TEXT,
	FOREIGN KEY(`bank_id`) REFERENCES banks ( _id )
);
DELETE FROM version WHERE version=6;
INSERT INTO version(version) VALUES(7);