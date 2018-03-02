package com.khizhny.smsbanking;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import static com.khizhny.smsbanking.MyApplication.LOG;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

class DbOpenHelper  extends SQLiteAssetHelper {

        private static final String DATABASE_NAME = "database.db";
        private static final int DATABASE_VERSION = 9;

        public DbOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onUpgrade(db, oldVersion, newVersion);
        Log.d(LOG, "DATABASE UPGRADE STARTED...");
        switch (oldVersion){
            case 4:
                // these version is used only in 1.09
                db.execSQL("ALTER TABLE banks ADD COLUMN editable;");
                db.execSQL("ALTER TABLE banks ADD COLUMN current_account_state;");
                db.execSQL("UPDATE banks SET editable=1, current_account_state=0");
                db.execSQL("UPDATE version SET version=5");
                Log.v(LOG, "DB Updated from 4 to 5");
            case 5: // these version is used only  in 1.10
                // adding rule_conflicts table.
                db.execSQL(
                        "CREATE TABLE \"rule_conflicts\" (\n" +
                                "\t`rule_id`\tNUMERIC NOT NULL,\n" +
                                "\t`message_date`\tTEXT NOT NULL UNIQUE,\n" +
                                "\tPRIMARY KEY(message_date),\n" +
                                "\tFOREIGN KEY(`rule_id`) REFERENCES rules ( _id )\n" +
                                ");");
                db.execSQL("UPDATE version SET version=6");
                Log.v(LOG, "DB Updated from 5 to 6");
                ///UPDATE DATABASE_VERSION on top !!!!!
                // no return for further updates.
                // All updates to versions 6+ handled by DatabaseAccess class+ update scripts in assets folder.
        }
        Log.d(LOG, "DATABASE UPGRADED.");
    }
}
