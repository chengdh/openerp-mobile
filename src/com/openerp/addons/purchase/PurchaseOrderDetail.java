package com.openerp.addons.purchase;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.openerp.OEArguments;
import com.openerp.R;
import com.openerp.addons.message.MessageDB;
import com.openerp.orm.OEDataRow;
import com.openerp.orm.OEDatabase;
import com.openerp.orm.OEHelper;
import com.openerp.orm.OEValues;
import com.openerp.providers.purchase.PurchaseOrderProvider;
import com.openerp.receivers.DataSetChangeReceiver;
import com.openerp.support.AppScope;
import com.openerp.support.BaseFragment;
import com.openerp.support.listview.OEListAdapter;
import com.openerp.util.Base64Helper;
import com.openerp.util.OEDate;
import com.openerp.util.contactview.OEContactView;
import com.openerp.util.drawer.DrawerItem;
import com.openerp.util.drawer.DrawerListener;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by chengdh on 14-7-23.
 */
public class PurchaseOrderDetail extends BaseFragment {
    public static final String TAG = "PurchaseOrderDetail";
    View mView = null;
    Integer mPurchaseOrderId = null;
    OEDataRow mPurchaseOrderData = null;
    ListView mPurchaseOrderLinesList = null;
    OEListAdapter mPurchaseOrderLinesAdapter = null;

    //message列表
    ListView mMessageListView = null;
    OEListAdapter mMessageListAdapter = null;

    //是否已操作
    Boolean mProcessed = false;
    //费用单明细
    List<Object> mPurchaseOrderLines = new ArrayList<Object>();
    //审批记录对象
    List<Object> mMessages = new ArrayList<Object>();

