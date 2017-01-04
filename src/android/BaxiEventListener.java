package baxi;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Message;
import eu.nets.baxi.client.BarcodeReaderEventArgs;
import eu.nets.baxi.client.BaxiErrorEventArgs;
import eu.nets.baxi.client.DisplayTextEventArgs;
import eu.nets.baxi.client.JsonReceivedEventArgs;
import eu.nets.baxi.client.LastFinancialResultEventArgs;
import eu.nets.baxi.client.LocalModeEventArgs;
import eu.nets.baxi.client.PrintTextEventArgs;
import eu.nets.baxi.client.StdRspReceivedEventArgs;
import eu.nets.baxi.client.TLDReceivedEventArgs;
import eu.nets.baxi.client.TerminalReadyEventArgs;
import eu.nets.baxi.client.TransactionEventArgs;
import eu.nets.baxi.ef.BaxiEFEventListener;
import eu.nets.baxi.ef.CardEventArgs;
import eu.nets.baxi.ef.CardInfoAllArgs;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;

enum CurrentListener {
	DISPLAY,
	LM,
	PRINT,
	ERROR,
	CONNECTED,
	DISCONNECTED,
	TLD_RECEIVED,
	TERMINAL_READY,
	STD_RSP,
	BARCODE_READER,
	JSON_RECEIVED,
	LAST_FIN_RES,
	OPTIONAL_DATA,
	CARD_INFO_ALL,
	CARD;
}

//Normally BaxiCtrlEventListener will be used unless the BaxiEF is used in the ECR for additional functionality
public class BaxiEventListener implements BaxiEFEventListener {
    protected final String newLine = System.getProperty("line.separator");
    public CallbackContext openCallback;
    public CallbackContext purchaseCallback;
    public List<String> printTextMessages = new ArrayList<String>();
    public List<String> displayTextMessages = new ArrayList<String>();

    protected void handleMessage(String messageName, String message, CurrentListener listener){
        android.util.Log.i("debug", "=====================handleMessage "+ messageName+ "============================>");
        android.util.Log.i("debug", message);
        android.util.Log.i("debug", "================== end handleMessage "+ messageName+ "============================>");
    }

    @Override
    public void OnStdRspReceived(StdRspReceivedEventArgs args) {
		handleMessage("StdRspReceived", "", CurrentListener.STD_RSP);
	}

	@Override
	public void OnPrintText(PrintTextEventArgs args) {
		String printText = args.getPrintText();
		this.printTextMessages.add(printText);
		handleMessage("PrintText", printText, CurrentListener.PRINT);
	}

	@Override
	public void OnDisplayText(DisplayTextEventArgs args) {
		String displayText = args.getDisplayText();
        this.displayTextMessages.add(displayText);
		handleMessage("DisplayText", displayText, CurrentListener.DISPLAY);
	}

	public JSONObject packJSON(){
        JSONObject json = new JSONObject();

        try {
            JSONArray ja = new JSONArray();
            for (String message : this.printTextMessages) {
                ja.put(message);
            }
            json.put("printTextMessages", ja);
            this.printTextMessages = new ArrayList<String>();

        } catch (JSONException e) {
            android.util.Log.i("error", e.getMessage());
        }

        try {
            JSONArray ja = new JSONArray();
            for (String message : this.displayTextMessages) {
                ja.put(message);
            }
            json.put("displayTextMessages", ja);
            this.displayTextMessages = new ArrayList<String>();

        } catch (JSONException e) {
            android.util.Log.i("error", e.getMessage());
        }

        return json;
    }

