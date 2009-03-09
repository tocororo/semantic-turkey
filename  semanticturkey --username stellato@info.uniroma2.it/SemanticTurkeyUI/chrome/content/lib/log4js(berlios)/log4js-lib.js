if (!Object.prototype.extend) {
	Object.extend = function(destination, source) {
		for (property in source) {
			destination[property] = source[property];
		}
		return destination;
	};
	Object.prototype.extend = function(object) {
		return Object.extend.apply(this, [this, object]);
	};
}
if (!Function.prototype.bind) {
	Function.prototype.bind = function(object) {
		var __method = this;
		return function() {
			return __method.apply(object, arguments);
		};
	};
}
var Log4js = {
	version : "1.0-RC1",
	applicationStartDate : new Date(),
	loggers : {},
	getLogger : function(categoryName) {
		if (!(typeof categoryName == "string")) {
			categoryName = "[default]";
		}
		if (!Log4js.loggers[categoryName]) {
			Log4js.loggers[categoryName] = new Log4js.Logger(categoryName);
		}
		return Log4js.loggers[categoryName];
	},
	getDefaultLogger : function() {
		return Log4js.getLogger("[default]");
	},
	attachEvent : function(element, name, observer) {
		if (element.addEventListener) {
			element.addEventListener(name, observer, false);
		} else if (element.attachEvent) {
			element.attachEvent('on' + name, observer);
		}
	}
};
Log4js.Level = function(level, levelStr) {
	this.level = level;
	this.levelStr = levelStr;
};
Log4js.Level.prototype = {
	toLevel : function(sArg, defaultLevel) {
		if (sArg === null) {
			return defaultLevel;
		}
		if (typeof sArg == "string") {
			var s = sArg.toUpperCase();
			if (s == "ALL") {
				return Log4js.Level.ALL;
			}
			if (s == "DEBUG") {
				return Log4js.Level.DEBUG;
			}
			if (s == "INFO") {
				return Log4js.Level.INFO;
			}
			if (s == "WARN") {
				return Log4js.Level.WARN;
			}
			if (s == "ERROR") {
				return Log4js.Level.ERROR;
			}
			if (s == "FATAL") {
				return Log4js.Level.FATAL;
			}
			if (s == "OFF") {
				return Log4js.Level.OFF;
			}
			if (s == "TRACE") {
				return Log4js.Level.TRACE;
			}
			return defaultLevel;
		} else if (typeof sArg == "number") {
			switch (sArg) {
				case ALL_INT :
					return Log4js.Level.ALL;
				case DEBUG_INT :
					return Log4js.Level.DEBUG;
				case INFO_INT :
					return Log4js.Level.INFO;
				case WARN_INT :
					return Log4js.Level.WARN;
				case ERROR_INT :
					return Log4js.Level.ERROR;
				case FATAL_INT :
					return Log4js.Level.FATAL;
				case OFF_INT :
					return Log4js.Level.OFF;
				case TRACE_INT :
					return Log4js.Level.TRACE;
				default :
					return defaultLevel;
			}
		} else {
			return defaultLevel;
		}
	},
	toString : function() {
		return this.levelStr;
	},
	valueOf : function() {
		return this.level;
	}
};
Log4js.Level.OFF_INT = Number.MAX_VALUE;
Log4js.Level.FATAL_INT = 50000;
Log4js.Level.ERROR_INT = 40000;
Log4js.Level.WARN_INT = 30000;
Log4js.Level.INFO_INT = 20000;
Log4js.Level.DEBUG_INT = 10000;
Log4js.Level.TRACE_INT = 5000;
Log4js.Level.ALL_INT = Number.MIN_VALUE;
Log4js.Level.OFF = new Log4js.Level(Log4js.Level.OFF_INT, "OFF");
Log4js.Level.FATAL = new Log4js.Level(Log4js.Level.FATAL_INT, "FATAL");
Log4js.Level.ERROR = new Log4js.Level(Log4js.Level.ERROR_INT, "ERROR");
Log4js.Level.WARN = new Log4js.Level(Log4js.Level.WARN_INT, "WARN");
Log4js.Level.INFO = new Log4js.Level(Log4js.Level.INFO_INT, "INFO");
Log4js.Level.DEBUG = new Log4js.Level(Log4js.Level.DEBUG_INT, "DEBUG");
Log4js.Level.TRACE = new Log4js.Level(Log4js.Level.TRACE_INT, "TRACE");
Log4js.Level.ALL = new Log4js.Level(Log4js.Level.ALL_INT, "ALL");
Log4js.CustomEvent = function() {
	this.listeners = [];
};
Log4js.CustomEvent.prototype = {
	addListener : function(method) {
		this.listeners.push(method);
	},
	removeListener : function(method) {
		var foundIndexes = this.findListenerIndexes(method);
		for (var i = 0; i < foundIndexes.length; i++) {
			this.listeners.splice(foundIndexes[i], 1);
		}
	},
	dispatch : function(handler) {
		for (var i = 0; i < this.listeners.length; i++) {
			try {
				this.listeners[i](handler);
			} catch (e) {
				alert("Could not run the listener " + this.listeners[i]
						+ ". \n" + e);
			}
		}
	},
	findListenerIndexes : function(method) {
		var indexes = [];
		for (var i = 0; i < this.listeners.length; i++) {
			if (this.listeners[i] == method) {
				indexes.push(i);
			}
		}
		return indexes;
	}
};
Log4js.LoggingEvent = function(categoryName, level, message, exception, logger) {
	this.startTime = new Date();
	this.categoryName = categoryName;
	this.message = message;
	this.exception = exception;
	this.level = level;
	this.logger = logger;
};
Log4js.LoggingEvent.prototype = {
	getFormattedTimestamp : function() {
		if (this.logger) {
			return this.logger.getFormattedTimestamp(this.startTime);
		} else {
			return this.startTime.toGMTString();
		}
	}
};
Log4js.Logger = function(name) {
	this.loggingEvents = [];
	this.appenders = [];
	this.category = name || "";
	this.level = Log4js.Level.FATAL;
	this.dateformat = Log4js.DateFormatter.DEFAULT_DATE_FORMAT;
	this.dateformatter = new Log4js.DateFormatter();
	this.onlog = new Log4js.CustomEvent();
	this.onclear = new Log4js.CustomEvent();
	this.appenders.push(new Log4js.Appender(this));
	try {
		window.onerror = this.windowError.bind(this);
	} catch (e) {
		log4jsLogger.fatal(e);
	}
};
Log4js.Logger.prototype = {
	addAppender : function(appender) {
		if (appender instanceof Log4js.Appender) {
			appender.setLogger(this);
			this.appenders.push(appender);
		} else {
			throw "Not instance of an Appender: " + appender;
		}
	},
	setAppenders : function(appenders) {
		for (var i = 0; i < this.appenders.length; i++) {
			this.appenders[i].doClear();
		}
		this.appenders = appenders;
		for (var j = 0; j < this.appenders.length; j++) {
			this.appenders[j].setLogger(this);
		}
	},
	setLevel : function(level) {
		this.level = level;
	},
	log : function(logLevel, message, exception) {
		var loggingEvent = new Log4js.LoggingEvent(this.category, logLevel,
				message, exception, this);
		this.loggingEvents.push(loggingEvent);
		this.onlog.dispatch(loggingEvent);
	},
	clear : function() {
		try {
			this.loggingEvents = [];
			this.onclear.dispatch();
		} catch (e) {
		}
	},
	isTraceEnabled : function() {
		if (this.level.valueOf() <= Log4js.Level.TRACE.valueOf()) {
			return true;
		}
		return false;
	},
	trace : function(message) {
		if (this.isTraceEnabled()) {
			this.log(Log4js.Level.TRACE, message, null);
		}
	},
	isDebugEnabled : function() {
		if (this.level.valueOf() <= Log4js.Level.DEBUG.valueOf()) {
			return true;
		}
		return false;
	},
	debug : function(message) {
		if (this.isDebugEnabled()) {
			this.log(Log4js.Level.DEBUG, message, null);
		}
	},
	debug : function(message, throwable) {
		if (this.isDebugEnabled()) {
			this.log(Log4js.Level.DEBUG, message, throwable);
		}
	},
	isInfoEnabled : function() {
		if (this.level.valueOf() <= Log4js.Level.INFO.valueOf()) {
			return true;
		}
		return false;
	},
	info : function(message) {
		if (this.isInfoEnabled()) {
			this.log(Log4js.Level.INFO, message, null);
		}
	},
	info : function(message, throwable) {
		if (this.isInfoEnabled()) {
			this.log(Log4js.Level.INFO, message, throwable);
		}
	},
	isWarnEnabled : function() {
		if (this.level.valueOf() <= Log4js.Level.WARN.valueOf()) {
			return true;
		}
		return false;
	},
	warn : function(message) {
		if (this.isWarnEnabled()) {
			this.log(Log4js.Level.WARN, message, null);
		}
	},
	warn : function(message, throwable) {
		if (this.isWarnEnabled()) {
			this.log(Log4js.Level.WARN, message, throwable);
		}
	},
	isErrorEnabled : function() {
		if (this.level.valueOf() <= Log4js.Level.ERROR.valueOf()) {
			return true;
		}
		return false;
	},
	error : function(message) {
		if (this.isErrorEnabled()) {
			this.log(Log4js.Level.ERROR, message, null);
		}
	},
	error : function(message, throwable) {
		if (this.isErrorEnabled()) {
			this.log(Log4js.Level.ERROR, message, throwable);
		}
	},
	isFatalEnabled : function() {
		if (this.level.valueOf() <= Log4js.Level.FATAL.valueOf()) {
			return true;
		}
		return false;
	},
	fatal : function(message) {
		if (this.isFatalEnabled()) {
			this.log(Log4js.Level.FATAL, message, null);
		}
	},
	fatal : function(message, throwable) {
		if (this.isFatalEnabled()) {
			this.log(Log4js.Level.FATAL, message, throwable);
		}
	},
	windowError : function(msg, url, line) {
		var message = "Error in (" + (url || window.location) + ") on line "
				+ line + " with message (" + msg + ")";
		this.log(Log4js.Level.FATAL, message, null);
	},
	setDateFormat : function(format) {
		this.dateformat = format;
	},
	getFormattedTimestamp : function(date) {
		return this.dateformatter.formatDate(date, this.dateformat);
	}
};
Log4js.Appender = function() {
	this.logger = null;
};
Log4js.Appender.prototype = {
	doAppend : function(loggingEvent) {
		return;
	},
	doClear : function() {
		return;
	},
	setLayout : function(layout) {
		this.layout = layout;
	},
	setLogger : function(logger) {
		logger.onlog.addListener(this.doAppend.bind(this));
		logger.onclear.addListener(this.doClear.bind(this));
		this.logger = logger;
	}
};
Log4js.Layout = function() {
	return;
};
Log4js.Layout.prototype = {
	format : function(loggingEvent) {
		return "";
	},
	getContentType : function() {
		return "text/plain";
	},
	getHeader : function() {
		return null;
	},
	getFooter : function() {
		return null;
	},
	getSeparator : function() {
		return "";
	}
};
Log4js.ConsoleAppender = function(isInline) {
	this.layout = new Log4js.PatternLayout(Log4js.PatternLayout.TTCC_CONVERSION_PATTERN);
	this.inline = isInline;
	this.accesskey = "d";
	this.tagPattern = null;
	this.commandHistory = [];
	this.commandIndex = 0;
	this.popupBlocker = false;
	this.outputElement = null;
	this.docReference = null;
	this.winReference = null;
	if (this.inline) {
		Log4js.attachEvent(window, 'load', this.initialize.bind(this));
	}
};
Log4js.ConsoleAppender.prototype = (new Log4js.Appender()).extend({
	setAccessKey : function(key) {
		this.accesskey = key;
	},
	initialize : function() {
		if (!this.inline) {
			var doc = null;
			var win = null;
			window.top.consoleWindow = window
					.open(
							"",
							this.logger.category,
							"left=0,top=0,width=700,height=700,scrollbars=no,status=no,resizable=yes;toolbar=no");
			window.top.consoleWindow.opener = self;
			win = window.top.consoleWindow;
			if (!win) {
				this.popupBlocker = true;
				alert("Popup window manager blocking the Log4js popup window to bedisplayed.\n\n"
						+ "Please disabled this to properly see logged events.");
			} else {
				doc = win.document;
				doc.open();
				doc
						.write("<!DOCTYPE html PUBLIC -//W3C//DTD XHTML 1.0 Transitional//EN ");
				doc
						.write("  http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd>\n\n");
				doc.write("<html><head><title>Log4js - " + this.logger.category
						+ "</title>\n");
				doc
						.write("</head><body style=\"background-color:darkgray\"></body>\n");
				win.blur();
				win.focus();
			}
			this.docReference = doc;
			this.winReference = win;
		} else {
			this.docReference = document;
			this.winReference = window;
		}
		this.outputCount = 0;
		this.tagPattern = ".*";
		this.logElement = this.docReference.createElement('div');
		this.docReference.body.appendChild(this.logElement);
		this.logElement.style.display = 'none';
		this.logElement.style.position = "absolute";
		this.logElement.style.left = '0px';
		this.logElement.style.width = '100%';
		this.logElement.style.textAlign = "left";
		this.logElement.style.fontFamily = "lucida console";
		this.logElement.style.fontSize = "100%";
		this.logElement.style.backgroundColor = 'darkgray';
		this.logElement.style.opacity = 0.9;
		this.logElement.style.zIndex = 2000;
		this.toolbarElement = this.docReference.createElement('div');
		this.logElement.appendChild(this.toolbarElement);
		this.toolbarElement.style.padding = "0 0 0 2px";
		this.buttonsContainerElement = this.docReference.createElement('span');
		this.toolbarElement.appendChild(this.buttonsContainerElement);
		if (this.inline) {
			var closeButton = this.docReference.createElement('button');
			closeButton.style.cssFloat = "right";
			closeButton.style.styleFloat = "right";
			closeButton.style.color = "black";
			closeButton.innerHTML = "close";
			closeButton.onclick = this.toggle.bind(this);
			this.buttonsContainerElement.appendChild(closeButton);
		}
		var clearButton = this.docReference.createElement('button');
		clearButton.style.cssFloat = "right";
		clearButton.style.styleFloat = "right";
		clearButton.style.color = "black";
		clearButton.innerHTML = "clear";
		clearButton.onclick = this.logger.clear.bind(this.logger);
		this.buttonsContainerElement.appendChild(clearButton);
		this.tagFilterContainerElement = this.docReference
				.createElement('span');
		this.toolbarElement.appendChild(this.tagFilterContainerElement);
		this.tagFilterContainerElement.style.cssFloat = 'left';
		this.tagFilterContainerElement.appendChild(this.docReference
				.createTextNode("Log4js - " + this.logger.category));
		this.tagFilterContainerElement.appendChild(this.docReference
				.createTextNode(" | Level Filter: "));
		this.tagFilterElement = this.docReference.createElement('input');
		this.tagFilterContainerElement.appendChild(this.tagFilterElement);
		this.tagFilterElement.style.width = '200px';
		this.tagFilterElement.value = this.tagPattern;
		this.tagFilterElement.setAttribute('autocomplete', 'off');
		Log4js.attachEvent(this.tagFilterElement, 'keyup', this.updateTags
						.bind(this));
		Log4js.attachEvent(this.tagFilterElement, 'click', function() {
					this.tagFilterElement.select();
				}.bind(this));
		this.outputElement = this.docReference.createElement('div');
		this.logElement.appendChild(this.outputElement);
		this.outputElement.style.overflow = "auto";
		this.outputElement.style.clear = "both";
		this.outputElement.style.height = (this.inline) ? ("200px") : ("650px");
		this.outputElement.style.width = "100%";
		this.outputElement.style.backgroundColor = 'black';
		this.inputContainerElement = this.docReference.createElement('div');
		this.inputContainerElement.style.width = "100%";
		this.logElement.appendChild(this.inputContainerElement);
		this.inputElement = this.docReference.createElement('input');
		this.inputContainerElement.appendChild(this.inputElement);
		this.inputElement.style.width = '100%';
		this.inputElement.style.borderWidth = '0px';
		this.inputElement.style.margin = '0px';
		this.inputElement.style.padding = '0px';
		this.inputElement.value = 'Type command here';
		this.inputElement.setAttribute('autocomplete', 'off');
		Log4js.attachEvent(this.inputElement, 'keyup', this.handleInput
						.bind(this));
		Log4js.attachEvent(this.inputElement, 'click', function() {
					this.inputElement.select();
				}.bind(this));
		if (this.inline) {
			window.setInterval(this.repositionWindow.bind(this), 500);
			this.repositionWindow();
			var accessElement = this.docReference.createElement('button');
			accessElement.style.position = "absolute";
			accessElement.style.top = "-100px";
			accessElement.accessKey = this.accesskey;
			accessElement.onclick = this.toggle.bind(this);
			this.docReference.body.appendChild(accessElement);
		} else {
			this.show();
		}
	},
	toggle : function() {
		if (this.logElement.style.display == 'none') {
			this.show();
			return true;
		} else {
			this.hide();
			return false;
		}
	},
	show : function() {
		this.logElement.style.display = '';
		this.outputElement.scrollTop = this.outputElement.scrollHeight;
		this.inputElement.select();
	},
	hide : function() {
		this.logElement.style.display = 'none';
	},
	output : function(message, style) {
		var shouldScroll = (this.outputElement.scrollTop + (2 * this.outputElement.clientHeight)) >= this.outputElement.scrollHeight;
		this.outputCount++;
		style = (style ? style += ';' : '');
		style += 'padding:1px;margin:0 0 5px 0';
		if (this.outputCount % 2 === 0) {
			style += ";background-color:#101010";
		}
		message = message || "undefined";
		message = message.toString();
		this.outputElement.innerHTML += "<pre style='" + style + "'>" + message
				+ "</pre>";
		if (shouldScroll) {
			this.outputElement.scrollTop = this.outputElement.scrollHeight;
		}
	},
	updateTags : function() {
		var pattern = this.tagFilterElement.value;
		if (this.tagPattern == pattern) {
			return;
		}
		try {
			new RegExp(pattern);
		} catch (e) {
			return;
		}
		this.tagPattern = pattern;
		this.outputElement.innerHTML = "";
		this.outputCount = 0;
		for (var i = 0; i < this.logger.loggingEvents.length; i++) {
			this.doAppend(this.logger.loggingEvents[i]);
		}
	},
	repositionWindow : function() {
		var offset = window.pageYOffset
				|| this.docReference.documentElement.scrollTop
				|| this.docReference.body.scrollTop;
		var pageHeight = self.innerHeight
				|| this.docReference.documentElement.clientHeight
				|| this.docReference.body.clientHeight;
		this.logElement.style.top = (offset + pageHeight - this.logElement.offsetHeight)
				+ "px";
	},
	doAppend : function(loggingEvent) {
		if (this.popupBlocker) {
			return;
		}
		if ((!this.inline) && (!this.winReference || this.winReference.closed)) {
			this.initialize();
		}
		if (this.tagPattern !== null
				&& loggingEvent.level.toString().search(new RegExp(
						this.tagPattern, 'igm')) == -1) {
			return;
		}
		var style = '';
		if (loggingEvent.level.toString().search(/ERROR/) != -1) {
			style += 'color:red';
		} else if (loggingEvent.level.toString().search(/FATAL/) != -1) {
			style += 'color:red';
		} else if (loggingEvent.level.toString().search(/WARN/) != -1) {
			style += 'color:orange';
		} else if (loggingEvent.level.toString().search(/DEBUG/) != -1) {
			style += 'color:green';
		} else if (loggingEvent.level.toString().search(/INFO/) != -1) {
			style += 'color:white';
		} else {
			style += 'color:yellow';
		}
		this.output(this.layout.format(loggingEvent), style);
	},
	doClear : function() {
		this.outputElement.innerHTML = "";
	},
	handleInput : function(e) {
		if (e.keyCode == 13) {
			var command = this.inputElement.value;
			switch (command) {
				case "clear" :
					this.logger.clear();
					break;
				default :
					var consoleOutput = "";
					try {
						consoleOutput = eval(this.inputElement.value);
					} catch (e) {
						this.logger.error("Problem parsing input <" + command
								+ ">" + e.message);
						break;
					}
					this.logger.trace(consoleOutput);
					break;
			}
			if (this.inputElement.value !== ""
					&& this.inputElement.value !== this.commandHistory[0]) {
				this.commandHistory.unshift(this.inputElement.value);
			}
			this.commandIndex = 0;
			this.inputElement.value = "";
		} else if (e.keyCode == 38 && this.commandHistory.length > 0) {
			this.inputElement.value = this.commandHistory[this.commandIndex];
			if (this.commandIndex < this.commandHistory.length - 1) {
				this.commandIndex += 1;
			}
		} else if (e.keyCode == 40 && this.commandHistory.length > 0) {
			if (this.commandIndex > 0) {
				this.commandIndex -= 1;
			}
			this.inputElement.value = this.commandHistory[this.commandIndex];
		} else {
			this.commandIndex = 0;
		}
	},
	toString : function() {
		return "Log4js.ConsoleAppender[inline=" + this.inline + "]";
	}
});
Log4js.MetatagAppender = function() {
	this.currentLine = 0;
};
Log4js.MetatagAppender.prototype = (new Log4js.Appender()).extend({
			doAppend : function(loggingEvent) {
				var now = new Date();
				var lines = loggingEvent.message.split("\n");
				var headTag = document.getElementsByTagName("head")[0];
				for (var i = 1; i <= lines.length; i++) {
					var value = lines[i - 1];
					if (i == 1) {
						value = loggingEvent.level.toString() + ": " + value;
					} else {
						value = "> " + value;
					}
					var metaTag = document.createElement("meta");
					metaTag
							.setAttribute("name", "X-log4js:"
											+ this.currentLine);
					metaTag.setAttribute("content", value);
					headTag.appendChild(metaTag);
					this.currentLine += 1;
				}
			},
			toString : function() {
				return "Log4js.MetatagAppender";
			}
		});
