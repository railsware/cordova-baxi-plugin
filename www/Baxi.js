var exec = require('cordova/exec');

// Open baxi connection
exports.openBaxi = function(success, error) {
  exec(success, error, "Baxi", "openBaxi", []);
}
// Transfer amount via connected baxi
exports.transferAmount = function(arg0, success, error) {
    exec(success, error, "Baxi", "transferAmount", [arg0]);
};
