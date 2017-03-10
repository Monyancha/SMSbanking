package com.khizhny.smsbanking;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Telephony.Sms;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.List;
import static com.khizhny.smsbanking.MyApplication.LOG;

import xml.SmsBankingWidget;

public class MainActivity extends AppCompatActivity implements OnMenuItemClickListener {

    private ListView listView;
    private List<Transaction> transactions;
    private SwipeRefreshLayout swipeRefreshLayout;

    Transaction selectedTransaction;
    private TransactionListAdapter transactionListAdapter;
    private Boolean hideCurrency;
    private Boolean inverseRate;
    private Boolean hideAds;
    private List <Bank> myBanks;
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG, "MainActivity creating...");


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            MyApplication.hasReadSmsPermission=(checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED);
            if (!MyApplication.hasReadSmsPermission) {
                requestPermissions(new String[]{Manifest.permission.READ_SMS},REQUEST_CODE_ASK_PERMISSIONS);
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
        swipeRefreshLayout=(SwipeRefreshLayout) findViewById(R.id.swipe_layout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(R.color.red,R.color.blue,R.color.green);
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
            intent = new Intent(this, Tip.class);
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
        //refreshAccountStates();
        //loadTransactionsTask = new LoadTransactionsTask();
        //loadTransactionsTask.execute();
        //updateMyAccountsState = new UpdateMyAccountsState();
        //updateMyAccountsState.execute();
        AdView mAdView = (AdView) findViewById(R.id.adView);
        hideAds = settings.getBoolean("hide_ads", false);
        if (!hideAds) {
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
        Log.d(MyApplication.LOG,"MainActivity stopping...");
        refreshScreenWidgets();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(MyApplication.LOG,"MainActivity destroying...");
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    MyApplication.hasReadSmsPermission=true;
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

    private void refreshScreenWidgets(){
        //Refreshing onscreen widgets
        if (myBanks.size() > 0) {
            ComponentName name = new ComponentName(MainActivity.this, SmsBankingWidget.class);
            int[] ids = AppWidgetManager.getInstance(MainActivity.this).getAppWidgetIds(name);
            Intent update = new Intent();
            update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            update.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            Log.d(MyApplication.LOG,"Sending broadcast to MainActivity to refresh widgets...");
            MainActivity.this.sendBroadcast(update);
        }
        Log.d(MyApplication.LOG,"UpdateMyAccountsState finished...");
    }

    private void refreshTransactionsList(){
        DatabaseAccess db = DatabaseAccess.getInstance(MainActivity.this);
        db.open();
        Bank bank = db.getActiveBank();
        db.close();
        if (bank!=null) { // setting list view to show active bank transactions
            Log.d(MyApplication.LOG,"LoadTransactions for "+bank.getName());
            transactions = Transaction.loadTransactions(bank, MainActivity.this);
        }
        if (transactions!=null) {
            transactionListAdapter = new TransactionListAdapter(MainActivity.this, transactions, hideCurrency, inverseRate);
            listView.setAdapter(transactionListAdapter);
        }
        swipeRefreshLayout.setRefreshing(false);
    }
}