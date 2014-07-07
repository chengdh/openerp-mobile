package com.openerp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.StrictMode;
import android.util.Base64;
import android.util.Log;
import java.io.*;
import java.util.Iterator;
import java.util.Scanner;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.json.*;

// Referenced classes of package openerp:
//            OEVersionException

public class OpenERP {

  // Referenced classes of package openerp:
  //            OpenERP

  public class OEVersion {

    public String getServer_serie() {
      return server_serie;
    }

    public void setServer_serie(String server_serie) {
      this.server_serie = server_serie;
    }

    public String getServer_version() {
      return server_version;
    }

    public void setServer_version(String server_version) {
      this.server_version = server_version;
    }

    public String getVersion_type() {
      return version_type;
    }

    public void setVersion_type(String version_type) {
      this.version_type = version_type;
    }

    public int getVersion_number() {
      return version_number;
    }

    public void setVersion_number(int version_number) {
      this.version_number = version_number;
    }

    public int getVersion_type_number() {
      return version_type_number;
    }

    public void setVersion_type_number(int version_type_number) {
      this.version_type_number = version_type_number;
    }

    String server_serie;
    String server_version;
    String version_type;
    int version_number;
    int version_type_number;
  }


  public static DefaultHttpClient getThreadSafeClient() {
    httpclient = new DefaultHttpClient();
    return httpclient;
  }

