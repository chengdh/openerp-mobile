package com.openerp.addons.purchase;

import android.content.Context;

import com.openerp.addons.message.MessageDB;
import com.openerp.base.res.ResPartnerDB;
import com.openerp.orm.OEColumn;
import com.openerp.orm.OEDatabase;
import com.openerp.orm.OEFields;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chengdh on 14-7-21.
 */
public class PurchaseOrderDB extends OEDatabase {
    public PurchaseOrderDB(Context context) {
        super(context);
    }

    @Override
    public String getModelName() {

        return "purchase.order";
    }

    @Override
    public List<OEColumn> getModelColumns() {
        List<OEColumn> cols = new ArrayList<OEColumn>();
        cols.add(new OEColumn("partner_id","Partner",OEFields.manyToOne(new ResPartnerDB(mContext))));
        cols.add(new OEColumn("date_order","Date Order",OEFields.varchar(20)));
        cols.add(new OEColumn("order_line","Order Lines",OEFields.oneToMany(new PurchaseOrderLineDB(mContext))));
        cols.add(new OEColumn("amount_total","Amount Total",OEFields.varchar(20)));
        cols.add(new OEColumn("state","State",OEFields.varchar(20)));
        cols.add(new OEColumn("next_workflow_signal","next_signal",OEFields.varchar(20)));
        cols.add(new OEColumn("processed","is processed",OEFields.varchar(20)));

        cols.add(new OEColumn("message_ids","messages",OEFields.oneToMany(new MessageDB(mContext))));

        return cols;
    }

    /**
     * The type Purchase order line dB.
     */
    public class PurchaseOrderLineDB extends OEDatabase {

        public PurchaseOrderLineDB(Context context) {
            super(context);
        }

        @Override
        public String getModelName() {
            return "purchase.order.line";
        }

        @Override
        public List<OEColumn> getModelColumns() {
            List<OEColumn> cols = new ArrayList<OEColumn>();
            cols.add(new OEColumn("order_id","Master ID", OEFields.integer(20)));
            cols.add(new OEColumn("name","name", OEFields.varchar(20)));
            cols.add(new OEColumn("product_qty","Quantity", OEFields.integer()));
            cols.add(new OEColumn("price_unit","Price", OEFields.varchar(20)));
            cols.add(new OEColumn("price_subtotal","SubTotal", OEFields.varchar(20)));
            return cols;
        }
    }
}

