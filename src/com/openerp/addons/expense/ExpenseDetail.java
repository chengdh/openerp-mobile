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

public class ExpenseDetail extends BaseFragment {

  public static final String TAG = "com.openerp.addons.expense.ExpenseDetail";
  View mView = null;
  Integer mExpenseId = null;
  OEDataRow mExpenseData = null;
  ListView mExpenseLinesView = null;
  OEListAdapter mExpenseLinesAdapter = null;
  //费用单明细
  List<Object> mExpenseLines = new ArrayList<Object>();
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    setHasOptionsMenu(true);
    mView = inflater.inflate(R.layout.fragment_expense_detail_view,container, false);
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
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    // handle item selection
    switch (item.getItemId()) {
      case R.id.menu_expense_detail_audit:
        Log.d(TAG, "ExpenseDetail#onOptionsItemSelected#ok");
        // 编写审批代码
        String signal = mExpenseData.getString("next_workflow_signal");
        exec_workflow(signal);
        return true;
      case R.id.menu_expense_detail_cancel:
        Log.d(TAG, "ExpenseDetail#onOptionsItemSelected#cancel");
        // 编写cancel代码
        exec_workflow("refuse");
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }
  //审核处理
  private void exec_workflow(String signal){
    OEHelper oe = db().getOEInstance();
    if (oe == null) {
      return ;
    }

    OEArguments arguments = new OEArguments();
    // Param 1 : model_name 
    String model_name = "hr.expense.expense";
    // Param 2 : res_id
    Integer res_id = mExpenseId;
    //params 3 : signal
    try{
      oe.exec_workflow(model_name,res_id,signal);
    }
    catch (Exception e) {
			e.printStackTrace();
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
