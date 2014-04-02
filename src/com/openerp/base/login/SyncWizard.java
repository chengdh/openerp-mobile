/*
 * OpenERP, Open Source Management Solution
 * Copyright (C) 2012-today OpenERP SA (<http:www.openerp.com>)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 * 
 */
package com.openerp.base.login;

import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import com.openerp.MainActivity;
import com.openerp.R;
import com.openerp.config.SyncWizardValues;
import com.openerp.support.AppScope;
import com.openerp.support.BaseFragment;
import com.openerp.support.SyncValue;
import com.openerp.util.controls.OECheckBox;
import com.openerp.util.controls.OERadioButton;
import com.openerp.util.controls.OETextView;
import com.openerp.util.drawer.DrawerItem;

public class SyncWizard extends BaseFragment {

	View rootView = null;
	MainActivity context = null;
	OECheckBox checkbox[] = null;
	RadioGroup[] rdoGroups = null;
	HashMap<String, String> authorities = new HashMap<String, String>();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		context = (MainActivity) getActivity();
		scope = new AppScope(this);
		rootView = inflater.inflate(R.layout.fragment_sync_wizard, container,
				false);
		getActivity().setTitle("Configuration");
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);
		getActivity().getActionBar().setHomeButtonEnabled(false);

		generateLayout();

		return rootView;
	}

  private String getStringByName(String resource_name){
    int id = scope.main().getResources().getIdentifier("sync_wizard_" + resource_name, "string", scope.main().getPackageName());
    String value = id == 0 ? "" : scope.main().getResources().getString(id);
    return value;
  }

	private void generateLayout() {

		LinearLayout layout = (LinearLayout) rootView
				.findViewById(R.id.layoutLoginConfig);
		SyncWizardValues syncValues = new SyncWizardValues();
		List<SyncValue> syncValuesList = syncValues.syncValues();
		checkbox = new OECheckBox[syncValuesList.size()];
		rdoGroups = new RadioGroup[syncValuesList.size()];
		OETextView[] txvTitles = new OETextView[syncValuesList.size()];
 
		int i = 0;
		int id = 1;
		for (SyncValue value : syncValuesList) {
      String localeTitle = getStringByName(value.getTitle());
			if (!value.getIsGroup()) {
				if (value.getType() == SyncValue.Type.CHECKBOX) {
					checkbox[i] = new OECheckBox(scope.context());
					checkbox[i].setId(id);
          //将英文title放入tag中,便于后期使用
          checkbox[i].setTag(value.getTitle());
          
					checkbox[i].setText(localeTitle);
					layout.addView(checkbox[i]);
				} else {
					rdoGroups[i] = new RadioGroup(scope.context());
					rdoGroups[i].setId(i + 50);
					OERadioButton[] rdoButtons = new OERadioButton[value
							.getRadioGroups().size()];
					int mId = 1;
					int j = 0;
					for (SyncValue rdoVal : value.getRadioGroups()) {

            String localeTitleRadio = getStringByName(rdoVal.getTitle());
						rdoButtons[j] = new OERadioButton(scope.context());
						rdoButtons[j].setId(mId);
            rdoButtons[j].setTag(rdoVal.getTitle());
						rdoButtons[j].setText(localeTitleRadio);
						rdoGroups[i].addView(rdoButtons[j]);
						mId++;
						j++;
					}
					layout.addView(rdoGroups[i]);
				}
				authorities.put(id + "", value.getAuthority());
				i++;
				id++;
			} else {
				txvTitles[i] = new OETextView(scope.context());
				txvTitles[i].setId(id);
				txvTitles[i].setText(localeTitle);
				txvTitles[i].setAllCaps(true);
				txvTitles[i].setPadding(0, 5, 0, 3);
				txvTitles[i].setTypeFace("bold");
				layout.addView(txvTitles[i]);
				View lineView = new View(scope.context());
				lineView.setBackgroundColor(Color.parseColor("#BEBEBE"));
				lineView.setLayoutParams(new LayoutParams(
						LayoutParams.MATCH_PARENT, 1));
				layout.addView(lineView);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.support.v4.app.Fragment#onCreateOptionsMenu(android.view.Menu,
	 * android.view.MenuInflater)
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_fragment_sync_wizard, menu);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.support.v4.app.Fragment#onOptionsItemSelected(android.view.MenuItem
	 * )
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// handle item selection

		switch (item.getItemId()) {
		case R.id.menu_start_application:
			for (OECheckBox chkBox : checkbox) {
				if (chkBox != null) {
					String authority = authorities.get(chkBox.getId() + "")
							.toString();
					scope.main().setAutoSync(authority, chkBox.isChecked());
					scope.main().cancelSync(authority);
				}
			}
			for (RadioGroup rdoGrp : rdoGroups) {
				if (rdoGrp != null) {
					for (int i = 0; i < rdoGrp.getChildCount(); i++) {
						OERadioButton rdoBtn = (OERadioButton) rdoGrp
								.getChildAt(i);
						SharedPreferences settings = PreferenceManager
								.getDefaultSharedPreferences(scope.context());
						Editor editor = settings.edit();
						if (rdoBtn.getTag().equals("local_contact")
								&& rdoBtn.isChecked()) {
							editor.putBoolean("local_contact_sync", true);
							editor.putBoolean("server_contact_sync", false);
						}
						if (rdoBtn.getTag().equals("all_contacts")
								&& rdoBtn.isChecked()) {
							editor.putBoolean("server_contact_sync", true);
							editor.putBoolean("local_contact_sync", false);
						}
						editor.commit();
						String authority = authorities.get(rdoBtn.getId() + "");
						scope.main().setAutoSync(authority, rdoBtn.isChecked());
						scope.main().cancelSync(authority);
					}
				}
			}
			// FragmentListener mFragment = (FragmentListener) getActivity();
			// mFragment.restart();
			getActivity().finish();
			getActivity().startActivity(getActivity().getIntent());
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public Object databaseHelper(Context context) {
		return null;
	}

	@Override
	public List<DrawerItem> drawerMenus(Context context) {
		return null;
	}

}
