package com.example.misbah.videocalling;

import android.content.pm.PackageInstaller;
import android.Manifest;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;



public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks,
        Session.SessionListener,
        Publisher.PublisherListener,
        Subscriber.VideoListener {

    private static final String TAG = "hello-world" + MainActivity.class.getSimpleName();

    private static final int RC_SETTINGS_SCREEN_PERM =123;
    public static final int RC_VIDEO_APP_PERM =124;

    private RelativeLayout relativeLayout;
    private LinearLayout linearLayout;
    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        relativeLayout = (RelativeLayout)findViewById(R.id.publisher);
        linearLayout   = (LinearLayout) findViewById(R.id.subscriber);
        requestPermisssion();

    }

    @Override
    protected void onStart(){
        Log.d(TAG,"onStart");
        super.onStart();
    }


    @Override
    protected void onRestart() {
        Log.d(TAG, "onRestart");

        super.onRestart();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");

        super.onResume();

        if (mSession == null) {
            return;
        }
        mSession.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");

        super.onPause();

        if (mSession == null) {
            return;
        }
        mSession.onPause();

        if (isFinishing()) {
            disconnectSession();
        }
    }



    @Override
    protected void onStop() {
        Log.d(TAG, "onPause");

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");

        disconnectSession();

        super.onDestroy();
    }


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,@NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults, this);

    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

        Log.d(TAG,"onPermissionGranted:"+ requestCode+ ":" +perms.size());


    }


    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());

        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this, getString(R.string.rationale_ask_again))
                    .setTitle(getString(R.string.title_settings_dialog))
                    .setPositiveButton(getString(R.string.setting))
                    .setNegativeButton(getString(R.string.cancel), null)
                    .setRequestCode(RC_SETTINGS_SCREEN_PERM)
                    .build()
                    .show();
        }
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermisssion(){
        String[] perms={Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO  };

        if (EasyPermissions.hasPermissions(this,perms)){
         mSession = new Session(MainActivity.this,OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID);
         mSession.setSessionListener(this);
         mSession.connect(OpenTokConfig.TOKEN);

    }else {
            EasyPermissions.requestPermissions(this,getString(R.string.rationale_video_app),RC_VIDEO_APP_PERM,perms);

        }
    }

    @Override
    public void onConnected(Session session) {

        Log.d(TAG,"onConnected: Connected to session"+ session.getSessionId());

        mPublisher = new Publisher(MainActivity.this,"publisher");
        mPublisher.setPublisherListener(this);
        mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,BaseVideoRenderer.STYLE_VIDEO_FILL);
        relativeLayout.addView(mPublisher.getView());
        mSession.publish(mPublisher);

    }

    @Override
    public void onDisconnected(Session session) {
        Log.d(TAG, "onDisconnected: disconnected from session"+ session.getSessionId());
        mSession = null;

    }

    @Override
    public void onError(Session session, OpentokError opentokError) {

        Log.d(TAG,"onError: Error("+ opentokError.getMessage()+") in session "+ session.getSessionId());
        Toast.makeText(this,"Session error. See the logcat please." ,Toast.LENGTH_LONG).show();
        finish();

    }
    @Override
    public void onStreamReceived(Session session, Stream stream) {

        Log.d(TAG, "OnStreamReceived: New Stream" + stream.getStreamId()+ "in session"+ session.getSessionId());
        if (!OpenTokConfig.SUBSCRIBE_TO_SELF){
            return;
        }
        if (mSubscriber != null){
            return;
        }
        subscribeTostream(stream);

    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {

        Log.d(TAG, "onStreamDropped: Stream" + stream.getStreamId()+ "dropped from session" +session.getSessionId());
        System.out.println("djfhdjhfdh");
        if (OpenTokConfig.SUBSCRIBE_TO_SELF){
            return;
        }
        if (mSubscriber == null){
            return;
        }
        if (mSubscriber.getStream().equals(stream)){
            linearLayout.removeView(mSubscriber.getView());
            mSubscriber.destroy();
            mSubscriber=null;

        }

    }



    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
        Log.d(TAG, "OnStreamCreated: Own stream "+ stream.getStreamId()+ "created");
        if(!OpenTokConfig.SUBSCRIBE_TO_SELF)
        {
            return;
        }
        subscribeTostream(stream);
    }

    private void subscribeTostream(Stream stream) {
        mSubscriber = new Subscriber(MainActivity.this, stream);
        mSubscriber.setVideoListener(this);
        mSession.subscribe(mSubscriber);
    }


    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
        Log.d(TAG, "onStreamDestroyed: Own stream " + stream.getStreamId() + " destroyed");


    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {
        Log.d(TAG, "onError: Error (" + opentokError.getMessage() + ") in publisher");

        Toast.makeText(this, "Session error. See the logcat please.", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onVideoDataReceived(SubscriberKit subscriberKit) {
        mSubscriber.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);
        linearLayout.addView(mSubscriber.getView());

    }

    @Override
    public void onVideoDisabled(SubscriberKit subscriberKit, String s) {

    }

    @Override
    public void onVideoEnabled(SubscriberKit subscriberKit, String s) {

    }

    @Override
    public void onVideoDisableWarning(SubscriberKit subscriberKit) {

    }

    @Override
    public void onVideoDisableWarningLifted(SubscriberKit subscriberKit) {

    }


    private void disconnectSession() {
        if (mSession == null) {
            return;
        }

        if (mSubscriber != null) {
            linearLayout.removeView(mSubscriber.getView());
            mSession.unsubscribe(mSubscriber);
            mSubscriber.destroy();
            mSubscriber = null;
        }

        if (mPublisher != null) {
            relativeLayout.removeView(mPublisher.getView());
            mSession.unpublish(mPublisher);
            mPublisher.destroy();
            mPublisher = null;
        }
        mSession.disconnect();
    }
}
