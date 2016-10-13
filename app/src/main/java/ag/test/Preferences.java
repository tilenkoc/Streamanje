package ag.test;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Preferences extends PreferenceActivity {


    private MainApplication mMainApplication = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMainApplication = (MainApplication) getApplication();
        addPreferencesFromResource(R.xml.preferences);

        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        final ListPreference audioEncoder = (ListPreference) findPreference("audioKodek");
        final ListPreference videoEncoder = (ListPreference) findPreference("videoKodek");
        final ListPreference videoResolution = (ListPreference) findPreference("kvaliteta");
        final EditTextPreference port = (EditTextPreference) findPreference("rtsp_port");
        final ListPreference fps = (ListPreference) findPreference("videoFPS");
        final ListPreference videoBitrate = (ListPreference) findPreference("video_bitrate");

        LinearLayout root = (LinearLayout)findViewById(android.R.id.list).getParent();
        Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.app_bar2, root, false);

        videoEncoder.setEnabled(true);
        videoResolution.setEnabled(true);
        videoEncoder.setValue(String.valueOf(mMainApplication.videoEncoder));
        audioEncoder.setValue(String.valueOf(mMainApplication.audioEncoder));
        videoResolution.setValue(mMainApplication.videoQuality.resX + "x" + mMainApplication.videoQuality.resY);


        videoResolution.setSummary("Trenutna resolucija: " + videoResolution.getValue() + "px");
        videoBitrate.setSummary("Bitrate: "+videoBitrate.getValue()+"kbps");
        fps.setSummary(fps.getValue() + "fps");


        videoResolution.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                SharedPreferences.Editor editor = settings.edit();
                Pattern pattern = Pattern.compile("([0-9]+)x([0-9]+)");
                Matcher matcher = pattern.matcher((String) newValue);
                matcher.find();
                editor.putInt("video_resX", Integer.parseInt(matcher.group(1)));
                editor.putInt("video_resY", Integer.parseInt(matcher.group(2)));
                editor.commit();
                videoResolution.setSummary("Trenutna resolucija:" + (String) newValue + "px");
                return true;
            }
        });

        fps.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                fps.setSummary((String) newValue + "fps");
                return true;
            }
        });

        videoBitrate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                videoBitrate.setSummary("Bitrate" + (String)newValue+"kbps");
                return true;
            }
        });



        bar.setTitle("Nastavitve");
        bar.setTitleTextColor(Color.BLACK);

        root.addView(bar, 0);

        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //delovanje home buttona
                finish();
                startActivity(new Intent(mMainApplication, MainActivity.class));
            }
        });
    }
}