	@Override
	public void OnLocalMode(LocalModeEventArgs args) {
		if (args.getResult() == 1) {
				this.openCallback.success("Opened");
		} else if (args.getResult() == 2) {
                try {
                    JSONObject json = this.packJSON();
                    json.put("result", args.getResult());
                    this.purchaseCallback.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, json));
                } catch (JSONException e) {
                    //some exception handler code.
                }
        } else if (args.getResult() == 0) {
			if (args.getResponseCode() == "Y") {
				try {
					JSONObject json = this.packJSON();
                    json.put("cardInfo", args.getTruncatedPan());
        	        this.purchaseCallback.sendPluginResult(new PluginResult(PluginResult.Status.OK, json));
				} catch (JSONException e) {
					//some exception handler code.
				}
			} else {
                this.purchaseCallback.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, this.packJSON()));
			}
		}
        handleMessage("Localmode ===========================> ", getLMText(args), CurrentListener.LM);
	}

	@Override
	public void OnTerminalReady(TerminalReadyEventArgs args) {
		handleMessage("TerminalReady", "", CurrentListener.TERMINAL_READY);
	}

	@Override
	public void OnJsonReceived(JsonReceivedEventArgs args) {
		handleMessage("JSONReceived", args.getJsonData(), CurrentListener.JSON_RECEIVED);
	}

	@Override
	public void OnTLDReceived(TLDReceivedEventArgs args) {
        String tldDataString;
        try {
            tldDataString = new String(args.TldData, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            tldDataString = "Encoding not supported";
        }

        handleMessage("TldReceived", tldDataString, CurrentListener.TLD_RECEIVED);
	}

	@Override
	public void OnLastFinancialResult(LastFinancialResultEventArgs args) {
        handleMessage("LastFinancialResult", getLMText(args), CurrentListener.LAST_FIN_RES);
	}

	private String getLMText(TransactionEventArgs args){
        String lmText = "";

        lmText += "  ResultData: " + args.getResultData() + newLine;
        lmText += "   Result: " + args.getResult() + newLine;
        lmText += "   AccumulatorUpdate: 0x" + String.format("%02x", args.getAccumulatorUpdate()) + newLine;
        lmText += "   IssuerId: " + String.format("%02d", args.getIssuerId()) + newLine;
        lmText += "   CardData: " + args.getTruncatedPan() + newLine;
        lmText += "   Timestamp: " + args.getTimestamp() + newLine;
        lmText += "   VerificationMethod: " + args.getVerificationMethod() + newLine;
        lmText += "   SessionNumber: " + args.getSessionNumber() + newLine;
        lmText += "   StanAuth: " + args.getStanAuth() + newLine;
        lmText += "   SequenceNumber: " + args.getSequenceNumber() + newLine;
        lmText += "   TotalAmount: " + args.getTotalAmount() + newLine;
        lmText += "   RejectionSource: " + args.getRejectionSource() + newLine;
        lmText += "   RejectionReason: " + args.getRejectionReason() + newLine;
        lmText += "   TipAmount: " + args.getTipAmount() + newLine;
        lmText += "   SurchargeAmount: " + args.getSurchargeAmount() + newLine;
        lmText += "   terminalID: " + args.getTerminalID() + newLine;
        lmText += "   acquirerMerchantID: " + args.getAcquirerMerchantID() + newLine;
        lmText += "   cardIssuerName: " + args.getCardIssuerName() + newLine;
        lmText += "   responseCode: " + args.getResponseCode() + newLine;
        lmText += "   TCC: " + args.getTCC() + newLine;
        lmText += "   AID: " + args.getAID() + newLine;
        lmText += "   TVR: " + args.getTVR() + newLine;
        lmText += "   TSI: " + args.getTSI() + newLine;
        lmText += "   ATC: " + args.getATC() + newLine;
        lmText += "   AED: " + args.getAED() + newLine;
        lmText += "   IAC: " + args.getIAC() + newLine;
        lmText += "   OrganisationNumber: " + args.getOrganisationNumber() + newLine;
        lmText += "   BankAgent : " + args.getBankAgent() + newLine;
        lmText += "   EncryptedPAN : " + args.getEncryptedPAN() + newLine;
        lmText += "   AccountType : " + args.getAccountType() + newLine;
        lmText += "   OptionalData : " + args.getOptionalData() + newLine;

        if (args.getOptionalData() != null && !args.getOptionalData().isEmpty()){
        	handleMessage("OptionalData", args.getOptionalData(), CurrentListener.OPTIONAL_DATA);
        }

        return lmText;
	}

	@Override
	public void OnBaxiError(BaxiErrorEventArgs args) {
		handleMessage("Error", args.getErrorCode() + " " + args.getErrorString(), CurrentListener.ERROR);
	}

	@Override
	public void OnConnected() {
		handleMessage("Baxi Connected", "", CurrentListener.CONNECTED);
	}

	@Override
	public void OnDisconnected() {
		handleMessage("Baxi Disconnected", "", CurrentListener.DISCONNECTED);
	}

	@Override
	public void OnBarcodeReader(BarcodeReaderEventArgs args) {
		handleMessage("BarcodeReader", args.getBarcodeText(), CurrentListener.BARCODE_READER);

	}

	@Override
	public void OnCardInfoAll(CardInfoAllArgs args) {
		String str = "";

		str += "  VAS: " + args.getVAS() + newLine;
		str += "  Customer id: " + args.getCustomerId() + newLine;
		str += "  Psp Command: " + args.getPspCommand() + newLine;
		str += "  Status Code: " + args.getStatusCode() + newLine;
		str += "  Information Field 1: " + args.getInformationField1() + newLine;
		str += "  Information Field 2: " + args.getInformationField2() + newLine;
		str += "  Psp Vas ID: " + args.getPspVasId() + newLine;
		str += "  Card Validation: " + args.getCardValidation() + newLine;
		str += "  ICC: " + args.getICCGroupId() + newLine;
		str += "  PAN: " + args.getPAN() + newLine;
		str += "  Issuer ID: " + args.getIssuerId() + newLine;
		str += "  Country Code: " + args.getCountryCode() + newLine;
		str += "  Card Restrictions: " + args.getCardRestrictions() + newLine;
		str += "  Card Fee: " + args.getCardFee() + newLine;
		str += "  Track2: " + args.getTrack2() + newLine;
		str += "  TCC: " + args.getTCC() + newLine;
		str += "  Bank Agent: " + args.getBankAgent() + newLine;

		handleMessage("CardInfoAll", str, CurrentListener.CARD_INFO_ALL);
	}

	@Override
	public void OnCard(CardEventArgs args) {
		String str = "";

		str += "  Issuer ID: " + args.getIssuerID() + newLine;
		str += "  Card Status: " + args.getCardStatus() + "(" + args.getCardStatus().getValue() + ")" + newLine;

		handleMessage("Card", str, CurrentListener.CARD);
	}
}
