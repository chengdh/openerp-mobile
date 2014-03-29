package openerp;

import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class OEArguments {

  public OEArguments() {
    mArguments = new ArrayList();
  }

  public void add(Object object) {
    mArguments.add(object);
  }

  public void addNull() {
    mArguments.add(null);
  }

  public void add(List objects) {
    JSONArray ids = new JSONArray();
    try {
      ids = new JSONArray(objects.toString());
    }
    catch(Exception exception) { }
    mArguments.add(ids);
  }

  public List getObjects() {
    return mArguments;
  }

  public JSONArray getArray() {
    JSONArray arguments = new JSONArray();
    for(Iterator iterator = mArguments.iterator(); iterator.hasNext();) {
      Object obj = iterator.next();
      if(obj instanceof JSONObject)
        arguments.put(obj);
      else
        arguments.put(obj);
    }

    return arguments;
  }

  public JSONArray get() {
    JSONArray arguments = new JSONArray();
    for(Iterator iterator = mArguments.iterator(); iterator.hasNext();) {
      Object obj = iterator.next();
      JSONArray data = new JSONArray();
      data.put(obj);
      if(obj instanceof JSONObject)
        arguments.put(obj);
      else
        arguments.put(data);
    }

    return arguments;
  }

  List mArguments;
}

