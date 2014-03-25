package com.openerp.orm;
import java.util.List;

//one to many record
public class OEO2MRecord {
	private OEColumn mCol = null;
	private String mValue = null;
	private int mId = 0;
	private OEDatabase mDatabase = null;

	public OEO2MRecord(OEDatabase oeDatabase, OEColumn col, int id) {
		mDatabase = oeDatabase;
		mCol = col;
		mId = id;
	}

	public List<OEDataRow> browseEach() {
		OEOneToMany o2m = (OEOneToMany) mCol.getType();
		return mDatabase.selectO2M(o2m.getDBHelper(), mDatabase.tableName()
				+ "_id = ?", new String[] { mId + "" });
	}

	public OEDataRow browseAt(int index) {
		List<OEDataRow> list = browseEach();
		if (list.size() == 0) {
			return null;
		}
		return list.get(index);
	}
}
