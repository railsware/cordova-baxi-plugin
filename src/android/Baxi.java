package baxi;

import java.util.List;

import android.content.Context;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import eu.nets.baxi.client.BaxiCtrl;
import eu.nets.baxi.ef.BaxiEF;
import eu.nets.baxi.pcl.PCLDevice;
import eu.nets.baxi.pcl.PCLReader;

import eu.nets.baxi.client.TransferAmountArgs;
import eu.nets.baxi.client.AdministrationArgs;

/**
 * This class echoes a string called from JavaScript.
 */
public class Baxi extends CordovaPlugin {

  private BluetoothDevice device;

	// This flag controls if ActivityDiscoverBTDevices should be launched or skipped (only 1 terminal paired)
	private boolean alwaysBTDevices;

	 public static BaxiCtrl BAXI;
   BaxiEventListener baxiEventListener;

   public void initialize(CordovaInterface cordova, CordovaWebView webView) {
       super.initialize(cordova, webView);
   }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
      android.util.Log.i("baxiPlugin", "=====================execute============================");
      android.util.Log.i("baxiPlugin", "executing: " + action);

        if (action.equals("transferAmount")) {
            JSONObject params = args.getJSONObject(0);
            this.transferAmount(params, callbackContext);
            return true;
        }

        if (action.equals("openBaxi")) {
          JSONObject params = args.getJSONObject(0);
            this.openBaxi(params, callbackContext);
            return true;
        }

        if(action.equals("isOpen")) {
          this.isOpen(callbackContext);
          return true;
        }

        if(action.equals("close")) {
          this.close(callbackContext);
          return true;
        }

        if(action.equals("administration")) {
          JSONObject params = args.getJSONObject(0);
          this.administration(params, callbackContext);
          return true;
        }

