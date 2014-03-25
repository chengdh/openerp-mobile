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
package com.openerp.services;

import java.util.ArrayList;
import java.util.List;

import openerp.OEArguments;
import openerp.OEDomain;

import org.json.JSONArray;
import org.json.JSONObject;

import android.accounts.Account;
import android.app.ActivityManager;
import android.app.Service;
import android.appwidget.AppWidgetManager;
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
import com.openerp.R;
import com.openerp.addons.message.MessageDB;
import com.openerp.addons.expense.ExpenseDBHelper;
import com.openerp.auth.OpenERPAccountManager;
import com.openerp.orm.OEDataRow;
import com.openerp.orm.OEHelper;
import com.openerp.orm.OEValues;
import com.openerp.receivers.SyncFinishReceiver;
import com.openerp.support.OEUser;
import com.openerp.util.OEDate;
import com.openerp.util.OENotificationHelper;
import com.openerp.util.PreferenceManager;
import com.openerp.widgets.message.MessageWidget;

/**
 * The Class ExpenseSyncService.
 */
public class ExpenseSyncService extends Service {

  /** The Constant TAG. */
  public static final String TAG = "com.openerp.services.ExpenseSyncService";

  /** The s sync adapter. */
  private static SyncAdapterImpl sSyncAdapter = null;

  /** The i. */
  static int i = 0;

  /** The context. */
  Context mContext = null;

  /**
   * Instantiates a new message sync service.
   */
  public ExpenseSyncService() {
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
   * @param context
   *            the context
   * @param account
   *            the account
   * @param extras
   *            the extras
   * @param authority
   *            the authority
   * @param provider
   *            the provider
   * @param syncResult
   *            the sync result
   */
  public void performSync(Context context, Account account, Bundle extras,
      String authority, ContentProviderClient provider,
      SyncResult syncResult) {

		Log.d(TAG, "ExpenseSyncService->performSync()");
    Intent intent = new Intent();
    Intent updateWidgetIntent = new Intent();
    updateWidgetIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
    intent.setAction(SyncFinishReceiver.SYNC_FINISH);
    OEUser user = OpenERPAccountManager.getAccountDetail(context,account.name);
    try {
      ExpenseDBHelper expense_db = new ExpenseDBHelper(context);
      expense_db.setAccountUser(user);
      OEHelper oe = expense_db.getOEInstance();
      if (oe == null) {
        return;
      }
      int user_id = user.getUser_id();

      // Updating User Context for OE-JSON-RPC
      JSONObject newContext = new JSONObject();
      newContext.put("default_model", "res.users");
      newContext.put("default_res_id", user_id);

      OEArguments arguments = new OEArguments();
      arguments.add(the_ids);
      // Param 2 : domain
      OEDomain domain = new OEDomain();
      List<Integer> ids = expense_db.ids();
      if (oe.syncWithMethod("read", arguments)) {
        int affected_rows = oe.getAffectedRows();
        Log.d(TAG, "ExpenseSyncService->affected_rows:" + affected_rows);
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
          Intent mainActiivty = new Intent(context,MainActivity.class);
          mNotification.setResultIntent(mainActiivty, context);
          mNotification.showNotification(context, affected_rows
              + " new expenses", affected_rows
              + " new expenses received (OpneERP)", authority,
              R.drawable.ic_oe_notification);
        }
        intent.putIntegerArrayListExtra("new_ids",(ArrayList<Integer>) affected_ids);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (user.getAndroidName().equals(account.name)) {
      context.sendBroadcast(intent);
      context.sendBroadcast(updateWidgetIntent);
    }
  }

  /**
   * The Class SyncAdapterImpl.
   */
  public class SyncAdapterImpl extends AbstractThreadedSyncAdapter {

    /** The m context. */
    private Context mContext;

    /**
     * Instantiates a new sync adapter impl.
     * 
     * @param context
     *            the context
     */
    public SyncAdapterImpl(Context context) {
      super(context, true);
      mContext = context;
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String str,ContentProviderClient providerClient, SyncResult syncResult) {
      if (account != null) {
        new ExpenseSyncService().performSync(mContext, account, bundle,str, providerClient, syncResult);
      }
    }
  }
}
