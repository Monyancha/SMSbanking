CREATE TABLE "words" (
              	  `rule_id`	INTEGER NOT NULL,
                  `first_letter_index`	INTEGER,
                  `last_letter_index`	INTEGER,
                  `word_type`	INTEGER NOT NULL,
              	FOREIGN KEY(`rule_id`) REFERENCES rules ( _id )
              );

ALTER TABLE "subrules" ADD COLUMN regex_phrase_index INTEGER DEFAULT 0;
ALTER TABLE "rules" ADD COLUMN advanced INTEGER DEFAULT 0;
DELETE FROM "transactions";