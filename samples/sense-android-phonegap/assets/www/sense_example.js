function onLoad() {
	// console.log('page loaded');
	document.addEventListener('deviceready', onDeviceReady, false);
};
function onDeviceReady() {
	// console.log('phonegap ready');
	if (!window) {
		console.log('no window');
		return;
	}
	if (!window.plugins) {
		console.log('no window.plugins');
		return;
	}
	if (!window.plugins.sense) {
		console.log('no window.plugins.sense');
		return;
	}
	window.plugins.sense.init();
};
function onLoginClick() {
	window.plugins.sense.changeLogin('foo', MD5('bar'), function(r) {
		alert('Login complete. Result: ' + r);
	}, function(e) {
		alert('changeLogin(\'foo\', \'' + MD5('bar') + '\') error: ' + e);
	});
};
function onLogoutClick() {
	window.plugins.sense.logout(function(r) {
		alert('Logout complete. Result: ' + r);
	}, function(e) {
		alert('logout() error: ' + e);
	});
};
function onRegisterClick() {
	window.plugins.sense.register('username', 'password', 'password', 'surname',
			'email@example.com', '+3112345678', function(r) {
				alert('Register complete. Result: ' + r);
			}, function(e) {
				alert('register() error: ' + e);
			});
};
function onStartSensingClick() {
	window.plugins.sense.toggleMain(true, function(r) {
		alert('Start sensing complete. Result: ' + r);
	}, function(e) {
		alert('toggleMain(true) error: ' + e);
	});
};
function onStopSensingClick() {
	window.plugins.sense.toggleMain(false, function(r) {
		alert('Stop sensing complete. Result: ' + r);
	}, function(e) {
		alert('toggleMain(false) error: ' + e);
	});
};
function onSampleClick(rate) {
	window.plugins.sense.setPref(SensePlatform.PREF_SAMPLE_RATE, rate);
};
function onSyncClick(rate) {
	window.plugins.sense.setPref(SensePlatform.PREF_SYNC_RATE, rate);
};
function onStartPhoneStateClick() {
	window.plugins.sense.togglePhoneState(true, function(r) {
		alert('togglePhoneState(true) result: ' + r);
	}, function(e) {
		alert('togglePhoneState(true) error: ' + e);
	});
};
function onStopPhoneStateClick() {
	window.plugins.sense.togglePhoneState(false, function(r) {
		alert('togglePhoneState(false) result: ' + r);
	}, function(e) {
		alert('togglePhoneState(false) error: ' + e);
	});
};
function onStartPositionClick() {
	window.plugins.sense.togglePosition(true, function(r) {
		alert('togglePosition(true) result: ' + r);
	}, function(e) {
		alert('togglePosition(true) error: ' + e);
	});
};
function onStopPositionClick() {
	window.plugins.sense.togglePosition(false, function(r) {
		alert('togglePosition(false) result: ' + r);
	}, function(e) {
		alert('togglePosition(false) error: ' + e);
	});
};
function onStartAmbienceClick() {
	window.plugins.sense.toggleAmbience(true, function(r) {
		alert('toggleAmbience(true) result: ' + r);
	}, function(e) {
		alert('toggleAmbience(true) error: ' + e);
	});
};
function onStopAmbienceClick() {
	window.plugins.sense.toggleAmbience(false, function(r) {
		alert('toggleAmbience(false) result: ' + r);
	}, function(e) {
		alert('toggleAmbience(false) error: ' + e);
	});
};
function onStartMotionClick() {
	window.plugins.sense.toggleMotion(true, function(r) {
		alert('toggleMotion(true) result: ' + r);
	}, function(e) {
		alert('toggleMotion(true) error: ' + e);
	});
};
function onStopMotionClick() {
	window.plugins.sense.toggleMotion(false, function(r) {
		alert('toggleMotion(false) result: ' + r);
	}, function(e) {
		alert('toggleMotion(false) error: ' + e);
	});
};
function onStartNeighDevClick() {
	window.plugins.sense.toggleNeighDev(true, function(r) {
		alert('toggleNeighDev(true) result: ' + r);
	}, function(e) {
		alert('toggleNeighDev(true) error: ' + e);
	});
};
function onStopNeighDevClick() {
	window.plugins.sense.toggleNeighDev(false, function(r) {
		alert('toggleNeighDev(false) result: ' + r);
	}, function(e) {
		alert('toggleNeighDev(false) error: ' + e);
	});
};
function onStartExternalClick() {
	window.plugins.sense.toggleExternal(true, function(r) {
		alert('toggleExternal(true) result: ' + r);
	}, function(e) {
		alert('toggleExternal(true) error: ' + e);
	});
};
function onStopExternalClick() {
	window.plugins.sense.toggleExternal(false, function(r) {
		alert('toggleExternal(false) result: ' + r);
	}, function(e) {
		alert('toggleExternal(false) error: ' + e);
	});
};
function onAddDataPointClick() {
	window.plugins.sense.addDataPoint('pgsensor', 'PhoneGap sensor', 'PhoneGap', 'string', 'test',
			new Date().getTime(), function(r) {
				alert('Data point stored. Result: ' + r);
			}, function(e) {
				alert('addDataPoint error: ' + e);
			});
};
function onGetLocalDataClick() {
	window.plugins.sense.getLocalData('pgsensor', 10, function(r) {
		var output = '';
		for ( var i = 0; i < r.length; i++) {
			var dataPoint = r[i];
			output += '\n' + i + ': ' + dataPoint['value'] + ' @' + dataPoint['date'] + ';';
		}
		alert('\'pgsensor\' data: ' + output);
	}, function(e) {
		alert('getLocalData() error: ' + e);
	});
};
function onGetRemoteDataClick() {
	window.plugins.sense.getRemoteData('pgsensor', true, 10, function(r) {
		var output = '';
		for ( var i = 0; i < r.length; i++) {
			var dataPoint = r[i];
			output += '\n' + i + ': ' + dataPoint['value'] + ' @' + dataPoint['date'] + ';';
		}
		alert('\'pgsensor\' data: ' + output);
	}, function(e) {
		alert('getRemoteData() error: ' + e);
	});
};
function onFlushBufferClick() {
	window.plugins.sense.flushBuffer(function(r) {
		alert('Data sent to CommonSense. Result: ' + r);
	}, function(e) {
		alert('flushBuffer() error: ' + e);
	});
};
function onGiveFeedbackClick() {
	var name = 'Activity';
	var end = new Date().getTime();
	var start = end - 1000 * 60 * 10; // 10 minutes
	var label = 'test';
	function onSuccess(r) {
		alert('giveFeedback() result: ' + r);
	}
	function onFailure(e) {
		alert('giveFeedback() error: ' + e);		
	}
	window.plugins.sense.giveFeedback(name, start, end, label, onSuccess, onFailure);
};
