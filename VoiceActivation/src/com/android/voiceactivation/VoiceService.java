package com.android.voiceactivation;

import java.lang.ref.WeakReference;
import java.util.*;

import android.app.Service;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.ContactsContract;
import android.speech.*;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;


public class VoiceService extends Service{

	final static String APP_INTENT = "";
	final static String CONTACT_NUMBER = "";		
	final static int STOP_LISTENING = 0;
	final static int START_LISTENING = 1;
	
	volatile boolean startCountDown;  //volatile: allow other thread to access most recent var's result
	final Messenger voiceMessenger = new Messenger(new VoiceHandler(this));

	Vibrator vbrate;
	List<ResolveInfo> AppList;
    TextToSpeech textToSpeech;
    SpeechRecognizer speechRecognizer;
    String speechResult = "", voiceResult;
	  
    Context context;
    Intent speechIntent; 
    boolean isListening;
    SharedPreferences voiceData, contactsData;
    SharedPreferences.Editor voiceEditor, contactsEditor;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	@Override
	public void onCreate(){
		super.onCreate();
		
		context = this;
		voiceData = getSharedPreferences("VoiceData", Context.MODE_PRIVATE);
        contactsData = getSharedPreferences("ContactsData", Context.MODE_PRIVATE);
        voiceEditor = voiceData.edit();
        contactsEditor = contactsData.edit();
                
        vbrate = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
		SpeechRecognitionListener speechListener = new SpeechRecognitionListener();
        speechRecognizer.setRecognitionListener(speechListener);
		
		speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-EN");
        speechIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.android.voiceactivation");
        speechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {		   
 		    
        	@Override
            public void onInit(final int status) {
        		
        		new Thread(new Runnable() {
	          	   public void run() { 
	          	  	  if(status != TextToSpeech.ERROR) {
	                     textToSpeech.setLanguage(Locale.UK);
	                  }
	         	   } 
        		}).start();
          	}
 		});
	}
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		super.onStartCommand(intent, flags, startId);
		
		Toast.makeText(getApplicationContext(), "Voice Service On", Toast.LENGTH_SHORT).show();
		try{ 
            Message msg = new Message();
            msg.what = START_LISTENING; 
            voiceMessenger.send(msg);
        
        }catch(Exception ex){ 
        	ex.printStackTrace();
        }
		
		return START_STICKY;
	}
	
	
	/* Open Application that Matches Voice Request */
	@SuppressWarnings("deprecation")
	void OpenPreferredApp(String app){
		boolean foundApp = false;
		PackageManager packageManager = this.getPackageManager();
	    Intent intent = new Intent(Intent.ACTION_MAIN, null);
	    intent.addCategory(Intent.CATEGORY_LAUNCHER);
	    AppList = packageManager.queryIntentActivities(intent, PackageManager.PERMISSION_GRANTED);
	    
	    if(voiceData.getString(app, APP_INTENT) != APP_INTENT){
	       foundApp = true;
	       String appName = voiceData.getString(app, APP_INTENT);
	       textToSpeech.speak("Opening "+app, TextToSpeech.QUEUE_FLUSH, null);
	       
	       Intent launchApp = getPackageManager().getLaunchIntentForPackage(appName);
	       launchApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		   startActivity(launchApp);
	    	
	    }else{
	       for(ResolveInfo resolveInfo : AppList){ 
			   String appName = resolveInfo.activityInfo.applicationInfo.loadLabel(packageManager).toString();
			   appName = appName.toLowerCase(Locale.getDefault());
			   
		       if(app.replaceAll("\\s+","").equalsIgnoreCase(appName.replaceAll("\\s+","")) || appName.matches(".*"+app+".*")){     
		    	  foundApp = true;
		          textToSpeech.speak("Opening "+appName, TextToSpeech.QUEUE_FLUSH, null);
		          voiceEditor.putString(app, resolveInfo.activityInfo.packageName); //Ex: (VoiceCommand: domain, AppName: Domain)
		          voiceEditor.commit();
		          
		          Intent launchApp = getPackageManager().getLaunchIntentForPackage(resolveInfo.activityInfo.packageName);
		          launchApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		 	      startActivity(launchApp);	          
		 	      break;		 	      
			   }
		   }
	    }
	    
	    if(!foundApp)
	       Toast.makeText(this, "Please Try Again", Toast.LENGTH_SHORT).show();
	}
	
	
	/* Call Specified Person that Matches Voice Request */
	void CallContact(String contact){
		boolean foundContact = false;
		contact = contact.toLowerCase(Locale.getDefault());
		ContentResolver contactsContent = getContentResolver();
		Cursor cursor = contactsContent.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null); 
		
		if(contactsData.getString(contact, CONTACT_NUMBER) != CONTACT_NUMBER){
		   foundContact = true;
		   String phoneNumber  = contactsData.getString(contact, CONTACT_NUMBER);
		   Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"+phoneNumber));
		   callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	       startActivity(callIntent);
		}
		
		else if(cursor.getCount() != 0){
		   while(cursor.moveToNext()){
		      String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
		      contactName = contactName.toLowerCase(Locale.getDefault());
		      
		      //If Voiced name Matches ContactsList, Call Specific Person
		      if(contact.contains(contactName) || contactName.matches(".*\\b"+contact+"\\b.*")){
		    	 foundContact = true;
		    	 String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
		    	 contactsEditor.putString(contact, phoneNumber);
		    	 contactsEditor.commit();
		    	 
		         Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"+phoneNumber));
		         callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		         startActivity(callIntent);
		         cursor.close();
		         break;	         
		      }
		   }
		}
		
		if(!foundContact)
		   Toast.makeText(this, "Please Try Again", Toast.LENGTH_SHORT).show();
	}
	
	
	/* Refresh Voice Recognition Interval Every few Seconds */ 
	CountDownTimer timer = new CountDownTimer(5000, 5000){ 

        @Override 
        public void onTick(long millisUntilFinished){ 
            // TODO Auto-generated method stub 
        } 

        @Override 
        public void onFinish(){ 
            startCountDown = false;
            Message stopVoiceMsg = Message.obtain(null, STOP_LISTENING);
                      
            try{
               voiceMessenger.send(stopVoiceMsg);
               Message startVoiceMsg = Message.obtain(null, START_LISTENING);
               voiceMessenger.send(startVoiceMsg);
               
            }catch(Exception ex){
            	ex.printStackTrace();
            }
        } 
    }; 
	
    
    /* Voice Recognition Thread Handler for Speech Listening */
    static class VoiceHandler extends Handler {
    	
    	//Put object in Garbage Collector when Object returns null
    	WeakReference<VoiceService> voiceRef;  
    	
    	public VoiceHandler(VoiceService voiceService){
    		voiceRef = new WeakReference<VoiceService>(voiceService);
    	}
    	   	
    	@Override
    	public void handleMessage(Message message){
    		VoiceService context = voiceRef.get();
    		
    		if(message.what == START_LISTENING)
    		   context.speechRecognizer.startListening(context.speechIntent);
    		
    		else if(message.what == STOP_LISTENING)
    		   context.speechRecognizer.cancel(); 		
    	}
    }
    
    
    /* Use Voice Recognition; Hide Google Voice Notification */
	class SpeechRecognitionListener implements RecognitionListener { 
	 
		@Override 
		/* Stop CountDown When Speaking */
	    public void onBeginningOfSpeech(){                
	         
			if(startCountDown){
			   startCountDown = false;
			   timer.cancel();
			}
	    } 
	 
	    @Override 
	    public void onBufferReceived(byte[] buffer){ 
	 
	    } 
	 
	    @Override 
	    /* Start Timer Again When Finished Speaking */
	    public void onEndOfSpeech(){  	  
	    	timer.start();
	    } 
	 
	    @Override 
	    public void onError(int error){ 
	    	
	    	//Keep going Reset Timer; Try Again
	    	if((error == SpeechRecognizer.ERROR_NO_MATCH) || (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)){ 
	            timer.start();
	    	}
	    } 
	 
	    @Override 
	    public void onEvent(int eventType, Bundle params){ 
	 
	    } 
	 
	    @Override 
	    public void onPartialResults(Bundle partialResults){ 
	 
	    } 
	 
	    @Override 
	    public void onReadyForSpeech(Bundle params){ 
	        
	    	startCountDown = true;
	    	timer.start();
	    	//vbrate.vibrate(17);
		Toast.makeText(getApplicationContext(), "Ready For Speech", Toast.LENGTH_SHORT).show();
	    } 
	 
	    @Override 
	    public void onResults(Bundle results){ 
	    	ArrayList<String> result = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
	        
	        if(result == null){
	           timer.start();
	           return;
	        }
	        
	        if(result != null){	                
		       speechResult = result.get(0);
			   
			   //Check For 'Open' Keyword
			   if(speechResult.contains("open")){
			      speechResult = speechResult.replace("open ", "");
					  
			      if(speechResult != "")
			         OpenPreferredApp(speechResult);
			   }
				
			   //Check For 'Call' Keyword
			   if(speechResult.contains("call")){
			      speechResult = speechResult.replace("call ", "");
				   
			      if(speechResult != "")
			    	 CallContact(speechResult);
			   }
	        }
		}
	 
	    @Override 
	    public void onRmsChanged(float rmsdB){ 
	    	
	    } 
	}
}