Log4js.AjaxAppender = function(loggingUrl) {
	this.isInProgress = false;
	this.loggingUrl = loggingUrl || "logging.log4js";
	this.threshold = 1;
	this.timeout = 2000;
	this.loggingEventMap = new Log4js.FifoBuffer();
	this.layout = new Log4js.XMLLayout();
	this.httpRequest = null;
};
Log4js.AjaxAppender.prototype = (new Log4js.Appender()).extend({
	doAppend : function(loggingEvent) {
		log4jsLogger.trace("> AjaxAppender.append");
		if (this.loggingEventMap.length() <= this.threshold
				|| this.isInProgress === true) {
			this.loggingEventMap.push(loggingEvent);
		}
		if (this.loggingEventMap.length() >= this.threshold
				&& this.isInProgress === false) {
			this.send();
		}
		log4jsLogger.trace("< AjaxAppender.append");
	},
	doClear : function() {
		log4jsLogger.trace("> AjaxAppender.doClear");
		if (this.loggingEventMap.length() > 0) {
			this.send();
		}
		log4jsLogger.trace("< AjaxAppender.doClear");
	},
	setThreshold : function(threshold) {
		log4jsLogger.trace("> AjaxAppender.setThreshold: " + threshold);
		this.threshold = threshold;
		log4jsLogger.trace("< AjaxAppender.setThreshold");
	},
	setTimeout : function(milliseconds) {
		this.timeout = milliseconds;
	},
	send : function() {
		if (this.loggingEventMap.length() > 0) {
			log4jsLogger.trace("> AjaxAppender.send");
			this.isInProgress = true;
			var a = [];
			for (var i = 0; i < this.loggingEventMap.length()
					&& i < this.threshold; i++) {
				a.push(this.layout.format(this.loggingEventMap.pull()));
			}
			var content = this.layout.getHeader();
			content += a.join(this.layout.getSeparator());
			content += this.layout.getFooter();
			var appender = this;
			if (this.httpRequest === null) {
				this.httpRequest = this.getXmlHttpRequest();
			}
			this.httpRequest.onreadystatechange = function() {
				appender.onReadyStateChanged.call(appender);
			};
			this.httpRequest.open("POST", this.loggingUrl, true);
			this.httpRequest.setRequestHeader("Content-type", this.layout
							.getContentType());
			this.httpRequest.setRequestHeader("REFERER", location.href);
			this.httpRequest.setRequestHeader("Content-length", content.length);
			this.httpRequest.setRequestHeader("Connection", "close");
			this.httpRequest.send(content);
			appender = this;
			try {
				window.setTimeout(function() {
							log4jsLogger.trace("> AjaxAppender.timeout");
							appender.httpRequest.onreadystatechange = function() {
								return;
							};
							appender.httpRequest.abort();
							appender.isInProgress = false;
							if (appender.loggingEventMap.length() > 0) {
								appender.send();
							}
							log4jsLogger.trace("< AjaxAppender.timeout");
						}, this.timeout);
			} catch (e) {
				log4jsLogger.fatal(e);
			}
			log4jsLogger.trace("> AjaxAppender.send");
		}
	},
	onReadyStateChanged : function() {
		log4jsLogger.trace("> AjaxAppender.onReadyStateChanged");
		var req = this.httpRequest;
		if (this.httpRequest.readyState != 4) {
			log4jsLogger
					.trace("< AjaxAppender.onReadyStateChanged: readyState "
							+ req.readyState + " != 4");
			return;
		}
		var success = ((typeof req.status === "undefined") || req.status === 0 || (req.status >= 200 && req.status < 300));
		if (success) {
			log4jsLogger.trace("  AjaxAppender.onReadyStateChanged: success");
			this.isInProgress = false;
		} else {
			var msg = "  AjaxAppender.onReadyStateChanged: XMLHttpRequest request to URL "
					+ this.loggingUrl
					+ " returned status code "
					+ this.httpRequest.status;
			log4jsLogger.error(msg);
		}
		log4jsLogger
				.trace("< AjaxAppender.onReadyStateChanged: readyState == 4");
	},
	getXmlHttpRequest : function() {
		log4jsLogger.trace("> AjaxAppender.getXmlHttpRequest");
		var httpRequest = false;
		try {
			if (window.XMLHttpRequest) {
				httpRequest = new XMLHttpRequest();
				if (httpRequest.overrideMimeType) {
					httpRequest.overrideMimeType(this.layout.getContentType());
				}
			} else if (window.ActiveXObject) {
				try {
					httpRequest = new ActiveXObject("Msxml2.XMLHTTP");
				} catch (e) {
					httpRequest = new ActiveXObject("Microsoft.XMLHTTP");
				}
			}
		} catch (e) {
			httpRequest = false;
		}
		if (!httpRequest) {
			log4jsLogger
					.fatal("Unfortunatelly your browser does not support AjaxAppender for log4js!");
		}
		log4jsLogger.trace("< AjaxAppender.getXmlHttpRequest");
		return httpRequest;
	},
	toString : function() {
		return "Log4js.AjaxAppender[loggingUrl=" + this.loggingUrl
				+ ", threshold=" + this.threshold + "]";
	}
});
Log4js.FileAppender = function(file) {
	this.layout = new Log4js.SimpleLayout();
	this.isIE = 'undefined';
	this.file = file || "log4js.log";
	try {
		this.fso = new ActiveXObject("Scripting.FileSystemObject");
		this.isIE = true;
	} catch (e) {
		try {
			netscape.security.PrivilegeManager
					.enablePrivilege("UniversalXPConnect");
			this.fso = Components.classes["@mozilla.org/file/local;1"]
					.createInstance(Components.interfaces.nsILocalFile);
			this.isIE = false;
		} catch (e) {
			log4jsLogger.error(e);
		}
	}
};
Log4js.FileAppender.prototype = (new Log4js.Appender()).extend({
	doAppend : function(loggingEvent) {
		try {
			var fileHandle = null;
			if (this.isIE === 'undefined') {
				log4jsLogger.error("Unsupported ")
			} else if (this.isIE) {
				fileHandle = this.fso.OpenTextFile(this.file, 8, true);
				fileHandle.WriteLine(this.layout.format(loggingEvent));
				fileHandle.close();
			} else {
				netscape.security.PrivilegeManager
						.enablePrivilege("UniversalXPConnect");
				this.fso.initWithPath(this.file);
				if (!this.fso.exists()) {
					this.fso.create(0x00, 0600);
				}
				fileHandle = Components.classes["@mozilla.org/network/file-output-stream;1"]
						.createInstance(Components.interfaces.nsIFileOutputStream);
				fileHandle.init(this.fso, 0x04 | 0x08 | 0x10, 064, 0);
				var line = this.layout.format(loggingEvent);
				fileHandle.write(line, line.length);
				fileHandle.close();
			}
		} catch (e) {
			log4jsLogger.error(e);
		}
	},
	doClear : function() {
		try {
			if (this.isIE) {
				var fileHandle = this.fso.GetFile(this.file);
				fileHandle.Delete();
			} else {
				netscape.security.PrivilegeManager
						.enablePrivilege("UniversalXPConnect");
				this.fso.initWithPath(this.file);
				if (this.fso.exists()) {
					this.fso.remove(false);
				}
			}
		} catch (e) {
			log4jsLogger.error(e);
		}
	},
	toString : function() {
		return "Log4js.FileAppender[file=" + this.file + "]";
	}
});
Log4js.WindowsEventAppender = function() {
	this.layout = new Log4js.SimpleLayout();
	try {
		this.shell = new ActiveXObject("WScript.Shell");
	} catch (e) {
		log4jsLogger.error(e);
	}
};
Log4js.WindowsEventAppender.prototype = (new Log4js.Appender()).extend({
			doAppend : function(loggingEvent) {
				var winLevel = 4;
				switch (loggingEvent.level) {
					case Log4js.Level.FATAL :
						winLevel = 1;
						break;
					case Log4js.Level.ERROR :
						winLevel = 1;
						break;
					case Log4js.Level.WARN :
						winLevel = 2;
						break;
					default :
						winLevel = 4;
						break;
				}
				try {
					this.shell.LogEvent(winLevel, this.level
									.format(loggingEvent));
				} catch (e) {
					log4jsLogger.error(e);
				}
			},
			toString : function() {
				return "Log4js.WindowsEventAppender";
			}
		});
