package com.openerp.addons.voucher;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.openerp.orm.OEColumn;
import com.openerp.orm.OEDBHelper;
import com.openerp.orm.OEDatabase;
import com.openerp.orm.OEFields;
import com.openerp.addons.message.MessageDB;
import com.openerp.base.res.ResPartnerDB;

//供应商付款凭证
public class VoucherDB extends OEDatabase {
  Context mContext = null;

  public VoucherDB(Context context) {
    super(context);
    mContext = context;
  }
  //获取osv name
  @Override
  public String getModelName() {
    return "account.voucher";
  }

  //获取columns
  @Override
  public List<OEColumn> getModelColumns() {
    List<OEColumn> cols = new ArrayList<OEColumn>();
    //名称
    cols.add(new OEColumn("partner_id","partner",OEFields.manyToOne(new ResPartnerDB(mContext))));
    cols.add(new OEColumn("date","Date",OEFields.varchar(20)));
    cols.add(new OEColumn("state","state",OEFields.varchar(20)));
    cols.add(new OEColumn("amount","amount",OEFields.integer(20)));
    cols.add(new OEColumn("type","amount",OEFields.varchar(20)));
    cols.add(new OEColumn("processed","is processed",OEFields.varchar(20)));

    cols.add(new OEColumn("message_ids","messages",OEFields.oneToMany(new MessageDB(mContext))));
    //明细
    cols.add(new OEColumn("line_ids","voucher lines",OEFields.oneToMany(new VoucherLineDB(mContext))));
    return cols;
  }

  //付款单明细
  public class VoucherLineDB extends OEDatabase {
    Context mContext = null;

    public VoucherLineDB(Context context) {
      super(context);
      mContext = context;
    }

    //获取osv name
    @Override
    public String getModelName() {
      return "account.voucher.line";
    }

    //获取columns
    @Override
    public List<OEColumn> getModelColumns() {
      List<OEColumn> cols = new ArrayList<OEColumn>();
      //主表id
      cols.add(new OEColumn("voucher_id","master voucher",OEFields.integer()));
      //名称
      cols.add(new OEColumn("name","Description",OEFields.varchar(200)));
      //对应的会计分录
      cols.add(new OEColumn("account_id","Account",OEFields.manyToOne(new AccountDB(mContext))));
      //业务发生日期
      cols.add(new OEColumn("date_original","Date Original",OEFields.varchar(20)));
      //本次付款金额
      cols.add(new OEColumn("amount","amount",OEFields.integer()));
      return cols;
    }

  }

  //会计分录
  public class AccountDB extends OEDatabase {
    Context mContext = null;

    public AccountDB(Context context) {
      super(context);
      mContext = context;
    }

    //获取osv name
    @Override
    public String getModelName() {
      return "account.account";
    }

    //获取columns
    @Override
    public List<OEColumn> getModelColumns() {
      List<OEColumn> cols = new ArrayList<OEColumn>();
      //名称
      cols.add(new OEColumn("name","name",OEFields.varchar(200)));
      //分录类型
      cols.add(new OEColumn("type","Type",OEFields.varchar(200)));
      //贷方金额
      cols.add(new OEColumn("credit","credit",OEFields.integer()));
      cols.add(new OEColumn("debit","debit",OEFields.integer()));
      cols.add(new OEColumn("balance","balance",OEFields.integer()));
      return cols;
    }
  }
}
