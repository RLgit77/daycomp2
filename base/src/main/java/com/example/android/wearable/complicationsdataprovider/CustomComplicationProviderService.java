/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wearable.complicationsdataprovider;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;
import android.util.Log;

import java.io.IOException;
import java.time.MonthDay;
import java.util.Calendar;
import java.util.Locale;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

import android.app.Activity;

public class CustomComplicationProviderService extends ComplicationProviderService {

    //use to filter logcat
    private static final String TAG = "lfilt Provider";

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onComplicationActivated(int complicationId, int dataType, ComplicationManager complicationManager) {
        Log.d(TAG, "onComplicationActivated() id: " + complicationId);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onComplicationUpdate(int complicationId, int dataType, ComplicationManager complicationManager) {

        Log.d(TAG, "onComplicationUpdate() id: "+complicationId);

        ComponentName thisProvider = new ComponentName(this, getClass());
        //TODO: update all? more useful
        // We pass the complication id, so we can only update the specific complication tapped.
        PendingIntent complicationPendingIntent = ComplicationTapBroadcastReceiver.getToggleIntent(this, thisProvider, complicationId);


        //returns getvalue --> "Day 1", "Day 2", "No School", "Weak Connection", "No Connection", "Get Error", or "Bound Error" --> formatted into complicationData var for update
        //region getting Data


        //1----------------------------------------------------check internet connection (getting data from web crashes if there is none)

        ConnectivityManager cm = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork;

        if (cm != null) {
            activeNetwork = cm.getActiveNetworkInfo();
        } else {
            activeNetwork = null;
        }

        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();


        //2------------------------------------ make async thread, get value from html if there is internet - android does not allow .get in main thread
        String getvalue;
        if(isConnected) {
            try {
                getvalue = new getData().execute().get();
            } catch (Exception e) {
                //connection is true, but still unable - usually just a weak signal/no internet
                getvalue = "Weak Connection";
            }
        } else {
            //TODO: Show this in a bubble popup, because why not?
            getvalue = "No Connection";
        }

        //errors from getData would show up here
        Log.d(TAG, "getting Data returned: "+getvalue);


        //3----------------------------------------------------------------- format ComplicationData for complication
        ComplicationData complicationData = null;
        switch (dataType) {
            //For small complications
            case ComplicationData.TYPE_SHORT_TEXT:
                complicationData =
                        new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                                .setShortText(ComplicationText.plainText(getvalue))
                                .setTapAction(complicationPendingIntent)
                                .build();
                break;
            //For long complications
            case ComplicationData.TYPE_LONG_TEXT:
                complicationData =
                        new ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                                .setLongText(ComplicationText.plainText(getvalue))
                                .setTapAction(complicationPendingIntent)
                                .build();
                break;
            default:
                if (Log.isLoggable(TAG, Log.WARN)) {
                    Log.w(TAG, "Unexpected complication type: " + dataType);
                }
        }

        //endregion getting Data


        SharedPreferences prefs = this.getSharedPreferences("DayCompPrefs",MODE_PRIVATE);
        SharedPreferences.Editor editprefs = getSharedPreferences("DayCompPrefs", MODE_PRIVATE).edit();

        if (complicationData != null) {

            if (prefs.getInt("date", -1) == Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) {
                //if current, update from saved value
                Log.d(TAG, "already success - updated from saved data");

                //region reformat pref to compData
                complicationData = null;
                switch (dataType) {
                    //For small complications
                    case ComplicationData.TYPE_SHORT_TEXT:
                        complicationData =
                                new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                                        .setShortText(ComplicationText.plainText(prefs.getString("data","Pref. Error")))
                                        .setTapAction(complicationPendingIntent)
                                        .build();
                        break;
                    //For long complications
                    case ComplicationData.TYPE_LONG_TEXT:
                        complicationData =
                                new ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                                        .setLongText(ComplicationText.plainText(prefs.getString("data","Pref. Error")))
                                        .setTapAction(complicationPendingIntent)
                                        .build();
                        break;
                    default:
                        if (Log.isLoggable(TAG, Log.WARN)) {
                            Log.w(TAG, "Unexpected complication type: " + dataType);
                        }
                }
                //endregion reformat pref to compData

                complicationManager.updateComplicationData(complicationId, complicationData);
            } else {

                //check error
                if(getvalue.equals("Day 1") || getvalue.equals("Day 2") || getvalue.equals("No School")){
                    //success - save both, update
                    editprefs.putString("data",getvalue);
                    editprefs.putInt("date",Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
                    editprefs.apply();

                    Log.d(TAG, "success - saved and updated");
                    complicationManager.updateComplicationData(complicationId, complicationData);
                } else {
                    //error - just update
                    Log.d(TAG, "error - temporarily updated");
                    complicationManager.updateComplicationData(complicationId, complicationData);
                }

            }

        } else {
            Log.d(TAG, "complicationData is null");
            complicationManager.noUpdateRequired(complicationId);
        }


        Log.d(TAG, "onUpdate finished");
        Log.d(TAG, "----------------------------------------");

    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onComplicationDeactivated(int complicationId) {
        Log.d(TAG, "onComplicationDeactivated() id: " + complicationId);
    }
}