Log4js.JSAlertAppender = function() {
	this.layout = new Log4js.SimpleLayout();
};
Log4js.JSAlertAppender.prototype = (new Log4js.Appender()).extend({
			doAppend : function(loggingEvent) {
				alert(this.layout.getHeader()
						+ this.layout.format(loggingEvent)
						+ this.layout.getFooter());
			},
			toString : function() {
				return "Log4js.JSAlertAppender";
			}
		});
Log4js.MozillaJSConsoleAppender = function() {
	this.layout = new Log4js.SimpleLayout();
	try {
		netscape.security.PrivilegeManager
				.enablePrivilege("UniversalXPConnect");
		this.jsConsole = Components.classes["@mozilla.org/consoleservice;1"]
				.getService(Components.interfaces.nsIConsoleService);
		this.scriptError = Components.classes["@mozilla.org/scripterror;1"]
				.createInstance(Components.interfaces.nsIScriptError);
	} catch (e) {
		log4jsLogger.error(e);
	}
};
Log4js.MozillaJSConsoleAppender.prototype = (new Log4js.Appender()).extend({
			doAppend : function(loggingEvent) {
				try {
					netscape.security.PrivilegeManager
							.enablePrivilege("UniversalXPConnect");
					this.scriptError.init(this.layout.format(loggingEvent),
							null, null, null, null, this.getFlag(loggingEvent),
							loggingEvent.categoryName);
					this.jsConsole.logMessage(this.scriptError);
				} catch (e) {
					log4jsLogger.error(e);
				}
			},
			toString : function() {
				return "Log4js.MozillaJSConsoleAppender";
			},
			getFlag : function(loggingEvent) {
				var retval;
				switch (loggingEvent.level) {
					case Log4js.Level.FATAL :
						retval = 2;
						break;
					case Log4js.Level.ERROR :
						retval = 0;
						break;
					case Log4js.Level.WARN :
						retval = 1;
						break;
					default :
						retval = 1;
						break;
				}
				return retval;
			}
		});