        return false;
    }

    private void administration(JSONObject params, final CallbackContext callbackContext) {
      
      this.baxiEventListener.administrationCallback = callbackContext;

      // make sure that baxi is opened, otherwise skip out on administration call
      if(this.isBaxiOpen() == false ) {
        callbackContext.error("Baxi not open error!");
        return;
      }

      String operID = "0000";
      String operation = "";
      String optionalData = "";

      // get operation from args
      try {
        operation = params.getString("operation");
      } catch (JSONException e) {
        
        // stop call, as missing admCode!
        android.util.Log.i("debug", "Administration: operation argument is missing error");
        callbackContext.error("Missing operation argument error");
        return;
      }

      // get optionalData from args
      try {
        optionalData = params.getString("optionalData");
      } catch (JSONException e) {
      }

      final AdministrationArgs args = new AdministrationArgs();

      args.OperID = operID;
      args.OptionalData = optionalData;

      if(operation.equalsIgnoreCase("Reconciliation")) {
          args.AdmCode = 0x3130;
      } else if(operation.equalsIgnoreCase("CLEAR")) {
          args.AdmCode = 0x3131;
      } else if(operation.equalsIgnoreCase("CANCEL")) {
          args.AdmCode = 0x3132;
      } else if(operation.equalsIgnoreCase("WRONG")) {
          args.AdmCode = 0x3133;
      } else if(operation.equalsIgnoreCase("ANNUL")) {
          args.AdmCode = 0x3134;
      } else if(operation.equalsIgnoreCase("Balance")) {
          args.AdmCode = 0x3135;
      } else if(operation.equalsIgnoreCase("X-Report")) {
          args.AdmCode = 0x3136;
      } else if(operation.equalsIgnoreCase("Z-Report")) {
          args.AdmCode = 0x3137;
      } else if(operation.equalsIgnoreCase("Send Offline")) {
          args.AdmCode = 0x3138;
      } else if(operation.equalsIgnoreCase("Turnover Report")) {
          args.AdmCode = 0x3139;
      } else if(operation.equalsIgnoreCase("Print EOT")) {
          args.AdmCode = 0x313A;
      } else if(operation.equalsIgnoreCase("Finished")) {
          args.AdmCode = 0x313B;
      } else if(operation.equalsIgnoreCase("Last Financial Receipt")) {
          args.AdmCode = 0x313C;
      } else if(operation.equalsIgnoreCase("Last Financial Result")) {
          args.AdmCode = 0x313D;
      } else if(operation.equalsIgnoreCase("Software Update")) {
          args.AdmCode = 0x313E;
      } else if(operation.equalsIgnoreCase("Dataset Download")) {
          args.AdmCode = 0x313F;
      }

      try{
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
              if (Baxi.BAXI.administration(args) != 1) {

                callbackContext.error("Unknown error");
                return;
              } else {
                // do nothing, eventlistener will "finish" transaction if possible
              }
            }
          });

        } catch(Exception e){
          callbackContext.error("Exception in administration: " + e.getMessage());
        }

    }

    private boolean isBaxiOpen() {
      if(Baxi.BAXI == null) {
        android.util.Log.i("baxiPlugin", "Baxi not initialised -> Returning false");
        return false;
      }

      if(Baxi.BAXI.isOpen() == true) {
        return true;
      } else {
        return false;
      }
    }

    private void transferAmount(JSONObject params, final CallbackContext callbackContext) {
        this.baxiEventListener.purchaseCallback = callbackContext;

        int amount;
        int amountVat;
        String operation;

        try {
          amount = params.getInt("amount");
        } catch (JSONException e) {
          amount = 0;
        }

        try {
          amountVat = params.getInt("amountVat");
        } catch (JSONException e) {
          amountVat = 0;
        }

        try {
          operation = params.getString("operation");
        } catch (JSONException e) {
          operation = "purchase";
        }

        String operID = "0000";
        String amount2 = "";
        String type3 = "";
        String articleDetails = "";
        String authCode = "";
        String hostData = "";
        String paymentConditionCode = "";
        String optionalData = "";

        final TransferAmountArgs args = new TransferAmountArgs();
        args.setOperID(operID);

        try{
            args.setAmount1(amount);
        }catch(NumberFormatException e){
            args.setAmount1(0);
        }

        int type1 = 0;
        if(operation.equalsIgnoreCase("Purchase")){
            type1 = 0x30;
        }else if(operation.equalsIgnoreCase("Return of Goods")){
            type1 = 0x31;
        } else if(operation.equalsIgnoreCase("Reversal")){
            type1 = 0x32;
        }else if(operation.equalsIgnoreCase("Cashback")){
            type1 = 0x33;
        }else if(operation.equalsIgnoreCase("Authorisation")){
            type1 = 0x34;
        }else if(operation.equalsIgnoreCase("Balance")){
            type1 = 0x36;
        }else if(operation.equalsIgnoreCase("Deposit")){
            type1 = 0x38;
        }else if(operation.equalsIgnoreCase("Cash Withdrawal")){
            type1 = 0x39;
        }else if(operation.equalsIgnoreCase("Force Offline")){
            type1 = 0x40;
        }else if(operation.equalsIgnoreCase("Incr PRE Auth")){
            type1 = 0x41;
        }else if(operation.equalsIgnoreCase("Reversal PRE Auth")){
            type1 = 0x42;
        }else if(operation.equalsIgnoreCase("SaleComp PRE Auth")){
            type1 = 0x43;
        }
        
        args.setType1(type1);

        android.util.Log.i("debug", "TransferAmountArgs->Type1: " + type1);

        args.setType1(type1);

        // if cashback, amount must be put into amount2
        if(operation.equalsIgnoreCase("cashback")) {
          args.setAmount2(amount);
        } else {

          if (amount2.isEmpty()){
            args.setAmount2(0);
          }else{
            args.setAmount2(Integer.parseInt(amount2));
          }
        }

        args.setType2(0x30);
        if (amountVat == 0){
        	args.setAmount3(0);
          args.setType3(0x30);
        }else{
        	args.setAmount3(amountVat);
          args.setType3(0x32);
        }

        try{
          cordova.getThreadPool().execute(new Runnable() {
              @Override
              public void run() {
                if (Baxi.BAXI.transferAmount(args) != 1){

                  callbackContext.error("Unknown error");
              		return;
              	} else {
                  // do nothing, eventlistener will "finish" transaction if possible
                }

              }
          });

        }catch(Exception e){
          callbackContext.error("Exception in transferamont: " + e.getMessage());
        }

    }

    private void isOpen(CallbackContext callbackContext) {

      android.util.Log.i("baxiPlugin", "isOpen called");

      if(isBaxiOpen() == true) {
        callbackContext.success("");
      } else {
        callbackContext.error("");
      }

    }

    private void close(CallbackContext callbackContext) {
      Baxi.BAXI.close();
      callbackContext.success("Baxi is closed");
    }

    private void openBaxi(JSONObject params, CallbackContext callbackContext) {
      this.initBaxi();
      this.configBaxi(params);
      this.openBaxiConnection(null, callbackContext);
    }

    private void configBaxi(JSONObject params) {
      try {
        Baxi.BAXI.setHostIpAddress(params.getString("HostIpAddress"));
      } catch (JSONException e) {
        android.util.Log.i("baxiPlugin", "error while set HostIpAddress");
      }

      try {
        Baxi.BAXI.setHostPort(params.getInt("HostPort"));
      } catch (JSONException e) {
        android.util.Log.i("baxiPlugin", "error while set HostPort");
      }

    }

    protected void initBaxi() {
      this.baxiEventListener = new BaxiEventListener();
  		BAXI = new BaxiCtrl(webView.getContext());
  		if (BAXI.getCardInfoAll()){
  			BAXI = new BaxiEF(webView.getContext());
  			((BaxiEF) BAXI).addBaxiEFListener(baxiEventListener);
  		} else {
  			BAXI.addBaxiCtrlEventListener(baxiEventListener);
  		}
  	}

    public void openBaxiConnection(BluetoothDevice bDevice, CallbackContext callbackContext){
      this.baxiEventListener.openCallback = callbackContext;

      if (Baxi.BAXI.isOpen()){
        callbackContext.success("Baxi is already open");
        return;
      }

      //Reload the class IF the setting has changed since first creation (see onCreate)
      //BaxiEF is a descendant with additional features
      if (BAXI.getCardInfoAll() && !(BAXI instanceof BaxiEF)){
        BAXI = new BaxiEF(webView.getContext());
        ((BaxiEF) BAXI).addBaxiEFListener(baxiEventListener);
      } else if (!BAXI.getCardInfoAll() && BAXI instanceof BaxiEF){
        BAXI = new BaxiCtrl(webView.getContext());
        BAXI.addBaxiCtrlEventListener(baxiEventListener);
      }

      if(Baxi.BAXI.getSerialDriver().equalsIgnoreCase("ingenico")){
        device = bDevice;
        alwaysBTDevices = false;
        enableBluetooth(callbackContext);
      } else if(Baxi.BAXI.getSerialDriver().equalsIgnoreCase("ip")){
        callbackContext.error("'ip' SerialDriver is not supported ");
      } 
    }

    private void enableBluetooth(CallbackContext callbackContext){
      android.util.Log.i("debug", "enableBluetooth ==>");
      BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
      if(bluetoothAdapter != null){
        boolean isEnabled = bluetoothAdapter.isEnabled();
        if (!isEnabled) {
          callbackContext.error("Bluetooth not enabled");
        }else{
          launchBTDevices(callbackContext);
        }
      }
    }

    private void launchBTDevices(CallbackContext callbackContext){
      android.util.Log.i("debug", "launchBTDevices ==>");
      PCLReader reader = new PCLReader(webView.getContext());
      if (device == null){
        List<PCLDevice> devices = reader.getPairedReaders();
        if (devices != null) {
          if(devices.size() == 0) {
            callbackContext.error("No paired Nets terminal found");
            return;
          } else if (devices.size() == 1) {
            //Open this if there is only one paired
            device = devices.get(0).getDevice();
          } else {
            callbackContext.error("Please disconnect all bluetooth devices except Nets terminal");
            return;
          }
        } else {
          callbackContext.error("Error getting connected Bluetooth devices");
          return;
        }
      }

      if (device == null) {
        callbackContext.error("No bluetooth device found!");
        return;
      }

      if(reader.setCurrentReader(device.getAddress())){
        try{
          if (BAXI.open() != 1){
            callbackContext.error("Failed to open Baxi.");
          }else{
            // success handled in listener
          }
        }catch(Exception e){
          callbackContext.error("Exception in open (launchBT): " + e.getMessage());
        }
      }
    }
}
