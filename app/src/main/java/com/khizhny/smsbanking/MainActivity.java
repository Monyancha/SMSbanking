package com.khizhny.smsbanking;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Telephony.Sms;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import static com.khizhny.smsbanking.MyApplication.LOG;


import jxl.Cell;
import jxl.CellView;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.NumberFormat;
import jxl.write.WritableCellFormat;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;
import xml.SmsBankingWidget;

public class MainActivity extends AppCompatActivity implements OnMenuItemClickListener {

    private ListView listView;
    private List<Transaction> transactions;
    private SwipeRefreshLayout swipeRefreshLayout;

    Transaction selectedTransaction;
    private TransactionListAdapter transactionListAdapter;
    private Boolean hideCurrency;
    private Boolean inverseRate;
    private List<Bank> myBanks;
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    private static final String EXPORT_FOLDER = "SMS banking";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG, "MainActivity creating...");
        setTitle("");


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            MyApplication.hasReadSmsPermission = (checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED);
            if (!MyApplication.hasReadSmsPermission) {
                requestPermissions(new String[]{Manifest.permission.READ_SMS}, REQUEST_CODE_ASK_PERMISSIONS);
            }
        }

        setContentView(R.layout.activity_main);
        listView = (ListView) findViewById(R.id.listView);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // List View popup menu listener
                PopupMenu popupMenu = new PopupMenu(MainActivity.this, view);
                popupMenu.setOnMenuItemClickListener(MainActivity.this);
                selectedTransaction = (Transaction) listView.getItemAtPosition(position);
                if (!selectedTransaction.hasCalculatedTransactionDate) {
                    if (selectedTransaction.ruleOptionsCount() >= 2) {
                        popupMenu.inflate(R.menu.popup_menu_main_with_switch_option);
                    } else {
                        popupMenu.inflate(R.menu.popup_menu_main);
                    }
                    popupMenu.show();
                }
            }

        });
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(R.color.red, R.color.blue, R.color.green);
        }
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshTransactionsList();
                //loadTransactionsTask = new LoadTransactionsTask();
                //loadTransactionsTask.execute();
            }
        });


        // Checking if Activity was called by widget clicking.
        // If so, changing active bank to the bank linked with this widget.
        this.getIntent().getComponent();
        int widget_bank_id = this.getIntent().getIntExtra("bank_id", 0);
        if (widget_bank_id > 0) {
            Log.d(MyApplication.LOG, "Main Activity launched from widget click and bank_id=" + widget_bank_id);
            DatabaseAccess db = DatabaseAccess.getInstance(this);
            db.open();
            db.setActiveBank(widget_bank_id);
            db.close();
        }

        DatabaseAccess db = DatabaseAccess.getInstance(this);
        db.open();
        myBanks = db.getMyBanks();
        db.close();
        if (myBanks.size() == 0) {
            // redirecting user to choose bank from template.
            Intent intent = new Intent(this, BankListActivity.class);
            intent.putExtra("bankFilter", "templates");
            startActivity(intent);
            // Showing the tip
            intent = new Intent(this, TipActivity.class);
            intent.putExtra("tip_res_id", R.string.tip_bank_1);
            startActivity(intent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(MyApplication.LOG, "MainActivity Start...");
        // Restoring preferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        hideCurrency = settings.getBoolean("hide_currency", false);
        inverseRate = settings.getBoolean("inverse_rate", false);

        // refreshing lists
        refreshTransactionsList();

        AdView mAdView = (AdView) findViewById(R.id.adView);
        if (!settings.getBoolean("hide_ads", false)) {
            // real: ca-app-pub-1260562111804726/2944681295
            MobileAds.initialize(getApplicationContext(), getResources().getString(R.string.banner_ad_unit_id));
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        } else {
            mAdView.setVisibility(View.GONE);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Listener for Menu options
        int id = item.getItemId();
        if (id == R.id.action_preferences) {
            Intent intent = new Intent(this, PrefActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_bank_list) {
            Intent intent = new Intent(this, BankListActivity.class);
            intent.putExtra("bankFilter", "templates");
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_bank_my_list) {
            Intent intent = new Intent(this, BankListActivity.class);
            intent.putExtra("bankFilter", "myBanks");
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_rule_list) {
            Intent intent = new Intent(this, RuleListActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_statistics) {
            Intent intent = new Intent(this, StatisticsActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_export_transactions) {
            exportToExcel();

            return true;
        }
        if (id == R.id.action_rate_app) {
            Uri uri = Uri.parse("market://details?id=" + getPackageName());
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
            // To count with Play market backstack, After pressing back button,
            // to taken back to our application, we need to add following flags to intent.
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            try {
                startActivity(goToMarket);
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
            }
            return true;
        }
        if (id == R.id.action_privacy) {
            String url = "http://4pda.ru/forum/index.php?showtopic=730676&st=20#entry58120636";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        }
        if (id == R.id.action_quit) {
            this.finish();
            System.exit(0);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean onMenuItemClick(MenuItem item) {
        // Listener for transactions popup menu
        Intent intent;
        switch (item.getItemId()) {
            case R.id.item_new_rule:
                intent = new Intent(this, RuleActivity.class);
                intent.putExtra("sms_body", selectedTransaction.getBody());
                intent.putExtra("todo", "add");
                startActivity(intent);
                return true;
            //deleting SMS
            case R.id.item_delete_sms:
                String defaultSmsApp;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    defaultSmsApp = Sms.getDefaultSmsPackage(this);
                    if (!defaultSmsApp.equals(getPackageName())) {
                        intent = new Intent(Sms.Intents.ACTION_CHANGE_DEFAULT);
                        intent.putExtra(Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
                        startActivity(intent);
                    }
                }
                try {
                    Uri uriSms = Uri.parse("content://sms/inbox");
                    Cursor c = getContentResolver().query(
                            uriSms,
                            new String[]{"_id", "thread_id", "address", "person",
                                    "date", "body"}, "_id=" + selectedTransaction.smsId, null, null);
                    if (c != null && c.moveToFirst()) {
                        long id = c.getLong(0);
                        ContentValues values = new ContentValues();
                        values.put("read", true);
                        getContentResolver().update(Uri.parse("content://sms/"),
                                values, "_id=" + id, null);
                        getContentResolver().delete(
                                Uri.parse("content://sms/" + id), "date=?",
                                new String[]{c.getString(4)});
                        c.close();
                        // refreshing.
                        transactions.remove(selectedTransaction);
                        transactionListAdapter.notifyDataSetChanged();
                    }
                } catch (Exception e) {
                    Log.e("log>>>", e.toString());
                }
                return true;
            case R.id.item_switch_rule:
                // if user clicked to change Rule
                selectedTransaction.switchRule(this);
                this.onStart();
                return true;
        }
        return false;
    }

    @Override
    protected void onStop() {
        Log.d(MyApplication.LOG, "MainActivity stopping...");
        refreshScreenWidgets();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(MyApplication.LOG, "MainActivity destroying...");
        // restoring default SMS managing application
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (!MyApplication.defaultSmsApp.equals(getPackageName())) {
                Intent intent = new Intent(Sms.Intents.ACTION_CHANGE_DEFAULT);
                intent.putExtra(Sms.Intents.EXTRA_PACKAGE_NAME, MyApplication.defaultSmsApp);
                startActivity(intent);
            }
        }
        super.onDestroy();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[]  permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    MyApplication.hasReadSmsPermission = true;
                    refreshTransactionsList();
                } else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "READ_SMS permision denied", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void refreshScreenWidgets() {
        //Refreshing onscreen widgets
        if (myBanks.size() > 0) {
            ComponentName name = new ComponentName(MainActivity.this, SmsBankingWidget.class);
            int[] ids = AppWidgetManager.getInstance(MainActivity.this).getAppWidgetIds(name);
            Intent update = new Intent();
            update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            update.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            Log.d(MyApplication.LOG, "Sending broadcast to MainActivity to refresh widgets...");
            MainActivity.this.sendBroadcast(update);
        }
        Log.d(MyApplication.LOG, "UpdateMyAccountsState finished...");
    }

    private class TransactionListAdapter extends ArrayAdapter<Transaction> {

        private final Context context;
        private List<Transaction> transactions;
        private boolean hideCurrency;
        private boolean inverseRate;

        TransactionListAdapter(Context context, List<Transaction> transactions, boolean hideCurrency, boolean inverseRate) {
            super(context, R.layout.activity_main_list_row, transactions);
            this.context = context;
            this.transactions = transactions;
            this.hideCurrency=hideCurrency;
            this.inverseRate=inverseRate;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View rowView = convertView;
            if (rowView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = inflater.inflate(R.layout.activity_main_list_row, parent, false);
            }
            Transaction t = transactions.get(position);
            TextView smsTextView;

            // Filling Massage text
            smsTextView = (TextView) rowView.findViewById(R.id.smsBody);
            if (t.ruleOptionsCount()>=2){
                ((TextView) rowView.findViewById(R.id.warning_sign)).setText(String.format("(%d)", t.ruleOptionsCount()));
                smsTextView.setText(t.getBody());
            }else{
                ((TextView) rowView.findViewById(R.id.warning_sign)).setText("");
                smsTextView.setText(t.getBody());
            }

            // Filling Before state
            TextView accountBeforeView = (TextView) rowView.findViewById(R.id.stateBefore);
            if (t.hasStateBefore){
                accountBeforeView.setText(t.getAccountStateBeforeAsString(hideCurrency));
            } else {
                accountBeforeView.setText("");
            }
            // Filling Transaction Date
            TextView dateView = (TextView) rowView.findViewById(R.id.transanction_date);
            if (t.hasTransactionDate){
                dateView.setText(t.getTransactionDateAsString("dd.MM.yyyy"));
            }else {
                dateView.setText("");
            }
            // Filling After state
            TextView accountAfterView = (TextView) rowView.findViewById(R.id.stateAfter);
            if (t.hasStateAfter){
                accountAfterView.setText(t.getAccountStateAfterAsString(hideCurrency));
            }else {
                accountAfterView.setText("");
            }
            // Filling comission
            TextView accountComissionView = (TextView) rowView.findViewById(R.id.transactionComission);
            if (t.getCommission().equals(new BigDecimal("0.00"))) {
                accountComissionView.setVisibility(View.GONE);
            } else {
                accountComissionView.setVisibility(View.VISIBLE);
                accountComissionView.setText(t.getCommissionAsString(hideCurrency));
                accountComissionView.setTextColor(Color.rgb(218, 48, 192)); //pink
            }
            // Filling difference
            TextView accountDifferenceView = (TextView) rowView.findViewById(R.id.stateDifference);
            if (t.hasStateDifference){
                accountDifferenceView.setVisibility(View.VISIBLE);
                accountDifferenceView.setText(t.getAccountDifferenceAsString(hideCurrency,inverseRate));
                switch (t.getStateDifference().signum()) {
                    case -1:
                        accountDifferenceView.setTextColor(Color.RED);
                        break;
                    case 0:
                        accountDifferenceView.setTextColor(Color.GRAY);
                        break;
                    case 1:
                        accountDifferenceView.setTextColor(Color.rgb(0,100,0)); // dark green
                }
            } else {
                //accountDifferenceView.setVisibility(View.GONE);
                accountDifferenceView.setText("");
            }
            // Changing icon
            ImageView iconView = (ImageView) rowView.findViewById(R.id.transanctionIcon);
            iconView.setImageResource(t.icon);

            // Filling Extra parameters
            if (t.hasExtras()){
                ((TextView) rowView.findViewById(R.id.extra1)).setText(t.getExtraParam1());
                ((TextView) rowView.findViewById(R.id.extra2)).setText(t.getExtraParam2());
                ((TextView) rowView.findViewById(R.id.extra3)).setText(t.getExtraParam3());
                ((TextView) rowView.findViewById(R.id.extra4)).setText(t.getExtraParam4());
                rowView.findViewById(R.id.extras_ll).setVisibility(View.VISIBLE);
            } else {
                rowView.findViewById(R.id.extras_ll).setVisibility(View.GONE);
            }

            return rowView;
        }

    }

    private void refreshTransactionsList() {
        DatabaseAccess db = DatabaseAccess.getInstance(MainActivity.this);
        db.open();
        Bank bank = db.getActiveBank();
        db.close();
        if (bank != null) { // setting list view to show active bank transactions
            Log.d(MyApplication.LOG, "LoadTransactions for " + bank.getName());
            transactions = Transaction.loadTransactions(bank, MainActivity.this);
        }
        if (transactions != null) {
            transactionListAdapter = new TransactionListAdapter(MainActivity.this, transactions, hideCurrency, inverseRate);
            listView.setAdapter(transactionListAdapter);
        }
        swipeRefreshLayout.setRefreshing(false);
    }

    private void exportToExcel() {        //Saving in external storage
        File sdCard = Environment.getExternalStorageDirectory();
        File directory = new File(sdCard.getAbsolutePath() + "/" + EXPORT_FOLDER);

        //create directory if not exist
        if (!directory.isDirectory()) {
            if (!directory.mkdirs()){
                Toast.makeText(this, "Can't create forder " + directory, Toast.LENGTH_SHORT).show();
            }
        }

        DatabaseAccess db = DatabaseAccess.getInstance(MainActivity.this);
        db.open();
        Bank bank = db.getActiveBank();
        db.close();

        //file path
        String FILE_NAME = bank.getName() + ".xls";
        File file = new File(directory, FILE_NAME);

        WorkbookSettings wbSettings = new WorkbookSettings();
        wbSettings.setLocale(new Locale("en", "EN"));
        WritableWorkbook workbook;

        try {
            workbook = Workbook.createWorkbook(file, wbSettings);
            WritableSheet worksheet = workbook.createSheet(bank.getName(), 0);

            try {
                int i = 0;
                worksheet.addCell(new Label(0, i, "N"));
                worksheet.addCell(new Label(1, i, "TransactionDate"));
                worksheet.addCell(new Label(2, i, "State before"));
                worksheet.addCell(new Label(3, i, "Difference"));
                worksheet.addCell(new Label(4, i, "DifferenceInNativeCurrency"));
                worksheet.addCell(new Label(5, i, "Rate"));
                worksheet.addCell(new Label(6, i, "Commission"));
                worksheet.addCell(new Label(7, i, "StateAfter"));
                worksheet.addCell(new Label(8, i, "TransactionCurrency"));
                worksheet.addCell(new Label(9, i, "ExtraParam1"));
                worksheet.addCell(new Label(10, i, "ExtraParam2"));
                worksheet.addCell(new Label(11, i, "ExtraParam3"));
                worksheet.addCell(new Label(12, i, "ExtraParam4"));
                worksheet.addCell(new Label(13, i, "TransactionType"));
                worksheet.addCell(new Label(14, i, "SMS"));

                for (Transaction t : transactions) {
                    i = i + 1;
                    worksheet.addCell(new Label(0, i, i + ""));
                    worksheet.addCell(new Label(1, i, t.getTransactionDateAsString("yyyy-MM-dd hh:mm:ss")));
                    if (t.hasStateBefore)     addBigDecimal(worksheet, 2, i, t.getStateBefore(),2);
                    if (t.hasStateDifference) {
                        addBigDecimal(worksheet, 3, i, t.getStateDifference(),2);
                        addBigDecimal(worksheet, 4, i, t.getStateDifferenceInNativeCurrency(),2);
                        if (!(t.getCurrencyRate().equals(new BigDecimal("1.000")))) addBigDecimal(worksheet, 5, i, t.getCurrencyRate(),3);
                    }
                    if (!(t.getCommission().equals(new BigDecimal("0.00")))) addBigDecimal(worksheet, 6, i, t.getCommission(),2);
                    if (t.hasStateAfter)  addBigDecimal(worksheet, 7, i, t.getStateAfter(),2);
                    worksheet.addCell(new Label(8, i, t.getTransactionCurrency()));
                    worksheet.addCell(new Label(9, i, t.getExtraParam1()));
                    worksheet.addCell(new Label(10, i, t.getExtraParam2()));
                    worksheet.addCell(new Label(11, i, t.getExtraParam3()));
                    worksheet.addCell(new Label(12, i, t.getExtraParam4()));
                    worksheet.addCell(new Label(13, i, t.getTransactionType()));
                    worksheet.addCell(new Label(14, i, t.getBody()));
                }
                sheetAutoFitColumns(worksheet);
                workbook.write();
                workbook.close();
                Toast.makeText(this, "File saved to " + directory + "/" + FILE_NAME, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.fromFile(file));
                startActivity(intent);
            } catch (RowsExceededException e) {
                Toast.makeText(this, "Too many rows to save.", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            } catch (WriteException e) {
                Toast.makeText(this, "Can't write to " + directory + "/" + FILE_NAME, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Can't write to " + directory + "/" + FILE_NAME, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void sheetAutoFitColumns(WritableSheet sheet) {
        for (int i = 0; i < sheet.getColumns(); i++) {
            Cell[] cells = sheet.getColumn(i);
            int longestStrLen = -1;

            if (cells.length == 0)
                continue;

        /* Find the widest cell in the column. */
            for (Cell cell : cells) {
                if (cell.getContents().length() > longestStrLen) {
                    String str = cell.getContents();
                    if (str == null || str.isEmpty())
                        continue;
                    longestStrLen = str.trim().length();
                }
            }

        /* If not found, skip the column. */
            if (longestStrLen == -1)
                continue;

        /* If wider than the max width, crop width */
            if (longestStrLen > 255)
                longestStrLen = 255;

            CellView cv = sheet.getColumnView(i);
            cv.setSize(longestStrLen * 256 + 100); /* Every character is 256 units wide, so scale it. */
            sheet.setColumnView(i, cv);
        }
    }

    private void addBigDecimal(WritableSheet sheet, int column, int row, BigDecimal value, int digits) throws WriteException {
        NumberFormat numberFormat;
        if (digits==3) {
            numberFormat = new NumberFormat("0.000");
        }else{
            numberFormat = new NumberFormat("0.00");
        }
        WritableCellFormat cellFormat = new WritableCellFormat(numberFormat);
        Double v = round(value.doubleValue(),digits);
        Number number = new Number(column, row, v, cellFormat);
        sheet.addCell(number);
    }

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}
