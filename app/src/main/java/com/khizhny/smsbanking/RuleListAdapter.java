package com.khizhny.smsbanking;

import java.util.List;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class RuleListAdapter extends ArrayAdapter<Rule> {

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