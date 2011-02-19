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

public class Run {
  private ArrayList<Coordinate> coordinates;
  private long runId;
  private long timeStart;
  private long timeEnd;
  private double distance;

  public Run(long runId, long timeStart, long timeEnd, double distance, ArrayList<Coordinate> coordinates) {
    this.runId = runId;
    this.timeStart = timeStart;
    this.timeEnd = timeEnd;
    this.distance = distance;
    this.coordinates = coordinates;
  }

  public void setTimeStart(long timeStart) {
    this.timeStart = timeStart;
  }

  public long getTimeStart() {
    return timeStart;
  }

  public void setTimeEnd(long timeEnd) {
    this.timeEnd = timeEnd;
  }

  public long getTimeEnd() {
    return timeEnd;
  }

  public void setCoordinates(ArrayList<Coordinate> coordinates) {
    this.coordinates = coordinates;
  }

  public ArrayList<Coordinate> getCoordinates() {
    return coordinates;
  }

  public void setDistance(double distance) {
    this.distance = distance;
  }

  public double getDistance() {
    return distance;
  }

  public void setRunId(long runId) {
    this.runId = runId;
  }

  public long getRunId() {
    return runId;
  }
}
