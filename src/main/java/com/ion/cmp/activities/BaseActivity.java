package com.ion.cmp.activities;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ion.cmp.models.PlayList;
import com.ion.cmp.utils.Constants;
import com.ion.cmp.utils.FileUtils;

public class BaseActivity extends AppCompatActivity {
  protected int viewHeight;
  protected int viewWidth;
  protected int leftBorder;
  protected int topBorder;
  protected int border;
  protected int actionBarHeight = -1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    try {
      getActionBar().hide();
    } catch (Exception e) {
      getSupportActionBar().hide();
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    getDimensions();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    getDimensions();
  }

  protected void getDimensions() {
    Display d = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

    DisplayMetrics realDisplayMetrics = new DisplayMetrics();
    d.getMetrics(realDisplayMetrics);
    if (actionBarHeight == -1) {
        int realViewHeight = this.findViewById(android.R.id.content).getHeight();
        actionBarHeight = realDisplayMetrics.heightPixels - realViewHeight;
    }

    viewHeight = realDisplayMetrics.heightPixels - actionBarHeight;
    viewWidth = realDisplayMetrics.widthPixels;

    leftBorder = (viewWidth*5) / 100;
    topBorder = (viewHeight*5) / 100;
    border = leftBorder<=topBorder?leftBorder:topBorder;
  }

  protected void savePlayListToFileSystem(PlayList playList) {
    try {
      FileUtils.writeObjectToFile(playList.getId() + Constants.playListFileExtension, playList, getApplicationContext());
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }
}
