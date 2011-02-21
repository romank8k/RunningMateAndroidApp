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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;

public class RunningMate extends Activity {
  private static LocationService locationService;
  private boolean isBound;

  private ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      locationService = ((LocationService.LocalBinder) service).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
      locationService = null;
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Settings.printLogMessage(getClass().getCanonicalName(), "onCreate() called.");

    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    // TODO: We only need to start this service when tracking a run and synchronizing.
    // The location service will not be destroyed unless explicitly called to or the system is severely out of resources.
    // We always want the service to run in the background even when we're not bound to it (as long as we're tracking a run, synchronizing, etc).
    startService(new Intent(this, LocationService.class));

    doBindService();

    Button startButton = (Button) this.findViewById(R.id.run_button);
    startButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent intent = new Intent(view.getContext(), TrackRun.class);
        startActivityForResult(intent, 0);
      }
    });

    Button syncButton = (Button) this.findViewById(R.id.sync_button);
    syncButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent intent = new Intent(view.getContext(), Synchronize.class);
        startActivity(intent);
      }
    });

    Button exitButton = (Button) this.findViewById(R.id.exit_button);
    exitButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        doUnbindService();
        locationService.stopService(getIntent());
        finish();
      }
    });
  }

  public static LocationService getLocationService() {
    return locationService;
  }

  public void doBindService() {
    bindService(new Intent(this, LocationService.class), serviceConnection, Context.BIND_AUTO_CREATE);
    isBound = true;
  }

  public void doUnbindService() {
    if (isBound) {
      unbindService(serviceConnection);
      isBound = false;
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == 0) {
      // The intent could be null if the user pressed the back button during the sub-activity.
      if (intent != null) {
      }
    }
  }

  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    Settings.printLogMessage(getClass().getCanonicalName(), "onSaveInstanceState() called.");
    super.onSaveInstanceState(savedInstanceState);
  }

  @Override
  public void onRestoreInstanceState(Bundle savedInstanceState) {
    Settings.printLogMessage(getClass().getCanonicalName(), "onRestoreInstanceState() called.");
    super.onRestoreInstanceState(savedInstanceState);
  }

  @Override
  protected void onDestroy() {
    Settings.printLogMessage(getClass().getCanonicalName(), "onDestroy() called.");
    super.onDestroy();
    doUnbindService();
    stopService(new Intent(this, LocationService.class));
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
