package ag.test;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspServer;
import java.util.Locale;

/**
 * Created by Tilen on 5. 05. 2016.
 */
public class MainActivity  extends AppCompatActivity {
    private final static String TAG = "MainActivity";
    private SurfaceView mSurfaceView;
    private RtspServer mRtspServer;
    private TextView mEditText, mTextViewStartStream;
    private SurfaceHolder mSurfaceHolder;
    private MainApplication mApplication;
    public static Context contextOfApplication;
    public boolean isBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mApplication = (MainApplication) getApplication();

        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        mEditText = (TextView) findViewById(R.id.textViewIP);
        mTextViewStartStream = (TextView) findViewById(R.id.textViewStartStream);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        SessionBuilder.getInstance().setSurfaceHolder(mSurfaceHolder);

        // Startamo service RTSP server
        this.startService(new Intent(this,RtspServer.class));
    }

    @Override
    public void onStart() {
        super.onStart();
        //bindamo/zaženemo service
        bindService(new Intent(this, RtspServer.class), mRtspServiceConnection, Context.BIND_AUTO_CREATE);
    }


    @Override
    public void onPause() {
        super.onPause();
        //prekinemo service
        if(isBound){
            unregisterReceiver(mWifiStateReceiver);
            unbindService(mRtspServiceConnection);
            isBound=false;
        }

    }

    public void update() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //preverjamo če server streama
                if(mRtspServer.isStreaming()){
                    Log.d(TAG, "Streamam!");
                }
                if (mTextViewStartStream != null) {
                    if ((mRtspServer != null && mRtspServer.isStreaming())) {
                        mTextViewStartStream.setVisibility(View.INVISIBLE);
                    }else {
                        mTextViewStartStream.setVisibility(View.VISIBLE);
                        displayIpAddress();
                    }
                }
            }
        });
    }

    private ServiceConnection mRtspServiceConnection = new ServiceConnection() {
        @Override
        //ko se service poveže, startamo RTSP server
        public void onServiceConnected(ComponentName name, IBinder service) {
            mRtspServer = ((RtspServer.LocalBinder)service).getService();
            //če je kakršenkoli error pri serverju, se pokliče listener
            mRtspServer.addCallbackListener(mRtspCallbackListener);
            mRtspServer.start();
            update();
            registerReceiver(mWifiStateReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
            Log.d(TAG, "Server started.");
            String sett = "Resolucija" + mApplication.videoQuality.resX + "x" + mApplication.videoQuality.resY;
            sett += "Video: " + mApplication.videoEncoder + "aUDIO:" + mApplication.audioEncoder + " FPS:" + mApplication.videoQuality.framerate;
            Log.d(TAG, sett);
            displayNotification();
            isBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {}
    };

    private RtspServer.CallbackListener mRtspCallbackListener = new RtspServer.CallbackListener() {
        @Override
        public void onError(RtspServer server, Exception e, int error) {
            if (error == RtspServer.ERROR_BIND_FAILED) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Port zaseden")
                        .setMessage("Izberite drugi port za RTSP server!")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                startActivityForResult(new Intent(MainActivity.this, Preferences.class),0);
                            }
                        })
                        .show();
            }
        }

        @Override
        public void onMessage(RtspServer server, int message) {
        }

    };

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.nastavitve:
                startActivity(new Intent(this, Preferences.class));//odpre se razred Preferences.class oz. zažene se activity Poreferences.class
                return true;
            case R.id.quit:
                quitApp();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void quitApp() {
        // izbrišemo notification
        removeNotification();
        // Kills RTSP server
        this.stopService(new Intent(this,RtspServer.class));
        finish();
    }

    private void displayIpAddress() {
        //metoda za izpis IP naslova servera. Če nismo povezani na wifi, se IP ne prikaže
        WifiManager wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        String ipaddress = null;
        if (info!=null && info.getNetworkId()>-1) {
            int i = info.getIpAddress();
            String ip = String.format(Locale.ENGLISH,"%d.%d.%d.%d", i & 0xff, i >> 8 & 0xff,i >> 16 & 0xff,i >> 24 & 0xff);
            mEditText.setText("rtsp://");
            mEditText.append(ip);
            mEditText.append(":" + mRtspServer.getPort());
        } else if((ipaddress = Utilities.getLocalIpAddress(true)) != null) {
            mEditText.setText("rtsp://");
            mEditText.append(ipaddress);
            mEditText.append(":" + mRtspServer.getPort());
        } else {
            mEditText.setText("Niste povezani na WIFI!");
        }
    }

    public void displayNotification(){
        //notification, da je uporabnik obveščen, da je server aktiven
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        Notification notification = builder.setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())
                .setTicker("Streamanje")
                .setSmallIcon(R.drawable.ikona)
                .setContentTitle("Streamanje")
                .setContentText("RTSP server teče v ozadju!").build();
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(0,notification);
    }

    private void removeNotification() {
        ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(0);
    }

    private final BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // This intent is also received when app resumes even if wifi state hasn't changed :/
            if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                update();
            }
        }
    };


}
