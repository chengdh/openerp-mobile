package com.openerp.services;

import android.accounts.Account;
import android.app.ActivityManager;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.openerp.MainActivity;
import com.openerp.OEArguments;
import com.openerp.OEDomain;
import com.openerp.R;
import com.openerp.addons.purchase.PurchaseOrderDB;
import com.openerp.auth.OpenERPAccountManager;
import com.openerp.orm.OEHelper;
import com.openerp.orm.OEValues;
import com.openerp.receivers.SyncFinishReceiver;
import com.openerp.support.OEUser;
import com.openerp.util.OENotificationHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chengdh on 14-7-22.
 */
public class PurchaseOrderSyncService extends Service {
    /**
     * The Constant TAG.
     */
    public static final String TAG = "com.openerp.services.PurchaseOrderSyncService";

    /**
     * The s sync adapter.
     */
    private static SyncAdapterImpl sSyncAdapter = null;

    /**
     * The i.
     */
    static int i = 0;

    /**
     * The context.
     */
    Context mContext = null;

    /**
     * Instantiates a new message sync service.
     */
    public PurchaseOrderSyncService() {
        super();
        mContext = this;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent intent) {
        IBinder ret = null;
        ret = getSyncAdapter().getSyncAdapterBinder();
        return ret;
    }

    /**
     * Gets the sync adapter.
     *
     * @return the sync adapter
     */
    public SyncAdapterImpl getSyncAdapter() {
        if (sSyncAdapter == null) {
            sSyncAdapter = new SyncAdapterImpl(this);
        }
        return sSyncAdapter;
    }

    /**
     * Perform sync.
     *
     * @param context    the context
     * @param account    the account
     * @param extras     the extras
     * @param authority  the authority
     * @param provider   the provider
     * @param syncResult the sync result
     */
    public void performSync(Context context, Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {

        Log.d(TAG, "PurchaseOrderSyncService->performSync()");
        Intent intent = new Intent();
        intent.setAction(SyncFinishReceiver.SYNC_FINISH);
        OEUser user = OpenERPAccountManager.getAccountDetail(context, account.name);
        try {
            PurchaseOrderDB purchaseOrderDB = new PurchaseOrderDB(context);
            purchaseOrderDB.setAccountUser(user);
            OEHelper oe = purchaseOrderDB.getOEInstance();
            if (oe == null) {
                return;
            }
            int user_id = user.getUser_id();

            // Updating User Context for OE-JSON-RPC
            JSONObject newContext = new JSONObject();
            //newContext.put("default_model", "res.users");
            //newContext.put("default_res_id", user_id);

            OEArguments arguments = new OEArguments();
            // Param 1 : domain
            //OEDomain domain = new OEDomain();
            //arguments.add(domain);
            // Param 2 : context
            arguments.add(oe.updateContext(newContext));

            //数据库中原有的数据也需要更新
            List<Integer> ids = purchaseOrderDB.ids();
            if (oe.syncWithMethod("get_waiting_audit_purchase_orders", arguments)) {
                int affected_rows = oe.getAffectedRows();
                Log.d(TAG, "PurchaseOrderSyncService[arguments]:" + arguments.toString());
                Log.d(TAG, "PurchaseOrderSyncService->affected_rows:" + affected_rows);
                List<Integer> affected_ids = oe.getAffectedIds();
                boolean notification = true;
                ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
                List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
                ComponentName componentInfo = taskInfo.get(0).topActivity;
                if (componentInfo.getPackageName().equalsIgnoreCase("com.openerp")) {
                    notification = false;
                }
                if (notification && affected_rows > 0) {
                    OENotificationHelper mNotification = new OENotificationHelper();
                    Intent mainActiivty = new Intent(context, MainActivity.class);
                    mainActiivty.setAction("PURCHASE");
                    mNotification.setResultIntent(mainActiivty, context);

                    String notify_title = context.getResources().getString(R.string.expenses_sync_notify_title);
                    notify_title = String.format(notify_title, affected_rows);

                    String notify_body = context.getResources().getString(R.string.expenses_sync_notify_body);
                    notify_body = String.format(notify_body, affected_rows);

                    mNotification.showNotification(context, notify_title, notify_body, authority, R.drawable.ic_oe_notification);
                }
                intent.putIntegerArrayListExtra("new_ids", (ArrayList<Integer>) affected_ids);
            }
            //更新数据库中已存在的expense信息
            List<Integer> updated_ids = updateOldPurchaseOrders(purchaseOrderDB, oe, user, ids);

        } catch (Exception e) {
            e.printStackTrace();
        }
        if (user.getAndroidName().equals(account.name)) {
            context.sendBroadcast(intent);
        }
    }

    //更新数据库中已有的expense信息
    private List<Integer> updateOldPurchaseOrders(PurchaseOrderDB db, OEHelper oe, OEUser user, List<Integer> ids) {
        Log.d(TAG, "PurchaseOrderSyncServide->updateOldPurchaseOrders()");
        List<Integer> updated_ids = new ArrayList<Integer>();
        try {
            JSONArray ids_array = new JSONArray();
            for (int id : ids)
                ids_array.put(id);

            JSONObject fields = new JSONObject();

            fields.accumulate("fields", "id");
            fields.accumulate("fields", "state");

            OEDomain domain = new OEDomain();
            domain.add("id", "in", ids_array);
            JSONObject result = oe.search_read("purchase.order", fields, domain.get());

            Log.d(TAG, "PurchaseOrderSyncService#updateOldPurchaseOrders" + result);
            for (int j = 0; j < result.getJSONArray("records").length(); j++) {
                JSONObject objRes = result.getJSONArray("records").getJSONObject(j);
                int purchase_order_id = objRes.getInt("id");
                String state = objRes.getString("state");
                OEValues values = new OEValues();
                values.put("state", state);
                db.update(values, purchase_order_id);
                updated_ids.add(purchase_order_id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return updated_ids;
    }

    /**
     * The Class SyncAdapterImpl.
     */
    public class SyncAdapterImpl extends AbstractThreadedSyncAdapter {

        /**
         * The m context.
         */
        private Context mContext;

        /**
         * Instantiates a new sync adapter impl.
         *
         * @param context the context
         */
        public SyncAdapterImpl(Context context) {
            super(context, true);
            mContext = context;
        }

        @Override
        public void onPerformSync(Account account, Bundle bundle, String str, ContentProviderClient providerClient, SyncResult syncResult) {
            if (account != null) {
                new PurchaseOrderSyncService().performSync(mContext, account, bundle, str, providerClient, syncResult);
            }
        }
    }
}
