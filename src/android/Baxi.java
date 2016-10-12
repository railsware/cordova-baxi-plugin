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
      android.util.Log.i("info", "=====================execute============================");
      android.util.Log.i("info", action);

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

        return false;
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

        int type1;
        if (operation.equals("force_offline")) {
          type1 = 0x40;
        } else if (operation.equals("reversal")) {
          type1 = 0x32;
        } else {
          type1 = 0x30;
        }

        args.setType1(type1);

        if (amount2.isEmpty()){
        	args.setAmount2(0);
        }else{
        	args.setAmount2(Integer.parseInt(amount2));
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
                  callbackContext.error("Transfer Amount failed");
              		return;
              	} else {
                }

              }
          });

        }catch(Exception e){
          callbackContext.error("Exception in transferamont: " + e.getMessage());
        }

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
        android.util.Log.i("info", "error while set HostIpAddress");
      }

      try {
        Baxi.BAXI.setHostPort(params.getInt("HostPort"));
      } catch (JSONException e) {
        android.util.Log.i("info", "error while set HostPort");
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
      }else if(Baxi.BAXI.getSerialDriver().equalsIgnoreCase("ip")){
        callbackContext.error("'ip' SerialDriver is not supported ");
      }
    }

    private void enableBluetooth(CallbackContext callbackContext){
      android.util.Log.i("debug", "enableBluetooth ==>");
      BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
      if(bluetoothAdapter != null){
        boolean isEnabled = bluetoothAdapter.isEnabled();
        if (!isEnabled) {
          callbackContext.error("enableBluetooth not enabled");
        }else{
          launchBTDevices(callbackContext);
        }
      }
    }

    private void launchBTDevices(CallbackContext callbackContext){
      PCLReader reader = new PCLReader(webView.getContext());
      if (device == null){
        List<PCLDevice> devices = reader.getPairedReaders();
        if (devices != null){
          if (devices.size() == 1 && !alwaysBTDevices){
            //Open this if there is only one paired
            device = devices.get(0).getDevice();
          }else{
            callbackContext.error("Please disconned all bluetooth devices except Nets terminal");
            return;
          }
        }
      }

      if (device == null){
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
