package com.example.coapshowlocationinmap;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ws4d.coap.Constants;
import org.ws4d.coap.connection.BasicCoapChannelManager;
import org.ws4d.coap.interfaces.CoapChannelManager;
import org.ws4d.coap.interfaces.CoapClient;
import org.ws4d.coap.interfaces.CoapClientChannel;
import org.ws4d.coap.interfaces.CoapRequest;
import org.ws4d.coap.interfaces.CoapResponse;
import org.ws4d.coap.messages.CoapRequestCode;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings.Secure;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends FragmentActivity implements CoapClient {

	Intent intent;
	
	private Handler uiHandler;
	
	private String deviceID;
	
	Timer timer;
	
	Marker current;
	
	JSONArray locationValues;
	
	private static final String SERVER_ADDRESS = "172.17.77.100";
    private static final int PORT = Constants.COAP_DEFAULT_PORT;
    static int counter = 0;
    CoapChannelManager channelManager = null;
    CoapClientChannel clientChannel = null;
    
    GoogleMap map;
    
    static final LatLng HAMBURG = new LatLng(53.558, 9.927);
    static final LatLng KIEL = new LatLng(53.551, 9.993);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		uiHandler = new Handler();
		
		intent = new Intent(this, LocationUpdateService.class);
		startService(intent);
		
		deviceID = Secure.getString(getBaseContext().getContentResolver(),
                Secure.ANDROID_ID);
    	Log.i("Device ID", deviceID);
		
    	
		
		
//		GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
		
		map = ((SupportMapFragment)  getSupportFragmentManager().findFragmentById(R.id.map))
	               .getMap();
		
		 
		
		timer = new Timer();
		
		timer.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				getData();
				
			}
		}, 3000, 1000);
		
	}
	
	public void getData(){
		try{
    		channelManager = BasicCoapChannelManager.getInstance();
    		
    		clientChannel = channelManager.connect(this, InetAddress.getByName(SERVER_ADDRESS), PORT);
    		
			CoapRequest coapRequest = clientChannel.createRequest(true, CoapRequestCode.GET);
			
			//coapRequest.setObserveOption(sequenceNumber);
			
			coapRequest.setUriPath("/devices/"+deviceID+"/sensors");
			
			//coapRequest.setObserveOption(1);
			
			clientChannel.sendMessage(coapRequest);
			
			
			System.out.println("Sent Request");
    	}
    	catch (UnknownHostException e) {
    		e.printStackTrace();
		}
	}
	
	public void onResponse(CoapClientChannel channel, CoapResponse response) {
		Log.i("Response Received", new String(response.getPayload()));
		if(!"Resource does not exist".equalsIgnoreCase(new String(response.getPayload()))){
			try{
				JSONObject responseObject = new JSONObject(new String(response.getPayload()));
				JSONObject sensorReadings = (JSONObject)responseObject.get("sensorReadings");
				JSONObject readings = (JSONObject)sensorReadings.get("readings");
				locationValues = (JSONArray)readings.get("values");
				System.out.println("JSONArray:::"+locationValues.toString());
				
				uiHandler.post(new Runnable() {
				      @Override
				      public void run() {
				        // This is run on the UI thread.
				    	  Toast.makeText(getApplicationContext(), "Location Tracked", Toast.LENGTH_SHORT).show();
				    	  try{
				    		  final LatLng CURRENT = new LatLng(locationValues.getDouble(0), locationValues.getDouble(1));
				    		  if(null != current){
				    			  current.remove();
				    		  }
				    		  current = map.addMarker(new MarkerOptions()
						        .position(CURRENT)
						        .title("My Location")
						        .snippet("This is cool"));

							    // Move the camera instantly to hamburg with a zoom of 15.
							    map.moveCamera(CameraUpdateFactory.newLatLngZoom(CURRENT, 15));
			
							    // Zoom in, animating the camera.
							    //map.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);
				    	  }
				    	  catch (JSONException e) {
							// TODO: handle exception
				    		  e.printStackTrace();
						}
				    	  
				    	  
				      }
				    });
				
				
				
			}catch (JSONException e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
		else{
			uiHandler.post(new Runnable() {
				@Override
			    public void run() {
					Toast.makeText(getApplicationContext(), "No Location found", Toast.LENGTH_SHORT).show();
				}
			});
		}
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		stopService(intent);
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		stopService(intent);
	}

	@Override
	public void onConnectionFailed(CoapClientChannel channel,
			boolean notReachable, boolean resetByServer) {
		// TODO Auto-generated method stub
		
	}

}
