package com.enlighten.transparentproxy.constants;

public class Constants {
	public static final String AWS_MESSAGE = "awsmessage";

	public static final String PREF_DIRECTORY_LISTING = "prefDirectotyListing";
	public static final String PREF_DIRECTORY = "prefDirectory";
	public static final String PREF_VIBRATE = "prefVibrate";
	public static final String PREF_PLAYSOUND = "prefPlaysound";
	public static final String PREF_RINGTONE = "prefMessageringtone";
	public static final String PREF_SERVER_PORT = "prefServerPort";
	public static final String UID = "[UID]";
	public static final String DESTINATION_IP = "[DESTINATION_IP]";
	public static final String OPENSSL_WORKING_DIRECTORY_PATH = "/data/data/com.enlighten.transparentproxy/files/openssl/";
	public static final String TRAFFIC_LOG_PATH = "/data/data/com.enlighten.transparentproxy/files/logs/";
	public static final String SYSTEM_CACERTS_PATH = "/system/etc/security/cacerts/";

	public static final int DEFAULT_SERVER_PORT = 8080;

	public static final String APP_OWNER = "user_id";
	public static final int PS_COMMAND_ID = 0x1;
	public static final String PS_COMMAND = "ps | grep com.enlighten.transparentproxy";

	public static final int IPTABLES_INIT_COMMAND_ID = 0x2;
	public static final String IPTABLES_INIT_COMMAND = "iptables -t nat -I OUTPUT -m owner --uid-owner [UID] -j ACCEPT";
	public static final String SYSTEM_LEVEL_FILTER_COMMAND = "iptables -t nat -A OUTPUT -p 6 --dport 443 -j DNAT --to 127.0.0.1:8080";
	// redirects tcp packets targeted to a system identified by [DESTINATION_IP]
	// and port 443 (https) from an app with uid identified by [UID] to
	// the service listening at 127.0.0.1:4443 i.e. socat in our case
	public static final String IPTABLES_APP_LEVEL_FILTER_COMMAND = "iptables -t nat -I OUTPUT -p 6 --dport 443 -d [DESTINATION_IP] -m owner --uid-owner [UID] -j DNAT --to 127.0.0.1:4443";
	public static final String IPTABLES_NAT_TABLE_CLEAR_COMMAND = "iptables -t nat -F";
	public static final String SOCAT_TRANSPARENT_PROXY_COMMAND = "socat -v openssl-listen:4443,reuseaddr,verify=0,cert="+OPENSSL_WORKING_DIRECTORY_PATH+"server.crt,key="+OPENSSL_WORKING_DIRECTORY_PATH+"server.key,cafile="+OPENSSL_WORKING_DIRECTORY_PATH+"ca.crt,debug,fork SYSTEM:'tee " +TRAFFIC_LOG_PATH+"requests.log | socat - openssl:[TARGET_SERVER]:443,verify=0,debug,capath="+SYSTEM_CACERTS_PATH+" | tee " +TRAFFIC_LOG_PATH+"responses.log'";

	// "openssl x509 -req -days 365 -in server.csr -CA ca.crt -CAkey ca.key -set_serial 01 -out server.crt"
	public static final String OPENSSL_CREATE_CERTIFICATE_COMMAND = "openssl x509 -req -days 365 -in "+OPENSSL_WORKING_DIRECTORY_PATH+"server.csr -CA "+OPENSSL_WORKING_DIRECTORY_PATH+"ca.crt -CAkey "+OPENSSL_WORKING_DIRECTORY_PATH+"ca.key -CAserial "+OPENSSL_WORKING_DIRECTORY_PATH+"serial.txt -out "+OPENSSL_WORKING_DIRECTORY_PATH+"server.crt";

	public static final String Shared_Preferences = "app_preferences";
	public static final String IPTABLE_INITIATED = "iptable_initiated";

	public static final int IPTABLE_INITIATED_MESSAGE_ID = 0x1;
	public static final int SYSTEM_LEVEL_FILTER_ENABLED_MESSAGE_ID = 0x2;
	public static final int IPTABLES_APP_LEVEL_FILTER_ENABLED_MESSAGE_ID = 0x3;
	public static final int SOCAT_STARTED_MESSAGE_ID = 0x4;

	public static final String FAILURE_REASON = "failure_reason";

	public static final String FILTER_ENABLED = "filter_enabled";

	public static final String FILTER = "current_filter";

	public static final String OPENSSL = "openssl";

	public static final String SOCAT = "socat";
}
