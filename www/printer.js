
var Printer = function () {};

Printer.prototype = {
    /**
     * Checks if the printer service is avaible (iOS)
     * or if a printing app is installed on the device (Android).
     * @param {Function} callback   A callback function
     * @param {Object?} scope       The scope of the callback (default: window)
     * @return {Boolean}
     */

     /**
     cordova exec function:
     cordova.exec( callbackFunction(winParam) {}, function(error) {}, "service",
                 "action", ["firstArgument", "secondArgument", 42,
                 false]);
     */

    isServiceAvailable: function (callback, scope) {
        var callbackFn = function () {
            var args = typeof arguments[0] == 'boolean' ? arguments : arguments[0];
            callback.apply(scope || window, args);
        };
        cordova.exec(callbackFn, null, 'Printer', 'isServiceAvailable', []);
    },

    /*
        when connect to the printer
        // at the end it was written the way that it takes the first printer (device)
        please specify:
            - printerModel
            - fontSize
    */
    connect: function (options, callback, scope) {
            var callbackFn = function () {
                var args = typeof arguments[0] == 'boolean' ? arguments : arguments[0];
                callback.apply(scope || window, args);
            };
            cordova.exec(callbackFn, null, 'Printer', 'connect', [options]);
        },


    /*
        not a printer Function
        it rescans a given directory
        so all files (media) are shown
        BUG_FIX for kitkat 4.4 media bug
    */
    scanDirectory: function (directory, callback, scope) {
                var callbackFn = function () {
                    var args = typeof arguments[0] == 'boolean' ? arguments : arguments[0];
                    callback.apply(scope || window, args);
                };
                cordova.exec(callbackFn, null, 'Printer', 'scanDirectory', [directory]);
            },


    /**
     * Sends the content to the printer app or service.
     */
    print: function (content, options) {
        var page    = content.innerHTML || content,
            options = options || {};
        if (typeof page != 'string') {
            console.log('Print function requires an HTML string. Not an object');
            return;
        }
        cordova.exec(null, function(err) {console.log( err );}, 'Printer', 'print', [page, options]);
    },


    /**
        list all the bluetooth devices
    */
    list: function (callback, scope) {
          var callbackFn = function () {
              var args = typeof arguments[0] == 'boolean' ? arguments : arguments[0];
              callback.apply(scope || window, args);
          };
          cordova.exec( callbackFn, function(err) {console.log( err );}, 'Printer', 'list', []);
       }
};

var plugin = new Printer();
module.exports = plugin;

