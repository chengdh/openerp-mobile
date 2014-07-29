/*
 * OpenERP, Open Source Management Solution
 * Copyright (C) 2012-today OpenERP SA (<http:www.openerp.com>)
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
 * along with this program.  If not, see <http:www.gnu.org/licenses/>
 * 
 */
package com.openerp.config;

import java.util.ArrayList;
import java.util.List;

import android.provider.CalendarContract;
import android.provider.ContactsContract;

import com.openerp.providers.expense.ExpenseProvider;
import com.openerp.providers.purchase.PurchaseOrderProvider;
import com.openerp.providers.voucher.VoucherProvider;
import com.openerp.providers.groups.MailGroupProvider;
import com.openerp.providers.message.MessageProvider;
import com.openerp.providers.note.NoteProvider;
import com.openerp.support.SyncValue;
import com.openerp.support.SyncWizardHelper;

public class SyncWizardValues implements SyncWizardHelper {

    @Override
    public List<SyncValue> syncValues() {
        List<SyncValue> list = new ArrayList<SyncValue>();

        //单据信息
        list.add(new SyncValue("bills"));
        list.add(new SyncValue("expenses", ExpenseProvider.AUTHORITY, SyncValue.Type.CHECKBOX));
        list.add(new SyncValue("vouchers", VoucherProvider.AUTHORITY, SyncValue.Type.CHECKBOX));
        list.add(new SyncValue("purchase_orders", PurchaseOrderProvider.AUTHORITY, SyncValue.Type.CHECKBOX));
        /* Social */
        list.add(new SyncValue("social"));
        list.add(new SyncValue("messages", MessageProvider.AUTHORITY,
                SyncValue.Type.CHECKBOX));
        list.add(new SyncValue("groups", MailGroupProvider.AUTHORITY,
                SyncValue.Type.CHECKBOX));

		/* Contacts */
/*		list.add(new SyncValue("contacts"));
		List<SyncValue> radioGroups = new ArrayList<SyncValue>();
		radioGroups.add(new SyncValue("all_contacts",
				ContactsContract.AUTHORITY, SyncValue.Type.RADIO));
		radioGroups.add(new SyncValue("local_contacts",
				ContactsContract.AUTHORITY, SyncValue.Type.RADIO));
		list.add(new SyncValue(radioGroups));

		*//* Notes *//*
		list.add(new SyncValue("notes"));
		list.add(new SyncValue("notes", NoteProvider.AUTHORITY,
				SyncValue.Type.CHECKBOX));

		*//* Meetings *//*
		list.add(new SyncValue("calendar"));
		list.add(new SyncValue("meetings", CalendarContract.AUTHORITY,
				SyncValue.Type.CHECKBOX));*/
        return list;
    }
}
