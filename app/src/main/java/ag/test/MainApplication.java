package ag.test;


import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.rtsp.RtspServer;
import net.majorkernelpanic.streaming.video.VideoQuality;

public class MainApplication extends android.app.Application {

    public final static String TAG = "MainApplication";

    public VideoQuality videoQuality = new VideoQuality(640,480,15,500000);

    public int audioEncoder = SessionBuilder.AUDIO_AMRNB;

    public int videoEncoder = SessionBuilder.VIDEO_H264;


    private static MainApplication mApplication;
    public String port;



    @Override
    public void onCreate() {
        mApplication = this;

        super.onCreate();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        audioEncoder = (Integer.parseInt(android.os.Build.VERSION.SDK)<14) ? SessionBuilder.AUDIO_AMRNB : SessionBuilder.AUDIO_AAC;
        audioEncoder = Integer.parseInt(settings.getString("audioKodek", String.valueOf(audioEncoder)));
        videoEncoder = Integer.parseInt(settings.getString("videoKodek", String.valueOf(videoEncoder)));

        // Read video quality settings from the preferences
        videoQuality = VideoQuality.merge(
                new VideoQuality(
                        settings.getInt("video_resX", 0),
                        settings.getInt("video_resY", 0),
                        Integer.parseInt(settings.getString("video_framerate", "0")),
                        Integer.parseInt(settings.getString("video_bitrate", "0"))*1000),
                videoQuality);

        SessionBuilder.getInstance()
                .setContext(getApplicationContext())
                .setAudioEncoder(audioEncoder)
                .setVideoEncoder(videoEncoder)
                .setVideoQuality(videoQuality);

        // Listens to changes of preferences
        settings.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
    }

    public static MainApplication getInstance() {
        return mApplication;
    }


    private SharedPreferences.OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("video_resX") || key.equals("video_resY")) {
                videoQuality.resX = sharedPreferences.getInt("video_resX", 0);
                videoQuality.resY = sharedPreferences.getInt("video_resY", 0);
                SessionBuilder.getInstance().setVideoQuality(videoQuality);
            }

            else if (key.equals("video_framerate")) {
                videoQuality.framerate = Integer.parseInt(sharedPreferences.getString("video_framerate", "0"));
                SessionBuilder.getInstance().setVideoQuality(videoQuality);
            }

            else if (key.equals("video_bitrate")) {
                videoQuality.bitrate = Integer.parseInt(sharedPreferences.getString("video_bitrate", "0"))*1000;
                SessionBuilder.getInstance().setVideoQuality(videoQuality);
            }

            else if (key.equals("audioKodek")) {
                audioEncoder = Integer.parseInt(sharedPreferences.getString("audioKodek", String.valueOf(audioEncoder)));
                SessionBuilder.getInstance().setAudioEncoder( audioEncoder );
            }
            else if(key.equals("videoFPS")) {
                videoQuality.framerate = Integer.parseInt(sharedPreferences.getString("videoFPS", "0"));
                SessionBuilder.getInstance().setVideoQuality(videoQuality);
            }

            else if (key.equals("videoKodek")) {
                videoEncoder = Integer.parseInt(sharedPreferences.getString("videoKodek", String.valueOf(videoEncoder)));
                SessionBuilder.getInstance().setVideoEncoder( videoEncoder );
            }
            else if(key.equals("rtsp_port")){
                port = sharedPreferences.getString("rtsp_port", RtspServer.KEY_PORT);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("rtsp_port", String.valueOf(port));
                editor.commit();
            }
        }
    };





}
