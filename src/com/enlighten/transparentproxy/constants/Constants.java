package com.enlighten.transparentproxy.constants;

public class Constants {
	public static final String AWS_MESSAGE = "awsmessage";

	public static final String PREF_DIRECTORY_LISTING = "prefDirectotyListing";
	public static final String PREF_DIRECTORY = "prefDirectory";
	public static final String PREF_VIBRATE = "prefVibrate";
	public static final String PREF_PLAYSOUND = "prefPlaysound";
	public static final String PREF_RINGTONE = "prefMessageringtone";
	public static final String PREF_SERVER_PORT = "prefServerPort";
	public static final String UID = "[uid]";

	public static final int DEFAULT_SERVER_PORT = 8080;

	public static final String APP_OWNER = "user_id";
	public static final int PS_COMMAND_ID = 0x1;
	public static final String PS_COMMAND = "ps | grep com.enlighten.transparentproxy";
	
	public static final int IPTABLES_INIT_COMMAND_ID = 0x2;
	public static final String IPTABLES_INIT_COMMAND = "iptables -t nat -I OUTPUT -m owner --uid-owner [UID] -j ACCEPT";
}
