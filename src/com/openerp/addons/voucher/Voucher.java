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
package com.openerp.addons.voucher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.openerp.OETouchListener;
import com.openerp.R;
import com.openerp.orm.OEDataRow;
import com.openerp.providers.voucher.VoucherProvider;
import com.openerp.receivers.DataSetChangeReceiver;
import com.openerp.receivers.SyncFinishReceiver;
import com.openerp.support.AppScope;
import com.openerp.support.BaseFragment;
import com.openerp.support.fragment.FragmentListener;
import com.openerp.support.listview.OEListAdapter;
import com.openerp.util.drawer.DrawerItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * The Class Expense.
 */
public class Voucher extends BaseFragment implements OETouchListener.OnPullListener,
        OnItemLongClickListener, OnItemClickListener {

    public static final String TAG = "Voucher";

    @Override
    public Object databaseHelper(Context context) {
        return new VoucherDB(context);
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
    List<Object> mVoucherObjects = new ArrayList<Object>();
    Integer tag_color_count = 0;
    Boolean isSynced = false;

    /**
     * Background data operations
     */
    VoucherLoader mVoucherLoader = null;

    HashMap<String, Integer> message_row_indexes = new HashMap<String, Integer>();
    HashMap<String, Integer> message_model_colors = new HashMap<String, Integer>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mSelectedItemPosition = savedInstanceState.getInt("mSelectedItemPosition", -1);
        }
        setHasOptionsMenu(true);
        mView = inflater.inflate(R.layout.fragment_voucher, container, false);
        scope = new AppScope(getActivity());
        init();
        return mView;
    }

    private void init() {
        Log.d(TAG, "Voucher->init()");
        mListView = (ListView) mView.findViewById(R.id.lstVouchers);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setOnItemLongClickListener(this);
        mListView.setOnItemClickListener(this);
        mListViewAdapter = new OEListAdapter(getActivity(), R.layout.fragment_voucher_listview_items, mVoucherObjects) {
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
        Log.d(TAG, "Voucher->initData()");
/*           if (mSelectedItemPosition > -1) {
             return;
           }*/
        Bundle bundle = getArguments();
        if (bundle != null) {
            if (mVoucherLoader != null) {
                mVoucherLoader.cancel(true);
                mVoucherLoader = null;
            }
            if (bundle.containsKey("type")) {
                mCurrentType = bundle.getString("type");
                String title = "Archive";
                if (mCurrentType.equals("inbox")) {
                    mVoucherLoader = new VoucherLoader(MType.INBOX);
                    mVoucherLoader.execute((Void) null);
                    title = "Inbox";
                } else if (mCurrentType.equals("archive")) {
                    mVoucherLoader = new VoucherLoader(MType.ARCHIVE);
                    mVoucherLoader.execute((Void) null);

                }
                scope.main().setTitle(title);
            } else {
                scope.main().setTitle("Inbox");
                mVoucherLoader = new VoucherLoader(MType.INBOX);
                mVoucherLoader.execute((Void) null);

            }
        }
    }

    // Handling each row view
    private View handleRowView(View mView, final int position) {
        final OEDataRow row = (OEDataRow) mVoucherObjects.get(position);

        TextView txvPartner, txvDate, txvAmount, txvState;

        txvPartner = (TextView) mView.findViewById(R.id.txvVoucherPartner);
        txvDate = (TextView) mView.findViewById(R.id.txvVoucherDate);
        txvAmount = (TextView) mView.findViewById(R.id.txvVoucherAmount);
        txvState = (TextView) mView.findViewById(R.id.txvVoucherState);

        OEDataRow partner = row.getM2ORecord("partner_id").browse();
        txvPartner.setText(partner.getString("name"));

        String date = row.getString("date");
        txvDate.setText(date);

        String amount = row.getString("amount");
        txvAmount.setText(amount);

        String state = row.getString("state");
        String status = getStatus(state);
        txvState.setText(status);
        return mView;
    }

    private String getStatus(String state) {
        int id = scope.main().getResources().getIdentifier("state_" + state, "string", scope.main().getPackageName());
        String value = id == 0 ? "" : scope.main().getResources().getString(id);
        return value;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_fragment_voucher, menu);
        mSearchView = (SearchView) menu.findItem(R.id.menu_voucher_search).getActionView();
        mSearchView.setOnQueryTextListener(getQueryListener(mListViewAdapter));
    }

    @Override
    public List<DrawerItem> drawerMenus(Context context) {
        List<DrawerItem> drawerItems = new ArrayList<DrawerItem>();
        String voucher_title = context.getResources().getString(R.string.voucher_group_title);

        String voucher_inbox = context.getResources().getString(R.string.voucher_draw_item_inbox);
        String voucher_archives = context.getResources().getString(R.string.voucher_draw_item_archives);

        VoucherDB db = new VoucherDB(context);

//           if (db.isInstalledOnServer()) {
        drawerItems.add(new DrawerItem(TAG, voucher_title, true));
        drawerItems.add(new DrawerItem(TAG, voucher_inbox, count(MType.INBOX, context), R.drawable.ic_action_inbox, getFragment("inbox")));
        drawerItems.add(new DrawerItem(TAG, voucher_archives, count(MType.ARCHIVE, context), R.drawable.ic_action_archive, getFragment("archive")));
//           }
        return drawerItems;
    }

    private int count(MType type, Context context) {
        int count = 0;
        VoucherDB db = new VoucherDB(context);
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
                whereArgs = new String[]{"false"};
                break;
            default:
                where = "processed = ? ";
                whereArgs = new String[]{"true"};
                break;
        }
        map.put("where", where);
        map.put("whereArgs", whereArgs);
        return map;
    }

    private BaseFragment getFragment(String value) {
        Voucher voucher = new Voucher();
        Bundle bundle = new Bundle();
        bundle.putString("type", value);
        voucher.setArguments(bundle);
        return voucher;
    }

    public class VoucherLoader extends AsyncTask<Void, Void, Boolean> {
        MType voucherType = null;

        public VoucherLoader(MType type) {
            voucherType = type;
            mView.findViewById(R.id.loadingProgress).setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            HashMap<String, Object> map = getWhere(voucherType);
            String where = (String) map.get("where");
            String whereArgs[] = (String[]) map.get("whereArgs");
            mType = voucherType;
            List<OEDataRow> result = db().select(where, whereArgs, null, null, "date DESC");
            mVoucherObjects.clear();
            if (result.size() > 0) {
                for (OEDataRow row : result) {
                    mVoucherObjects.add(row);
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mView.findViewById(R.id.loadingProgress).setVisibility(View.GONE);
            mListViewAdapter.notifiyDataChange(mVoucherObjects);
            mVoucherLoader = null;
            checkExpenseStatus();
        }
    }

    private void checkExpenseStatus() {
        // Fetching parent ids from Child row with order by date desc
        if (mVoucherObjects.size() == 0) {
            if (db().isEmptyTable() && !isSynced) {
                isSynced = true;

                if (mView.findViewById(R.id.waitingForSyncToStart) != null) {
                    mView.findViewById(R.id.waitingForSyncToStart).setVisibility(View.VISIBLE);
                }
                try {
                    Thread.sleep(2000);
                    scope.main().requestSync(VoucherProvider.AUTHORITY);
                } catch (Exception e) {
                }
            } else {
                mView.findViewById(R.id.waitingForSyncToStart).setVisibility(View.GONE);
                TextView txvMsg = (TextView) mView.findViewById(R.id.txvVoucherAllReadMessage);
                txvMsg.setVisibility(View.VISIBLE);
                //txvMsg.setText(getStatusMessage(mType));
            }
        }
    }

    /**
     * On message item click
     */

    @Override
    public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {

        mSelectedItemPosition = position;
        OEDataRow row = (OEDataRow) mVoucherObjects.get(position);
        VoucherDetail detail = new VoucherDetail();
        Bundle bundle = new Bundle();
        bundle.putInt("voucher_id", row.getInt("id"));
        bundle.putInt("position", position);
        detail.setArguments(bundle);
        FragmentListener listener = (FragmentListener) getActivity();
        listener.startDetailFragment(detail);
    }

    /**
     * on message item long press
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> adapter, View view, int position, long id) {
        return false;
    }

    /**
     * on pulled for sync message
     */
    @Override
    public void onPullStarted(View arg0) {
        scope.main().requestSync(VoucherProvider.AUTHORITY);
    }

    @Override
    public void onResume() {
        super.onResume();
        scope.context().registerReceiver(voucherSyncFinish, new IntentFilter(SyncFinishReceiver.SYNC_FINISH));
        scope.context().registerReceiver(datasetChangeReceiver, new IntentFilter(DataSetChangeReceiver.DATA_CHANGED));
    }

    @Override
    public void onPause() {
        super.onPause();
        scope.context().unregisterReceiver(voucherSyncFinish);
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
    private SyncFinishReceiver voucherSyncFinish = new SyncFinishReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mTouchAttacher.setPullComplete();
            scope.main().refreshDrawer(TAG);
            mListViewAdapter.clear();
            mVoucherObjects.clear();
            mListViewAdapter.notifiyDataChange(mVoucherObjects);
            new VoucherLoader(mType).execute();
        }
    };
    private DataSetChangeReceiver datasetChangeReceiver = new DataSetChangeReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            try {

                Log.d(TAG, "Voucher->datasetChangeReceiver@onReceive");
                mView.findViewById(R.id.waitingForSyncToStart).setVisibility(View.GONE);

                String id = intent.getExtras().getString("id");
                String model = intent.getExtras().getString("model");
                if (model.equals("account.voucher")) {
                    OEDataRow row = db().select(Integer.parseInt(id));
                    mVoucherObjects.add(0, row);
                    mListViewAdapter.notifiyDataChange(mVoucherObjects);
                }

            } catch (Exception e) {
            }

        }
    };
}
