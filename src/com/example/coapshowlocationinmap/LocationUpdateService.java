package com.example.coapshowlocationinmap;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

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
import org.ws4d.coap.messages.CoapMediaType;
import org.ws4d.coap.messages.CoapRequestCode;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings.Secure;
import android.util.Log;

public class LocationUpdateService extends Service implements CoapClient, LocationListener {

	
/* Sensor reading holders */
	
	//private String[] READ_ARRAY;
	
	/* Data store for sensor reading */
	private JSONObject jsonLocationReadingStore;
	
	/* Timer */
	Long startTime;
	
	private String deviceID;
	
	
	/*
	 * Location
	 */
	
	private LocationManager locationManager;
	
	private static final long MINIMUM_DISTANCE_CHANGE_FOR_UPDATES = 1; // in Meters
    private static final long MINIMUM_TIME_BETWEEN_UPDATES = 1000; // in Milliseconds
	
	
	
	private static final String SERVER_ADDRESS = "172.17.77.100";
	//private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = Constants.COAP_DEFAULT_PORT;
    static int counter = 0;
    CoapChannelManager channelManager = null;
    CoapClientChannel clientChannel = null;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	
	public int onStartCommand(Intent intent, int flags, int startId) {
	    Log.i("Service", "Running");
	    System.out.println("Service Running");
	    
	    deviceID = Secure.getString(getBaseContext().getContentResolver(),
                Secure.ANDROID_ID);
    	Log.i("Device ID", deviceID);
    	
    	try{
    		jsonLocationReadingStore = new JSONObject().put("sensor", "location");
    	}
	    catch(JSONException e){
	    	e.printStackTrace();
	    }
    	
    	
	    locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		//locationListener = new GPSLocationListener();
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MINIMUM_TIME_BETWEEN_UPDATES, MINIMUM_DISTANCE_CHANGE_FOR_UPDATES, this);
	    
	    return START_STICKY;
	}
	
	
	/*
	 * 
	 * Fetches sampled data into final JSON Object for sending
	 */
	private void fetchCurrentReadingIntoJsonStructure(int sensorType, JSONArray jsonInputArray){
		try{
			JSONObject jsonObjCurrentReading = new JSONObject();
			jsonObjCurrentReading.put("time", ""+getCurrentTimeInMilSec());
			jsonObjCurrentReading.put("values", jsonInputArray);
			//jsonSensorReadingsStore[sensorType].accumulate("readings", jsonObjCurrentReading);
			jsonLocationReadingStore.accumulate("readings", jsonObjCurrentReading);
		}
		catch (JSONException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	/*
	 * 
	 * Sends sampled Location data to server
	 */
	
	private void sendCurrentReadingsToCoapServer(int sensorType){
		try{
			JSONObject jsonToSend = new JSONObject().put("sensorReadings", (JSONObject)jsonLocationReadingStore);
			Log.i("JSONToSend", jsonToSend.toString());
			sendRequest(jsonToSend, CoapRequestCode.PUT, "/devices/"+deviceID+"/sensors");
			jsonLocationReadingStore.remove("readings");
		}
		catch (JSONException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	
	/*
	 * 
	 * Sends sampled reading to JCoap Server and flushes the Storage
	 */
	
	private void sendRequest(JSONObject jsonObjectForRequest, CoapRequestCode code, String URI){
		try{
    		channelManager = BasicCoapChannelManager.getInstance();
    		
    		clientChannel = channelManager.connect(this, InetAddress.getByName(SERVER_ADDRESS), PORT);
    		
			CoapRequest coapRequest = clientChannel.createRequest(true, code);
			coapRequest.setUriPath(URI);
			
			coapRequest.setContentType(CoapMediaType.text_plain);
			coapRequest.setPayload(jsonObjectForRequest.toString());
			clientChannel.sendMessage(coapRequest);
			System.out.println("Sent Request");
    	}
    	catch (UnknownHostException e) {
    		e.printStackTrace();
		}
	}
	
	private long getCurrentTimeInMilSec(){
    	Date dt = new Date();
    	return dt.getTime();
    }

	@Override
	public void onResponse(CoapClientChannel channel, CoapResponse response) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onConnectionFailed(CoapClientChannel channel,
			boolean notReachable, boolean resetByServer) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		String locationValue = "Latitude: " + location.getLatitude() + " Longitude: " + location.getLongitude() + 
								" Altitude: " + location.getAltitude();
		//Toast.makeText(getApplicationContext(), locationValue, Toast.LENGTH_LONG).show();
		
		JSONArray jsonArrayForLocation = new JSONArray();
		try{
			jsonArrayForLocation.put(location.getLatitude());
			jsonArrayForLocation.put(location.getLongitude());
			jsonArrayForLocation.put(location.getAltitude());
			/*
			 * 
			 * Send Location Sensor Data to Coap
			 */
			fetchCurrentReadingIntoJsonStructure(13, jsonArrayForLocation);
			sendCurrentReadingsToCoapServer(13);
		}catch (JSONException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

}
