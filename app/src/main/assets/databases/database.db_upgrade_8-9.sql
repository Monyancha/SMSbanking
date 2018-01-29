CREATE TABLE "words" (
              	  `rule_id`	INTEGER NOT NULL,
                  `first_letter_index`	INTEGER,
                  `last_letter_index`	INTEGER,
                  `word_type`	INTEGER NOT NULL,
              	FOREIGN KEY(`rule_id`) REFERENCES rules ( _id )
              );