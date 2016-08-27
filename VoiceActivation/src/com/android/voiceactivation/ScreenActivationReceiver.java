package com.android.voiceactivation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ScreenActivationReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		
		if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) || intent.getAction().equals(Intent.ACTION_USER_PRESENT) || intent.getAction().equals(Intent.ACTION_TIME_TICK)){
           Intent screenStateIntent = new Intent(context, ScreenStateService.class);
           context.startService(screenStateIntent);
        }	
	}
}
