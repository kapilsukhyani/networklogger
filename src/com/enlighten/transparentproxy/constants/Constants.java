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

	public static final int DEFAULT_SERVER_PORT = 8080;

	public static final String APP_OWNER = "user_id";
	public static final int PS_COMMAND_ID = 0x1;
	public static final String PS_COMMAND = "ps | grep com.enlighten.transparentproxy";

	public static final int IPTABLES_INIT_COMMAND_ID = 0x2;
	public static final String IPTABLES_INIT_COMMAND = "iptables -t nat -I OUTPUT -m owner --uid-owner [UID] -j ACCEPT";
	public static final String SYSTEM_LEVEL_FILTER_COMMAND = "iptables -t nat -A OUTPUT -p 6 --dport 443 -j DNAT --to 127.0.0.1:8080";
	public static final String Shared_Preferences = "app_preferences";
	public static final String IPTABLE_INITIATED = "iptable_initiated";

	public static final int IPTABLE_INITIATED_MESSAGE_ID = 0x1;
	public static final int SYSTEM_LEVEL_FILTER_ENABLED_MESSAGE_ID = 0x2;

	public static final String FAILURE_REASON = "failure_reason";

	public static final String FILTER_ENABLED = "filter_enabled";

	public static final String FILTER = "current_filter";

	
}
