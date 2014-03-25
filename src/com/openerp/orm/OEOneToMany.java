package com.openerp.orm;

public class OEOneToMany {
	OEDBHelper mDb = null;

	public OEOneToMany(OEDBHelper db) {
		mDb = db;
	}

	public OEDBHelper getDBHelper() {
		return mDb;
	}
}
