package com.sasteysaudey;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class MainActivity extends Activity {

	 public static final String EXTRA_MESSAGE = "message";
	    public static final String PROPERTY_REG_ID = "registration_id";
	    private static final String PROPERTY_APP_VERSION = "appVersion";
	    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

	    /**
	     * Substitute you own sender ID here. This is the project number you got
	     * from the API Console, as described in "Getting Started."
	     */
	    String SENDER_ID = "949315846489";

	    /**
	     * Tag used on log messages.
	     */
	    static final String TAG = "GCM Demo";

	    TextView mDisplay;
	    GoogleCloudMessaging gcm;
	    AtomicInteger msgId = new AtomicInteger();
	    Context context;

	    String regid;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mDisplay = (TextView) findViewById(R.id.display);

	    context = getApplicationContext();

	    checkConnection();
	        
	}
	
	private void checkConnection() {
		ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

		boolean is3g = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
		            .isConnectedOrConnecting();
		boolean isWifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
		            .isConnectedOrConnecting();

		if (is3g || isWifi) { 
			try{
				// Check device for Play Services APK. If check succeeds, proceed with GCM registration.
		        if (checkPlayServices()) {
		        	//Toast.makeText(getApplicationContext(), "Check Play service ",Toast.LENGTH_SHORT).show();
		            gcm = GoogleCloudMessaging.getInstance(this);
		            regid = getRegistrationId(context);

		            if (regid.isEmpty()) {
		            	//Toast.makeText(getApplicationContext(), "register In Background",Toast.LENGTH_SHORT).show();
		            	registerInBackground();
		            }
		            else{
		            	registerInBackground();
		            	Toast.makeText(getApplicationContext(), "Device already Registered", Toast.LENGTH_SHORT).show();
		            }
		        } else {
		            Log.i(TAG, "No valid Google Play Services APK found.");
		        }
			}finally{
				onDestroy();
			}
		} 
		else{
			Toast.makeText(getApplicationContext(), "Check your internet connection",Toast.LENGTH_SHORT).show();
			onDestroy();
		}
		
	}

	 	@Override
	    protected void onResume() {
	        super.onResume();
	        // Check device for Play Services APK.
	        checkPlayServices();
	    }

	    /**
	     * Check the device to make sure it has the Google Play Services APK. If
	     * it doesn't, display a dialog that allows users to download the APK from
	     * the Google Play Store or enable it in the device's system settings.
	     */
	    private boolean checkPlayServices() {
	        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
	        if (resultCode != ConnectionResult.SUCCESS) {
	            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
	                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
	                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
	            } else {
	                Log.i(TAG, "This device is not supported.");
	                finish();
	            }
	            return false;
	        }
	        return true;
	    }
	    
	    /**
	     * Stores the registration ID and the app versionCode in the application's
	     * {@code SharedPreferences}.
	     *
	     * @param context application's context.
	     * @param regId registration ID
	     */
	    private void storeRegistrationId(Context context, String regId) {
	        final SharedPreferences prefs = getGcmPreferences(context);
	        int appVersion = getAppVersion(context);
	        Log.i(TAG, "Saving regId on app version " + appVersion);
	        SharedPreferences.Editor editor = prefs.edit();
	        editor.putString(PROPERTY_REG_ID, regId);
	        editor.putInt(PROPERTY_APP_VERSION, appVersion);
	        editor.commit();
	    }
	    
	    /**
	     * Gets the current registration ID for application on GCM service, if there is one.
	     * <p>
	     * If result is empty, the app needs to register.
	     *
	     * @return registration ID, or empty string if there is no existing
	     *         registration ID.
	     */
	    private String getRegistrationId(Context context) {
	        final SharedPreferences prefs = getGcmPreferences(context);
	        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
	        if (registrationId.isEmpty()) {
	            Log.i(TAG, "Registration not found.");
	            return "";
	        }
	        // Check if app was updated; if so, it must clear the registration ID
	        // since the existing regID is not guaranteed to work with the new
	        // app version.
	        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
	        int currentVersion = getAppVersion(context);
	        if (registeredVersion != currentVersion) {
	            Log.i(TAG, "App version changed.");
	            return "";
	        }
	        return registrationId;
	    }
	    
	    /**
	     * Registers the application with GCM servers asynchronously.
	     * <p>
	     * Stores the registration ID and the app versionCode in the application's
	     * shared preferences.
	     */
	    private void registerInBackground() {
	    	
	    	Toast.makeText(getApplicationContext(), "In register Background ",Toast.LENGTH_SHORT).show();
            gcm = GoogleCloudMessaging.getInstance(this);
	    	
	        new AsyncTask<Void, Void, String>() {
	            @Override
	            protected String doInBackground(Void... params) {
	                String msg = "";
	                try {
	                    if (gcm == null) {
	                        gcm = GoogleCloudMessaging.getInstance(context);
	                    }
	                    regid = gcm.register(SENDER_ID);
	                    msg = "Device registered, registration ID=" + regid;

	                    // You should send the registration ID to your server over HTTP, so it
	                    // can use HTTP to send messages to your app.
	                    sendRegistrationIdToBackend();


	                    // Persist the regID - no need to register again.
	                    storeRegistrationId(context, regid);
	                } catch (IOException ex) {
	                    msg = "Error :" + ex.getMessage();
	                    // If there is an error, don't just keep trying to register.
	                    // Require the user to click a button again, or perform
	                    // exponential back-off.
	                }
	                return msg;
	            }

	            @Override
	            protected void onPostExecute(String msg) {
	                mDisplay.append(msg + "\n");
	                showToast();
	            }

	        }.execute(null, null, null);
	    }

	    protected void showToast() {
			// TODO Auto-generated method stub
			Toast.makeText(getApplicationContext(), "This is regId "+regid, Toast.LENGTH_SHORT).show();
		}

	    
	    // Send an upstream message.
	    public void onClick(final View view) {

	        if (view == findViewById(R.id.send)) {
	            new AsyncTask<Void, Void, String>() {
	                @Override
	                protected String doInBackground(Void... params) {
	                    String msg = "";
	                    try {
	                        Bundle data = new Bundle();
	                        data.putString("my_message", "Hello World");
	                        data.putString("my_action", "com.google.android.gcm.demo.app.ECHO_NOW");
	                        String id = Integer.toString(msgId.incrementAndGet());
	                        gcm.send(SENDER_ID + "@gcm.googleapis.com", id, data);
	                        msg = "Sent message";
	                    } catch (IOException ex) {
	                        msg = "Error :" + ex.getMessage();
	                    }
	                    return msg;
	                }

	                @Override
	                protected void onPostExecute(String msg) {
	                    mDisplay.append(msg + "\n");
	                }
	            }.execute(null, null, null);
	        } else if (view == findViewById(R.id.clear)) {
	            mDisplay.setText("");
	        }
	    }

	    @Override
	    protected void onDestroy() {
	        super.onDestroy();
	    }

	    /**
	     * @return Application's version code from the {@code PackageManager}.
	     */
	    private static int getAppVersion(Context context) {
	        try {
	            PackageInfo packageInfo = context.getPackageManager()
	                    .getPackageInfo(context.getPackageName(), 0);
	            return packageInfo.versionCode;
	        } catch (NameNotFoundException e) {
	            // should never happen
	            throw new RuntimeException("Could not get package name: " + e);
	        }
	    }

	    /**
	     * @return Application's {@code SharedPreferences}.
	     */
	    private SharedPreferences getGcmPreferences(Context context) {
	        //  persisting the registration ID in shared preferences
	        return getSharedPreferences(MainActivity.class.getSimpleName(),
	                Context.MODE_PRIVATE);
	    }
	    /**
	     * Sends the registration ID to your server over HTTP, so it can use HTTP to send
	     * messages to your device.
	     */
	    
	    public void sendRegistrationIdToBackend() {
	      
	    	HttpURLConnection connection;
	        OutputStreamWriter request = null;

	             URL url = null;   
	             String response = null;    
	             String parameters = "regId="+regid;   

	             try
	             {
	                 url = new URL("http://sasteysaudey.esy.es/register.php");
	                 connection = (HttpURLConnection) url.openConnection();
	                 connection.setDoOutput(true);
	                 connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	                 connection.setRequestMethod("POST");    

	                 request = new OutputStreamWriter(connection.getOutputStream());
	                 request.write(parameters);
	                 request.flush();
	                 request.close();            
	                 String line = "";               
	                 InputStreamReader isr = new InputStreamReader(connection.getInputStream());
	                 BufferedReader reader = new BufferedReader(isr);
	                 StringBuilder sb = new StringBuilder();
	                 while ((line = reader.readLine()) != null)
	                 {
	                     sb.append(line + "\n");
	                 }
	                 // Response from server after login process will be stored in response variable.                
	                 response = sb.toString();
	                 // You can perform UI operations here
	                 //Toast.makeText(this,"Message from Server: \n"+ response, 0).show();     
	                 Log.d(" REG ID ",regid);
	                 Log.d("Response string ",response);
	                 isr.close();
	                 reader.close();

	             }
	             catch(IOException e)
	             {
	                 // Error
	             }
	    }

		// Define the Handler that receives messages from the thread and update the progress
        private final Handler handler = new Handler() {
            public void handleMessage(Message msg) {
         
                String aResponse = msg.getData().getString("message");
                if ((null != aResponse)) {
                    // ALERT MESSAGE
                    Toast.makeText(getBaseContext(),"Server Response: "+aResponse,Toast.LENGTH_SHORT).show();
                }
                else{
                        // ALERT MESSAGE
                        Toast.makeText(getBaseContext(),"Not Got Response From Server.",Toast.LENGTH_SHORT).show();
                }    
            }
        };
	    
}
