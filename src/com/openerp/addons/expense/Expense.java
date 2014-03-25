/*
 * OpenERP, Open Source Management Solution
 * Copyright (C) 2012-today OpenERP SA (<http://www.openerp.com>)
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * 
 */

package com.openerp.addons.expense;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.openerp.R;
import com.openerp.support.BaseFragment;
import com.openerp.util.drawer.DrawerItem;
import com.openerp.util.drawer.DrawerListener;

/**
 * The Class Expense.
 */
public class Expense extends BaseFragment {

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_expense, container,false);
		DrawerListener drawer = (DrawerListener) getActivity();
		drawer.refreshDrawer("expense");
		return rootView;
	}

	@Override
	public Object databaseHelper(Context context) {
		return new ExpenseDBHelper(context);
	}

	@Override
	public List<DrawerItem> drawerMenus(Context context) {
		List<DrawerItem> menu = new ArrayList<DrawerItem>();
		menu.add(new DrawerItem("expense_home", "Expense", true));
		Expense expense = new Expense();
		Bundle args = new Bundle();
		args.putString("key", "expense");
		expense.setArguments(args);
		menu.add(new DrawerItem("expense_home", "Expense", 5, 0, expense));
		return menu;
	}
}
