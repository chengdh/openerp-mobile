package com.openerp.addons.expense;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import openerp.OEArguments;

import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.openerp.R;
import com.openerp.orm.OEDataRow;
import com.openerp.orm.OEHelper;
import com.openerp.orm.OEValues;
import com.openerp.support.BaseFragment;
import com.openerp.support.OEUser;
import com.openerp.support.listview.OEListAdapter;
import com.openerp.util.Base64Helper;
import com.openerp.util.OEBinaryDownloadHelper;
import com.openerp.util.OEDate;
import com.openerp.util.OEFileSizeHelper;
import com.openerp.util.contactview.OEContactView;
import com.openerp.util.drawer.DrawerItem;
import com.openerp.util.drawer.DrawerListener;
import com.openerp.providers.expense.ExpenseProvider;
import com.openerp.receivers.DataSetChangeReceiver;
import com.openerp.support.AppScope;

public class ExpenseDetail extends BaseFragment {

  public static final String TAG = "com.openerp.addons.expense.ExpenseDetail";
  View mView = null;
  Integer mExpenseId = null;
  OEDataRow mExpenseData = null;
  ListView mExpenseLinesView = null;
  OEListAdapter mExpenseLinesAdapter = null;
  //是否已操作
  Boolean mProcessed = false;
  //费用单明细
  List<Object> mExpenseLines = new ArrayList<Object>();
  //工作流审批对象
  WorkflowOperation mWorkflowOperation = null;
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
    setHasOptionsMenu(true);
    mView = inflater.inflate(R.layout.fragment_expense_detail_view,container, false);
    scope = new AppScope(getActivity());
    init();
    return mView;
  }

  private void init() {
    Log.d(TAG, "ExpenseDetail->init()");
    Bundle bundle = getArguments();
    if (bundle != null) {
      mExpenseId = bundle.getInt("expense_id");
      mExpenseData = db().select(mExpenseId);
      List<OEDataRow> lines = mExpenseData.getO2MRecord("line_ids").browseEach();
      for(Object l : lines){
        mExpenseLines.add(l);
      }
      initControls();
    }
  }

  private void initControls() {
    //设置主表内容
    TextView txvName,txvStatus,txvEmployee,txvDate,txvAmount;
    txvName = (TextView) mView.findViewById(R.id.txvExpenseName);
    txvEmployee = (TextView) mView.findViewById(R.id.txvExpenseEmployee);
    txvDate = (TextView) mView.findViewById(R.id.txvExpenseDate);
    txvAmount = (TextView) mView.findViewById(R.id.txvExpenseAmount);

    String name = mExpenseData.getString("name");
    String status = mExpenseData.getString("state");
    txvName.setText(name + "(" + status + ")");

    OEDataRow employee = mExpenseData.getM2ORecord("employee_id").browse();
    txvEmployee.setText(employee.getString("name"));
    String date = mExpenseData.getString("date");
    txvDate.setText(date);
    String amount = mExpenseData.getString("amount");
    txvAmount.setText("合计：" + amount);

    mExpenseLinesView = (ListView) mView.findViewById(R.id.lstLineIds);
    mExpenseLinesAdapter = new OEListAdapter(getActivity(),R.layout.fragment_expense_detail_expense_lines,mExpenseLines) {
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        View mView = convertView;
        if (mView == null)
          mView = getActivity().getLayoutInflater().inflate(getResource(), parent, false);
        mView = createListViewRow(mView, position);
        return mView;
      }
    };
    mExpenseLinesView.setAdapter(mExpenseLinesAdapter);
  }

  @SuppressLint("CutPasteId")
  private View createListViewRow(View mView, final int position) {
    final OEDataRow row = (OEDataRow) mExpenseLines.get(position);
    TextView txvName, txvQuantity, txvTotalAmount;
    txvName = (TextView) mView.findViewById(R.id.txvExpenseLineName);
    txvQuantity = (TextView) mView.findViewById(R.id.txvExpenseLineUnitQuantity);
    txvTotalAmount = (TextView) mView.findViewById(R.id.txvExpenseLineTotalAmount);

    String name = row.getString("name");
    String quantity = row.getString("unit_quantity");
    String total_amount = row.getString("total_amount");
    //txvNo.setText(1);
    txvName.setText(name);
    txvQuantity.setText(quantity);
    txvTotalAmount.setText(total_amount);

    return mView;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.menu_fragment_expense_detail, menu);
    getActivity().supportInvalidateOptionsMenu();
  }

  //workflow处理完毕后,需要禁用审核按钮
  @Override
  public void onPrepareOptionsMenu (Menu menu) {
    MenuItem item_ok= menu.findItem(R.id.menu_expense_detail_audit);
    MenuItem item_cancel= menu.findItem(R.id.menu_expense_detail_cancel);

    if (mProcessed){
      Log.d(TAG, "ExpenseDetail#onPrepareOptionsMenu:set menuitem disabeld");
      item_ok.setVisible(false);
      item_cancel.setVisible(false);
    }
    else{
      Log.d(TAG, "ExpenseDetail#onPrepareOptionsMenu:set menuitem disabeld");
      item_ok.setVisible(true);
      item_cancel.setVisible(true);
    }
    super.onPrepareOptionsMenu(menu);
  }
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    // handle item selection
    switch (item.getItemId()) {
      case R.id.menu_expense_detail_audit:
        Log.d(TAG, "ExpenseDetail#onOptionsItemSelected#ok");
        // 编写审批代码
        String signal = mExpenseData.getString("next_workflow_signal");
        mWorkflowOperation = new WorkflowOperation(signal);
        mWorkflowOperation.execute();
        return true;
      case R.id.menu_expense_detail_cancel:
        Log.d(TAG, "ExpenseDetail#onOptionsItemSelected#cancel");
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
    scope.context().registerReceiver(datasetChangeReceiver,new IntentFilter(DataSetChangeReceiver.DATA_CHANGED));
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
        if (model.equals("hr.expense.expense") && mExpenseId == Integer.parseInt(id)) {
          Log.d(TAG, "ExpenseDetail->datasetChangeReceiver@onReceive");
          OEDataRow row = db().select(Integer.parseInt(id));
          mExpenseData = row;
          //更新界面上state的显示
          String name = mExpenseData.getString("name");
          String status = mExpenseData.getString("state");
          TextView txvName = (TextView) mView.findViewById(R.id.txvExpenseName);
          txvName.setText(name + "(" + status + ")");
        }
      } catch (Exception e) {}

    }
  };

  /*
   *工作流处理类,用于异步处理工作流,处理过程如下:
   *1 用户点击[通过]或[不通过]按钮
   *2 系统异步调用服务端的exec_workflow
   *3 系统更新db中的操作状态为已操作,同时从服务器获取expense的最后状态,并更新到本地
   *4 将审批通过按钮设置为disable
   *5 使用Toast提示用户操作完成
   *6 更新drawer的状态
   * */
  public class WorkflowOperation extends AsyncTask<Void,Void,Boolean> {
    boolean isConnection = true;
    OEHelper mOE = null;
    ProgressDialog mProgressDialog = null;
    String mSignal = null;

    public WorkflowOperation(String signal){
      mSignal = signal;
			mOE = db().getOEInstance();
			if (mOE == null)
				isConnection = false;
			mProgressDialog = new ProgressDialog(getActivity());
			mProgressDialog.setMessage("Working...");
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
      String modelName = "hr.expense.expense";
      // Param 2 : res_id
      Integer resId = mExpenseId;
      //params 3 : signal
      try{
        mOE.exec_workflow(modelName,resId,mSignal);
        execSuccess = true;
      }
      catch (Exception e) {
        e.printStackTrace();
      }

			// Creating Local Database Requirement Values
			OEValues values = new OEValues();
			String value = (execSuccess) ? "true" : "false";
      mProcessed = true;
			values.put("processed", value);

			if (execSuccess) {
				try {
						db().update(values,mExpenseId);
				} catch (Exception e) {}
			}
			return execSuccess;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
        //刷新菜单栏
        scope.main().supportInvalidateOptionsMenu();

        //重新同步数据
        scope.main().requestSync(ExpenseProvider.AUTHORITY);

				DrawerListener drawer = (DrawerListener) getActivity();
				drawer.refreshDrawer(Expense.TAG);
				Toast.makeText(getActivity(), "expense has processed",Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(getActivity(), "No connection",Toast.LENGTH_LONG).show();
			}
			mProgressDialog.dismiss();
		}
  }

  @Override
  public Object databaseHelper(Context context) {
    return new ExpenseDBHelper(context);
  }

  @Override
  public List<DrawerItem> drawerMenus(Context context) {
    return null;
  }
}