Log4js.OperaJSConsoleAppender = function() {
	this.layout = new Log4js.SimpleLayout();
};
Log4js.OperaJSConsoleAppender.prototype = (new Log4js.Appender()).extend({
			doAppend : function(loggingEvent) {
				opera.postError(this.layout.format(loggingEvent));
			},
			toString : function() {
				return "Log4js.OperaJSConsoleAppender";
			}
		});
Log4js.SafariJSConsoleAppender = function() {
	this.layout = new Log4js.SimpleLayout();
};
Log4js.SafariJSConsoleAppender.prototype = (new Log4js.Appender()).extend({
			doAppend : function(loggingEvent) {
				window.console.log(this.layout.format(loggingEvent));
			},
			toString : function() {
				return "Log4js.SafariJSConsoleAppender";
			}
		});
Log4js.BrowserConsoleAppender = function() {
	this.consoleDelegate = null;
	if (window.console) {
		this.consoleDelegate = new Log4js.SafariJSConsoleAppender();
	} else if (window.opera) {
		this.consoleDelegate = new Log4js.OperaJSConsoleAppender();
	} else if (netscape) {
		this.consoleDelegate = new Log4js.MozJSConsoleAppender();
	} else {
		log4jsLogger.error("Unsupported Browser");
	}
};
Log4js.BrowserConsoleAppender.prototype = (new Log4js.Appender()).extend({
			doAppend : function(loggingEvent) {
				this.consoleDelegate.doAppend(loggingEvent);
			},
			doClear : function() {
				this.consoleDelegate.doClear();
			},
			setLayout : function(layout) {
				this.consoleDelegate.setLayout(layout);
			},
			toString : function() {
				return "Log4js.BrowserConsoleAppender: "
						+ this.consoleDelegate.toString();
			}
		});
