Components.utils.import("resource://stmodules/STRequests.jsm");
Components.utils.import("resource://stmodules/Logger.jsm");
Components.utils.import("resource://stmodules/STHttpMgrFactory.jsm");
Components.utils.import("resource://stmodules/Context.jsm");
Components.utils.import("resource://stmodules/STInfo.jsm");

EXPORTED_SYMBOLS = [ "STRequests" ];

var service = STRequests.XMLSchema;
var serviceName = service.serviceName;

/**
 * Compose the date in standard format yyyy-MM-ddThh:mm:ss
 * @param year
 * @param month
 * @param day
 * @param hour
 * @param minute
 * @param second
 * @param offset Optional
 * @returns
 */
function formatDateTime(year, month, day, hour, minute, second, offset) {
	Logger.debug('[SERVICE_XMLSchema.jsm] formatDateTime');
	var p_year = "year="+year;
	var p_month = "month="+month;
	var p_day = "day="+day;
	var p_hour = "hour="+hour;
	var p_minute = "minute="+minute;
	var p_second = "second="+second;
	if (typeof offset != 'undefined'){
		var p_offset = "offset="+offset;
	} else {
		var p_offset = "offset=";
	}
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.formatDateTimeRequest, this.context,
			p_year, p_month, p_day, p_hour, p_minute, p_second, p_offset);
}

function formatDate(year, month, day) {
	Logger.debug('[SERVICE_XMLSchema.jsm] formatDate');
	var p_year = "year="+year;
	var p_month = "month="+month;
	var p_day = "day="+day;
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.formatDateRequest, this.context, p_year, p_month, p_day);
}

function formatTime(hour, minute, second){
	Logger.debug('[SERVICE_XMLSchema.jsm] formatTime');
	var p_hour = "hour="+hour;
	var p_minute = "minute="+minute;
	var p_second = "second="+second;
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.formatTimeRequest, this.context, p_hour, p_minute, p_second);
}

function formatDuration(isPositive, year, month, day, hour, minute, second){
	Logger.debug('[SERVICE_XMLSchema.jsm] formatDuration');
	var p_isPositive = "isPositive="+isPositive;
	var p_year = "year="+year;
	var p_month = "month="+month;
	var p_day = "day="+day;
	var p_hour = "hour="+hour;
	var p_minute = "minute="+minute;
	var p_second = "second="+second;
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.formatDurationRequest, this.context,
			p_isPositive, p_year, p_month, p_day, p_hour, p_minute, p_second);
}

function formatCurrentLocalDateTime(){
	Logger.debug('[SERVICE_XMLSchema.jsm] formatCurrentLocalDateTime');
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.formatCurrentLocalDateTimeRequest, this.context);
}

function formatCurrentUTCDateTime(){
	Logger.debug('[SERVICE_XMLSchema.jsm] formatCurrentUTCDateTime');
	var currentSTHttpMgr = STHttpMgrFactory.getInstance(STInfo.getGroupId(), STInfo.getArtifactId());
	return currentSTHttpMgr.GET(null, serviceName, service.formatCurrentUTCDateTimeRequest, this.context);
}

service.prototype.formatDateTime = formatDateTime;
service.prototype.formatDate = formatDate;
service.prototype.formatTime = formatTime;
service.prototype.formatDuration = formatDuration;
service.prototype.formatCurrentLocalDateTime = formatCurrentLocalDateTime;
service.prototype.formatCurrentUTCDateTime = formatCurrentUTCDateTime;
service.prototype.context = new Context();  // set the default context
service.constructor = service;
service.__proto__ = service.prototype;
