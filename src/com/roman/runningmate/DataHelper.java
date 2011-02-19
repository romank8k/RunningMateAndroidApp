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
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

public class DataHelper {
  private static final String DATABASE_NAME = "runningmate.db";
  private static final int DATABASE_VERSION = 1;

  private SQLiteDatabase database;

  private SQLiteStatement insertRun;
  private SQLiteStatement insertCoordinate;

  public DataHelper(Context context) {
    OpenHelper openHelper = new OpenHelper(context);
    database = openHelper.getWritableDatabase();
    insertRun = database.compileStatement("INSERT INTO runs(time_start, time_end, distance) VALUES(?, ?, ?)");
    insertCoordinate = database.compileStatement("INSERT INTO coordinates(run_id, latitude, longitude, elevation, time_elapsed) VALUES(?, ?, ?, ?, ?)");
  }

  public long insertRun(long startTime, long endTime, double distance) {
    insertRun.bindLong(1, startTime);
    insertRun.bindLong(2, endTime);
    insertRun.bindDouble(3, distance);
    return insertRun.executeInsert();
  }

  public long insertCoordinate(Coordinate coordinate, long runId) {
    insertCoordinate.bindDouble(1, runId);
    insertCoordinate.bindDouble(2, coordinate.getLatitude());
    insertCoordinate.bindDouble(3, coordinate.getLongitude());
    insertCoordinate.bindDouble(4, coordinate.getElevation());
    insertCoordinate.bindLong(5, coordinate.getTimeElapsed());
    return insertCoordinate.executeInsert();
  }

  public List<Coordinate> getCoordinates(long runId) {
    List<Coordinate> coordinates = new ArrayList<Coordinate>();
    Cursor cursor = database.rawQuery("SELECT coordinate_id, latitude, longitude, elevation, time_elapsed FROM coordinates WHERE run_id = ? ORDER BY coordinate_id ASC",
        new String[] { Long.toString(runId) });

    if (cursor.moveToFirst()) {
      do {
        double latitude = cursor.getDouble(1);
        double longitude = cursor.getDouble(2);
        double elevation = cursor.getDouble(3);
        long timeElapsed = cursor.getLong(4);
        coordinates.add(new Coordinate(latitude, longitude, elevation, timeElapsed));
      } while (cursor.moveToNext());
    }
    if (cursor != null && !cursor.isClosed()) {
      cursor.close();
    }
    return coordinates;
  }

  public List<Run> getAllRunsSince(long notEarlierThanTimeStart) {
    List<Run> runs = new ArrayList<Run>();

    Cursor cursor = database.query("runs", new String[] { "run_id", "time_start", "time_end", "distance" }, "time_start > ?",
        new String[] { Long.toString(notEarlierThanTimeStart) }, null, null, null);
    if (cursor.moveToFirst()) {
      do {
        runs.add(new Run(cursor.getLong(0), cursor.getLong(1), cursor.getLong(2), cursor.getDouble(3), null));
      } while (cursor.moveToNext());
    }
    if (cursor != null && !cursor.isClosed()) {
      cursor.close();
    }
    return runs;
  }

  public void updateRun(long runId, double distance, long timeEnd) {
    ContentValues values = new ContentValues();
    values.put("distance", distance);
    values.put("time_end", timeEnd);
    database.update("runs", values, "run_id = ?", new String[] { Long.toString(runId) });
  }

  private static class OpenHelper extends SQLiteOpenHelper {
    public OpenHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
      database.execSQL("CREATE TABLE runs(" +
                   "run_id INTEGER PRIMARY KEY," +
                   "time_start INTEGER NOT NULL, /* Unix Time */" +
                   "time_end INTEGER NOT NULL, /* Unix Time */" +
                   "distance REAL NOT NULL" +
                 ")");

      database.execSQL("CREATE TABLE coordinates(" +
                   "coordinate_id INTEGER PRIMARY KEY," +
                   "run_id INTEGER NOT NULL," +
                   "latitude REAL NOT NULL," +
                   "longitude REAL NOT NULL," +
                   "elevation REAL NOT NULL," +
                   "time_elapsed INTEGER NOT NULL, /* time elapsed since start of run in seconds */" +
                   "FOREIGN KEY(run_id) REFERENCES runs(run_id)" +
                 ")");

      if (Settings.getDebug()) {
        ExecuteFromNetwork("http://running.mindcache.net/get_runs.php", database);
      }
    }

    void ExecuteFromNetwork(String url, SQLiteDatabase database) {
      DefaultHttpClient httpClient = new DefaultHttpClient();
      HttpGet httpGet = new HttpGet(url);
      try {
        HttpResponse response = httpClient.execute(httpGet);
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

        String sqlStatement;
        while ((sqlStatement = reader.readLine()) != null) {
          if (sqlStatement.length() > 0) {
            try {
              database.execSQL(sqlStatement);
              Log.d(Settings.getLogTag(), "Executing SQL statement: " + sqlStatement);
            } catch (SQLException e) {
              e.getMessage();
            }
          }
        }
      } catch (ClientProtocolException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
  }
}