  public OpenERP(SharedPreferences pref) {
    debugMode = false;
    _base_url = null;
    user_context = null;
    sessionInfo = null;
    kwargs = null;
    _base_location = null;
    _port = null;
    _session_id = null;
    serverVersionInfo = null;
    mOEVersion = null;
    try {
      get_session_info();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    user_context = pref.getString("user_context", null);
    _base_url = stripURL(pref.getString("base_url", null));
  }

  public OpenERP(String base_url, long port)
    throws ClientProtocolException, JSONException, IOException, OEVersionException {
    debugMode = false;
    _base_url = null;
    user_context = null;
    sessionInfo = null;
    kwargs = null;
    _base_location = null;
    _port = null;
    _session_id = null;
    serverVersionInfo = null;
    mOEVersion = null;
    _port = String.valueOf(port);
    _base_url = (new StringBuilder(String.valueOf(stripURL(base_url)))).append(":").append(_port).toString();
    sessionInfo = get_session_info();
  }

  public OpenERP(String base_url)
    throws ClientProtocolException, JSONException, IOException, OEVersionException {
    debugMode = false;
    _base_url = null;
    user_context = null;
    sessionInfo = null;
    kwargs = null;
    _base_location = null;
    _port = null;
    _session_id = null;
    serverVersionInfo = null;
    mOEVersion = null;
    _base_url = stripURL(base_url);
    sessionInfo = get_session_info();
  }

  public OpenERP(String base_url, boolean isnetwork)
    throws ClientProtocolException, JSONException, IOException, OEVersionException {
    debugMode = false;
    _base_url = null;
    user_context = null;
    sessionInfo = null;
    kwargs = null;
    _base_location = null;
    _port = null;
    _session_id = null;
    serverVersionInfo = null;
    mOEVersion = null;
    _base_url = stripURL(base_url);
    if(isnetwork)
      sessionInfo = get_session_info();
  }

  public void debugMode(boolean on) {
    debugMode = on;
  }

  private synchronized JSONObject callHTTP(String RequestURL, String jsonString)
    throws ClientProtocolException, IOException, JSONException {
    if(android.os.Build.VERSION.SDK_INT > 9) {
      android.os.StrictMode.ThreadPolicy policy = (new android.os.StrictMode.ThreadPolicy.Builder()).permitAll().build();
      StrictMode.setThreadPolicy(policy);
    }
    HttpPost httppost = new HttpPost(RequestURL);
    StringEntity se = new StringEntity(jsonString);
    JSONObject obj = null;
    se.setContentEncoding(new BasicHeader("Content-Type", "application/json"));
    httppost.setEntity(se);
    HttpResponse httpresponse = null;
    httpresponse = httpclient.execute(httppost);
    if(httpresponse != null) {
      String a = "";
      try {
        InputStream in = httpresponse.getEntity().getContent();
        a = convertStreamToString(in);
        obj = new JSONObject(a);
        in.close();
        httpresponse.getEntity().consumeContent();
      }
      catch(JSONException jexe) {
        return null;
      }
    }
    if(debugMode) {
      Log.i("OPENERP_POST_URL", RequestURL);
      Log.i("OPENERP_POST", jsonString);
      Log.i("OPENERP_RESPONSE", obj.toString());
    }
    return obj;
  }

  public JSONArray getDatabaseList()
    throws JSONException, ClientProtocolException, IOException {
    String req_url = (new StringBuilder(String.valueOf(_base_url))).append("/web/database/get_list").toString();
    JSONObject obj = new JSONObject();
    JSONObject params = new JSONObject();
    if(is7_0Version)
      params.put("session_id", _session_id);
    params.put("context", new JSONObject());
    String jsonString = generate_json_request(params);
    obj = callHTTP(req_url, jsonString);
    return obj.getJSONArray("result");
  }

  private JSONObject get_session_info()
    throws JSONException, ClientProtocolException, IOException, OEVersionException {
    String req_url = (new StringBuilder(String.valueOf(_base_url))).append("/web/session/get_session_info").toString();
    JSONObject obj = null;
    if(isValidVersion()) {
      JSONObject params = new JSONObject();
      if(is7_0Version)
        params.put("session_id", "");
      String jsonString = generate_json_request(params);
      obj = callHTTP(req_url, jsonString);
      if(obj != null && obj.has("result"))
        _session_id = obj.getJSONObject("result").getString("session_id");
    } else {
      throw new OEVersionException("Server version is different from the application supported version.");
    }
    return obj;
  }

  private boolean isValidVersion()
    throws ClientProtocolException, JSONException, IOException {
    Log.d("openerp.OpenERP", "OpenERP->isValidVersion()");
    JSONObject version = serverVersion().getJSONObject("result");
    if(version.has("server_version_info")) {
      boolean flag = false;
      mOEVersion = new OEVersion();
      mOEVersion.setServer_version(version.getString("server_version"));
      mOEVersion.setServer_serie(version.getString("server_serie"));
      JSONArray version_info = version.getJSONArray("server_version_info");
      mOEVersion.setVersion_number(version_info.getInt(0));
      mOEVersion.setVersion_type(version_info.getString(3));
      mOEVersion.setVersion_type_number(version_info.getInt(4));
      if(version_info.getInt(0) >= 7) {
        Log.d("openerp.OpenERP", (new StringBuilder("isValidVersion() : ")).append(version_info.toString()).toString());
        int subVersion = 0;
        if(version_info.get(1) instanceof String)
          subVersion = Integer.parseInt(version_info.getString(1).split("\\~")[1]);
        else
          subVersion = version_info.getInt(1);
        if(version_info.getInt(0) == 7 && subVersion == 0)
          is7_0Version = true;
        flag = true;
      } else {
        flag = false;
      }
      return flag;
    } else {
      return false;
    }
  }

  public OEVersion getOEVersion() {
    if(mOEVersion == null)
      try {
        isValidVersion();
      }
    catch(Exception e) {
      e.printStackTrace();
    }
    return mOEVersion;
  }

  public JSONObject serverVersion()
    throws JSONException, ClientProtocolException, IOException {
    String url = (new StringBuilder(String.valueOf(_base_url))).append("/web/webclient/version_info").toString();
    JSONObject obj = new JSONObject("{\"jsonrpc\":\"2.0\",\"method\":\"call\",\"params\":{},\"id\":1}");
    if(serverVersionInfo == null)
      serverVersionInfo = callHTTP(url, obj.toString());
    return serverVersionInfo;
  }

  public static Object getResourceID() {
    Object id = String.valueOf(rID);
    rID++;
    if(is7_0Version)
      return (new StringBuilder("r")).append(id).toString();
    else
      return Integer.valueOf(Integer.parseInt(id.toString()));
  }

  public JSONObject authenticate(String Username, String password, String dbName)
    throws JSONException, ClientProtocolException, IOException {
    String req_url = (new StringBuilder(String.valueOf(_base_url))).append("/web/session/authenticate").toString();
    JSONObject obj = null;
    JSONObject params = new JSONObject();
    params.put("db", dbName);
    params.put("login", Username);
    params.put("password", password);
    if(is7_0Version)
      params.put("session_id", _session_id);
    params.put("context", new JSONObject());
    String jsonString = generate_json_request(params);
    obj = callHTTP(req_url, jsonString);
    user_context = obj.getJSONObject("result").getJSONObject("user_context").toString();
    return obj.getJSONObject("result");
  }

  public JSONObject search_count(String model, JSONArray args)
    throws ClientProtocolException, JSONException, IOException {
    return call_kw(model, "search_count", args);
  }

  public JSONObject search_read(String model, JSONObject fieldsAccumulates, JSONObject domainAccumulates, int offset, int limit, String sortField, String sortType)
    throws JSONException, ClientProtocolException, IOException {
    String req_url = (new StringBuilder(String.valueOf(_base_url))).append("/web/dataset/search_read").toString();
    JSONObject obj = null;
    JSONObject params = new JSONObject();
    params.put("model", model);
    if(fieldsAccumulates != null) {
      fieldsAccumulates.accumulate("fields", "id");
      params.put("fields", fieldsAccumulates.get("fields"));
    } else {
      params.put("fields", new JSONArray());
    }
    if(domainAccumulates != null)
      params.put("domain", domainAccumulates.get("domain"));
    else
      params.put("domain", new JSONArray());
    params.put("context", new JSONObject(user_context));
    params.put("offset", offset);
    params.put("limit", limit);
    if(sortField != null && sortType != null)
      params.put("sort", (new StringBuilder(String.valueOf(sortField))).append(" ").append(sortType).toString());
    else
      params.put("sort", "");
    if(is7_0Version)
      params.put("session_id", _session_id);
    String jsonString = generate_json_request(params);
    obj = callHTTP(req_url, jsonString);
    return obj.getJSONObject("result");
  }

  public JSONObject search_read(String model, JSONObject fieldsAccumulates, JSONObject domainAccumulates)
    throws JSONException, ClientProtocolException, IOException {
    return search_read(model, fieldsAccumulates, domainAccumulates, 0, 0, null, null);
  }

  public JSONObject search_read(String model, JSONObject fieldsAccumulates)
    throws JSONException, ClientProtocolException, IOException {
    return search_read(model, fieldsAccumulates, null, 0, 0, null, null);
  }

  private JSONObject createWriteParams(String modelName, String methodName, JSONObject args, Integer id)
    throws JSONException {
    JSONObject params = new JSONObject();
    params.put("model", modelName);
    params.put("method", methodName);
    JSONArray _args = null;
    if(id != null && args != null)
      _args = new JSONArray((new StringBuilder("[[")).append(String.valueOf(id)).append("], ").append(args.toString()).append("]").toString());
    else
      if(id == null && args != null)
        _args = new JSONArray((new StringBuilder("[")).append(args.toString()).append("]").toString());
      else
        if(id != null && args == null)
          _args = new JSONArray((new StringBuilder("[[")).append(String.valueOf(id)).append("]]").toString());
    params.put("args", _args);
    JSONObject kwargs = new JSONObject();
    kwargs.put("context", new JSONObject(user_context));
    params.put("kwargs", kwargs);
    if(is7_0Version)
      params.put("session_id", _session_id);
    params.put("context", new JSONObject(user_context));
    return params;
  }

  public JSONObject callMethod(String modelName, String methodName, JSONObject args, Integer id)
    throws JSONException, ClientProtocolException, IOException {
    String req_url = (new StringBuilder(String.valueOf(_base_url))).append("/web/dataset/call_kw/").append(modelName).append(":").append(methodName).toString();
    JSONObject params = new JSONObject();
    params = createWriteParams(modelName, methodName, args, id);
    String jsonString = generate_json_request(params);
    JSONObject response = callHTTP(req_url, jsonString);
    return response;
  }

  public JSONObject createNew(String modelName, JSONObject arguments)
    throws JSONException, ClientProtocolException, IOException {
    return callMethod(modelName, "create", arguments, null);
  }

  public boolean updateValues(String modelName, JSONObject arguments, Integer id)
    throws ClientProtocolException, JSONException, IOException {
    JSONObject response = null;
    response = callMethod(modelName, "write", arguments, id);
    return response.getBoolean("result");
  }
  //执行workflow
  public JSONObject exec_workflow(String modelName, Integer res_id, String signal)
        throws JSONException, ClientProtocolException, IOException {
        JSONObject response = null;
        String req_url = (new StringBuilder(String.valueOf(_base_url))).append("/web/dataset/exec_workflow").toString();
        JSONObject params = new JSONObject();
        params.put("model", modelName);
        params.put("id", res_id);
        params.put("signal", signal);
        JSONObject kwargs = null;
        if(is7_0Version)
            params.put("session_id", _session_id);
        params.put("context", new JSONObject(user_context));
        String jsonString = generate_json_request(params);
        Log.d("Openerp->exec_workflow","params = " + jsonString);
        response = callHTTP(req_url, jsonString);
        Log.d("Openerp->exec_workflow","response = " + response);
        return response;
  }

  public boolean unlink(String modelName, Integer id)
    throws ClientProtocolException, JSONException, IOException {
    JSONObject response = callMethod(modelName, "unlink", null, id);
    return response.getBoolean("result");
  }

  public File getImage(Context context, String model, String field, int id)
    throws IOException {
    JSONObject fields = new JSONObject();
    try {
      fields.accumulate("fields", field);
      String imagestring = null;
      JSONArray domainarr = new JSONArray((new StringBuilder("[[\"id\",\"=\",")).append(id).append("]]").toString());
      JSONObject domain = new JSONObject();
      domain.accumulate("domain", domainarr);
      JSONObject res = search_read(model, fields, domain, 0, 1, null, null);
      JSONArray result = res.getJSONArray("records");
      if(result.getJSONObject(0).getString("image") != "false") {
        imagestring = result.getJSONObject(0).getString(field);
        byte imageAsBytes[] = Base64.decode(imagestring.getBytes(), 5);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
        File imgFile = new File(context.getCacheDir(), (new StringBuilder("img")).append(String.valueOf(id)).toString());
        imgFile.createNewFile();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 0, bos);
        byte bitmapdata[] = bos.toByteArray();
        FileOutputStream fos = new FileOutputStream(imgFile);
        fos.write(bitmapdata);
        fos.flush();
        fos.close();
        return imgFile;
      }
    }
    catch(JSONException e) {
      e.printStackTrace();
      return null;
    }
    return null;
  }

