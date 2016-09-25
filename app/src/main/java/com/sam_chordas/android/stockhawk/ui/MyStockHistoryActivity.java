package com.sam_chordas.android.stockhawk.ui;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MyStockHistoryActivity extends AppCompatActivity{

  private String symbol;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_line_graph);
    symbol = getIntent().getStringExtra("symbol");
    setTitle(symbol);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    new AsyncTask<Void, Void, String>(){

      @Override
      protected String doInBackground(Void... params) {

        return getResponse();
      }

      @Override
      protected void onPostExecute(String responceJson) {
        super.onPostExecute(responceJson);
        JSONArray quoteList = getQuoteList(responceJson);
        LineChartView lineChartView = (LineChartView) findViewById(R.id.linechart);
        LineSet lineSet = new LineSet();
        float max = 0;
        float min = 0;
        for(int i = quoteList.length() - 1; i >= 0; i--){
          try {
            String date = quoteList.getJSONObject(i).getString("Date");
            String adjClose = quoteList.getJSONObject(i).getString("Adj_Close");
            float aFloat = Float.parseFloat(adjClose);
            if(i == quoteList.length() - 1){
              max = aFloat;
              min = aFloat;
            }else {
              if(aFloat > max) max = aFloat;
              if(aFloat < min) min = aFloat;
            }
            lineSet.addPoint(date.substring(5), aFloat);
          } catch (JSONException e) {

          }
        }
        lineChartView.addData(lineSet);
        lineChartView.setAxisBorderValues((int)Math.floor(min), (int)Math.ceil(max)+1);
        lineChartView.show();
      }
    }.execute();

  }

  private JSONArray getQuoteList(String json){
    try {
      JSONObject jsonObject = new JSONObject(json);
      jsonObject = jsonObject.getJSONObject("query");
      jsonObject = jsonObject.getJSONObject("results");
      JSONArray jsonArray = jsonObject.getJSONArray("quote");
      return jsonArray;
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return new JSONArray();
  }

  private String getResponse(){
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    Date currentDate = new Date();
    Calendar dateEnd = Calendar.getInstance();
    dateEnd.setTime(currentDate);
    dateEnd.add(Calendar.DATE, 0);
    Calendar dateStart = Calendar.getInstance();
    dateStart.setTime(currentDate);
    dateStart.add(Calendar.WEEK_OF_YEAR, -2);
    String startDate = dateFormat.format(dateStart.getTime());
    String endDate = dateFormat.format(dateEnd.getTime());
    String query = "select * from yahoo.finance.historicaldata where symbol=\"" +
            symbol +
            "\" and startDate=\"" + startDate + "\" and endDate=\"" + endDate + "\"";
    StringBuilder urlStringBuilder = new StringBuilder();
    urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
    try {
      urlStringBuilder.append(URLEncoder.encode(query, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
            + "org%2Falltableswithkeys&callback=");
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder()
            .url(urlStringBuilder.toString())
            .build();

    Response response = null;
    try {
      response = client.newCall(request).execute();
      return response.body().string();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "";
  }

}
