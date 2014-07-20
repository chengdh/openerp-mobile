package com.openerp.support.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.openerp.util.drawer.DrawerItem;

import java.util.List;

public interface FragmentHelper {
    public Object databaseHelper(Context context);

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState);

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater);

    public List<DrawerItem> drawerMenus(Context context);
}