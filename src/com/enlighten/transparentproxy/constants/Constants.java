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
	public static final String DOMAIN_NAME = "[DOMAIN_NAME]";

	public static final String Shared_Preferences = "app_preferences";
	public static final String IPTABLE_INITIATED = "iptable_initiated";
	public static final String PREVIOUS_DOMAIN_NAMES = "domain_names";
	public static final String APPLICAITON_INITIALIZED = "applciation_initialized";
	public static final String APP_INFO = "app_info";
	public static final String DOMAIN_NAME_PARAM = "domain_name";
	public static final String CALLBACK = "callback";

	public static final int STARTED_INTERCEPTING = 0x1;
	public static final int STOPPED_INTERCEPTING = 0x2;
	public static final int INVALID_DOMAIN_NAME = 0x3;
	public static final int ERROR_WHILE_RUNNING = 0x4;

	public static final String FAILURE_REASON = "failure_reason";

	public static final String FILTER_ENABLED = "filter_enabled";

	public static final String FILTER = "current_filter";

	public static final String OPENSSL = "openssl";

	public static final String SOCAT = "socat";

	public static final String SYSTEM_CACERTS_PATH = "/system/etc/security/cacerts/";
	public static String OPENSSL_WORKING_DIRECTORY_PATH;
	public static String TRAFFIC_LOG_PATH;
	public static String CA_CERT_FILE_PATH;
	public static String CA_KEY_FILE_PATH;
	public static String SERVER_CSR_FILE_PATH;
	public static String SERVER_KEY_FILE_PATH;
	public static String SERVER_CERT_FILE_PATH;
	public static String SERIAL_FILE_PATH;

	public static String REQUESTS_FILE_PATH;
	public static String RESPONSES_FILE_PATH;

	public static String PATH_SEPARATOR = "/";

	public static final String CA_FILE_NAME = "ca.crt";
	public static final String CA_KEY_FILE_NAME = "cauth.key";
	public static final String SERVER_CSR_FILE_NAME = "server.csr";
	public static final String SERVER_KEY_FILE_NAME = "server.key";
	public static final String SERVER_CERT_FILE_NAME = "server.crt";
	public static final String REQUESTS_FILE_NAME = "requests.log";
	public static final String RESPONSES_FILE_NAME = "responses.log";
	public static final String SERIAL_FILE_NAME = "serial.txt";

	public static final int PS_COMMAND_ID = 0x1;
	public static final int IPTABLES_INIT_COMMAND_ID = 0x2;
	public static final int IPTABLES_NAT_CLEAR_COMMAND_ID = 0x3;
	public static final int IPTABLES_APP_LEVEL_FILTER_COMMAND_ID = 0x4;
	public static final int GENERATE_FAKE_SERVER_CSR_COMAND_ID = 0x5;
	public static final int GENERATE_FAKE_SERVER_CRT_COMMAND_ID = 0x6;
	public static final int CONFIGURE_SOCAT_COMMAND_ID = 0x7;
	public static final int KILL_SOCAT_COMMAND_ID = 0x8;
	public static final int STOP_HACKING_COMMAND_ID = 0x9;

	public static final String APP_OWNER = "user_id";

	public static final String PS_COMMAND = "ps | grep com.enlighten.transparentproxy";
	public static final String IPTABLES_INIT_COMMAND = "iptables -t nat -I OUTPUT -m owner --uid-owner [UID] -j ACCEPT";
	public static final String SYSTEM_LEVEL_FILTER_COMMAND = "iptables -t nat -A OUTPUT -p 6 --dport 443 -j DNAT --to 127.0.0.1:8080";
	// redirects tcp packets targeted to a system identified by [DESTINATION_IP]
	// and port 443 (https) from an app with uid identified by [UID] to
	// the service listening at 127.0.0.1:4443 i.e. socat in our case
	public static final String IPTABLES_APP_LEVEL_FILTER_COMMAND = "iptables -t nat -I OUTPUT -p 6 --dport 443 -d [DESTINATION_IP] -m owner --uid-owner [UID] -j DNAT --to 127.0.0.1:4443";
	public static final String IPTABLES_NAT_TABLE_CLEAR_COMMAND = "iptables -t nat -F OUTPUT";

	public static final int SOCAT_COMMAND_TIMEOUT = 120000;
	public static final String SOCAT_KILL_COMMAND = "pkill socat";

	public static String SOCAT_TRANSPARENT_PROXY_COMMAND;

	// "openssl x509 -req -days 365 -in server.csr -CA ca.crt -CAkey ca.key -set_serial 01 -out server.crt"
	public static String OPENSSL_CREATE_CERTIFICATE_COMMAND;
	public static String OPENSSL_CREATE_SERVER_CSR_COMMAND;

	public static final void initPaths(String internalFileDirPath) {
		OPENSSL_WORKING_DIRECTORY_PATH = internalFileDirPath + PATH_SEPARATOR
				+ OPENSSL;
		CA_CERT_FILE_PATH = OPENSSL_WORKING_DIRECTORY_PATH + PATH_SEPARATOR
				+ CA_FILE_NAME;
		CA_KEY_FILE_PATH = OPENSSL_WORKING_DIRECTORY_PATH + PATH_SEPARATOR
				+ CA_KEY_FILE_NAME;
		SERVER_CSR_FILE_PATH = OPENSSL_WORKING_DIRECTORY_PATH + PATH_SEPARATOR
				+ SERVER_CSR_FILE_NAME;
		SERVER_KEY_FILE_PATH = OPENSSL_WORKING_DIRECTORY_PATH + PATH_SEPARATOR
				+ SERVER_KEY_FILE_NAME;
		SERVER_CERT_FILE_PATH = OPENSSL_WORKING_DIRECTORY_PATH + PATH_SEPARATOR
				+ SERVER_CERT_FILE_NAME;
		SERIAL_FILE_PATH = OPENSSL_WORKING_DIRECTORY_PATH + PATH_SEPARATOR
				+ SERIAL_FILE_NAME;

		TRAFFIC_LOG_PATH = internalFileDirPath + PATH_SEPARATOR + "logs";
		REQUESTS_FILE_PATH = TRAFFIC_LOG_PATH + PATH_SEPARATOR
				+ REQUESTS_FILE_NAME;
		RESPONSES_FILE_PATH = TRAFFIC_LOG_PATH + PATH_SEPARATOR
				+ RESPONSES_FILE_NAME;

		initCommands();

	}

	private static void initCommands() {
		SOCAT_TRANSPARENT_PROXY_COMMAND = "socat -v openssl-listen:4443,reuseaddr,verify=0,cert="
				+ SERVER_CERT_FILE_PATH
				+ ",key="
				+ SERVER_KEY_FILE_PATH
				+ ",cafile="
				+ CA_CERT_FILE_PATH
				+ ",debug,fork SYSTEM:'tee "
				+ REQUESTS_FILE_PATH
				+ " | socat - \"openssl:[DOMAIN_NAME]:443,verify=0,debug,capath="
				+ SYSTEM_CACERTS_PATH + "\" | tee " + RESPONSES_FILE_PATH + "'";

		// this comand will not ask for ca key password as non secure(password
		// less key) is used
		OPENSSL_CREATE_CERTIFICATE_COMMAND = "openssl x509 -req -days 365 -in "
				+ SERVER_CSR_FILE_PATH + " -CA " + CA_CERT_FILE_PATH
				+ " -CAkey " + CA_KEY_FILE_PATH + " -CAserial "
				+ SERIAL_FILE_PATH + " -out " + SERVER_CERT_FILE_PATH;

		// this command would not ask for anything from user before creating csr
		// as everything is provided in the command as well
		// to kepp it not interactive and the private key used itself is a non
		// secure key(without any password)
		OPENSSL_CREATE_SERVER_CSR_COMMAND = "openssl req -new -key "
				+ SERVER_KEY_FILE_PATH
				+ " -out "
				+ SERVER_CSR_FILE_PATH
				+ " -subj '/C=IN/ST=MAHA/L=PUNE/O=Enlighten/OU=Development/CN=[DOMAIN_NAME]'";

	}
}
