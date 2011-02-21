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

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

public class TrackRun extends MapActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.track_run);

    final MapView mapView = (MapView) findViewById(R.id.mapview);
    mapView.setBuiltInZoomControls(true);

    final MapController mapController = mapView.getController();
    mapController.setZoom(16);

    final ArrayList<GeoPoint> points = new ArrayList<GeoPoint>();
    final Projection projection = mapView.getProjection();

    final LocationListener locationListener = new LocationListener() {
      @Override
      public void onLocationChanged(Location loc) {
        double lat = loc.getLatitude();
        double lng = loc.getLongitude();

        GeoPoint p = new GeoPoint((int) (lat * 1E6), (int) (lng * 1E6));
        mapController.animateTo(p);
        mapView.invalidate();

        points.add(p);

        List<Overlay> mapOverlays = mapView.getOverlays();
        mapOverlays.clear();
        mapOverlays.add(new PathOverlay(projection, points));
      }

      @Override
      public void onProviderDisabled(String provider) {
      }

      @Override
      public void onProviderEnabled(String provider) {
      }

      @Override
      public void onStatusChanged(String provider, int status, Bundle extras) {
      }
    };

    RunningMate.getLocationService().startRun();
    RunningMate.getLocationService().registerLocationListener(locationListener);

    final Button stopButton = (Button) this.findViewById(R.id.stop_button);
    stopButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        RunningMate.getLocationService().removeLocationListener(locationListener);
        RunningMate.getLocationService().stopRun();

        // We don't need to pass any result back to the calling activity.
        setResult(0, new Intent(view.getContext(), RunningMate.class));
        finish();
      }
    });
  }

  @Override
  protected boolean isRouteDisplayed() {
    return false;
  }

  class PathOverlay extends Overlay {
    Projection projection;
    List<GeoPoint> points;

    public PathOverlay(Projection projection, List<GeoPoint> points) {
      this.projection = projection;
      this.points = points;
    }

    public void draw(Canvas canvas, MapView mapv, boolean shadow) {
      super.draw(canvas, mapv, shadow);

      Paint mPaint = new Paint();
      mPaint.setDither(true);
      mPaint.setColor(Color.BLUE);
      mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
      mPaint.setStrokeJoin(Paint.Join.ROUND);
      mPaint.setStrokeCap(Paint.Cap.ROUND);
      mPaint.setStrokeWidth(4);

      GeoPoint lastGeoPoint = points.get(0);
      for (int i = 1; i < points.size(); ++i) {
        GeoPoint currGeoPoint = points.get(i);

        Point lastPoint = new Point();
        Point currPoint = new Point();

        Path path = new Path();

        projection.toPixels(lastGeoPoint, lastPoint);
        projection.toPixels(currGeoPoint, currPoint);

        path.moveTo(currPoint.x, currPoint.y);
        path.lineTo(lastPoint.x, lastPoint.y);

        canvas.drawPath(path, mPaint);

        lastGeoPoint = currGeoPoint;
      }
    }
  }

  @Override
  protected void onDestroy() {
    Settings.printLogMessage(getClass().getCanonicalName(), "onDestroy() called.");
    super.onDestroy();
  }

  @Override
  protected void onPause() {
    Settings.printLogMessage(getClass().getCanonicalName(), "onPause() called.");
    super.onPause();
  }

  @Override
  protected void onStop() {
    Settings.printLogMessage(getClass().getCanonicalName(), "onStop() called.");
    super.onStop();
  }

  @Override
  protected void onStart() {
    Settings.printLogMessage(getClass().getCanonicalName(), "onStart() called.");
    super.onStart();
  }

  @Override
  protected void onResume() {
    Settings.printLogMessage(getClass().getCanonicalName(), "onResume() called.");
    super.onResume();
  }

  @Override
  protected void onRestart() {
    Settings.printLogMessage(getClass().getCanonicalName(), "onRestart() called.");
    super.onRestart();
  }
}
