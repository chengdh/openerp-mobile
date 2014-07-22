package com.openerp.addons.purchase;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.openerp.OETouchListener;
import com.openerp.R;
import com.openerp.orm.OEDataRow;
import com.openerp.providers.purchase.PurchaseOrderProvider;
import com.openerp.receivers.DataSetChangeReceiver;
import com.openerp.receivers.SyncFinishReceiver;
import com.openerp.support.AppScope;
import com.openerp.support.BaseFragment;
import com.openerp.support.listview.OEListAdapter;
import com.openerp.util.drawer.DrawerItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.widget.AdapterView.OnItemLongClickListener;

/**
 * 采购订单显示界面.
 */
public class PurchaseOrder extends BaseFragment implements OETouchListener.OnPullListener,
        OnItemLongClickListener, AdapterView.OnItemClickListener {

    public static final String TAG = "com.openerp.addons.purchase.PurchaseOrder";

    @Override
    public Object databaseHelper(Context context) {
        return new PurchaseOrderDB(context);
    }


    private enum MType {
        INBOX, ARCHIVE
    }

    Integer mSelectedItemPosition = -1;
    MType mType = MType.INBOX;
    String mCurrentType = "inbox";
    View mView = null;
    SearchView mSearchView = null;
    OETouchListener mTouchAttacher;
    @SuppressLint("UseSparseArrays")
    OEListAdapter mListViewAdapter = null;
    ListView mListView = null;
    List<Object> mPurchaseOrderObjects = new ArrayList<Object>();
    Boolean isSynced = false;

    /**
     * Background data operations
     */
    PurchaseOrderLoader mPurchaseOrderLoader = null;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mSelectedItemPosition = savedInstanceState.getInt("mSelectedItemPosition", -1);
        }
        setHasOptionsMenu(true);
        mView = inflater.inflate(R.layout.fragment_purchase_order, container, false);
        scope = new AppScope(getActivity());
        return mView;
    }

    private void init() {
        Log.d(TAG, "Purchase->init()");
        mListView = (ListView) mView.findViewById(R.id.lstExpenses);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setOnItemLongClickListener(this);
        mListView.setOnItemClickListener(this);
        mListViewAdapter = new OEListAdapter(getActivity(), R.layout.fragment_purchase_order_listview_item, mPurchaseOrderObjects) {
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
        Log.d(TAG, "PurchaseOrder->initData()");
/*        if (mSelectedItemPosition > -1) {
            return;
        }*/
        Bundle bundle = getArguments();
        if (bundle != null) {
            if (mPurchaseOrderLoader != null) {
                mPurchaseOrderLoader.cancel(true);
                mPurchaseOrderLoader = null;
            }
            if (bundle.containsKey("type")) {
                mCurrentType = bundle.getString("type");
                String title = "Archive";
                if (mCurrentType.equals("inbox")) {
                    mPurchaseOrderLoader = new PurchaseOrderLoader(MType.INBOX);
                    mPurchaseOrderLoader.execute((Void) null);
                    title = "Inbox";
                } else if (mCurrentType.equals("archive")) {
                    mPurchaseOrderLoader = new PurchaseOrderLoader(MType.ARCHIVE);
                    mPurchaseOrderLoader.execute((Void) null);

                }
                scope.main().setTitle(title);
            } else {
                scope.main().setTitle("Inbox");
                mPurchaseOrderLoader = new PurchaseOrderLoader(MType.INBOX);
                mPurchaseOrderLoader.execute((Void) null);

            }
        }
    }

    // Handling each row view
    private View handleRowView(View mView, final int position) {
        final OEDataRow row = (OEDataRow) mPurchaseOrderObjects.get(position);

        TextView txvName, txvDate, txvAmountTotal, txvState;

        txvName = (TextView) mView.findViewById(R.id.txvPartnerName);
        txvDate = (TextView) mView.findViewById(R.id.txvDateOrder);
        txvAmountTotal = (TextView) mView.findViewById(R.id.txvAmountTotal);

        OEDataRow partner = row.getM2ORecord("partner_id").browse();
        String name = partner.getString("name");
        txvName.setText(name);

        String date = row.getString("date_order");
        txvDate.setText(date);

        String amountTotal = row.getString("amount_total");
        txvAmountTotal.setText(amountTotal);

        String state = row.getString("state");
        String status = getStatus(state);
        txvState = (TextView) mView.findViewById(R.id.txvState);

        txvState.setText(status);
        return mView;
    }

    private String getStatus(String state) {
        int id = scope.main().getResources().getIdentifier("state_purchase_order_" + state, "string", scope.main().getPackageName());
        String value = id == 0 ? "" : scope.main().getResources().getString(id);
        return value;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_fragment_purchase_order, menu);
        mSearchView = (SearchView) menu.findItem(R.id.menu_purchase_order_search).getActionView();
        init();
    }

    @Override
    public List<DrawerItem> drawerMenus(Context context) {
        List<DrawerItem> drawerItems = new ArrayList<DrawerItem>();
        PurchaseOrderDB db = new PurchaseOrderDB(context);

//        if (db.isInstalledOnServer()) {
        String purchase_order_title = context.getResources().getString(R.string.purchase_order_group_title);
        String purchase_order_inbox = context.getResources().getString(R.string.purchase_order_draw_item_inbox);
        String purchase_order_archives = context.getResources().getString(R.string.purchase_order_draw_item_archives);

        drawerItems.add(new DrawerItem(TAG, purchase_order_title, true));
        drawerItems.add(new DrawerItem(TAG, purchase_order_inbox, count(MType.INBOX, context), R.drawable.ic_action_inbox, getFragment("inbox")));
        drawerItems.add(new DrawerItem(TAG, purchase_order_archives, count(MType.ARCHIVE, context), R.drawable.ic_action_archive, getFragment("archive")));
//        }
        return drawerItems;
    }

    private int count(MType type, Context context) {
        int count = 0;
        PurchaseOrderDB db = new PurchaseOrderDB(context);
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
        PurchaseOrder purchaseOrder = new PurchaseOrder();
        Bundle bundle = new Bundle();
        bundle.putString("type", value);
        purchaseOrder.setArguments(bundle);
        return purchaseOrder;
    }

    public class PurchaseOrderLoader extends AsyncTask<Void, Void, Boolean> {
        MType purchaseOrderType = null;

        public PurchaseOrderLoader(MType type) {
            purchaseOrderType = type;
            mView.findViewById(R.id.loadingProgress).setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            HashMap<String, Object> map = getWhere(purchaseOrderType);
            String where = (String) map.get("where");
            String whereArgs[] = (String[]) map.get("whereArgs");
            mType = purchaseOrderType;
            List<OEDataRow> result = db().select(where, whereArgs, null, null, "date_order DESC");
            mPurchaseOrderObjects.clear();
            if (result.size() > 0) {
                for (OEDataRow row : result) {
                    mPurchaseOrderObjects.add(row);
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mView.findViewById(R.id.loadingProgress).setVisibility(View.GONE);
            mListViewAdapter.notifiyDataChange(mPurchaseOrderObjects);
            Log.d(TAG, "mSearchView:" + mSearchView);
            Log.d(TAG, "mListViewAdapter:" + mListViewAdapter);
            Log.d(TAG, "getQueryListener:" + getQueryListener(mListViewAdapter));
            mSearchView.setOnQueryTextListener(getQueryListener(mListViewAdapter));
            mPurchaseOrderLoader = null;
            checkPurchaseOrderStatus();
        }
    }

    private void checkPurchaseOrderStatus() {
        // Fetching parent ids from Child row with order by date desc
        if (mPurchaseOrderObjects.size() == 0) {
            if (db().isEmptyTable() && !isSynced) {
                isSynced = true;

                if (mView.findViewById(R.id.waitingForSyncToStart) != null) {
                    mView.findViewById(R.id.waitingForSyncToStart).setVisibility(View.VISIBLE);
                }
                try {
                    //Thread.sleep(2000);
                    scope.main().requestSync(PurchaseOrderProvider.AUTHORITY);
                } catch (Exception e) {
                }
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
    public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
        mSelectedItemPosition = position;
/*        OEDataRow row = (OEDataRow) mPurchaseOrderObjects.get(position);
        ExpenseDetail detail = new ExpenseDetail();
        Bundle bundle = new Bundle();
        bundle.putInt("purchase_order_id", row.getInt("id"));
        bundle.putInt("position", position);
        detail.setArguments(bundle);
        FragmentListener listener = (FragmentListener) getActivity();
        listener.startDetailFragment(detail);*/
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
        scope.main().requestSync(PurchaseOrderProvider.AUTHORITY);
    }

    @Override
    public void onResume() {
        super.onResume();
        scope.context().registerReceiver(purchaseOrderSyncFinish, new IntentFilter(SyncFinishReceiver.SYNC_FINISH));
        scope.context().registerReceiver(datasetChangeReceiver, new IntentFilter(DataSetChangeReceiver.DATA_CHANGED));
    }

    @Override
    public void onPause() {
        super.onPause();
        scope.context().unregisterReceiver(purchaseOrderSyncFinish);
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
    private SyncFinishReceiver purchaseOrderSyncFinish = new SyncFinishReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mTouchAttacher.setPullComplete();
            scope.main().refreshDrawer(TAG);
            mListViewAdapter.clear();
            mPurchaseOrderObjects.clear();
            mListViewAdapter.notifiyDataChange(mPurchaseOrderObjects);
            new PurchaseOrderLoader(mType).execute();
        }
    };
    private DataSetChangeReceiver datasetChangeReceiver = new DataSetChangeReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {

                Log.d(TAG, "PurchaseOrder->datasetChangeReceiver@onReceive");
                mView.findViewById(R.id.waitingForSyncToStart).setVisibility(View.GONE);

                String id = intent.getExtras().getString("id");
                String model = intent.getExtras().getString("model");
                if (model.equals("purchase.order")) {
                    OEDataRow row = db().select(Integer.parseInt(id));
                    mPurchaseOrderObjects.add(0, row);
                    mListViewAdapter.notifiyDataChange(mPurchaseOrderObjects);
                }

            } catch (Exception e) {
            }

        }
    };
}