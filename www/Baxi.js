var exec = require('cordova/exec');

// Open baxi connection
exports.connect = function(arg0, success, error) {
  exec(success, error, "Baxi", "connect", [arg0]);
};

// return true if the terminal is connected and ready
exports.isConnected = function(success, error) {
  exec(success, error, "Baxi", "isConnected", []);
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
    displayevent:cordova.addWindowEventHandler("displayevent"),
  };

  // hook onto subscribe / unsubscribe events for channel
  for (var key in this.channels) {
    this.channels[key].onHasSubscribersChange = Listener.onHasSubscribersChange;
    console.log("Added handler for key: " + key);
  }

  console.log("Listener object created with success!");
};

Listener.onHasSubscribersChange = function () {

  console.log("Listener.onHasSubscribersChange called ...");

  if(listener.channels.displayevent.numHandlers === 1) {
    exec(listener._displayEvent, listener._error, "Baxi", "start", []);
    console.log("Added");
  } else if (listener.channels.displayevent.numHandlers === 0) {
    exec(null, null, "Baxi", "stop", []);
    console.log("Removed");
  } else {
    console.log("numhandlers : " + listener.channels.displayevent.numHandlers);
  }
};

Listener.prototype._displayEvent = function(info) {
  console.log("_displayEvent called : " + info);
  cordova.fireWindowEvent("displayevent", { "displayText": info });
};

Listener.prototype._error = function(e) {
  console.log("Error initializing Baxi display listener : " + e);
};

var listener = new Listener();

exports = listener;