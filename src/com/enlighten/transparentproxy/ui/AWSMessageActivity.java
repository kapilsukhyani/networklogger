package com.enlighten.transparentproxy.ui;



import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.enlighten.transparentproxy.R;
import com.enlighten.transparentproxy.app.AppLog;
import com.enlighten.transparentproxy.constants.Constants;

public class AWSMessageActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.message);
		
		setMessage();
		setTitle();
	}

	private void setTitle(){
		((TextView)findViewById(R.id.txtTitle)).setText(getString(R.string.message_title));
	}

	private void setMessage() {
		Bundle bundle = getIntent().getExtras();
		
		if(null != bundle){
			String message = bundle.getString(Constants.AWS_MESSAGE);
			
			AppLog.logInfo("AWS Message: " + message);
			
			((TextView)findViewById(R.id.txtMessage)).setText(message);
		}
		else{
			AppLog.logInfo("Activity has no extras!");
		}
	}
}
