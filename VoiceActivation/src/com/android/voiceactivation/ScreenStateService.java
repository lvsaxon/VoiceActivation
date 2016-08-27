package com.android.voiceactivation;

import android.app.Service;
import android.content.*;
import android.os.IBinder;
import android.os.PowerManager;


public class ScreenStateService extends Service{

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	@Override
	@SuppressWarnings("deprecation")
	public void onCreate() {
		super.onCreate();
		
		Intent voiceServiceIntent = new Intent(this, VoiceService.class);
		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
		
	    if(powerManager.isScreenOn())
	       startService(new Intent(voiceServiceIntent));    
	    else
	       stopService(new Intent(voiceServiceIntent));
	    
		/*IntentFilter screenIntentFilter = new IntentFilter();
		screenIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
		screenIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(screenReceiver, screenIntentFilter);*/
	}
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		super.onStartCommand(intent, flags, startId);
		
		return START_STICKY;
	}
	
	
	/* Restart Service When Stopped or Terminated */
	@Override	
	public void onDestroy(){
		super.onDestroy();
		
		Intent screenStateService = new Intent(this, ScreenStateService.class);
        startService(screenStateService);
	}
}