Log4js.SimpleLayout = function() {
	this.LINE_SEP = "\n";
	this.LINE_SEP_LEN = 1;
};
Log4js.SimpleLayout.prototype = (new Log4js.Layout()).extend({
			format : function(loggingEvent) {
				return loggingEvent.level.toString() + " - "
						+ loggingEvent.message + this.LINE_SEP;
			},
			getContentType : function() {
				return "text/plain";
			},
			getHeader : function() {
				return "";
			},
			getFooter : function() {
				return "";
			}
		});
Log4js.BasicLayout = function() {
	this.LINE_SEP = "\n";
};
Log4js.BasicLayout.prototype = (new Log4js.Layout()).extend({
			format : function(loggingEvent) {
				return loggingEvent.categoryName + "~"
						+ loggingEvent.startTime.toLocaleString() + " ["
						+ loggingEvent.level.toString() + "] "
						+ loggingEvent.message + this.LINE_SEP;
			},
			getContentType : function() {
				return "text/plain";
			},
			getHeader : function() {
				return "";
			},
			getFooter : function() {
				return "";
			}
		});
Log4js.HtmlLayout = function() {
	return;
};
Log4js.HtmlLayout.prototype = (new Log4js.Layout()).extend({
			format : function(loggingEvent) {
				return "<div style=\"" + this.getStyle(loggingEvent) + "\">"
						+ loggingEvent.getFormattedTimestamp() + " - "
						+ loggingEvent.level.toString() + " - "
						+ loggingEvent.message + "</div>\n";
			},
			getContentType : function() {
				return "text/html";
			},
			getHeader : function() {
				return "<html><head><title>log4js</head><body>";
			},
			getFooter : function() {
				return "</body></html>";
			},
			getStyle : function(loggingEvent) {
				var style;
				if (loggingEvent.level.toString().search(/ERROR/) != -1) {
					style = 'color:red';
				} else if (loggingEvent.level.toString().search(/FATAL/) != -1) {
					style = 'color:red';
				} else if (loggingEvent.level.toString().search(/WARN/) != -1) {
					style = 'color:orange';
				} else if (loggingEvent.level.toString().search(/DEBUG/) != -1) {
					style = 'color:green';
				} else if (loggingEvent.level.toString().search(/INFO/) != -1) {
					style = 'color:white';
				} else {
					style = 'color:yellow';
				}
				return style;
			}
		});
