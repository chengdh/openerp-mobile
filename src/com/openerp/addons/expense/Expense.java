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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

import openerp.OEArguments;

import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.openerp.OETouchListener;
import com.openerp.R;
import com.openerp.orm.OEDataRow;
import com.openerp.orm.OEHelper;
import com.openerp.orm.OEValues;
import com.openerp.providers.expense.ExpenseProvider;
import com.openerp.receivers.DataSetChangeReceiver;
import com.openerp.receivers.SyncFinishReceiver;
import com.openerp.support.AppScope;
import com.openerp.support.BaseFragment;
import com.openerp.support.fragment.FragmentListener;
import com.openerp.support.listview.OEListAdapter;
import com.openerp.util.HTMLHelper;
import com.openerp.util.OEDate;
import com.openerp.util.StringHelper;
import com.openerp.util.drawer.DrawerItem;
import com.openerp.util.drawer.DrawerListener;


/**
 * The Class Expense.
 */
public class Expense extends BaseFragment  implements OETouchListener.OnPullListener,
       OnItemLongClickListener,OnItemClickListener {

         public static final String TAG = "com.openerp.addons.expense.Expense";

         @Override
         public Object databaseHelper(Context context) {
           return new ExpenseDBHelper(context);
         }


         private enum MType {
           INBOX, ARCHIVE
         }

         Integer mSelectedItemPosition = -1;
         Integer selectedCounter = 0;
         MType mType = MType.INBOX;
         String mCurrentType = "inbox";
         View mView = null;
         SearchView mSearchView = null;
         OETouchListener mTouchAttacher;
         ActionMode mActionMode;
         @SuppressLint("UseSparseArrays")
         OEListAdapter mListViewAdapter = null;
         ListView mListView = null;
         List<Object> mExpenseObjects = new ArrayList<Object>();
         Integer tag_color_count = 0;
         Boolean isSynced = false;

         /**
          * Background data operations
          */
         ExpensesLoader mExpenseLoader = null;

         HashMap<String, Integer> message_row_indexes = new HashMap<String, Integer>();
         HashMap<String, Integer> message_model_colors = new HashMap<String, Integer>();

         @Override
         public View onCreateView(LayoutInflater inflater, ViewGroup container,
             Bundle savedInstanceState) {
           if (savedInstanceState != null) {
             mSelectedItemPosition = savedInstanceState.getInt("mSelectedItemPosition", -1);
           }
           setHasOptionsMenu(true);
           mView = inflater.inflate(R.layout.fragment_expense, container, false);
           scope = new AppScope(getActivity());
           init();
           return mView;
         }

         private void init() {
           Log.d(TAG, "Expense->init()");
           mListView = (ListView) mView.findViewById(R.id.lstExpenses);
           mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
           mListView.setOnItemLongClickListener(this);
           mListView.setOnItemClickListener(this);
           mListViewAdapter = new OEListAdapter(getActivity(),R.layout.fragment_expense_listview_items, mExpenseObjects) {
             @Override
             public View getView(int position, View convertView, ViewGroup parent) {
               View mView = convertView;
               if (mView == null)
                 mView = getActivity().getLayoutInflater().inflate(getResource(), parent, false);
               mView = handleRowView(mView, position);
               return mView;
             }
           };
           mListView.setAdapter(mListViewAdapter);
           mTouchAttacher = scope.main().getTouchAttacher();
           mTouchAttacher.setPullableView(mListView, this);
           initData();
         }

         private void initData() {
           Log.d(TAG, "Expense->initData()");
           if (mSelectedItemPosition > -1) {
             return;
           }
           Bundle bundle = getArguments();
           if (bundle != null) {
             if (mExpenseLoader != null) {
               mExpenseLoader.cancel(true);
               mExpenseLoader = null;
             }
             if (bundle.containsKey("type")) {
               mCurrentType = bundle.getString("type");
               String title = "Archive";
               if (mCurrentType.equals("inbox")) {
                 mExpenseLoader = new ExpensesLoader(MType.INBOX);
                 mExpenseLoader.execute((Void) null);
                 title = "Inbox";
               } else if (mCurrentType.equals("archive")) {
                 mExpenseLoader = new ExpensesLoader(MType.ARCHIVE);
                 mExpenseLoader.execute((Void) null);

               }
               scope.main().setTitle(title);
             } else {
               scope.main().setTitle("Inbox");
               mExpenseLoader = new ExpensesLoader(MType.INBOX);
               mExpenseLoader.execute((Void) null);

             }
           }
         }

         // Handling each row view
         private View handleRowView(View mView, final int position) {
           final OEDataRow row = (OEDataRow) mExpenseObjects.get(position);

           TextView txvName, txvDate, txvEmployee,txvState;

           txvName = (TextView) mView.findViewById(R.id.txvExpenseName);
           txvDate = (TextView) mView.findViewById(R.id.txvExpenseDate);
           txvEmployee = (TextView) mView.findViewById(R.id.txvExpenseEmployee);

           String name = row.getString("name");
           txvName.setText(name);

           String date = row.getString("date");
           txvDate.setText(date);
           //txvDate.setText(OEDate.getDate(date, TimeZone.getDefault().getID()));

           OEDataRow employee = row.getM2ORecord("employee_id").browse();
           txvEmployee.setText(employee.getString("name"));

           String state = row.getString("state");
           txvState = (TextView) mView.findViewById(R.id.txvExpenseState);
           txvState.setText(state);
           return mView;
         }
         @Override
         public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
           inflater.inflate(R.menu.menu_fragment_expense, menu);
           mSearchView = (SearchView) menu.findItem(R.id.menu_expense_search)
             .getActionView();
         }

         @Override
         public List<DrawerItem> drawerMenus(Context context) {
           List<DrawerItem> drawerItems = new ArrayList<DrawerItem>();
           ExpenseDBHelper db = new ExpenseDBHelper(context);

           if (db.isInstalledOnServer()) {
             String expense_title = context.getResources().getString(R.string.expense_group_title);
             String expense_inbox = context.getResources().getString(R.string.expense_draw_item_inbox);
             String expense_archives = context.getResources().getString(R.string.expense_draw_item_archives);

             drawerItems.add(new DrawerItem(TAG, expense_title, true));
             drawerItems.add(new DrawerItem(TAG, expense_inbox, count(MType.INBOX,context), R.drawable.ic_action_inbox,getFragment("inbox")));
             drawerItems.add(new DrawerItem(TAG, expense_archives, 0,R.drawable.ic_action_archive, getFragment("archive")));
           }
           return drawerItems;
         }

         private int count(MType type, Context context) {
           int count = 0;
           ExpenseDBHelper db = new ExpenseDBHelper(context);
           String where = null;
           String whereArgs[] = null;
           HashMap<String, Object> obj = getWhere(type);
           where = (String) obj.get("where");
           whereArgs = (String[]) obj.get("whereArgs");
           count = db.count(where, whereArgs);
           return count;
         }

         public HashMap<String, Object> getWhere(MType type) {
           HashMap<String, Object> map = new HashMap<String, Object>();
           String where = null;
           String[] whereArgs = null;
           switch (type) {
             case INBOX:
               where = "processed = ? ";
               whereArgs = new String[] { "false" };
               break;
             default:
               where = "processed = ? ";
               whereArgs = new String[] { "true" };
               break;
           }
           map.put("where", where);
           map.put("whereArgs", whereArgs);
           return map;
         }

         private BaseFragment getFragment(String value) {
           Expense expense = new Expense();
           Bundle bundle = new Bundle();
           bundle.putString("type", value);
           expense.setArguments(bundle);
           return expense;
         }

         public class ExpensesLoader extends AsyncTask<Void, Void, Boolean> {
           MType expenseType = null;

           public ExpensesLoader(MType type) {
             expenseType = type;
             mView.findViewById(R.id.loadingProgress).setVisibility(View.VISIBLE);
           }

           @Override
           protected Boolean doInBackground(Void... arg0) {
             HashMap<String, Object> map = getWhere(expenseType);
             String where = (String) map.get("where");
             String whereArgs[] = (String[]) map.get("whereArgs");
             mType = expenseType;
             List<OEDataRow> result = db().select(where, whereArgs, null, null,"date DESC");
             mExpenseObjects.clear();
             if (result.size() > 0) {
               for (OEDataRow row : result) {
                 mExpenseObjects.add(row);
               }
             }
             return true;
           }

           @Override
           protected void onPostExecute(final Boolean success) {
             mView.findViewById(R.id.loadingProgress).setVisibility(View.GONE);
             mListViewAdapter.notifiyDataChange(mExpenseObjects);
             mSearchView.setOnQueryTextListener(getQueryListener(mListViewAdapter));
             mExpenseLoader = null;
             checkExpenseStatus();
           }
         }

         private void checkExpenseStatus() {
           // Fetching parent ids from Child row with order by date desc
           if (mExpenseObjects.size() == 0) {
             if (db().isEmptyTable() && !isSynced) {
               isSynced = true;

               if (mView.findViewById(R.id.waitingForSyncToStart) != null) {
                 mView.findViewById(R.id.waitingForSyncToStart).setVisibility(View.VISIBLE);
               }
               try {
                 Thread.sleep(2000);
                 scope.main().requestSync(ExpenseProvider.AUTHORITY);
               } catch (Exception e) {}
             } else {
               mView.findViewById(R.id.waitingForSyncToStart).setVisibility(View.GONE);
               TextView txvMsg = (TextView) mView.findViewById(R.id.txvExpenseAllReadMessage);
               txvMsg.setVisibility(View.VISIBLE);
               //txvMsg.setText(getStatusMessage(mType));
             }
           }
         }

         /**
          * On message item click
          */
         @Override
         public void onItemClick(AdapterView<?> adapter, View view, int position,long id) {
           mSelectedItemPosition = position;
           OEDataRow row = (OEDataRow) mExpenseObjects.get(position);
           ExpenseDetail detail = new ExpenseDetail();
           Bundle bundle = new Bundle();
           bundle.putInt("expense_id", row.getInt("id"));
           bundle.putInt("position", position);
           detail.setArguments(bundle);
           FragmentListener listener = (FragmentListener) getActivity();
           listener.startDetailFragment(detail);
         }

         /**
          * on message item long press
          */
         @Override
         public boolean onItemLongClick(AdapterView<?> adapter, View view,int position, long id) {
           return false;
         }

         /**
          * on pulled for sync message
          */
         @Override
         public void onPullStarted(View arg0) {
           scope.main().requestSync(ExpenseProvider.AUTHORITY);
         }

         @Override
         public void onResume() {
           super.onResume();
           scope.context().registerReceiver(expenseSyncFinish,new IntentFilter(SyncFinishReceiver.SYNC_FINISH));
           scope.context().registerReceiver(datasetChangeReceiver,new IntentFilter(DataSetChangeReceiver.DATA_CHANGED));
         }

         @Override
         public void onPause() {
           super.onPause();
           scope.context().unregisterReceiver(expenseSyncFinish);
           scope.context().unregisterReceiver(datasetChangeReceiver);
           Bundle outState = new Bundle();
           outState.putInt("mSelectedItemPosition", mSelectedItemPosition);
           onSaveInstanceState(outState);
         }

         /*
          * Used for Synchronization : Register receiver and unregister receiver
          * 
          * SyncFinishReceiver
          */
         private SyncFinishReceiver expenseSyncFinish = new SyncFinishReceiver() {
           @Override
           public void onReceive(Context context, Intent intent) {
             mTouchAttacher.setPullComplete();
             scope.main().refreshDrawer(TAG);
             mListViewAdapter.clear();
             mExpenseObjects.clear();
             mListViewAdapter.notifiyDataChange(mExpenseObjects);
             new ExpensesLoader(mType).execute();
           }
         };
         private DataSetChangeReceiver datasetChangeReceiver = new DataSetChangeReceiver() {
           @Override
           public void onReceive(Context context, Intent intent) {

             try {

               Log.d(TAG, "Expense->datasetChangeReceiver@onReceive");
               mView.findViewById(R.id.waitingForSyncToStart).setVisibility(View.GONE);

               String id = intent.getExtras().getString("id");
               String model = intent.getExtras().getString("model");
               if (model.equals("hr.expense.expense")) {
                 OEDataRow row = db().select(Integer.parseInt(id));
                 mExpenseObjects.add(0, row);
                 mListViewAdapter.notifiyDataChange(mExpenseObjects);
               }

             } catch (Exception e) {}

           }
         };
}
