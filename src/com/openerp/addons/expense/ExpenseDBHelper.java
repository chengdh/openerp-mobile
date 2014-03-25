package com.openerp.addons.expense;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.openerp.orm.OEColumn;
import com.openerp.orm.OEDBHelper;
import com.openerp.orm.OEDatabase;
import com.openerp.orm.OEFields;

public class ExpenseDBHelper extends OEDatabase {
  Context mContext = null;

  public ExpenseDBHelper(Context context) {
    super(context);
    mContext = context;
  }
  //hr.expense.line
  public class  ExpenseLine extends OEDatabase {
    Context mContext = null;

    public ExpenseLine(Context context) {
      super(context);
      mContext = context;
    }

    @Override
    public String getModelName() {
      return "hr.expense.line";
    }

    @Override
    public List<OEColumn> getModelColumns(){
      List<OEColumn> cols = new ArrayList<OEColumn>();
      cols.add(new OEColumn("name","Name",OEFields.varchar(128)));
      cols.add(new OEColumn("date_value","Date",OEFields.varchar(20)));
      cols.add(new OEColumn("total_amount","total amount",OEFields.integer()));
      cols.add(new OEColumn("unit_amount","unit amount",OEFields.integer()));
      cols.add(new OEColumn("unit_quantity","unit quantity",OEFields.integer()));
      //cols.add(new OEColumn("uom_id","unit of measure",OEFields.varchar(20)));
      //cols.add(new OEColumn("product_id","product",OEFields.varchar(20)));
      cols.add(new OEColumn("description","description",OEFields.text()));
      return cols;
    }
  }
  //hr.department
  public class Department extends OEDatabase {
    Context mContext = null;

    public Department(Context context) {
      super(context);
      mContext = context;
    }

    @Override
    public String getModelName() {
      return "hr.department";
    }

    @Override
    public List<OEColumn> getModelColumns(){
      List<OEColumn> cols = new ArrayList<OEColumn>();
      cols.add(new OEColumn("name","Name",OEFields.varchar(128)));
      return cols;
    }
  }
  //hr.employee
  public class Employee extends OEDatabase {
    Context mContext = null;

    public Employee(Context context) {
      super(context);
      mContext = context;
    }

    @Override
    public String getModelName() {
      return "hr.employee";
    }

    @Override
    public List<OEColumn> getModelColumns(){
      List<OEColumn> cols = new ArrayList<OEColumn>();
      cols.add(new OEColumn("name","Name",OEFields.varchar(128)));
      return cols;
    }
  }

  //获取osv name
  @Override
  public String getModelName() {
    return "hr.expense.expense";
  }

  //获取columns
  @Override
  public List<OEColumn> getModelColumns(){
    List<OEColumn> cols = new ArrayList<OEColumn>();
    //名称
    cols.add(new OEColumn("name","Name",OEFields.varchar(128)));
    cols.add(new OEColumn("date","Date",OEFields.varchar(20)));
    cols.add(new OEColumn("employee_id","Employee",OEFields.manyToOne(new Employee(mContext))));
    cols.add(new OEColumn("date_confirm","Date Confirm",OEFields.varchar(20)));
    cols.add(new OEColumn("date_valid","Date Valid",OEFields.varchar(20)));
    cols.add(new OEColumn("line_ids","Date Valid",OEFields.oneToMany(new ExpenseLine(mContext))));
    cols.add(new OEColumn("note","note",OEFields.varchar(200)));
    cols.add(new OEColumn("amount","amount",OEFields.integer()));
    cols.add(new OEColumn("department_id","department",OEFields.manyToOne(new Department(mContext))));
    cols.add(new OEColumn("state","state",OEFields.varchar(20)));
    return cols;
  }

}
