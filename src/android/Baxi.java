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

// import eu.nets.baxi.android.testgui.R;
import eu.nets.baxi.client.BaxiCtrl;
import eu.nets.baxi.ef.BaxiEF;
import eu.nets.baxi.pcl.PCLDevice;
import eu.nets.baxi.pcl.PCLReader;

import eu.nets.baxi.client.TransferAmountArgs;

import android.widget.Toast;

// import eu.nets.baxi.testgui.listener.BaxiEventListener;

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
            this.openBaxi(callbackContext);
            return true;
        }

        return false;
    }

    private void transferAmount(JSONObject params, final CallbackContext callbackContext) {
      this.baxiEventListener.purchaseCallback = callbackContext;
      android.util.Log.i("debug", "=====================transferAmount============================");

        //     // unpack json
        //     // call transferAmount function
        //     // add event listener
        //     callbackContext.success(message);
        // } else {
        //     callbackContext.error("Expected one non-empty string argument.");
        // }
        String operID = "0000";
        String myOperation = "Purchase";
        String amount = "10";
        String amount2 = "";
        String amount3 = "";
        String type3 = "";
        String articleDetails = "";
        String authCode = "";
        String hostData = "";
        String paymentConditionCode = "";
        String optionalData = "";

        final TransferAmountArgs args = new TransferAmountArgs();
        // TODO: Take this value from operID login
        args.setOperID(operID);

        try{
            args.setAmount1(Integer.parseInt(amount));
        }catch(NumberFormatException e){
            args.setAmount1(0);
        }

        // int type1 = 0;
        // if(myOperation.equalsIgnoreCase("Purchase")){
        //     type1 = 0x30;
        // }else if(myOperation.equalsIgnoreCase("Return of Goods")){
        //     type1 = 0x31;
        // } else if(myOperation.equalsIgnoreCase("Reversal")){
        //     type1 = 0x32;
        // }else if(myOperation.equalsIgnoreCase("Cashback")){
        //     type1 = 0x33;
        // }else if(myOperation.equalsIgnoreCase("Authorisation")){
        //     type1 = 0x34;
        // }else if(myOperation.equalsIgnoreCase("Balance")){
        //     type1 = 0x36;
        // }else if(myOperation.equalsIgnoreCase("Deposit")){
        //     type1 = 0x38;
        // }else if(myOperation.equalsIgnoreCase("Cash Withdrawal")){
        //     type1 = 0x39;
        // }else if(myOperation.equalsIgnoreCase("Force Offline")){
        //     type1 = 0x40;
        // }else if(myOperation.equalsIgnoreCase("Incr PRE Auth")){
        //     type1 = 0x41;
        // }else if(myOperation.equalsIgnoreCase("Reversal PRE Auth")){
        //     type1 = 0x42;
        // }else if(myOperation.equalsIgnoreCase("SaleComp PRE Auth")){
        //     type1 = 0x43;
        // }
        int type1 = 0x40;
        args.setType1(type1);

        if (amount2.isEmpty()){
        	args.setAmount2(0);
        }else{
        	args.setAmount2(Integer.parseInt(amount2));
        }
        args.setType2(0x30);
        if (amount3.isEmpty()){
        	args.setAmount3(0);
        }else{
        	args.setAmount3(Integer.parseInt(amount3));
        }
        if(type3.equalsIgnoreCase("32 VAT/Moms")){
        	args.setType3(0x32);
        }else{
        	args.setType3(0x30);
        }

        args.setArticleDetails(articleDetails);
        args.setAuthCode(authCode);
        args.setHostData(hostData);
        args.setPaymentConditionCode(paymentConditionCode);
        args.setOptionalData(optionalData);

        try{
          cordova.getThreadPool().execute(new Runnable() {
              @Override
              public void run() {
                android.util.Log.i("debug", "=====================firing transferAmount============================");

                if (Baxi.BAXI.transferAmount(args) != 1){
                  android.util.Log.i("debug", "transferAmount ==> e: Transfer Amount failed");
                  callbackContext.error("Transfer Amount failed");
              		return;
              	} else {
                  // callbackContext.success("transfer success");
                }

              }
          });


        }catch(Exception e){
          android.util.Log.i("debug", "transferAmount ==> e: " + e.getMessage());
          callbackContext.error("Exception in transferamont: " + e.getMessage());

        	// Toast.makeText(v.getContext(), "Exception in transferamont: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }


        // if (!Baxi.BAXI.getCardInfoAll()) {
        	//Reset this on every transaction,
        	//except for when we have CardInfoAll=1 where we want to keep the data collected from the card
        	// ActivityViewMessages.clearAllMessages();
        // }
        // GOTO: Result activity
        // Intent intent = new Intent(v.getContext(), ActivityViewMessages.class);
		    // startActivityForResult(intent, 0);
    }


    private void openBaxi(CallbackContext callbackContext) {
      android.util.Log.i("debug", "=====================openBaxi============================");
      this.initBaxi();
      this.openBaxiConnection(null, callbackContext);
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
      android.util.Log.i("debug", "openBaxiConnection ==>");
      if (Baxi.BAXI.isOpen()){
        Toast.makeText(webView.getContext(), "Baxi is already open", Toast.LENGTH_LONG).show();
        return;
      }

      // smells like a duplication
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
        // do not support it

        // try{
        //   if (BAXI.open() != 1){
        //     raiseMethodRejectToast(ctx, "Failed to open Baxi.");
        //   }else{
        //     Intent intent = new Intent(ctx, ActivityViewMessages.class);
        //     intent.putExtra("gotOpenLM", false);
        //     ctx.startActivity(intent);
        //   }
        // }catch(Exception e){
        //   Toast.makeText(ctx, "Exception in open (ip): " + e.getMessage(), Toast.LENGTH_LONG).show();
        // }
      }
    }

    private void enableBluetooth(CallbackContext callbackContext){
      android.util.Log.i("debug", "enableBluetooth ==>");
      BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
      if(bluetoothAdapter != null){
        boolean isEnabled = bluetoothAdapter.isEnabled();
        if (!isEnabled) {
          callbackContext.error("enableBluetooth not enabled");
          // alert
          // Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
          // startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);
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
        if (devices != null){
          if (devices.size() == 1 && !alwaysBTDevices){
            //Open this if there is only one paired
            device = devices.get(0).getDevice();
          }else{
            // do not support for now, alert
            //Open the selection dialog if there are multiple paired
            // context.startActivity(new Intent(context, ActivityDiscoverBTDevices.class));
            return;
          }
        }
      }

      if (device == null){
        Toast.makeText(webView.getContext(), "No Paired Device available", Toast.LENGTH_LONG).show();
        return;
      }else{
        Toast.makeText(webView.getContext(), "Opening Terminal " + device.getName(), Toast.LENGTH_LONG).show();
      }

      android.util.Log.i("debug", "launchBTDevices address  ==>  " + device.getAddress());

      if(reader.setCurrentReader(device.getAddress())){
        try{
          if (BAXI.open() != 1){
            callbackContext.error("Failed to open Baxi.");
            // raiseMethodRejectToast(context, "Failed to open Baxi.");
          }else{
            // success return
            // callbackContext.success("Paired successfully");
            // Intent intent = new Intent(context, ActivityViewMessages.class);
            // intent.putExtra("gotOpenLM", false);
            // startActivity(intent);
          }
        }catch(Exception e){
          Toast.makeText(webView.getContext(), "Exception in open (launchBT): " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
      }
    }

}