  public Bitmap getBitmapImage(Context context, String model, String field, int id)
    throws IOException {
    JSONObject fields = new JSONObject();
    try {
      fields.accumulate("fields", field);
      String imagestring = null;
      JSONArray domainarr = new JSONArray((new StringBuilder("[[\"id\",\"=\",")).append(id).append("]]").toString());
      JSONObject domain = new JSONObject();
      domain.accumulate("domain", domainarr);
      JSONObject res = search_read(model, fields, domain, 0, 1, null, null);
      JSONArray result = res.getJSONArray("records");
      if(result.getJSONObject(0).getString("image") != "false") {
        imagestring = result.getJSONObject(0).getString(field);
        byte imageAsBytes[] = Base64.decode(imagestring.getBytes(), 5);
        return BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
      }
    }
    catch(JSONException e) {
      e.printStackTrace();
      return null;
    }
    return null;
  }

  private String generate_json_request(JSONObject params)
    throws JSONException {
    JSONObject postObj = new JSONObject();
    postObj.put("jsonrpc", "2.0");
    postObj.put("method", "call");
    postObj.put("params", params);
    postObj.put("id", getResourceID());
    return postObj.toString();
  }

  private String convertStreamToString(InputStream is) {
    Scanner s = (new Scanner(is)).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }

