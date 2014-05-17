package com.enlighten.transparentproxy;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.CommandCapture;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewDebug.CapturedViewProperty;

public class ConfigureLogger extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	private void generateCSR() throws IOException, TimeoutException, RootDeniedException {
CommandCapture cc = new CommandCapture(1, ""){
	
};

RootTools.getShell(true).add(cc);
	}

}
