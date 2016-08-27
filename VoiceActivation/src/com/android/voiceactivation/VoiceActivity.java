package com.android.voiceactivation;

import android.app.Activity;
import android.content.Intent;
import android.os.*;

public class VoiceActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent screenIntent = new Intent(this, ScreenStateService.class);
	    startService(new Intent(screenIntent));
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		
		Intent screenIntent = new Intent(this, ScreenStateService.class);
	    startService(new Intent(screenIntent));
	}
}
