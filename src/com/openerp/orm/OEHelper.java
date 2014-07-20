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
package com.openerp.orm;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.openerp.OEArguments;
import com.openerp.OEDomain;
import com.openerp.OEVersionException;
import com.openerp.base.ir.Ir_model;
import com.openerp.orm.OEFieldsHelper.OERelationData;
import com.openerp.support.OEUser;
import com.openerp.util.OEDate;
import com.openerp.util.PreferenceManager;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OEHelper extends com.openerp.OpenERP {
    public static final String TAG = "com.openerp.orm.OEHelper";
    Context mContext = null;
    OEDatabase mDatabase = null;
    OEUser mUser = null;
    PreferenceManager mPref = null;
    int mAffectedRows = 0;
    List<Long> mResultIds = new ArrayList<Long>();
    List<OEDataRow> mRemovedRecordss = new ArrayList<OEDataRow>();

    public OEHelper(SharedPreferences pref) {
        super(pref);
        init();
    }

    public OEHelper(Context context, String host)
            throws ClientProtocolException, JSONException, IOException,
            OEVersionException {
        super(host);
        mContext = context;
        init();
    }

    public OEHelper(Context context, OEUser data, OEDatabase oeDatabase) throws ClientProtocolException, JSONException, IOException, OEVersionException {
        super(data.getHost());
        Log.d(TAG, "OEHelper->OEHelper(Context, OEUser, OEDatabase)");
        Log.d(TAG, "Called from OEDatabase->getOEInstance()");
        mContext = context;
        mDatabase = oeDatabase;
        mUser = data;
        init();

        /*
         * Required to login with server.
		 */

        login(mUser.getUsername(), mUser.getPassword(), mUser.getDatabase(), mUser.getHost());
    }

    private void init() {
        Log.d(TAG, "OEHelper->init()");
        mPref = new PreferenceManager(mContext);
    }

    public OEUser login(String username, String password, String database, String serverURL) {
        OEUser userObj = null;
        try {
            JSONObject response = this.authenticate(username, password, database);
            int userId = 0;
            if (response.get("uid") instanceof Integer) {
                userId = response.getInt("uid");

                OEFieldsHelper fields = new OEFieldsHelper(new String[]{"partner_id", "tz", "image", "company_id"});
                OEDomain domain = new OEDomain();
                domain.add("id", "=", userId);
                JSONObject res = search_read("res.users", fields.get(), domain.get()).getJSONArray("records").getJSONObject(0);

                userObj = new OEUser();
                userObj.setAvatar(res.getString("image"));

                userObj.setDatabase(database);
                userObj.setHost(serverURL);
                userObj.setIsactive(true);
                userObj.setAndroidName(androidName(username, database));
                userObj.setPartner_id(res.getJSONArray("partner_id").getInt(0));
                userObj.setTimezone(res.getString("tz"));
                userObj.setUser_id(userId);
                userObj.setUsername(username);
                userObj.setPassword(password);
                String company_id = new JSONArray(res.getString("company_id")).getString(0);
                userObj.setCompany_id(company_id);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return userObj;
    }

    public OEUser getUser() {
        return mUser;
    }

    private String androidName(String username, String database) {
        StringBuffer android_name = new StringBuffer();
        android_name.append(username);
        android_name.append("[");
        android_name.append(database);
        android_name.append("]");
        return android_name.toString();
    }

    public boolean syncWithServer() {
        return syncWithServer(false, null, null, false, -1, false);
    }

    public boolean syncWithServer(boolean removeLocalIfNotExists) {
        return syncWithServer(false, null, null, false, -1,
                removeLocalIfNotExists);
    }

    public boolean syncWithServer(OEDomain domain,
                                  boolean removeLocalIfNotExists) {
        return syncWithServer(false, domain, null, false, -1,
                removeLocalIfNotExists);
    }

    public boolean syncWithServer(OEDomain domain) {
        return syncWithServer(false, domain, null, false, -1, false);
    }

    public boolean syncWithServer(boolean twoWay, OEDomain domain,
                                  List<Object> ids) {
        return syncWithServer(twoWay, domain, ids, false, -1, false);
    }

    public int getAffectedRows() {
        return mAffectedRows;
    }

    public List<OEDataRow> getRemovedRecords() {
        return mRemovedRecordss;
    }

    public List<Integer> getAffectedIds() {
        List<Integer> ids = new ArrayList<Integer>();
        for (Long id : mResultIds) {
            ids.add(Integer.parseInt(id.toString()));
        }
        return ids;
    }

    public boolean syncWithMethod(String method, OEArguments args) {
        return syncWithMethod(method, args, false);
    }

    public boolean syncWithMethod(String method, OEArguments args, boolean removeLocalIfNotExists) {
        Log.d(TAG, "OEHelper->syncWithMethod()");
        Log.d(TAG, "Model: " + mDatabase.getModelName());
        Log.d(TAG, "User: " + mUser.getAndroidName());
        Log.d(TAG, "Method: " + method);
        boolean synced = false;
        OEFieldsHelper fields = new OEFieldsHelper(mDatabase.getDatabaseColumns());
        try {
            JSONObject result = call_kw(mDatabase.getModelName(), method, args.getArray());

            Log.d(TAG, "result: " + result);
            if (result.getJSONArray("result").length() > 0)
                mAffectedRows = result.getJSONArray("result").length();

            synced = handleResultArray(fields, result.getJSONArray("result"), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return synced;
    }

    public boolean syncWithServer(boolean twoWay, OEDomain domain,
                                  List<Object> ids, boolean limitedData, int limits,
                                  boolean removeLocalIfNotExists) {
        boolean synced = false;
        Log.d(TAG, "OEHelper->syncWithServer()");
        Log.d(TAG, "Model: " + mDatabase.getModelName());
        Log.d(TAG, "User: " + mUser.getAndroidName());
        OEFieldsHelper fields = new OEFieldsHelper(
                mDatabase.getDatabaseColumns());
        try {
            if (domain == null) {
                domain = new OEDomain();
            }
            if (ids != null) {
                domain.add("id", "in", ids);
            }
            if (limitedData) {
                int data_limit = mPref.getInt("sync_data_limit", 60);
                domain.add("create_date", ">=",
                        OEDate.getDateBefore(data_limit));
            }

            if (limits == -1) {
                limits = 50;
            }
            JSONObject result = search_read(mDatabase.getModelName(), fields.get(), domain.get(), 0, limits, null, null);
            mAffectedRows = result.getJSONArray("records").length();
            synced = handleResultArray(fields, result.getJSONArray("records"), removeLocalIfNotExists);

        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, mDatabase.getModelName() + " synced");
        return synced;
    }

    private boolean handleResultArray(OEFieldsHelper fields, JSONArray results,
                                      boolean removeLocalIfNotExists) {
        boolean flag = false;
        try {
            fields.addAll(results);
            // Handling many2many and many2one records
            // 通过id从服务器端获取many2many many2one one2many表的相关值
            List<OERelationData> rel_models = fields.getRelationData();
            for (OERelationData rel : rel_models) {
                OEHelper oe = rel.getDb().getOEInstance();
                oe.syncWithServer(false, null, rel.getIds(), false, 0, false);
            }
            List<Long> result_ids = mDatabase.createORReplace(fields.getValues(), removeLocalIfNotExists);
            mResultIds.addAll(result_ids);
            mRemovedRecordss.addAll(mDatabase.getRemovedRecords());
            if (result_ids.size() > 0) {
                flag = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    public boolean isModelInstalled(String model) {
        boolean installed = false;
        Ir_model ir_model = new Ir_model(mContext);
        try {
            OEFieldsHelper fields = new OEFieldsHelper(new String[]{"model"});
            OEDomain domain = new OEDomain();
            domain.add("model", "=", model);
            JSONObject result = search_read(ir_model.getModelName(),
                    fields.get(), domain.get());
            if (result.getInt("length") > 0) {
                installed = true;
                JSONObject record = result.getJSONArray("records").getJSONObject(0);
                OEValues values = new OEValues();
                values.put("id", record.getInt("id"));
                values.put("model", record.getString("model"));
                values.put("is_installed", installed);
                int count = ir_model.count("model = ?", new String[]{model});
                if (count > 0)
                    ir_model.update(values, "model = ?", new String[]{model});
                else
                    ir_model.create(values);
            }

        } catch (Exception e) {
            Log.d(TAG, "OEHelper->isModuleInstalled()");
            Log.e(TAG, e.getMessage() + ". No connection with OpenERP server");
        }
        return installed;
    }

    public List<OEDataRow> search_read_remain() {
        Log.d(TAG, "OEHelper->search_read_remain()");
        return search_read(true);
    }

    private OEDomain getLocalIdsDomain(String operator) {
        OEDomain domain = new OEDomain();
        JSONArray ids = new JSONArray();
        for (OEDataRow row : mDatabase.select()) {
            ids.put(row.getInt("id"));
        }
        domain.add("id", operator, ids);
        return domain;
    }

    private List<OEDataRow> search_read(boolean getRemain) {
        List<OEDataRow> rows = new ArrayList<OEDataRow>();
        try {
            OEFieldsHelper fields = new OEFieldsHelper(
                    mDatabase.getDatabaseServerColumns());
            JSONObject domain = null;
            if (getRemain)
                domain = getLocalIdsDomain("not in").get();
            JSONObject result = search_read(mDatabase.getModelName(),
                    fields.get(), domain, 0, 100, null, null);
            for (int i = 0; i < result.getJSONArray("records").length(); i++) {
                JSONObject record = result.getJSONArray("records")
                        .getJSONObject(i);
                OEDataRow row = new OEDataRow();
                row.put("id", record.getInt("id"));
                for (OEColumn col : mDatabase.getDatabaseServerColumns()) {
                    row.put(col.getName(), record.get(col.getName()));
                }
                rows.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rows;
    }

    public List<OEDataRow> search_read() {
        Log.d(TAG, "OEHelper->search_read()");
        return search_read(false);
    }

    public void delete(int id) {
        Log.d(TAG, "OEHelper->delete()");
        try {
            unlink(mDatabase.getModelName(), id);
            mDatabase.delete(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Object call_kw(String method, OEArguments arguments) {
        return call_kw(method, arguments, new JSONObject());
    }

    public Object call_kw(String method, OEArguments arguments,
                          JSONObject context) {
        return call_kw(null, method, arguments, context);
    }

    public Object call_kw(String model, String method, OEArguments arguments,
                          JSONObject context) {
        Log.d(TAG, "OEHelper->call_kw()");
        JSONObject result = null;
        if (model == null) {
            model = mDatabase.getModelName();
        }
        try {
            if (context != null) {
                arguments.add(updateContext(context));
            }
            result = call_kw(model, method, arguments.getArray());
            return result.get("result");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Integer create(OEValues values) {
        Log.d(TAG, "OEHelper->create()");
        Integer newId = null;
        try {
            JSONObject result = createNew(mDatabase.getModelName(),
                    generateArguments(values));
            newId = result.getInt("result");
            values.put("id", newId);
            mDatabase.create(values);
            return newId;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newId;
    }

    public Boolean update(OEValues values, Integer id) {
        Log.d(TAG, "OEHelper->update()");
        Boolean flag = false;
        try {
            flag = updateValues(mDatabase.getModelName(),
                    generateArguments(values), id);
            if (flag)
                mDatabase.update(values, id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    private JSONObject generateArguments(OEValues values) {
        Log.d(TAG, "OEHelper->generateArguments()");
        JSONObject arguments = new JSONObject();
        try {
            for (String key : values.keys()) {
                if (values.get(key) instanceof OEM2MIds) {
                    OEM2MIds m2mIds = (OEM2MIds) values.get(key);
                    JSONArray m2mArray = new JSONArray();
                    m2mArray.put(6);
                    m2mArray.put(false);
                    m2mArray.put(m2mIds.getJSONIds());
                    arguments.put(key, new JSONArray("[" + m2mArray.toString()
                            + "]"));
                } else {
                    arguments.put(key, values.get(key));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arguments;
    }

    public boolean moduleExists(String name) {
        Log.d(TAG, "OEHelper->moduleExists()");
        boolean flag = false;
        try {
            OEDomain domain = new OEDomain();
            domain.add("name", "ilike", name);
            OEFieldsHelper fields = new OEFieldsHelper(new String[]{"state"});
            JSONObject result = search_read("ir.module.module", fields.get(),
                    domain.get());
            JSONArray records = result.getJSONArray("records");
            if (records.length() > 0
                    && records.getJSONObject(0).getString("state")
                    .equalsIgnoreCase("installed")) {
                flag = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }
}