Log4js.XMLLayout = function() {
	return;
};
Log4js.XMLLayout.prototype = (new Log4js.Layout()).extend({
			format : function(loggingEvent) {
				var useragent = "unknown";
				try {
					useragent = navigator.userAgent;
				} catch (e) {
					useragent = "unknown";
				}
				var referer = "unknown";
				try {
					referer = location.href;
				} catch (e) {
					referer = "unknown";
				}
				var content = "<log4js:event logger=\"";
				content += loggingEvent.categoryName + "\" level=\"";
				content += loggingEvent.level.toString() + "\" useragent=\"";
				content += useragent + "\" referer=\"";
				content += referer + "\" timestamp=\"";
				content += loggingEvent.getFormattedTimestamp() + "\">\n";
				content += "\t<log4js:message><![CDATA["
						+ this.escapeCdata(loggingEvent.message)
						+ "]]></log4js:message>\n";
				if (loggingEvent.exception) {
					content += "\t<log4js:exception><![CDATA["
							+ this.formatException(loggingEvent.exception)
							+ "]]></log4js:exception>\n";
				}
				content += "</log4js:event>\n";
				return content;
			},
			getContentType : function() {
				return "text/xml";
			},
			getHeader : function() {
				return "<log4js:eventSet version=\""
						+ Log4js.version
						+ "\" xmlns:log4js=\"http://log4js.berlios.de/log4js/\">\n";
			},
			getFooter : function() {
				return "</log4js:eventSet>\n";
			},
			getSeparator : function() {
				return "\n";
			},
			formatException : function(ex) {
				if (ex) {
					var exStr = "";
					if (ex.message) {
						exStr += ex.message;
					} else if (ex.description) {
						exStr += ex.description;
					}
					if (ex.lineNumber) {
						exStr += " on line number " + ex.lineNumber;
					}
					if (ex.fileName) {
						exStr += " in file " + ex.fileName;
					}
					return exStr;
				}
				return null;
			},
			escapeCdata : function(str) {
				return str.replace(/\]\]>/, "]]>]]&gt;<![CDATA[");
			}
		});
