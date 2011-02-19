// Copyright (c) 2011, Roman Khmelichek
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
//  1. Redistributions of source code must retain the above copyright notice,
//     this list of conditions and the following disclaimer.
//  2. Redistributions in binary form must reproduce the above copyright notice,
//     this list of conditions and the following disclaimer in the documentation
//     and/or other materials provided with the distribution.
//  3. Neither the name of Roman Khmelichek nor the names of its contributors
//     may be used to endorse or promote products derived from this software
//     without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR "AS IS" AND ANY EXPRESS OR IMPLIED
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
// MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
// EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
// OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
// WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
// OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
// ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.roman.runningmate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class LocationService extends Service {
  private final IBinder binder = new LocalBinder();

  private NotificationManager notificationManager;

  private DataHelper dataHelper;

  private LocationManager locationManager = null;
  private LocationListener locationListener;

  private Coordinate currCoordinate = null;
  private long runId = -1;
  private long runStartedTime = 0;
  private double runDistance = 0;

  private ArrayList<Coordinate> coordinates = new ArrayList<Coordinate>();

  // Class for clients to access.
  public class LocalBinder extends Binder {
    LocationService getService() {
      return LocationService.this;
    }
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    // We want this service to continue running until it is explicitly stopped, so return sticky.
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    Log.d(Settings.getLogTag(), "onDestroy() called.");

    // Clean up here.
    locationManager = null;
    currCoordinate = null;
    runId = -1;
    runStartedTime = 0;
    runDistance = 0;

    notificationManager.cancel(0);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }

  @Override
  public void onCreate() {
    Log.d(Settings.getLogTag(), "onCreate() called.");

    addNotification();

    dataHelper = new DataHelper(this);

    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    locationListener = new LocationListener() {
      @Override
      public void onLocationChanged(Location loc) {
        long timeElapsed = loc.getTime() - runStartedTime;
        Coordinate coordinate = new Coordinate(loc.getLatitude(), loc.getLongitude(), loc.getAltitude(), timeElapsed);

        if (currCoordinate != null) {
          runDistance += distance(currCoordinate, coordinate);
        }
        currCoordinate = coordinate;

        coordinates.add(coordinate);

        if (coordinates.size() >= 256) {
          saveCoordinates();
        }
      }

      @Override
      public void onProviderDisabled(String provider) {
        // Might want to tell user that the GPS is disabled and we can't collect coordinates.
      }

      @Override
      public void onProviderEnabled(String provider) {
      }

      @Override
      public void onStatusChanged(String provider, int status, Bundle extras) {
      }
    };
  }

  public class MockLocationProvider extends Thread {
    private List<String> locations;

    public MockLocationProvider(List<String> locations) {
      setDaemon(true);
      this.locations = locations;
    }

    @Override
    public void run() {
      for (String str : locations) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }

        String[] parts = str.split(",");
        Double latitude = Double.valueOf(parts[0]);
        Double longitude = Double.valueOf(parts[1]);
        Double altitude = Double.valueOf(parts[2]);
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAltitude(altitude);

        //Log.d(Settings.getLogTag(), "Parsing mock location: " + location.toString());

        // Must set the time.
        location.setTime(System.currentTimeMillis());

        locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, location);
      }
    }
  }

  // TODO: The intent should not create a new activity on the history stack. It should return the last thing the user was looking at.
  public void addNotification() {
    notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    Notification notification = new Notification(R.drawable.icon, "Running Mate Started", System.currentTimeMillis());

    // The PendingIntent to launch our activity if the user selects this notification
    PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, RunningMate.class), PendingIntent.FLAG_UPDATE_CURRENT);
    notification.setLatestEventInfo(this, "Running Mate", "Select to launch.", contentIntent);
    notificationManager.notify(0, notification);
  }

  public void registerLocationListener(LocationListener locationListener) {
    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, locationListener);
  }

  public void removeLocationListener(LocationListener locationListener) {
    locationManager.removeUpdates(locationListener);
  }

  public void startRun() {
    if (runId == -1) {
      if (Settings.getDebug()) {
        // Use mock locations.
        locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false, true, true, true, 0, 5);
        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

        List<String> locations = new ArrayList<String>();
        String locationsUrl = "http://running.mindcache.net/locations.php";
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(locationsUrl);
        try {
          HttpResponse response = httpClient.execute(httpGet);
          BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
          String location;
          while ((location = reader.readLine()) != null) {
            if (location.length() > 0) {
              try {
                locations.add(location);
              } catch (SQLException e) {
                e.getMessage();
              }
            }
          }

          new MockLocationProvider(locations).start();
        } catch (ClientProtocolException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else {
        // We send location updates every 10 seconds.
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, locationListener);
      }

      // Insert a run into the database. We'll update the end time and distance as we get more coordinates.
      long currTime = System.currentTimeMillis();
      runId = dataHelper.insertRun(currTime, currTime, 0);
    }
  }

  // Returns the distance between two coordinates in meters using the spherical law of cosines formula.
  // http://www.movable-type.co.uk/scripts/latlong.html
  public double distance(Coordinate a, Coordinate b) {
    double R = 6371000; // Radius of the Earth in meters.
    return Math.toRadians(Math.acos(Math.sin(a.getLatitude()) * Math.sin(b.getLatitude()) + Math.cos(a.getLatitude()) * Math.cos(b.getLatitude())
        * Math.cos(b.getLongitude() - a.getLongitude())))
        * R;
  }

  public void stopRun() {
    if (runId != -1) {
      locationManager.removeUpdates(locationListener);
      saveCoordinates();
      dataHelper.updateRun(runId, runDistance, System.currentTimeMillis());

      currCoordinate = null;
      runId = -1;
      runStartedTime = 0;
      runDistance = 0;

      Toast.makeText(this, "Run Saved!", Toast.LENGTH_LONG).show();
    }
  }

  // Insert coordinates into database and update the run end time, and distance.
  public void saveCoordinates() {
    if (coordinates.size() > 0) {
      for (Coordinate coordinate : coordinates) {
        dataHelper.insertCoordinate(coordinate, runId);
      }

      Coordinate lastCoordinate = coordinates.get(coordinates.size() - 1);
      dataHelper.updateRun(runId, runDistance, runStartedTime + lastCoordinate.getTimeElapsed());

      coordinates.clear();
    }
  }

  public String getRunsJson() {
    long timeBegin = System.nanoTime();

    // TODO: Find the smallest run start time that you need to sync.
    // long lastSyncedRunStartTime = lastSyncedRun();
    long lastSyncedRunStartTime = 0;

    // Build JSON string based on all runs later than the last synced run.
    StringBuilder json = new StringBuilder("{\"runs\":[");

    List<Run> allRuns = dataHelper.getAllRunsSince(lastSyncedRunStartTime);
    List<Run> runs = allRuns.subList(allRuns.size() - 2, allRuns.size());// TODO: For debugging.

    // TODO: We get out of memory exception when the json string becomes too large! Need to split this up.
    for (Run run : runs) {
      if (runs.get(0) != run)
        json.append(",");

      json.append("{\"time_start\":\"");
      json.append(run.getTimeStart());

      json.append("\",\"time_end\":\"");
      json.append(run.getTimeEnd());

      json.append("\",\"distance\":\"");
      json.append(run.getDistance());

      json.append("\",\"coordinates\":[");
      List<Coordinate> coordinates = dataHelper.getCoordinates(run.getRunId());
      for (Coordinate coordinate : coordinates) {
        if (coordinates.get(0) != coordinate)
          json.append(",");

        json.append("{\"latitude\":\"");
        json.append(coordinate.getLatitude());

        json.append("\",\"longitude\":\"");
        json.append(coordinate.getLongitude());

        json.append("\",\"elevation\":\"");
        json.append(coordinate.getElevation());

        json.append("\",\"time_elapsed\":\"");
        json.append(coordinate.getTimeElapsed());
        json.append("\"}");
      }

      json.append("]}");
    }
    json.append("]}");

    long timeEnd = System.nanoTime();

    Log.d(Settings.getLogTag(), "Time to get runs: " + ((timeEnd - timeBegin) / 1000.0) + " millis.\n");

    return json.toString();
  }

  public long lastSyncedRun() {
    try {
      URL url = new URL("");
      BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
      // We only expect one line.
      String str = in.readLine();
      in.close();
      Log.d(Settings.getLogTag(), "Last synced run: " + str);

      return Long.parseLong(str);
    } catch (MalformedURLException e) {
      Log.e(Settings.getLogTag(), e.getMessage(), e.getCause());
    } catch (IOException e) {
      Log.e(Settings.getLogTag(), e.getMessage(), e.getCause());
    } catch (NumberFormatException e) {
      Log.e(Settings.getLogTag(), e.getMessage(), e.getCause());
    }

    // If we had an error getting the last synced run from the cloud, we return the largest possible value
    // so that we don't re-insert any existing runs by accident.
    return Long.MAX_VALUE;
  }
}
