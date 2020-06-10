package kr.co.jahwa.plugin.rfid.btprotocol;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.flutter.plugin.common.EventChannel;

import co.kr.shark.btprotocol.BTConsts;
import co.kr.shark.btprotocol.BTReaderManager;


/** BtprotocolPlugin */
public class BtprotocolPlugin implements FlutterPlugin,  MethodCallHandler, EventChannel.StreamHandler {
  private static final String TAG = BtprotocolPlugin.class.getSimpleName();
  BTReaderManager mReader;
  private static Context context;

  private BluetoothAdapter mBluetoothAdapter;
  private String mBTAddress = null;
  private boolean mConnected = false;
  private String mStatus = null;
  private ArrayList<DataInfo> mItems;
  private DataAdapter mAdapter;

  private int RFIDPower = 0;

  //private ActivityPluginBinding activityBinding;

  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  private EventChannel stateChannel;

  private EventChannel.EventSink eventSink=null;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "btprotocol/method");
    channel.setMethodCallHandler(this);

    stateChannel = new EventChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "btprotocol/state");
    stateChannel.setStreamHandler(this);


    context = flutterPluginBinding.getApplicationContext();


    mItems = new ArrayList<DataInfo>();

    //mAdapter = new DataAdapter(this, mItems);

  }

  // This static function is optional and equivalent to onAttachedToEngine. It supports the old
  // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
  // plugin registration via this function while apps migrate to use the new Android APIs
  // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
  //
  // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
  // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
  // depending on the user's project. onAttachedToEngine or registerWith must both be defined
  // in the same class.
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "btprotocol/method");
    channel.setMethodCallHandler(new BtprotocolPlugin());

    //final EventChannel dataChannel = new EventChannel(registrar.messager(), "btprotocol/data");
    //dataChannel.setStreamHandler(new BtprotocolPlugin());

    context=registrar.activity().getApplication();

    if(context == null){
      Log.d(TAG, "registerWith context null");
    }else{
      Log.d(TAG, "registerWith context not null");
    }

  }



  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  @Override
  public void onListen(Object o, EventChannel.EventSink eventSink) {
    this.eventSink = eventSink;
    // Numbers every second+1
//    handler = new Handler(message -> {
//      // Then send the number to Flutter
//      eventSink.success(++count);
//      handler.sendEmptyMessageDelayed(0, 1000);
//      return false;
//    });
//    handler.sendEmptyMessage(0);

  }

  @Override
  public void onCancel(Object o) {
    this.eventSink = null;
//    handler.removeMessages(0);
//    handler = null;
//    count = 0;
  }

  private void OpenBluetooth(){
    if(mReader == null){
      try{
        if (context != null && mHandler != null) {
          mReader = BTReaderManager.getReader(context, mHandler);
          mReader.BT_Open();
        }else{
          if(context == null){
            Log.d(TAG, "context null");
          }
          if(mHandler == null){
            Log.d(TAG, "mHandler is null");
          }
        }
      }catch (Exception ex){
        Log.d(TAG, ex.getMessage());
      }
    }else{
      Log.d(TAG, "mReader Not null");
    }
  }

  /// Helper function to check is device connected
  static private boolean checkIsDeviceConnected(BluetoothDevice device) {
    try {
      java.lang.reflect.Method method;
      method = device.getClass().getMethod("isConnected");
      boolean value = (Boolean) method.invoke(device);
      return value;
    }
    catch (Exception ex) {
      return false;
    }
  }

  private  boolean isConnected(){
    return mConnected;
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
  private List<Map<String, Object>> getListPairedDevice(){
    Log.d(TAG, "getListPaireDevcie");

    if(mReader == null){
      OpenBluetooth();
    }

    if(mReader != null){
      Set<BluetoothDevice> pairedDevices = mReader.BT_GetPairedDevices();
      if (pairedDevices.size() > 0) {
        Log.d(TAG,"pairedDevices > 0");
        List<Map<String, Object>> list = new ArrayList<>();

        for (BluetoothDevice device : pairedDevices) {
          Map<String, Object> entry = new HashMap<>();
          entry.put("address", device.getAddress());
          entry.put("name", device.getName());
          entry.put("type", device.getType());
          entry.put("isConnected", checkIsDeviceConnected(device));
          entry.put("bondState", BluetoothDevice.BOND_BONDED);
          list.add(entry);
          //ScanInfo scanResult = new ScanInfo(device.getName(), device.getAddress());
          //mPairResults.add(scanResult);
          //mPairResultsAdapter.notifyDataSetChanged();
        }

        return list;
      } else {
        Log.d(TAG,"pairedDevices ><= 0");
        return null;
        //mPairResultsAdapter.notifyDataSetChanged();
      }
    }
    return null;
  }

  private List<Map<String, Object>> getListTag(){
    //Log.d(TAG, "getListTag");
    List<Map<String, Object>> list = new ArrayList<>();
    for(DataInfo data: mItems){
      Map<String, Object> entry = new HashMap<>();
      entry.put("type", data.type);
      entry.put("tagdata", data.tagdata);
      list.add(entry);
      //list.add(data.type+""+data.tagdata);
    }
    return list;
  }

  private void stopScan(){
    if(mReader != null){
      mReader.BT_StopScan();
    }
  }

  private int connectDevice(String address){
    if(mReader == null){
      OpenBluetooth();
    }
    mBTAddress = address;

    int rtn = mReader.BT_Connect(address);
    if(rtn == 0){
      //RFID Power Init
      mReader.BT_Reader_GetPower();
    }
    return rtn;
  }

  private int disconnectDevice(){
    if(mReader != null){
      return mReader.BT_Disconnect();
    }
    mBTAddress = null;
    return 0;
  }

  private int barcode_StartDecode(){
    return mReader.BT_Barcode_StartDecode();
  }

  private int clearData(){
    Log.d(TAG, "clearData()");
    if(mReader != null){
      mItems.clear();
      //mAdapter.notifyDataSetChanged();

      Log.d(TAG, "BT_Shark_ClearData");
      return mReader.BT_Shark_ClearData();
    }
    return -99;
  }

  private void RFIDCmdMsg(String data){
    if(data.indexOf("GpV")>-1){
      RFIDPower = Integer.parseInt( data.replace("GpV",""));
    }
  }


  public Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg){
      Log.d(TAG, "mainmHandler what = " + msg.what);
      Log.d(TAG, "mainmHandler arg1 = " + msg.arg1);
      Log.d(TAG, "mainmHandler arg2 = " + msg.arg2);
      Log.d(TAG, "mainmHandler obj = " + msg.obj);

      int nState = msg.arg1;
      String data = (String) msg.obj;
      DataInfo mResult;

      switch (msg.what){
        case BTConsts.Msg.MESSAGE_CONNECTION:
          switch (nState) {
            case BTConsts.ConnectionState.STATE_NONE :
              mStatus = "none";
              break;
            case BTConsts.ConnectionState.STATE_CONNECTING :
              mStatus = "connecting";
              break;
            case BTConsts.ConnectionState.STATE_CONNECTED :
              mStatus = "connected";
              break;
            case BTConsts.ConnectionState.STATE_BT_BOND_STATE_CHAGNED:
              if (mConnected) {
                Log.d(TAG, "STATE_BT_BOND_STATE_CHAGNED mConnected = " + mConnected);
                mReader.BT_Connect(mBTAddress);
                mConnected = false;
              }
              break;
            case BTConsts.ConnectionState.STATE_BT_DISCOVERY_FINISHED:
              mConnected = true;
              break;
          }
          break;
        case BTConsts.Msg.MESSAGE_RFID:
          // Rfid
          switch (nState) {
            case BTConsts.RFIDCmdMsg.INVENTORY:
            case BTConsts.RFIDCmdMsg.READ:
            case BTConsts.RFIDCmdMsg.TRIGGER_PRESSED:
              if (data.length() == 0) {
                break;
              }
              mResult = new DataInfo("R",data);
              mItems.add(mResult);
              break;
            case BTConsts.RFIDCmdMsg.WRITE:
            case BTConsts.RFIDCmdMsg.LOCK:
            case BTConsts.RFIDCmdMsg.READER_COMMAND:
            case BTConsts.RFIDCmdMsg.RESPONSE_CODE:
              //Toast.makeText(mContext, "result code = " + data, Toast.LENGTH_SHORT).show();
              Log.d(TAG, "RFIDCmdMsg result code = "+data);
              RFIDCmdMsg(data);
              break;
            case BTConsts.RFIDCmdMsg.NONE:
              break;
          }
          break;
        case BTConsts.Msg.MESSAGE_BARCODE:
          // Barcode
          switch (nState) {
            case BTConsts.BarcodeCmdMsg.STARTDECODE:
            case BTConsts.BarcodeCmdMsg.TRIGGER_PRESSED:
              if (data.length() == 0) {
                break;
              }
              mResult = new DataInfo("B",data);
              mItems.add(mResult);
              break;
            case BTConsts.BarcodeCmdMsg.BARCODE_COMMAND:
              //Toast.makeText(mContext, "result code = " + data, Toast.LENGTH_SHORT).show();
              break;
            case BTConsts.BarcodeCmdMsg.NONE:
              break;
          }
          break;
        case BTConsts.Msg.MESSAGE_SHARK:
          // Shark
          switch (nState) {
            case BTConsts.SharkCmdMsg.SHARK_COMMAND:
              //Toast.makeText(mContext, "result code = " + data, Toast.LENGTH_SHORT).show();
              Log.d(TAG, "SHARK_COMMAND result code = "+data);
              break;
            case BTConsts.SharkCmdMsg.NONE:
              break;
          }
          break;
      }
      //mAdapter.notifyDataSetChanged();
      if(eventSink != null){
        eventSink.success(0);
      }


    }

  };

  private class DataAdapter extends BaseAdapter{
    private Activity activity;
    private ArrayList<DataInfo> data;

    public DataAdapter(Activity a, ArrayList<DataInfo> object) {
      activity = a;
      data = object;
    }

    public int getCount() {
      return data.size();
    }

    public Object getItem(int position) {
      return data.get(position);
    }

    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      return null;
    }
  }

  private class DataInfo {
    public String type;
    public String tagdata;


    public DataInfo(String type, String tagdata) {
      this.type = type;
      this.tagdata = tagdata;
    }
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    switch (call.method) {
      case "getPlatformVersion":
        result.success("Android==> " + android.os.Build.VERSION.RELEASE);
        break;
      case "getListPairedDevice":
        result.success(getListPairedDevice());
        break;
      case "disconnectDevice":
        result.success(disconnectDevice());
        break;
      case "getListTag":
        result.success(getListTag());
        break;
      case "isConnected":
        result.success(isConnected());
        break;
      case "clearData":
        result.success(clearData());
        //clearData();
        break;
      case "barcode_StartDecode":
        result.success(barcode_StartDecode());
        break;
      case "connectDevice":
        if (!call.hasArgument("address")) {
          result.error("invalid_argument", "argument 'address' not found", null);
          break;
        }
        String address;
        try {
          address = call.argument("address");

          if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            throw new ClassCastException();
          }
        }
        catch (ClassCastException ex) {
          result.error("invalid_argument", "'address' argument is required to be string containing remote MAC address", null);
          break;
        }
        result.success(connectDevice(address));
        break;
      case "setPower":
        if (!call.hasArgument("power")) {
          result.error("invalid_argument", "argument 'power' not found", null);
          break;
        }
        int power;
        try {
          power = call.argument("power");
          result.success(mReader.BT_Reader_SetPower(power));
        }
        catch (ClassCastException ex) {
          result.error("invalid_argument", "'power' argument is required ", null);
          break;
        }
        break;
      case "initPower":
        mReader.BT_Reader_GetPower();
        break;
      case "getPower":
        //mReader.BT_Reader_GetPower();
        Log.d(TAG, "getPower Value : "+RFIDPower);
        result.success(RFIDPower);
        break;
      default:
        result.notImplemented();
        break;
    }
  }



}
