/*
 Copyright (C)2011 Paul Houx
 All rights reserved.
 
 Based on code written by Pedro Assuncao, see:
 http://pedroassuncao.com/2009/11/android-location-provider-mock/

 Redistribution and use in source and binary forms, with or without modification, 
 are permitted without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
*/
/* Modified by Baoliang Wang for CMPUT301 lab usage only.  2nd, Feb., 2014 */

package ualberta.cmput301.mocklocation;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import nl.cowlumbus.android.mockgps.R;
import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.TextView;

public class MockGpsProviderActivity extends Activity implements LocationListener {

	
	private MockLocationProvider mockLocationProvider = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){ 
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }
        else if(!locationManager.isProviderEnabled(MockLocationProvider.MOCK_PROVIDER)) {
        	locationManager.addTestProvider(MockLocationProvider.MOCK_PROVIDER, false, false,
        			false, false, true, false, false, 0, 5);
        	locationManager.setTestProviderEnabled(MockLocationProvider.MOCK_PROVIDER, true);
        }  
        
        if(locationManager.isProviderEnabled(MockLocationProvider.MOCK_PROVIDER)) {
        	locationManager.requestLocationUpdates(MockLocationProvider.MOCK_PROVIDER, 0, 0, this);

        	/** Load mock GPS data from file and create mock GPS provider. */
        	try {
        		// create a list of Strings that can dynamically grow
        		List<String> data = new ArrayList<String>();
		
        		InputStream is = getAssets().open("mock_gps_data.csv");
        		BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        		String line = null;
        		while ((line = reader.readLine()) != null) {
        			data.add(line);
        		}

        		// convert to a simple array so we can pass it to the AsyncTask
        		String[] coordinates = new String[data.size()];
        		data.toArray(coordinates);

        		// create new AsyncTask and pass the list of mock GPS coordinates
        		mockLocationProvider = new MockLocationProvider();
        		mockLocationProvider.execute(coordinates);
        	} 
        	catch (Exception e) {}
        }
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	// stop the mock GPS provider by calling the 'cancel(true)' method
    	try {
    		mockLocationProvider.cancel(true);
    		mockLocationProvider = null;
    	}
    	catch (Exception e) {}
    	
    	// remove it from the location manager
    	try {
    		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    		locationManager.removeTestProvider(MockLocationProvider.MOCK_PROVIDER);
    	}
    	catch (Exception e) {}
    }

	@Override
	public void onLocationChanged(Location location) {
		// show the received location in the view
		TextView view = (TextView) findViewById(R.id.text);
		view.setText("Longitude:" + location.getLongitude() 
				+ "\nLatitude:" + location.getLatitude() );		
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
    

	/** Define a mock GPS provider as an asynchronous task of this Activity. */
	private class MockLocationProvider extends AsyncTask<String, Integer, Void> {
		public static final String MOCK_PROVIDER = "mockLocationProvider";

		@Override
		protected Void doInBackground(String... data) {			
			// process data
			for (String str : data) {
						
				publishProgress();
				
				// retrieve data from the current line of text
				Double latitude = null;
				Double longitude = null;
				try {
					String[] parts = str.split(",");
					latitude = Double.valueOf(parts[0]);
					longitude = Double.valueOf(parts[1]);
				}
				catch(NullPointerException e) { break; }		// no data available
				catch(Exception e) { continue; }				// empty or invalid line

				// set the Location object
				Location location = new Location(MOCK_PROVIDER);
				location.setLatitude(latitude);
				location.setLongitude(longitude);
				location.setTime(System.currentTimeMillis());

				// provide the new location
				LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
				locationManager.setTestProviderLocation(MOCK_PROVIDER, location);
				
				// wait for a while and then process the next line
				SystemClock.sleep(200);
			}

			return null;
		}
	}
}