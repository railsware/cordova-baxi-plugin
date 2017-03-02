var exec = require('cordova/exec');

// Open baxi connection
exports.openBaxi = function(arg0, success, error) {
  exec(success, error, "Baxi", "openBaxi", [arg0]);
}
// Transfer amount via connected baxi
exports.transferAmount = function(arg0, success, error) {
    exec(success, error, "Baxi", "transferAmount", [arg0]);
};
// This method performs shutdown of communication with the terminal
exports.close = function(success, error) {
  exec(success, error, "Baxi", "close", []);
}
//This method will go down to the communication layer and ask it if the communication port is opened or not
exports.isOpen = function(success, error) {
  exec(success, error, "Baxi", "isOpen", []);
}
//Starts one of the administration dialogues.
exports.administration = function(arg0, success, error) {
  exec(success, error, "Baxi", "administration", [arg0]);
}