package xml;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import com.khizhny.smsbanking.model.Bank;
import com.khizhny.smsbanking.ColorPickerView;
import com.khizhny.smsbanking.R;

import java.util.List;

import static com.khizhny.smsbanking.MyApplication.db;

/**
 * The configuration screen for the {@link SmsBankingWidget SmsBankingWidget} AppWidget.
 */
public class SmsBankingWidgetConfigureActivity extends Activity {

    private static final String PREFS_NAME = "xml.SmsBankingWidget";
    public static final String PREF_BANK_ID = "bank_id_for_widget_";
    public static final String PREF_COLOR = "text_color_for_widget_";
    public static final String PREF_BACKGROUND = "text_backgound_for_widget_";
    public static final String PREF_SIZE = "text_size_for_widget_";
    public static final String PREF_LAST_USED_COLOR = "last_used_text_color";
    public static final String PREF_LAST_USED_BACKGROUND = "last_used_text_backgound";
    public static final String PREF_LAST_USED_SIZE = "last_used_text_size";
    private static final int MIN_FONT_SIZE = 10;

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    Spinner myBank;
    TextView sampleText;
    ImageView sampleBackground;
    SeekBar fontSize;
    int textColor;
    int backColor;
    int textSize;

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            // Add button listener
            final Context context = SmsBankingWidgetConfigureActivity.this;

            // When the button is clicked, store the string locally
            Bank bank = (Bank) myBank.getSelectedItem();
            if (bank!=null) {
                saveWidgetPref(context, mAppWidgetId, bank.getId(), textColor, backColor, textSize);
                // It is the responsibility of the configuration activity to update the app widget
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                SmsBankingWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId);
                // Make sure we pass back the original appWidgetId
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
            }
            finish();
        }
    };

    public SmsBankingWidgetConfigureActivity() {
        super();
    }

    // Write the prefix to the SharedPreferences object for this widget
    static void saveWidgetPref(Context context, int appWidgetId, int bankId,int color,int background,int textSize) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putInt(PREF_BANK_ID + appWidgetId, bankId);
        prefs.putInt(PREF_COLOR + appWidgetId, color);
        prefs.putInt(PREF_BACKGROUND + appWidgetId, background);
        prefs.putInt(PREF_SIZE + appWidgetId, textSize);
        prefs.putInt(PREF_LAST_USED_SIZE, textSize);
        prefs.putInt(PREF_LAST_USED_COLOR, color);
        prefs.putInt(PREF_LAST_USED_BACKGROUND, background);
        prefs.apply();
    }

    /**
     * Reads saved parameters from SharedPreferences object for this widget
     * @param context context
     * @param appWidgetId widget ID
     * @param parameter String parameter name
     * @return Int value saved in pref. If there is no preference saved, default is 0.
     */
    static int loadSavedIntFromPref(Context context, int appWidgetId, String parameter) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getInt(parameter + appWidgetId, 0);
    }

    /**
     * Reads common widgets int value saved in shared pref.
     * @param context context
     * @param parameter name of the parameter
     * @return Int value saved. If not found 0.
     */
    static int loadSavedIntFromPref(Context context, String parameter) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getInt(parameter, 0);
    }

    static void deleteWidgetInfoFromPref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_BANK_ID + appWidgetId);
        prefs.remove(PREF_COLOR + appWidgetId);
        prefs.remove(PREF_BACKGROUND + appWidgetId);
        prefs.remove(PREF_SIZE + appWidgetId);
        prefs.apply();
    }

    /**
     * Deletes all common settings when no widgets used any more.
     * @param context context
     */
    static void deleteWidgetInfoFromPref(Context context) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_LAST_USED_COLOR);
        prefs.remove(PREF_LAST_USED_BACKGROUND );
        prefs.remove(PREF_LAST_USED_SIZE);
        prefs.apply();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.sms_banking_widget_configure);

        findViewById(R.id.add_button).setOnClickListener(mOnClickListener);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        // Filling Spinner vith myBanks options
        List <Bank> banks = db.getMyBanks(getCountry());
        ArrayAdapter <Bank> myBankAdapter = new ArrayAdapter <Bank>(this,
                android.R.layout.simple_spinner_item, banks);
        myBank = (Spinner) findViewById(R.id.my_bank);
        myBank.setAdapter(myBankAdapter);

        // last used default colors and sizes loading from preferences
        textColor = loadSavedIntFromPref(this, SmsBankingWidgetConfigureActivity.PREF_LAST_USED_COLOR);
        backColor = loadSavedIntFromPref(this, SmsBankingWidgetConfigureActivity.PREF_LAST_USED_BACKGROUND);
        textSize  = loadSavedIntFromPref(this, SmsBankingWidgetConfigureActivity.PREF_LAST_USED_SIZE);
        if (textColor==0) textColor=Color.YELLOW;
        if (backColor==0) backColor=Color.BLUE;
        if (textSize<MIN_FONT_SIZE) textSize=MIN_FONT_SIZE;

        // Aplying to sample
        sampleText = (TextView)  findViewById(R.id.widget_sample);
        sampleText.setTextColor(textColor);
        sampleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);

        sampleBackground = (ImageView)  findViewById(R.id.widget_sample_background);
        sampleBackground.setColorFilter(backColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            sampleBackground.setImageAlpha(Color.alpha(backColor));
        } else {
            //noinspection deprecation
            sampleBackground.setAlpha(Color.alpha(backColor));
        }

        ColorPickerView textColorPickerView = (ColorPickerView)  findViewById(R.id.text_color);
        textColorPickerView.setAlphaSliderVisible(true);
        textColorPickerView.setColor(textColor);
        textColorPickerView.setOnColorChangedListener(new ColorPickerView.OnColorChangedListener() {
            @Override
            public void onColorChanged(int color) {
                sampleText.setTextColor(color);
                textColor = color;
            }
        });

        ColorPickerView backColorPickerView = (ColorPickerView)  findViewById(R.id.text_background);
        backColorPickerView.setAlphaSliderVisible(true);
        backColorPickerView.setColor(backColor);

        backColorPickerView.setOnColorChangedListener(new ColorPickerView.OnColorChangedListener() {
            @Override
            public void onColorChanged(int color) {
                // Setting background of the sample
                backColor = color;
                sampleBackground.setColorFilter(backColor);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    sampleBackground.setImageAlpha(Color.alpha(backColor));
                } else {
                    //noinspection deprecation
                    sampleBackground.setAlpha(Color.alpha(backColor));
                }
            }
        });

        fontSize  = (SeekBar)  findViewById(R.id.font_size);
        fontSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textSize = progress + MIN_FONT_SIZE;
                sampleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    private String getCountry(){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return settings.getString("country_preference",null);
    }
}