    //工作流程记录对象
    MessagesLoader mMessagesLoader = null;
    //工作流审批对象
    WorkflowOperation mWorkflowOperation = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        mView = inflater.inflate(R.layout.fragment_purchase_order_detail_view, container, false);
        scope = new AppScope(getActivity());
        init();
        return mView;
    }

    private void init() {
        Log.d(TAG, "PurchaseOrderDetail->init()");
        Bundle bundle = getArguments();
        if (bundle != null) {
            mPurchaseOrderId = bundle.getInt("purchase_order_id");
            mPurchaseOrderData = db().select(mPurchaseOrderId);
            mProcessed = mPurchaseOrderData.getBoolean("processed");

            List<OEDataRow> lines = mPurchaseOrderData.getO2MRecord("order_line").browseEach();
            for (Object l : lines) {
                mPurchaseOrderLines.add(l);
            }
            initControls();
            initLstMessages();
            initData();
        }
    }

    private void initControls() {
        //设置主表内容
        TextView txvPartnerName, txvDate, txvAmount;
        txvPartnerName = (TextView) mView.findViewById(R.id.txvPurchaseOrderPartnerName);
        txvDate = (TextView) mView.findViewById(R.id.txvPurchaseOrderDateOrder);
        txvAmount = (TextView) mView.findViewById(R.id.txvPurchaseOrderAmountTotal);


        OEDataRow partner = mPurchaseOrderData.getM2ORecord("partner_id").browse();
        String name = partner.getString("name");
        String state = mPurchaseOrderData.getString("state");
        String status = getStatus(state);
        txvPartnerName.setText(name + "(" + status + ")");

        String date = mPurchaseOrderData.getString("date_order");
        txvDate.setText(date);
        String amount = mPurchaseOrderData.getString("amount_total");
        txvAmount.setText("合计：" + amount);

        mPurchaseOrderLinesList = (ListView) mView.findViewById(R.id.lstPurchaseOrderLineIds);
        mPurchaseOrderLinesAdapter = new OEListAdapter(getActivity(), R.layout.fragment_purchase_order_detail_lines, mPurchaseOrderLines) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View mView = convertView;
                if (mView == null)
                    mView = getActivity().getLayoutInflater().inflate(getResource(), parent, false);

                mView = createListViewRow(mView, position);
                return mView;
            }
        };
        mPurchaseOrderLinesList.setAdapter(mPurchaseOrderLinesAdapter);
    }

    //初始化message_ids list
    private void initLstMessages() {
        mMessageListView = (ListView) mView.findViewById(R.id.lstMessages);
        mMessageListAdapter = new OEListAdapter(getActivity(),
                R.layout.fragment_message_ids_listview_items,
                mMessages) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View mView = convertView;
                if (mView == null)
                    mView = getActivity().getLayoutInflater().inflate(
                            getResource(), parent, false);
                mView = createListMessagesViewRow(mView, position);
                return mView;
            }
        };
        mMessageListView.setAdapter(mMessageListAdapter);
    }

    @SuppressLint("CutPasteId")
    private View createListMessagesViewRow(View mView, final int position) {

        final OEDataRow row = (OEDataRow) mMessages.get(position);
        TextView txvAuthor, txvEmail, txvTime;
        final TextView txvVoteNumber;
        txvAuthor = (TextView) mView.findViewById(R.id.txvMessageAuthor);
        txvEmail = (TextView) mView.findViewById(R.id.txvAuthorEmail);
        txvTime = (TextView) mView.findViewById(R.id.txvTime);

        String author = row.getString("email_from");
        String email = author;
        OEDataRow author_id = null;
        if (author.equals("false")) {
            author_id = row.getM2ORecord("author_id").browse();
            if (author_id != null) {
                author = author_id.getString("name");
                email = author_id.getString("email");
            }
        }
        txvAuthor.setText(author);
        txvEmail.setText(email);

        txvTime.setText(OEDate.getDate(row.getString("date"), TimeZone
                .getDefault().getID(), "MM dd, yyyy,  hh:mm a"));

        Log.d(TAG, "message body : " + row.getString("body"));
        WebView webView = (WebView) mView.findViewById(R.id.webViewMessageBody);
        //FIXME 此处参考了
        //http://wj495175289.blog.163.com/blog/static/1620826662012364512840/
        //对webview处理乱码的方法
        webView.loadData(row.getString("body"), "text/html;charset=UTF-8", null);

        ImageView imgUserPicture;
        imgUserPicture = (ImageView) mView.findViewById(R.id.imgUserPicture);

        if (author_id != null
                && !author_id.getString("image_small").equals("false")) {
            imgUserPicture.setImageBitmap(Base64Helper.getBitmapImage(
                    getActivity(), author_id.getString("image_small")));
        }

        // handling contact view
        OEContactView oe_contactView = (OEContactView) mView
                .findViewById(R.id.imgUserPicture);
        int partner_id = 0;
        if (author_id != null)
            partner_id = author_id.getInt("id");
        oe_contactView.assignPartnerId(partner_id);

        return mView;
    }

    @SuppressLint("CutPasteId")
    private View createListViewRow(View mView, final int position) {
        final OEDataRow row = (OEDataRow) mPurchaseOrderLines.get(position);
        TextView txvName, txvQuantity, txvPrice, txvTotalAmount;
        txvName = (TextView) mView.findViewById(R.id.txvPurchaseOrderLineName);
        txvQuantity = (TextView) mView.findViewById(R.id.txvPurchaseOrderLineProductQty);
        txvTotalAmount = (TextView) mView.findViewById(R.id.txvPurchaseOrderLineSubTotal);
        txvPrice = (TextView) mView.findViewById(R.id.txvPurchaseOrderLinePriceUnit);

        String name = row.getString("name");
        String quantity = row.getString("product_qty");
        String price = row.getString("price_unit");
        String total_amount = row.getString("price_subtotal");
        txvName.setText(name);
        txvQuantity.setText(quantity);
        txvPrice.setText(price);
        txvTotalAmount.setText(total_amount);

        return mView;
    }

    private void initData() {
        mMessagesLoader = new MessagesLoader();
        mMessagesLoader.execute((Void) null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_fragment_purchase_order_detail, menu);
        setMenuVisible(menu, !mProcessed);
    }

    //workflow处理完毕后,需要禁用审核按钮
    private void setMenuVisible(Menu menu, Boolean visible) {
        MenuItem item_ok = menu.findItem(R.id.menu_purchase_order_detail_audit);
        MenuItem item_cancel = menu.findItem(R.id.menu_purchase_order_detail_cancel);
        item_ok.setVisible(visible);
        item_cancel.setVisible(visible);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        setMenuVisible(menu, !mProcessed);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // handle item selection
        switch (item.getItemId()) {
            case R.id.menu_purchase_order_detail_audit:
                Log.d(TAG, "PurchaseOrderDetail#onOptionsItemSelected#ok");
                // 编写审批代码
                String signal = mPurchaseOrderData.getString("next_workflow_signal");
                mWorkflowOperation = new WorkflowOperation(signal);
                mWorkflowOperation.execute();
                return true;
            case R.id.menu_purchase_order_detail_cancel:
                Log.d(TAG, "PurchaseOrderDetail#onOptionsItemSelected#cancel");
                // 编写cancel代码
                mWorkflowOperation = new WorkflowOperation("refuse");
                mWorkflowOperation.execute();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        scope.context().registerReceiver(datasetChangeReceiver, new IntentFilter(DataSetChangeReceiver.DATA_CHANGED));
    }

    @Override
    public void onPause() {
        super.onPause();
        scope.context().unregisterReceiver(datasetChangeReceiver);
    }

    private DataSetChangeReceiver datasetChangeReceiver = new DataSetChangeReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String id = intent.getExtras().getString("id");
                String model = intent.getExtras().getString("model");
                if (model.equals("purchase_order") && mPurchaseOrderId == Integer.parseInt(id)) {
                    Log.d(TAG, "PurchaseOrderDetail->datasetChangeReceiver@onReceive");
                    OEDataRow row = db().select(Integer.parseInt(id));
                    mPurchaseOrderData = row;
                    //更新界面上state的显示
                    String name = mPurchaseOrderData.getString("name");
                    String state = mPurchaseOrderData.getString("state");
                    String status = getStatus(state);
                    TextView txvName = (TextView) mView.findViewById(R.id.txvExpenseName);

                    txvName.setText(name + "(" + status + ")");
                }
            } catch (Exception e) {
            }

        }
    };

    private String getStatus(String state) {
        int id = scope.main().getResources().getIdentifier("state_purchase_order_" + state, "string", scope.main().getPackageName());
        String value = id == 0 ? "" : scope.main().getResources().getString(id);
        return value;
    }


    public class MessagesLoader extends AsyncTask<Void, Void, Boolean> {
        //获取MessageDB,用于获取purchase_order的message_ids
        private OEDatabase messageDb() {
            return new MessageDB(scope.main());
        }

        public MessagesLoader() {
            mView.findViewById(R.id.loadingProgress).setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            String where = "res_id = ? and model = ?";
            String[] whereArgs = new String[]{mPurchaseOrderId + "", "purchase.order"};
            List<OEDataRow> result = messageDb().select(where, whereArgs, null, null, "date DESC");
            for (OEDataRow r : result)
                mMessages.add(r);

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mView.findViewById(R.id.loadingProgress).setVisibility(View.GONE);
            mMessageListAdapter.notifiyDataChange(mMessages);
            mMessagesLoader = null;
        }
    }

    /*
     *工作流处理类,用于异步处理工作流,处理过程如下:
     *1 用户点击[通过]或[不通过]按钮
     *2 系统异步调用服务端的exec_workflow
     *3 系统更新db中的操作状态为已操作,同时从服务器获取purchase order,并更新到本地
     *4 将审批通过按钮设置为disable
     *5 使用Toast提示用户操作完成
     *6 更新drawer的状态
     * */
    public class WorkflowOperation extends AsyncTask<Void, Void, Boolean> {
        boolean isConnection = true;
        OEHelper mOE = null;
        ProgressDialog mProgressDialog = null;
        String mSignal = null;

        public WorkflowOperation(String signal) {
            mSignal = signal;
            mOE = db().getOEInstance();
            if (mOE == null)
                isConnection = false;

            String working_text = scope.main().getResources().getString(R.string.working_text);
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage(working_text);
            if (isConnection) {
                mProgressDialog.show();
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Boolean execSuccess = false;
            if (!isConnection) {
                return false;
            }
            OEArguments arguments = new OEArguments();
            // Param 1 : model_name
            String modelName = "purchase.order";
            // Param 2 : res_id
            Integer resId = mPurchaseOrderId;
            //params 3 : signal
            try {
                mOE.exec_workflow(modelName, resId, mSignal);
                execSuccess = true;
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Creating Local Database Requirement Values
            OEValues values = new OEValues();
            String value = (execSuccess) ? "true" : "false";
            mProcessed = true;
            values.put("processed", value);

            if (execSuccess) {
                try {
                    db().update(values, mPurchaseOrderId);
                } catch (Exception e) {
                }
            }
            return execSuccess;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                //刷新菜单栏
                scope.main().supportInvalidateOptionsMenu();

                //重新同步数据
                scope.main().requestSync(PurchaseOrderProvider.AUTHORITY);

                DrawerListener drawer = (DrawerListener) getActivity();
                drawer.refreshDrawer(PurchaseOrder.TAG);

                String toast_text = scope.main().getResources().getString(R.string.purchase_order_processed_text);
                Toast.makeText(getActivity(), toast_text, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), "No connection", Toast.LENGTH_LONG).show();
            }
            mProgressDialog.dismiss();
        }
    }


    @Override
    public Object databaseHelper(Context context) {
        return new PurchaseOrderDB(context);
    }

    @Override
    public List<DrawerItem> drawerMenus(Context context) {
        return null;
    }
}
