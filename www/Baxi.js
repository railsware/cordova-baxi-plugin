var exec = require('cordova/exec');

// Open baxi connection
exports.connect = function(arg0, success, error) {
  exec(success, error, "Baxi", "connect", [arg0]);
};

exports.disconnect = function(arg0, success, error) {
  exec(success, error, "Baxi", "disconnect", [arg0]);
};

// return true if the terminal is connected and ready
exports.isConnected = function(success, error) {
  exec(success, error, "Baxi", "isConnected", []);
};

// hack used to check if connection IS open and VALID
exports.isReady = function(success, error) {
  exec(success, error, "Baxi", "isReady", []);
};

// Transfer amount via connected baxi
exports.transferAmount = function(arg0, success, error) {
    exec(success, error, "Baxi", "transferAmount", [arg0]);
};

//Starts one of the administration dialogues.
exports.administration = function(arg0, success, error) {
  exec(success, error, "Baxi", "administration", [arg0]);
};

var Listener = function() {

  this.channels = {
    displayEvent:cordova.addWindowEventHandler("displayevent"),
    errorEvent:cordova.addWindowEventHandler("errorevent"),
  };

  // hook onto subscribe / unsubscribe events for channel
  for (var key in this.channels) {
    this.channels[key].onHasSubscribersChange = Listener.onHasSubscribersChange;
  }
};

Listener.onHasSubscribersChange = function () {

  if(listener.channels.displayEvent.numHandlers === 1) {
    exec(listener._displayEvent, listener._error, "Baxi", "startDisplay", []);
  } else if (listener.channels.displayEvent.numHandlers === 0) {
    exec(null, null, "Baxi", "stopDisplay", []);
  } else if(listener.channels.errorEvent.numHandlers === 1 ) {
    exec(listener._errorEvent, listener._error, "Baxi", "startError");
  } else if(listener.channels.errorEvent.numHandlers === 0 ) {
    exec(null, null, "Baxi", "stopError", []);
  }
};

Listener.prototype._displayEvent = function(info) {
  cordova.fireWindowEvent("displayevent", { "displayText": info });
};

Listener.prototype._errorEvent = function(error) {
  cordova.fireWindowEvent("errorevent", { "errorText": error });
};

Listener.prototype._error = function(e) {
  console.log("Error initializing Baxi display listener : " + e);
};

var listener = new Listener();

exports = listener;