Log4js.JSONLayout = function() {
	this.df = new Log4js.DateFormatter();
};
Log4js.JSONLayout.prototype = (new Log4js.Layout()).extend({
			format : function(loggingEvent) {
				var useragent = "unknown";
				try {
					useragent = navigator.userAgent;
				} catch (e) {
					useragent = "unknown";
				}
				var referer = "unknown";
				try {
					referer = location.href;
				} catch (e) {
					referer = "unknown";
				}
				var jsonString = "{\n \"LoggingEvent\": {\n";
				jsonString += "\t\"logger\": \"" + loggingEvent.categoryName
						+ "\",\n";
				jsonString += "\t\"level\": \"" + loggingEvent.level.toString()
						+ "\",\n";
				jsonString += "\t\"message\": \"" + loggingEvent.message
						+ "\",\n";
				jsonString += "\t\"referer\": \"" + referer + "\",\n";
				jsonString += "\t\"useragent\": \"" + useragent + "\",\n";
				jsonString += "\t\"timestamp\": \""
						+ this.df.formatDate(loggingEvent.startTime,
								"yyyy-MM-ddThh:mm:ssZ") + "\",\n";
				jsonString += "\t\"exception\": \"" + loggingEvent.exception
						+ "\"\n";
				jsonString += "}}";
				return jsonString;
			},
			getContentType : function() {
				return "text/json";
			},
			getHeader : function() {
				return "{\"Log4js\": [\n";
			},
			getFooter : function() {
				return "\n]}";
			},
			getSeparator : function() {
				return ",\n";
			}
		});
