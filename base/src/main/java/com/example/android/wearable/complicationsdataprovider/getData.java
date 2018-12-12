package com.example.android.wearable.complicationsdataprovider;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

//Separate thread using AsyncTask to retrieve values from website (unable to access from main thread)

//params, progress, result
public class getData extends AsyncTask<Void, Void, String>{

    public getData(){}

    @Override
    //takes in var args, so needs ...
    protected String doInBackground(Void... v) {
        String finaldata = "Get Error";

        String myURL = "https://staugustinechs.netfirms.com/stadayonetwo/";

        //use below for testing unwanted return values, returns  nt"><  from this site's code
        //myURL =  "https://staugustinechs.netfirms.com/staannounce/";

        StringBuilder sBuild = new StringBuilder();
        URLConnection urlCon = null;
        InputStreamReader in = null;
        try {
            URL url = new URL(myURL);
            urlCon = url.openConnection();
            if (urlCon != null) {
                urlCon.setReadTimeout(60000);
            }
            if (urlCon != null && urlCon.getInputStream() != null) {
                in = new InputStreamReader(urlCon.getInputStream(), Charset.defaultCharset());
                BufferedReader bRead = new BufferedReader(in);

                int cP;
                while ((cP = bRead.read()) != -1) {
                    sBuild.append((char) cP);
                }
                bRead.close();

            }
            in.close();
        } catch (IOException e) {
            Log.d("lfilt", "IOException in getData");
            //throw new RuntimeException("Exception: ", e);
        }

        String convert = sBuild.toString();

        try {
            int len = convert.length();
            finaldata = convert.substring(len-44,len-39);
        } catch (StringIndexOutOfBoundsException e) {
            Log.d("lfilt getData", "Bound Error in getData");
            finaldata = "Bound Error";
        }

        //Returns a value other than Day 1/2 or an error --> either no school or the website changed
        //both errors should never occur, would show up in providerservice's log if they do
        if(!finaldata.equals("Day 1") && !finaldata.equals("Day 2") && !finaldata.equals("Get Error") && !finaldata.equals("Bound Error")) {
            finaldata = "No School";
        }

        return finaldata;

    }

}
