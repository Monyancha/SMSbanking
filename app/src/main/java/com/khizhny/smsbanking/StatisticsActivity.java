package com.khizhny.smsbanking;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.*;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.khizhny.smsbanking.model.Bank;
import com.khizhny.smsbanking.model.Transaction;

import static com.khizhny.smsbanking.MyApplication.LOG;
import static com.khizhny.smsbanking.MyApplication.db;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatisticsActivity extends AppCompatActivity{
    private List<Transaction> transactions;
    private int step=0;
    private boolean showValues;
    private boolean showIncome;
    private boolean showOutcome;
    private boolean showBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG,"Statistics Activity Creating...");
        setContentView(R.layout.activity_statistics);
        Bank bank=db.getActiveBank();
        if (bank!=null) {
            transactions = Transaction.loadTransactions(bank,this);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG,"Statistics Activity Resuming...");
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        step = settings.getInt("step", 5);
        showValues = settings.getBoolean("showValues", false);
        showIncome = settings.getBoolean("showIncome", true);
        showOutcome = settings.getBoolean("showOutcome", true);
        showBalance = settings.getBoolean("showBalance", true);
        CombinedChart chart = findViewById(R.id.chart);
        if (chart != null) {
            chart.setDescription("Statistics");
            chart.setDescriptionPosition(0, 0);
            chart.setBackgroundColor(Color.WHITE);
            chart.setDrawGridBackground(false);
            chart.setDrawBarShadow(false);
            chart.setDrawValueAboveBar(true);
            chart.getAxisRight().setEnabled(false);
            chart.getLegend().setEnabled(true);
            chart.setTouchEnabled(true);
            // draw bars behind lines
            chart.setDrawOrder(new CombinedChart.DrawOrder[]{
                    CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.BUBBLE, CombinedChart.DrawOrder.CANDLE, CombinedChart.DrawOrder.LINE, CombinedChart.DrawOrder.SCATTER
            });

            MyMarkerView mv = new MyMarkerView(this, R.layout.stats_marker);
            // set the marker to the chart
            chart.setMarkerView(mv);

            YAxis rightAxis = chart.getAxisRight();
            rightAxis.setDrawGridLines(false);
            rightAxis.setAxisMinValue(0f); // this replaces setStartAtZero(true)

            YAxis leftAxis = chart.getAxisLeft();
            leftAxis.setDrawGridLines(false);
            leftAxis.setDrawZeroLine(true); // draw a zero line
            leftAxis.setZeroLineColor(Color.GRAY);
            leftAxis.setZeroLineWidth(0.7f);
            //leftAxis.setAxisMinValue(0f); // this replaces setStartAtZero(true)

            XAxis xAxis = chart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTH_SIDED);

            AppCompatSpinner stepView = findViewById(R.id.rule_type);
            if (stepView != null) {
                stepView.setSelection(step);
                stepView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parentView, View v, int selectedPosition, long id) {
                        if (selectedPosition >= 1 && selectedPosition != step) {
                            SharedPreferences.Editor settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                            settings.putInt("step", selectedPosition);
                            settings.apply();
                            onStart();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parentView) {
                    }
                });
            }

            if (transactions!=null) {
                if (transactions.size() > 1) {
                    Date start_date = transactions.get(transactions.size() - 1).getTransactionDate();
                    /*shifting to the beginning of the day*/
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(start_date);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    start_date = cal.getTime();

                    int lastBarIndex = transactions.get(0).getDateIndex(start_date, step);
                    //int lastTransactionIndex = transactions.size() - 1;
                    // Filling categories array
                    List<String> cat = new ArrayList<String>(); // categories list
                    ArrayList<BarEntry> barEntries = new ArrayList<BarEntry>();  // list for positive BarValues
                    ArrayList<Entry> lineEntries = new ArrayList<Entry>();  // list for line value (max ballance state)
                    int transactionIndex = transactions.size() - 1;
                    int currentTransactionBarIndex = transactions.get(transactionIndex).getDateIndex(start_date, step);

                    Transaction t;
                    float diff;
                    float currentBalance;
                    if (transactions.get(transactionIndex).hasStateAfter) {
                        currentBalance = transactions.get(transactionIndex).getStateAfter().floatValue();
                    } else {
                        currentBalance = 0;
                    }
                    float balance;
                    float totalIncome;
                    float totalOutcome;

                    for (int barIndex = 0; barIndex <= lastBarIndex; barIndex++) { // i - Bar index
                        balance = currentBalance;
                        totalIncome = 0;
                        totalOutcome = 0;
                        // calculating total balance income and outcome and maximum balance
                        while (currentTransactionBarIndex == barIndex && transactionIndex >= 0) {
                            t = transactions.get(transactionIndex);
                            if (t.hasStateAfter) {
                                currentBalance = t.getStateAfter().floatValue();
                            }
                            if (currentBalance > balance) {
                                balance = currentBalance;
                            }
                            diff = 0;
                            if (t.hasStateDifference) {
                                diff = t.getStateDifferenceInNativeCurrency(true).floatValue();
                            }
                            if (diff > 0) {
                                totalIncome = totalIncome + diff;
                            } else {
                                totalOutcome = totalOutcome + diff;
                            }
                            transactionIndex = transactionIndex - 1;
                            if (transactionIndex >= 0) {
                                currentTransactionBarIndex = transactions.get(transactionIndex).getDateIndex(start_date, step);
                            }
                        }

                        cat.add(barIndex, getDateIndexLabel(start_date, step, barIndex));
                        if (totalIncome != 0 || totalOutcome != 0)
                            if (showIncome) {
                                if (showOutcome) {
                                    barEntries.add(new BarEntry(new float[]{totalIncome, totalOutcome}, barIndex));
                                } else {
                                    barEntries.add(new BarEntry(new float[]{totalIncome, 0.0f}, barIndex));
                                }
                            } else {
                                if (showOutcome) {
                                    barEntries.add(new BarEntry(new float[]{0.0f, totalOutcome}, barIndex));
                                } else {
                                    barEntries.add(new BarEntry(new float[]{0.0f, 0.0f}, barIndex));
                                }
                            }

                        lineEntries.add(new Entry(balance, barIndex));
                    }

                    BarDataSet barDataSet = new BarDataSet(barEntries, "");
                    barDataSet.setStackLabels(new String[]{getString(R.string.stats_income), getString(R.string.stats_outcome)});
                    barDataSet.setValueTextColor(Color.RED);
                    barDataSet.setValueTextSize(10f);
                    barDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
                    barDataSet.setColors(new int[]{Color.rgb(60, 220, 78), Color.rgb(250, 0, 0)});
                    //List <Integer> colors = new ArrayList<Integer>();
                    //colors.add(Color.rgb(60, 220, 78));
                    //colors.add(Color.rgb(250, 0, 0));
                    //barDataSet.setValueTextColors(colors);
                    barDataSet.setValueFormatter(new ValueFormatter() {

                        @Override
                        public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                            BigDecimal bd = new BigDecimal(Float.toString(value));
                            bd = bd.setScale(0, BigDecimal.ROUND_HALF_UP);
                            switch (bd.signum()) {
                                case 0:
                                    return "";
                                case 1:
                                    return "+" + bd.toString();
                                default:
                                    return bd.toString();
                            }
                        }
                    });
                    BarData barData = new BarData();
                    barData.addDataSet(barDataSet);

                    LineData maxLineData = new LineData();
                    LineDataSet maxLineDataSet = new LineDataSet(lineEntries, getString(R.string.stats_balance));
                    maxLineDataSet.setColor(Color.BLUE);
                    maxLineDataSet.setLineWidth(2.5f);
                    maxLineDataSet.setCircleColor(Color.BLUE);
                    maxLineDataSet.setCircleRadius(2f);
                    maxLineDataSet.setFillColor(Color.rgb(240, 238, 70));
                    maxLineDataSet.setDrawCubic(false);
                    maxLineDataSet.setValueTextSize(10f);
                    maxLineDataSet.setValueTextColor(Color.BLUE);
                    maxLineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
                    if (showValues) {
                        maxLineDataSet.setDrawValues(true);
                    } else {
                        maxLineDataSet.setDrawValues(false);
                    }
                    maxLineData.addDataSet(maxLineDataSet);

                    ArrayList<ILineDataSet> lineDataSets = new ArrayList<ILineDataSet>();
                    lineDataSets.add(maxLineDataSet);

                    LineData lineData = new LineData(cat, lineDataSets);

                    CombinedData combinedData = new CombinedData(cat);

                    if (showBalance) {
                        combinedData.setData(lineData);
                    }
                    combinedData.setData(barData);
                    chart.setData(combinedData);
                    chart.animateXY(2500, 2500);
                }
            }
        }
    }

    /**
     * Function gives a pretty label for a statistics chart categories.
     * @param startDate The starting date of the chart.
     * @param step - step size. 1-Year, 2-Quater, 3-Month, 4-Week,  5-Day
     * @param index - index of bar chart that have to be labeled.
     * @return Label for a BAR value.
     */
    private String getDateIndexLabel(Date startDate,int step, int index){
        Calendar cal = Calendar.getInstance();
        cal.setMinimalDaysInFirstWeek(7);
        cal.setTime(startDate);


        /*shifting calendar from start date */
        switch (step){
            case 1: //Year
                cal.add(Calendar.YEAR,index);
                break;
            case 2: //Quarter
                cal.add(Calendar.MONTH,index*3);
                break;
            case 3: //Month
                cal.add(Calendar.MONTH,index);
                break;
            case 4: //Week
                cal.add(Calendar.DAY_OF_YEAR,7*index);
                break;
            case 5: //Day
                cal.add(Calendar.DAY_OF_YEAR,index);
                break;
        }
        int year=cal.get(Calendar.YEAR);
        int week=cal.get(Calendar.WEEK_OF_YEAR); // 0-53
        int month=cal.get(Calendar.MONTH);  // 0-11
        int day=cal.get(Calendar.DAY_OF_MONTH); // 1-365
        int quarter = month /3;  //0-3
        switch (step){
            case 1: //Year
                return "Y"+year;
            case 2: //Quarter
                return "Y"+year+"Q"+(quarter+1);
            case 3: //Month
                return "Y"+year+"M"+(month+1);
            case 4: //Week
                return "Y"+year+"W"+week;
            case 5: //Day
                //
                return String.format(Locale.getDefault(), "%1$td/%1$tm/%1$tY", cal);
            default:
                return "";
        }
    }

    @Override
    protected void onStop() {
        Log.d (LOG,"Statistics Activity stopped.");
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_statistics_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Listener for Menu options
        SharedPreferences.Editor settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        switch (item.getItemId()){
            case R.id.item_stat_balance:
                showBalance=!showBalance;
                settings.putBoolean("showBalance", showBalance);
                break;
            case R.id.item_stat_values:
                showValues=!showValues;
                settings.putBoolean("showValues", showValues);
                break;
            case R.id.item_stat_income:
                showIncome=!showIncome;
                settings.putBoolean("showIncome", showIncome);
                break;
            case R.id.item_stat_outcome:
                showOutcome=!showOutcome;
                settings.putBoolean("showOutcome", showOutcome);
                break;
            case android.R.id.home:
                finish();
        }
        settings.apply();
        onStart();
        return true;
    }

    private class MyMarkerView extends MarkerView {

        private TextView tvContent;

        public MyMarkerView(Context context, int layoutResource) {
            super(context, layoutResource);

            tvContent = findViewById(R.id.tvContent);
        }

        // callbacks everytime the MarkerView is redrawn, can be used to update the
        // content (user-interface)
        @Override
        public void refreshContent(Entry e, Highlight highlight) {
            String value;
            if (e instanceof CandleEntry) {
                CandleEntry ce = (CandleEntry) e;
                value= Utils.formatNumber(ce.getLow(), 0, true);
            } else {
                value=Utils.formatNumber(e.getVal(), 0, true);
            }
            tvContent.setText(value);
        }

        @Override
        public int getXOffset(float xpos) {
            // this will center the marker-view horizontally
            return -(getWidth() / 2);
        }

        @Override
        public int getYOffset(float ypos) {
            // this will cause the marker-view to be above the selected value
            return -getHeight();
        }
    }
}