Log4js.PatternLayout = function(pattern) {
	if (pattern) {
		this.pattern = pattern;
	} else {
		this.pattern = Log4js.PatternLayout.DEFAULT_CONVERSION_PATTERN;
	}
};
Log4js.PatternLayout.TTCC_CONVERSION_PATTERN = "%r %p %c - %m%n";
Log4js.PatternLayout.DEFAULT_CONVERSION_PATTERN = "%m%n";
Log4js.PatternLayout.ISO8601_DATEFORMAT = "yyyy-MM-dd HH:mm:ss,SSS";
Log4js.PatternLayout.DATETIME_DATEFORMAT = "dd MMM YYYY HH:mm:ss,SSS";
Log4js.PatternLayout.ABSOLUTETIME_DATEFORMAT = "HH:mm:ss,SSS";
Log4js.PatternLayout.prototype = (new Log4js.Layout()).extend({
	getContentType : function() {
		return "text/plain";
	},
	getHeader : function() {
		return null;
	},
	getFooter : function() {
		return null;
	},
	format : function(loggingEvent) {
		var regex = /%(-?[0-9]+)?(\.?[0-9]+)?([cdmnpr%])(\{([^\}]+)\})?|([^%]+)/;
		var formattedString = "";
		var result;
		var searchString = this.pattern;
		while ((result = regex.exec(searchString))) {
			var matchedString = result[0];
			var padding = result[1];
			var truncation = result[2];
			var conversionCharacter = result[3];
			var specifier = result[5];
			var text = result[6];
			if (text) {
				formattedString += "" + text;
			} else {
				var replacement = "";
				switch (conversionCharacter) {
					case "c" :
						var loggerName = loggingEvent.categoryName;
						if (specifier) {
							var precision = parseInt(specifier, 10);
							var loggerNameBits = loggingEvent.categoryName
									.split(".");
							if (precision >= loggerNameBits.length) {
								replacement = loggerName;
							} else {
								replacement = loggerNameBits
										.slice(loggerNameBits.length
												- precision).join(".");
							}
						} else {
							replacement = loggerName;
						}
						break;
					case "d" :
						var dateFormat = Log4js.PatternLayout.ISO8601_DATEFORMAT;
						if (specifier) {
							dateFormat = specifier;
							if (dateFormat == "ISO8601") {
								dateFormat = Log4js.PatternLayout.ISO8601_DATEFORMAT;
							} else if (dateFormat == "ABSOLUTE") {
								dateFormat = Log4js.PatternLayout.ABSOLUTETIME_DATEFORMAT;
							} else if (dateFormat == "DATE") {
								dateFormat = Log4js.PatternLayout.DATETIME_DATEFORMAT;
							}
						}
						replacement = (new Log4js.SimpleDateFormat(dateFormat))
								.format(loggingEvent.startTime);
						break;
					case "m" :
						replacement = loggingEvent.message;
						break;
					case "n" :
						replacement = "\n";
						break;
					case "p" :
						replacement = loggingEvent.level.toString();
						break;
					case "r" :
						replacement = ""
								+ loggingEvent.startTime.toLocaleTimeString();
						break;
					case "%" :
						replacement = "%";
						break;
					default :
						replacement = matchedString;
						break;
				}
				var len;
				if (truncation) {
					len = parseInt(truncation.substr(1), 10);
					replacement = replacement.substring(0, len);
				}
				if (padding) {
					if (padding.charAt(0) == "-") {
						len = parseInt(padding.substr(1), 10);
						while (replacement.length < len) {
							replacement += " ";
						}
					} else {
						len = parseInt(padding, 10);
						while (replacement.length < len) {
							replacement = " " + replacement;
						}
					}
				}
				formattedString += replacement;
			}
			searchString = searchString.substr(result.index + result[0].length);
		}
		return formattedString;
	}
});
if (!Array.prototype.push) {
	Array.prototype.push = function() {
		var startLength = this.length;
		for (var i = 0; i < arguments.length; i++) {
			this[startLength + i] = arguments[i];
		}
		return this.length;
	};
}
Log4js.FifoBuffer = function() {
	this.array = new Array();
};
Log4js.FifoBuffer.prototype = {
	push : function(obj) {
		this.array[this.array.length] = obj;
		return this.array.length;
	},
	pull : function() {
		if (this.array.length > 0) {
			var firstItem = this.array[0];
			for (var i = 0; i < this.array.length - 1; i++) {
				this.array[i] = this.array[i + 1];
			}
			this.array.length = this.array.length - 1;
			return firstItem;
		}
		return null;
	},
	length : function() {
		return this.array.length;
	}
};
Log4js.DateFormatter = function() {
	return;
};
Log4js.DateFormatter.DEFAULT_DATE_FORMAT = "yyyy-MM-ddThh:mm:ssO";
Log4js.DateFormatter.prototype = {
	formatDate : function(vDate, vFormat) {
		var vDay = this.addZero(vDate.getDate());
		var vMonth = this.addZero(vDate.getMonth() + 1);
		var vYearLong = this.addZero(vDate.getFullYear());
		var vYearShort = this.addZero(vDate.getFullYear().toString().substring(
				3, 4));
		var vYear = (vFormat.indexOf("yyyy") > -1 ? vYearLong : vYearShort);
		var vHour = this.addZero(vDate.getHours());
		var vMinute = this.addZero(vDate.getMinutes());
		var vSecond = this.addZero(vDate.getSeconds());
		var vTimeZone = this.O(vDate);
		var vDateString = vFormat.replace(/dd/g, vDay).replace(/MM/g, vMonth)
				.replace(/y{1,4}/g, vYear);
		vDateString = vDateString.replace(/hh/g, vHour).replace(/mm/g, vMinute)
				.replace(/ss/g, vSecond);
		vDateString = vDateString.replace(/O/g, vTimeZone);
		return vDateString;
	},
	addZero : function(vNumber) {
		return ((vNumber < 10) ? "0" : "") + vNumber;
	},
	O : function(date) {
		var os = Math.abs(date.getTimezoneOffset());
		var h = String(Math.floor(os / 60));
		var m = String(os % 60);
		h.length == 1 ? h = "0" + h : 1;
		m.length == 1 ? m = "0" + m : 1;
		return date.getTimezoneOffset() < 0 ? "+" + h + m : "-" + h + m;
	}
};
var log4jsLogger = Log4js.getLogger("Log4js");
log4jsLogger.addAppender(new Log4js.ConsoleAppender());
log4jsLogger.setLevel(Log4js.Level.ALL);
