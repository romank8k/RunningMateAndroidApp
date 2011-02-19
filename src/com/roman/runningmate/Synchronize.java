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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

// Note: Much of the authentication code originally comes from Nick Johnson's blog post at
// http://blog.notdot.net/2010/05/Authenticating-against-App-Engine-from-an-Android-app
public class Synchronize extends Activity {
  private DefaultHttpClient httpClient = new DefaultHttpClient();
  private TextView textView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.synchronize);

    textView = (TextView) findViewById(R.id.scroll_view);
    textView.setText("Synchronizing run with server. Please wait...");

    Button finishButton = (Button) this.findViewById(R.id.finish_button);
    finishButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        finish();
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    Intent intent = getIntent();
    AccountManager accountManager = AccountManager.get(getApplicationContext());
    Account account = (Account) intent.getExtras().get("account");

    // The auth token can expire or become invalid. We must remove it from the cache and get a new one if this happens.
    // TODO: For now, we just invalidate it every time...
    AccountManagerFuture<Bundle> future = accountManager.getAuthToken(account, "ah", false, null, null);
    Bundle bundle;
    try {
      bundle = future.getResult();
      String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
      accountManager.invalidateAuthToken(account.type, token);

    } catch (OperationCanceledException e) {
      e.printStackTrace();
    } catch (AuthenticatorException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    accountManager.getAuthToken(account, "ah", false, new GetAuthTokenCallback(), null);
  }

  private class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {
    public void run(AccountManagerFuture<Bundle> result) {
      Bundle bundle;
      try {
        bundle = result.getResult();
        Intent intent = (Intent) bundle.get(AccountManager.KEY_INTENT);
        if (intent != null) {
          // User input is required.
          startActivity(intent);
        } else {
          onGetAuthToken(bundle);
        }
      } catch (OperationCanceledException e) {
        e.printStackTrace();
      } catch (AuthenticatorException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  };

  protected void onGetAuthToken(Bundle bundle) {
    String authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
    new GetCookieTask().execute(authToken);
  }

  private class GetCookieTask extends AsyncTask<String, Void, Boolean> {
    protected Boolean doInBackground(String... tokens) {
      try {
        // Don't follow redirects.
        httpClient.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);

        String url = "http://running-mate.appspot.com/_ah/login?continue=" + URLEncoder.encode("http://localhost/") + "&auth=" + URLEncoder.encode(tokens[0]);
        HttpGet httpGet = new HttpGet(url);

        HttpResponse response = httpClient.execute(httpGet);
        if (response.getStatusLine().getStatusCode() != 302) {
          // Response should be a redirect.
          return false;
        }

        for (Cookie cookie : httpClient.getCookieStore().getCookies()) {
          if (cookie.getName().equals("ACSID"))
            return true;
        }
      } catch (ClientProtocolException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        httpClient.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
      }
      return false;
    }

    protected void onPostExecute(Boolean result) {
      new AuthenticatedRequestTask().execute("http://running-mate.appspot.com/data_service");
    }
  }

  private class AuthenticatedRequestTask extends AsyncTask<String, Void, HttpResponse> {
    @Override
    protected HttpResponse doInBackground(String... urls) {
      try {
        HttpPost httpPost = new HttpPost(urls[0]);

        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("runs", RunningMate.getLocationService().getRunsJson()));
        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

        return httpClient.execute(httpPost);
      } catch (ClientProtocolException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
      return null;
    }

    protected void onPostExecute(HttpResponse result) {
      try {
        BufferedReader reader = new BufferedReader(new InputStreamReader(result.getEntity().getContent()));
        StringBuffer content = new StringBuffer();
        String line = "";
        while ((line = reader.readLine()) != null) {
          content.append(line + '\n');
        }
        textView.setText(content.toString());
      } catch (IllegalStateException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}