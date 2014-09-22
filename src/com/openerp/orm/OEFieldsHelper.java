package com.openerp.orm;

import android.util.Log;

import com.openerp.orm.OEM2MIds.Operation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class OEFieldsHelper {
    public static final String TAG = "com.openerp.orm.OEFieldsHelper";
    JSONObject mFields = new JSONObject();
    List<OEValues> mValues = new ArrayList<OEValues>();
    List<OEColumn> mColumns = new ArrayList<OEColumn>();
    OERelRecord mRelRecord = new OERelRecord();
    ValueWatcher mValueWatcher = null;

    public OEFieldsHelper(String[] fields) {
        addAll(fields);
    }

    public OEFieldsHelper(List<OEColumn> cols) {
        addAll(cols);
        mColumns.addAll(cols);
        mColumns.add(new OEColumn("id", "id", OEFields.integer()));
    }

    public OEFieldsHelper(JSONArray records) {
        addAll(records);
    }

    public void addAll(JSONArray records) {
        try {
            for (int i = 0; i < records.length(); i++) {
                JSONObject record = records.getJSONObject(i);
                OEValues cValue = new OEValues();
                for (OEColumn col : mColumns) {
                    if (col.canSync()) {
                        String key = col.getName();
                        Object value = false;
                        if (record.has(key)) {
                            value = record.get(key);
                        }
                        if (col.getmValueWatcher() != null) {
                            OEValues values = col.getmValueWatcher().getValue(col, value);
                            cValue.setAll(values);
                        }
                        if (col.getType() instanceof OEManyToOne) {
                            if (value instanceof JSONArray) {
                                JSONArray m2oRec = new JSONArray(value.toString());
                                value = m2oRec.get(0);
                                if ((Integer) value != 0) {
                                    OEManyToOne m2o = (OEManyToOne) col.getType();
                                    OEDatabase db = (OEDatabase) m2o.getDBHelper();
                                    mRelRecord.add(db, value);
                                } else {
                                    value = false;
                                }
                            }
                        } else if (col.getType() instanceof OEManyToMany) {
                            if (value instanceof JSONArray) {
                                JSONArray m2mRec = new JSONArray(value.toString());
                                List<Integer> ids = getIdsList(m2mRec);
                                OEM2MIds mIds = new OEM2MIds(Operation.REPLACE, ids);
                                value = mIds;
                                OEManyToMany m2m = (OEManyToMany) col.getType();
                                OEDatabase db = (OEDatabase) m2m.getDBHelper();
                                mRelRecord.add(db, ids);
                            }
                        }
                        //处理one to many字段
                        else if (col.getType() instanceof OEOneToMany) {
                            if (value instanceof JSONArray) {
                                JSONArray o2mRec = new JSONArray(value.toString());
                                List<Integer> ids = getIdsList(o2mRec);
                                OEO2MIds mIds = new OEO2MIds(OEO2MIds.Operation.REPLACE, ids);
                                value = mIds;
                                OEOneToMany o2m = (OEOneToMany) col.getType();
                                OEDatabase db = (OEDatabase) o2m.getDBHelper();
                                mRelRecord.add(db, ids);
                                continue;
                            }
                        }
                        //处理外联的integer字段
                        else if (value instanceof JSONArray) {
                            JSONArray m2oRec = new JSONArray(value.toString());
                            value = m2oRec.get(0);
                        }
                        cValue.put(key, value);
                    }
                }
                mValues.add(cValue);
            }
        } catch (Exception e) {
            Log.d(TAG, "OEFieldsHelper->addAll(JSONArray records)");
            e.printStackTrace();
        }
    }

    private List<Integer> getIdsList(JSONArray array) {
        Log.d(TAG, "OEFieldsHelper->getIdsList()");
        List<Integer> ids = new ArrayList<Integer>();
        try {
            int length = array.length();
            if (length > 50) {
                Log.i(TAG,
                        "Many2Many or One2Many records more than 50... - Limiting to 50 records only");
                length = 50;
            }
            for (int i = 0; i < length; i++) {
                if (array.get(i) instanceof JSONArray)
                    ids.add(array.getJSONArray(i).getInt(0));
                else if (array.get(i) instanceof JSONObject) {
                    JSONObject rec = (JSONObject) array.get(i);
                    if (rec.has("id"))
                        ids.add(rec.getInt("id"));
                } else
                    ids.add(array.getInt(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ids;
    }

    public void addAll(String[] fields) {
        try {
            for (int i = 0; i < fields.length; i++) {
                mFields.accumulate("fields", fields[i]);
            }
            if (fields.length == 1) {
                mFields.accumulate("fields", fields[0]);
            }
        } catch (Exception e) {
        }
    }

    public void addAll(List<OEColumn> cols) {
        try {
            for (OEColumn col : cols) {
                if (col.canSync()) {
                    mFields.accumulate("fields", col.getName());
                }
            }
            if (cols.size() == 1) {
                mFields.accumulate("fields", cols.get(0));
            }
        } catch (Exception e) {
        }
    }

    public void addManyToOneId(Object model, int id) {

    }

    public JSONObject get() {
        return mFields;
    }

    public List<OEValues> getValues() {
        return mValues;
    }

    public List<OERelationData> getRelationData() {
        return mRelRecord.getAll();
    }

    class OERelRecord {
        private HashMap<String, Object> _models = new HashMap<String, Object>();
        private HashMap<String, List<Object>> _model_ids = new HashMap<String, List<Object>>();

        @SuppressWarnings("unchecked")
        public void add(OEDatabase db, Object ids) {
            if (!_models.containsKey(db.getModelName())) {
                _models.put(db.getModelName(), db);
            }
            List<Object> _ids = new ArrayList<Object>();
            if (ids instanceof List) {
                _ids = (List<Object>) ids;
            }
            if (ids instanceof Integer) {
                _ids.add(ids);
            }
            if (_model_ids.containsKey(db.getModelName())) {
                if (!_model_ids.containsValue(_ids))
                    _model_ids.get(db.getModelName()).addAll(_ids);
            } else {
                _model_ids.put(db.getModelName(), _ids);
            }
        }

        public List<OERelationData> getAll() {
            List<OERelationData> datas = new ArrayList<OEFieldsHelper.OERelationData>();
            Set<String> keys = _models.keySet();
            for (String key : keys) {
                OEDatabase db = (OEDatabase) _models.get(key);
                datas.add(new OERelationData(db, _model_ids.get(key)));
            }
            return datas;
        }
    }

    public class OERelationData {
        OEDatabase db;
        List<Object> ids;

        public OERelationData(OEDatabase db, List<Object> ids) {
            super();
            this.db = db;
            this.ids = ids;
        }

        public OEDatabase getDb() {
            return db;
        }

        public void setDb(OEDatabase db) {
            this.db = db;
        }

        public List<Object> getIds() {
            return ids;
        }

        public void setIds(List<Object> ids) {
            this.ids = ids;
        }

    }

    public interface ValueWatcher {
        public OEValues getValue(OEColumn col, Object value);
    }
}