  public static boolean putSessionData(Context context, String key, String value) {
    pref = context.getApplicationContext().getSharedPreferences("OpenERP_Preferences", 1);
    android.content.SharedPreferences.Editor editor = pref.edit();
    editor.putString(key, value);
    editor.commit();
    return true;
  }

  public JSONObject call_kw(String modelName, String methodName, JSONArray args)
    throws JSONException, ClientProtocolException, IOException {
    JSONObject response = null;
    String req_url = (new StringBuilder(String.valueOf(_base_url))).append("/web/dataset/call_kw").toString();
    JSONObject params = new JSONObject();
    params.put("model", modelName);
    params.put("method", methodName);
    params.put("args", args);
    JSONObject kwargs = null;
    if(this.kwargs != null)
      kwargs = new JSONObject(this.kwargs);
    else
      kwargs = new JSONObject();
    params.put("kwargs", kwargs);
    if(is7_0Version)
      params.put("session_id", _session_id);
    params.put("context", new JSONObject(user_context));
    String jsonString = generate_json_request(params);
    response = callHTTP(req_url, jsonString);
    return response;
  }

  public boolean updateKWargs(JSONObject newValues)
    throws JSONException {
    JSONObject kwargs = null;
    if(newValues != null) {
      if(this.kwargs != null)
        kwargs = new JSONObject(this.kwargs);
      else
        kwargs = new JSONObject();
      String key;
      for(Iterator iter = newValues.keys(); iter.hasNext(); kwargs.put(key, newValues.get(key)))
        key = (String)iter.next();

      this.kwargs = kwargs.toString();
    } else {
      this.kwargs = (new JSONObject()).toString();
    }
    return true;
  }

