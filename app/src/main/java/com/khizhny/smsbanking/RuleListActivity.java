package com.khizhny.smsbanking;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.support.v7.widget.PopupMenu;
import android.widget.AdapterView.OnItemClickListener;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import static com.khizhny.smsbanking.MyApplication.LOG;
import static com.khizhny.smsbanking.MyApplication.db;

public class RuleListActivity extends AppCompatActivity implements OnMenuItemClickListener{
	private ListView listView;
	private Bank activeBank;
    private RuleListAdapter adapter;
	int selected_row;
	boolean tipWasSeen;
    private AlertDialog alertDialog;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Log.d(LOG,"RuleListActivity starting...");
		setContentView(R.layout.activity_bank_list);
		listView = (ListView) findViewById(R.id.listView);
		listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {              
            	// if some rule clicked open popup window with options Add,Edit,Delete.
            	selected_row=position;
            	PopupMenu popupMenu = new PopupMenu(RuleListActivity.this, view);
        		popupMenu.setOnMenuItemClickListener(RuleListActivity.this);
        		popupMenu.inflate(R.menu.popup_menu_rule_list);
        		popupMenu.show();
            }
       });
        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null) actionBar.setDisplayHomeAsUpEnabled(true);
	}
	
    @Override
    protected void onStart() {
        super.onStart();
        activeBank=db.getActiveBank();
        if (activeBank!=null) {
            adapter = new RuleListAdapter(this, activeBank.ruleList);
            listView.setAdapter(adapter);
            if (activeBank.ruleList.isEmpty() && !tipWasSeen) {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getResources().getString(R.string.tip_rules_1));
                alertDialog =builder.create();
                alertDialog.show();
                tipWasSeen = true;
            }
        }
    }

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		// handles popup menu items click
		Rule r=adapter.getItem(selected_row);
		Intent intent;
		switch (item.getItemId()) {
		case R.id.item_new_rule:
			Toast.makeText(this, getString(R.string.rule_add_tip), Toast.LENGTH_SHORT).show();
			return true;
		case R.id.item_edit_rule:
			intent = new Intent(this, RuleActivity.class);
			intent.putExtra("rule_id", r.getId());
			intent.putExtra("todo", "edit");
		    startActivity(intent);		    
			return true;
		case R.id.item_delete_rule:
            db.deleteRule(r.getId());
            activeBank.ruleList.remove(selected_row);
			adapter.notifyDataSetChanged();
		    return true;
		}
	return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_rules_activity, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.rule_add:
				Toast.makeText(this, getString(R.string.rule_add_tip), Toast.LENGTH_LONG).show();
				return true;
            case android.R.id.home:
                finish();
		}
		return false;
	}

    private class RuleListAdapter extends ArrayAdapter<Rule> {

        private final Context context;
        private final List<Rule> ruleList;

        public RuleListAdapter(Context context, List<Rule> ruleList) {
            super(context, R.layout.activity_rule_list_row, ruleList);
            this.context = context;
            this.ruleList = ruleList;
        }
        @Override
        public View getView(int position, View rowView , ViewGroup parent) {
            if (rowView == null) {
                LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = vi.inflate(R.layout.activity_rule_list_row, parent, false);
            }

            TextView ruleNameView = (TextView) rowView.findViewById(R.id.ruleName);
            ruleNameView.setText(ruleList.get(position).getName());

            Drawable icon = ResourcesCompat.getDrawable(context.getResources(), ruleList.get(position).getRuleTypeDrawable(), null);
            if (icon != null) {
                icon.setBounds(0, 0, icon.getMinimumWidth(), icon.getMinimumHeight());
            }
            ruleNameView.setCompoundDrawables(icon,null,null,null);
            return rowView;
        }

    }
}
