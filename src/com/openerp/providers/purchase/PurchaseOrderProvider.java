package com.openerp.providers.purchase;

import com.openerp.support.provider.OEContentProvider;

/**
 * Created by chengdh on 14-7-22.
 */
public class PurchaseOrderProvider extends OEContentProvider {
    public static String CONTENTURI = "com.openerp.providers.purchase.PurchaseOrderProvider";
    public static String AUTHORITY = "com.openerp.providers.purchase";

    @Override
    public String authority() {
        return AUTHORITY;
    }

    @Override
    public String contentUri() {
        return CONTENTURI;
    }
}
