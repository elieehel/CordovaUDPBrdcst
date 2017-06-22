
var exec = require('cordova/exec'),
    cordova = require('cordova');

var BroadcastManager = function() {
	console.log("PHONESTATE BEING INITIATED");
	this.channels = {
		watchingnetwork: cordova.addWindowEventHandler("watchingnetwork")
	};
	for (var key in this.channels) {
		this.channels[key].onHasSubscribersChange = BroadcastManager.change;
	}
};

BroadcastManager.prototype.startListen = function() {
	console.log("Trying to start listener");
	exec(broadcastmanager.change, broadcastmanager.error, "BroadcastManager", "listen", [""]);
};

BroadcastManager.prototype.stopListen = function() {
	console.log("Trying to stop listener");
	exec(broadcastmanager.change, broadcastmanager.error, "BroadcastManager", "deaf", [""]);
};

BroadcastManager.prototype.findServers = function() {
	console.log("Trying to send broadcast");
	exec(broadcastmanager.change, broadcastmanager.error, "BroadcastManager", "send", [""]);
};

BroadcastManager.prototype.stopFind = function() {
	console.log("Trying to stop sending");
	exec(broadcastmanager.change, broadcastmanager.error, "BroadcastManager", "stopsend", [""]);
};

BroadcastManager.prototype.addCb = function(cb) {
	this.cb = cb;
};

BroadcastManager.prototype.error = function(e) {
	//console.log("ERROR IN PHONESTATE");
	//console.log(e);
};

BroadcastManager.prototype.change = function(obj) {
	console.log("CHANGE IN BroadcastManager");
	console.log(obj);
	if (obj.type === "server")
		broadcastmanager.serverState = obj.state;
	else 
		broadcastmanager.sendState = obj.state;
	this.cb(obj);
	cordova.fireWindowEvent("watchingnetwork", obj);
};


/*window.echo = function(str, callback) {
        exec(callback, function(err) {
            callback('Nothing to echo. '+err);
        }, "BroadcastManager", "echo", [str]);
    };*/

var broadcastmanager = new BroadcastManager();



/*broadcastmanager.pluginReload = function() {
	exec(function() {console.log("RELOADED");}, function(err) {console.log("reload error"); console.log(err);}, "BroadcastManager", "resetplugin", ["asd"]);
};*/
module.exports = broadcastmanager;
