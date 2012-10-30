package com.toychest3d.Jigsaw3d.Preferences;

import com.toychest3d.Jigsaw3d.R;
import com.toychest3d.Jigsaw3d.Puzzle.Persistance;
import com.toychest3d.Jigsaw3d.Selector.SelectorActivity;
import com.toychest3d.Jigsaw3d.Welcome.WelcomeActivity;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;

@SuppressWarnings("deprecation")
public class PreferencesActivity extends android.preference.PreferenceActivity implements OnSharedPreferenceChangeListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	 
		addPreferencesFromResource(R.xml.preferences);
		
		PreferenceScreen screen = getPreferenceScreen();
		
        for(int i=0; i<screen.getPreferenceCount(); i++)
        	updateSummary(screen.getPreference(i));
        
        screen.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	};
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		
		updateSummary(findPreference(key));
		Persistance.loadUserPrefs(this);
		
		if(key.equals(this.getResources().getString(R.string.prefs_backdrops_key))) {
			Persistance.setMenuBackdrop(WelcomeActivity.mLayout);
			Persistance.setMenuBackdrop(SelectorActivity.mLayoutMain);
			Persistance.setMenuBackdrop(SelectorActivity.mLayoutDif);
			Persistance.setMenuBackdrop(SelectorActivity.mLayoutSel);
		}
	}
	
	void updateSummary(Preference p) {
		
		if (p instanceof ListPreference) {
            ListPreference listPref = (ListPreference) p; 
            p.setSummary(listPref.getEntry()); 
        }
	}
}
