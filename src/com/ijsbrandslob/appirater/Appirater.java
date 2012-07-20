/*
 This file is part of Appirater.

 Copyright (c) 2010, Arash Payan
 All rights reserved.

 Permission is hereby granted, free of charge, to any person
 obtaining a copy of this software and associated documentation
 files (the "Software"), to deal in the Software without
 restriction, including without limitation the rights to use,
 copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the
 Software is furnished to do so, subject to the following
 conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 OTHER DEALINGS IN THE SOFTWARE.
 */
/*
 * Appirater.java
 * Port of Appirater to Android.
 *
 * Original created by Arash Payan on 9/5/09.
 * http://arashpayan.com
 * Copyright 2010 Arash Payan. All rights reserved.
 *
 * Ported by IJsbrand Slob on 3/7/11.
 * http://ijsbrandslob.com
 */

package com.ijsbrandslob.appirater;

import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class Appirater {
   private static final int NO_VERSION = -1;

   private final Context mContext;
   private final Handler mHandler;
   private final Config mConfig;
   
   private Date mFirstUseDate;
   private Date mReminderRequestDate;
   private int mUseCount;
   private int mSignificantEventCount;
   private int mCurrentVersion;
   private boolean mRatedCurrentVersion;
   private boolean mDeclinedToRate;
    
   /**
    * Creates an Appirater with a custom Config.
    *
    * @param context The Activity where the Appirater should appear.
    * @param handler Handler for UI updates.
    * @param config The configuration instance with custom settings.
    */
   public Appirater( Context context, Handler handler, Config config ) {
	   mContext = context;
	   mHandler = handler;
	   mConfig  = config;
	   
	   loadSettings();
   }
   
   /**
    * Creates an Appirater with default settings.
    * 
    * @param context The Activity where the Appirater should appear.
    * @param handler Handler for UI updates.
    */
   public Appirater( Context context, Handler handler ) {
      this(context, handler, new Config.Builder().build());
   }

   /*
    * Tells Appirater that the app has launched. You should call this method at
    * the end of your application main activity delegate's Activity.onStart()
    * method.
    *
    * If the app has been used enough to be rated (and enough significant
    * events), you can suppress the rating alert by passing false for
    * canPromptForRating. The rating alert will simply be postponed until it is
    * called again with true for canPromptForRating. The rating alert can also
    * be triggered by appEnteredForeground() and userDidSignificantEvent() (as
    * long as you pass true for canPromptForRating in those methods).
    */
   public void appLaunched( final boolean canPromptForRating ) {
      new Thread(new Runnable() {
          public void run() {
              incrementAndRate( canPromptForRating );
          }
      }).start();
   }

   /*
    * Tells Appirater that the app was brought to the foreground on multitasking
    * devices. You should call this method from the application delegate's
    * Activity.onResume() method.
    *
    * If the app has been used enough to be rated (and enough significant
    * events), you can suppress the rating alert by passing false for
    * canPromptForRating. The rating alert will simply be postponed until it is
    * called again with true for canPromptForRating. The rating alert can also
    * be triggered by appLaunched() and userDidSignificantEvent() (as long as
    * you pass true for canPromptForRating in those methods).
    */
   public void appEnteredForeground( final boolean canPromptForRating ) {
      new Thread(new Runnable() {
          public void run() {
              incrementAndRate( canPromptForRating );
          }
      }).start();
   }

   /*
    * Tells Appirater that the user performed a significant event. A significant
    * event is whatever you want it to be. If you're app is used to make VoIP
    * calls, then you might want to call this method whenever the user places a
    * call. If it's a game, you might want to call this whenever the user beats
    * a level boss.
    *
    * If the user has performed enough significant events and used the app
    * enough, you can suppress the rating alert by passing false for
    * canPromptForRating. The rating alert will simply be postponed until it is
    * called again with true for canPromptForRating. The rating alert can also
    * be triggered by appLaunched() and appEnteredForeground() (as long as you
    * pass true for canPromptForRating in those methods).
    */
   public void userDidSignificantEvent( boolean canPromptForRating ) {
      incrementSignificantEventAndRate( canPromptForRating );
   }

   private void incrementAndRate( boolean canPromptForRating ) {
      incrementUseCount();
      if (canPromptForRating && ratingConditionsHaveBeenMet() && connectedToNetwork()) {
         mHandler.post(new Runnable() {
             public void run() {
                 showRatingAlert();
             }
         });
      }
   }

   private void incrementSignificantEventAndRate( boolean canPromptForRating ) {
      incrementSignificantEventCount();
      if (canPromptForRating && ratingConditionsHaveBeenMet() && connectedToNetwork()) {
         mHandler.post(new Runnable() {
             public void run() {
                 showRatingAlert();
             }
         });
      }
   }

   private boolean connectedToNetwork() {
      try {
         HttpClient httpclient = new DefaultHttpClient();
         HttpGet request       = new HttpGet( "http://www.google.com/" );
         HttpResponse result   = httpclient.execute( request );
         int statusCode        = result.getStatusLine().getStatusCode();
      
         if( statusCode < 400 )
            return true;
      } catch( Exception ex ) {
         Log.w("Appirater", ex.toString());
      }
      
      return false;
   }

   private void showRatingAlert() {
      final Dialog rateDialog = new Dialog( mContext );
      final Resources res = mContext.getResources();

      CharSequence appname = "unknown";
      try {
         appname = mContext.getPackageManager().getApplicationLabel( mContext.getPackageManager().getApplicationInfo( mContext.getPackageName(), 0 ) );
      } catch(NameNotFoundException ex) { /* Do nothing */ }
      
      rateDialog.setTitle( String.format( res.getString( R.string.APPIRATER_MESSAGE_TITLE ), appname ) );
      rateDialog.setContentView( R.layout.appirater );

      TextView messageArea = (TextView)rateDialog.findViewById( R.id.appirater_message_area );
      messageArea.setText( String.format( res.getString( R.string.APPIRATER_MESSAGE ), appname ) );

      Button rateButton        = (Button)rateDialog.findViewById( R.id.appirater_rate_button );
      Button remindLaterButton = (Button)rateDialog.findViewById( R.id.appirater_rate_later_button );
      Button cancelButton      = (Button)rateDialog.findViewById( R.id.appirater_cancel_button );

      rateButton.setText( String.format( res.getString( R.string.APPIRATER_RATE_BUTTON ), appname ) );
      
      rateButton.setOnClickListener( new OnClickListener() {
         @Override
         public void onClick( View v ) {
            rateDialog.dismiss();

            PackageManager packageManager = mContext.getPackageManager();
            Uri marketUri = Uri.parse( String.format( "market://details?id=%s", mContext.getPackageName() ) );
            Intent marketIntent = new Intent( Intent.ACTION_VIEW ).setData( marketUri );

            List<?> list = packageManager.queryIntentActivities(marketIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (list.size() > 0) {
                mContext.startActivity( marketIntent );
            } else {
                Uri webUri = Uri.parse( String.format( "http://play.google.com/store/apps/details?id=%s", mContext.getPackageName() ) );
                Intent webIntent = new Intent( Intent.ACTION_VIEW ).setData( webUri );
                mContext.startActivity( webIntent );
            }

            mRatedCurrentVersion = true;
            saveSettings();
         }
      });

      remindLaterButton.setOnClickListener( new OnClickListener() {
         @Override
         public void onClick( View v ) {
            mReminderRequestDate = new Date();
            saveSettings();
            rateDialog.dismiss();
         }
      });

      cancelButton.setOnClickListener( new OnClickListener() {
         @Override
         public void onClick( View v ) {
            mDeclinedToRate = true;
            saveSettings();
            rateDialog.dismiss();
         }
      });
      
      rateDialog.show();
   }

   private boolean ratingConditionsHaveBeenMet() {
      if( mConfig.debug )
         return true;

      Date now = new Date();
      long timeSinceFirstLaunch = now.getTime() - mFirstUseDate.getTime();
      long timeUntilRate = 1000 * 60 * 60 * 24 * mConfig.daysUntilPrompt;
      
      if( timeSinceFirstLaunch < timeUntilRate )
         return false;

      // check if the app has been used enough
      if( mUseCount < mConfig.usesUntilPrompt )
         return false;

      // check if the user has done enough significant events
      if( mSignificantEventCount < mConfig.sigEventsBeforePrompt )
         return false;

      // has the user previously declined to rate this version of the app?
      if( mDeclinedToRate )
         return false;

      // has the user already rated the app?
      if( mRatedCurrentVersion )
         return false;

      // if the user wanted to be reminded later, has enough time passed?
      if( null != mReminderRequestDate ) {
         long timeSinceReminderRequest = mReminderRequestDate.getTime() - now.getTime();
         long timeUntilReminder = 1000 * 60 * 60 * 24 * mConfig.timeBeforeReminding;

         if( timeSinceReminderRequest < timeUntilReminder )
            return false;
      }
      
      return true;
   }

   private void incrementUseCount() {
      int version = appVersion();

      // get the version number that we've been tracking
      if( mCurrentVersion == NO_VERSION )
         mCurrentVersion = version;

      if( mConfig.debug )
         System.out.println( String.format( "APPIRATER Tracking version: %d", mCurrentVersion ) );

      if( mCurrentVersion == version ) {
         // check if the first use date has been set. if not, set it.
         if( mFirstUseDate == null )
            mFirstUseDate = new Date();

         if( mConfig.debug )
            System.out.println( String.format( "APPIRATER Use count: %d", mUseCount ) );
      }
      else {
         // it's a new version of the app, so restart tracking
    	 resetTracking(version);
      }

      // increment the use count
      ++mUseCount;
      
      saveSettings();
   }

   private void incrementSignificantEventCount() {
      int version = appVersion();

      // get the version number that we've been tracking
      if( mCurrentVersion == NO_VERSION )
         mCurrentVersion = version;

      if( mConfig.debug )
         System.out.println( String.format( "APPIRATER Tracking version: %d", mCurrentVersion ) );

      if( mCurrentVersion == version ) {
         // check if the first use date has been set. if not, set it.
         if( mFirstUseDate == null )
            mFirstUseDate = new Date();

         if( mConfig.debug )
            System.out.println( String.format( "APPIRATER Significant event count: %d", mSignificantEventCount ) );
      }
      else {
    	 // it's a new version of the app, so restart tracking
         resetTracking(version);
      }

      // increment the significant event count
      ++mSignificantEventCount;
      
      saveSettings();
   }
   
   /**
    * Resets the Appirater tracking when there's a new version of the app.
    * @param version The new version to track.
    */
   private void resetTracking(int version) {
	   mCurrentVersion        = version;
       mFirstUseDate          = null;
       mUseCount              = 0;
       mSignificantEventCount = 0;
       mRatedCurrentVersion   = false;
       mDeclinedToRate        = false;
       mReminderRequestDate   = null;
   }
   
   private int appVersion() {
	   try {
	       return mContext.getPackageManager().getPackageInfo( mContext.getPackageName(), 0 ).versionCode;
	   } catch( NameNotFoundException ex ) {
		   return NO_VERSION;
	   }
   }

   // Settings
   private static final String APPIRATER_FIRST_USE_DATE        = "APPIRATER_FIRST_USE_DATE";
   private static final String APPIRATER_REMINDER_REQUEST_DATE = "APPIRATER_REMINDER_REQUEST_DATE";
   private static final String APPIRATER_USE_COUNT             = "APPIRATER_USE_COUNT";
   private static final String APPIRATER_SIG_EVENT_COUNT       = "APPIRATER_SIG_EVENT_COUNT";
   private static final String APPIRATER_CURRENT_VERSION       = "APPIRATER_CURRENT_VERSION";
   private static final String APPIRATER_RATED_CURRENT_VERSION = "APPIRATER_RATED_CURRENT_VERSION";
   private static final String APPIRATER_DECLINED_TO_RATE      = "APPIRATER_DECLINED_TO_RATE";

   private void loadSettings() {
      //Resources res = mContext.getResources();
      SharedPreferences settings = mContext.getSharedPreferences( mContext.getPackageName(), Context.MODE_PRIVATE );

      // Did we save settings before?
      if( settings.contains( APPIRATER_FIRST_USE_DATE ) ) {
         long firstUseDate = settings.getLong( APPIRATER_FIRST_USE_DATE, -1 );
         if( -1 != firstUseDate )
            mFirstUseDate = new Date( firstUseDate );

         long reminderRequestDate = settings.getLong( APPIRATER_REMINDER_REQUEST_DATE, -1 );
         if( -1 != reminderRequestDate )
            mReminderRequestDate = new Date( reminderRequestDate );

         mUseCount              = settings.getInt( APPIRATER_USE_COUNT, 0 );
         mSignificantEventCount = settings.getInt( APPIRATER_SIG_EVENT_COUNT, 0 );
         mCurrentVersion        = settings.getInt( APPIRATER_CURRENT_VERSION, 0 );
         mRatedCurrentVersion   = settings.getBoolean( APPIRATER_RATED_CURRENT_VERSION, false );
         mDeclinedToRate        = settings.getBoolean( APPIRATER_DECLINED_TO_RATE, false );
      }
   }

   private void saveSettings() {
      //Resources res = mContext.getResources();
      SharedPreferences prefs = mContext.getSharedPreferences( mContext.getPackageName(), Context.MODE_PRIVATE );
      SharedPreferences.Editor editor = prefs.edit();

      long firstUseDate = -1;
      if( mFirstUseDate != null )
         firstUseDate = mFirstUseDate.getTime();
      editor.putLong( APPIRATER_FIRST_USE_DATE, firstUseDate );

      long reminderRequestDate = -1;
      if( mReminderRequestDate != null )
         reminderRequestDate = mReminderRequestDate.getTime();
      editor.putLong( APPIRATER_REMINDER_REQUEST_DATE, reminderRequestDate );

      editor.putInt( APPIRATER_USE_COUNT, mUseCount );
      editor.putInt( APPIRATER_SIG_EVENT_COUNT, mSignificantEventCount );
      editor.putInt( APPIRATER_CURRENT_VERSION, mCurrentVersion );
      editor.putBoolean( APPIRATER_RATED_CURRENT_VERSION, mRatedCurrentVersion );
      editor.putBoolean( APPIRATER_DECLINED_TO_RATE, mDeclinedToRate );

      editor.commit();
   }
}