  public JSONObject updateContext(JSONObject newValues)
    throws JSONException {
    JSONObject userContext = new JSONObject(user_context);
    String key;
    for(Iterator iter = newValues.keys(); iter.hasNext(); userContext.put(key, newValues.get(key)))
      key = (String)iter.next();

    return userContext;
  }

  public static String getSessionData(Context context, String key) {
    pref = context.getApplicationContext().getSharedPreferences("OpenERP_Preferences", 1);
    return pref.getString(key, null);
  }

  public static boolean generateSessions(Context context, OpenERP openerp, JSONObject response)
    throws JSONException {
    putSessionData(context, "username", response.getString("username"));
    putSessionData(context, "user_context", response.getJSONObject("user_context").toString());
    putSessionData(context, "db", response.getString("db"));
    putSessionData(context, "uid", response.getString("uid"));
    putSessionData(context, "session_id", response.getString("session_id"));
    putSessionData(context, "base_url", openerp.getServerURL());
    return true;
  }

  public String getServerURL() {
    return _base_url;
  }

  public String downloadUrl(String type, String model, int id, String method, int attachment_id) {
    String url = "";
    url = (new StringBuilder(String.valueOf(_base_url))).append("/").append(type).append("/download_attachment?model=").append(model).append("&id=").append(String.valueOf(id)).toString();
    url = (new StringBuilder(String.valueOf(url))).append("&method=").append(method).append("&attachment_id=").append(String.valueOf(attachment_id)).append("&session_id=").append(_session_id).toString();
    return url;
  }

  public static SharedPreferences getSessions(Context context) {
    return context.getApplicationContext().getSharedPreferences("OpenERP_Preferences", 1);
  }

  private String cleanServerURL(String url) {
    StringBuffer newURL = new StringBuffer();
    if(url.charAt(url.length() - 1) == '\\') {
      String subStr = url.substring(0, url.length() - 1);
      newURL.append(subStr);
    } else {
      newURL.append(url);
    }
    return newURL.toString();
  }

  public boolean isServerDatabaseExists(String databaseName) {
    boolean flag = false;
    try {
      JSONArray dbList = getDatabaseList();
      for(int i = 0; i < dbList.length(); i++)
        if(dbList.getString(i).equals(databaseName))
          flag = true;

    }
    catch(Exception exception) { }
    return flag;
  }

  public String stripURL(String url) {
    if(url.endsWith("/"))
      return url.substring(0, url.lastIndexOf("/"));
    else
      return url;
  }

  public static final String TAG = "openerp.OpenERP";
  public static DefaultHttpClient httpclient = new DefaultHttpClient();
  private boolean debugMode;
  public String _base_url;
  public static int rID = 0;
  protected String user_context;
  public final String DESC = "DESC";
  private JSONObject sessionInfo;
  public final String ASC = "ASC";
  private String kwargs;
  private static SharedPreferences pref;
  protected String _base_location;
  protected String _port;
  public String _session_id;
  public static boolean is7_0Version = false;
  public JSONObject serverVersionInfo;
  OEVersion mOEVersion;
}

