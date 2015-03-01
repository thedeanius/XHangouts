/*
 * Copyright (C) 2014-2015 Kevin Mark
 *
 * This file is part of XHangouts.
 *
 * XHangouts is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XHangouts is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XHangouts.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.versobit.kmark.xhangouts;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

final public class SettingsActivity extends PreferenceActivity {

    static void setDefaultPreferences(Context ctx) {
        PreferenceManager.setDefaultValues(ctx, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(ctx, R.xml.pref_mms, false);
        PreferenceManager.setDefaultValues(ctx, R.xml.pref_ui, false);
        PreferenceManager.setDefaultValues(ctx, R.xml.pref_about, false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
        if(!XApp.isActive()) {
            Toast.makeText(this, R.string.warning_not_loaded, Toast.LENGTH_LONG).show();
        }
    }

    public static final class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            PreferenceCategory header;

            // Add general preferences.
            addPreferencesFromResource(R.xml.pref_general);
            setupModEnabledPreference(findPreference(Setting.MOD_ENABLED.toString()));

            // Add MMS preferences, and a corresponding header.
            header = new PreferenceCategory(getActivity());
            header.setTitle(R.string.pref_header_mms);
            getPreferenceScreen().addPreference(header);
            addPreferencesFromResource(R.xml.pref_mms);

            final Preference scale = findPreference(Setting.MMS_SCALE_PREFKEY.toString());
            final Preference image = findPreference(Setting.MMS_IMAGE_PREFKEY.toString());

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            int scaleWidth = prefs.getInt(Setting.MMS_SCALE_WIDTH.toString(), 1024);
            int scaleHeight = prefs.getInt(Setting.MMS_SCALE_HEIGHT.toString(), 1024);
            updateMmsScaleSummary(scale, scaleWidth, scaleHeight);

            scale.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    MmsScaleDialog dialog = new MmsScaleDialog(scale);
                    dialog.show();
                    return true;
                }
            });

            Setting.ImageFormat format = Setting.ImageFormat.fromInt(prefs.getInt(Setting.MMS_IMAGE_TYPE.toString(), Setting.ImageFormat.JPEG.toInt()));
            int quality = prefs.getInt(Setting.MMS_IMAGE_QUALITY.toString(), 80);
            updateMmsTypeQualitySummary(image, format, quality);

            image.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    MmsTypeQualityDialog dialog = new MmsTypeQualityDialog(image);
                    dialog.show();
                    return true;
                }
            });

            bindPreferenceSummaryToValue(findPreference(Setting.MMS_ROTATE_MODE.toString()));

            final Preference apnConfig = findPreference(Setting.MMS_APN_SPLICING_APN_CONFIG_PREFKEY.toString());
            final Setting.ApnPreset apnPreset = Setting.ApnPreset.fromInt(prefs.getInt(Setting.MMS_APN_SPLICING_APN_CONFIG_PRESET.toString(), Setting.ApnPreset.CUSTOM.toInt()));
            updateMmsApnConfigSummary(apnConfig, apnPreset);

            apnConfig.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    MmsApnConfigDialog dialog = new MmsApnConfigDialog(apnConfig);
                    dialog.show();
                    return true;
                }
            });

            // Add UI Tweaks preferences, and a corresponding header.
            header = new PreferenceCategory(getActivity());
            header.setTitle(R.string.pref_header_ui);
            getPreferenceScreen().addPreference(header);
            addPreferencesFromResource(R.xml.pref_ui);
            bindPreferenceSummaryToValue(findPreference(Setting.UI_ENTER_KEY.toString()));

            // Add About preferences, and a corresponding header.
            header = new PreferenceCategory(getActivity());
            header.setTitle(R.string.pref_header_about);
            getPreferenceScreen().addPreference(header);
            addPreferencesFromResource(R.xml.pref_about);
            setupVersionPreference(findPreference(Setting.ABOUT_VERSION.toString()));
        }

        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                String stringValue = value.toString();

                if (preference instanceof ListPreference) {
                    // For list preferences, look up the correct display value in
                    // the preference's 'entries' list.
                    ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue(stringValue);

                    // Set the summary to reflect the new value.
                    preference.setSummary(
                            index >= 0
                                    ? listPreference.getEntries()[index]
                                    : null);

                } else {
                    // For all other preferences, set the summary to the value's
                    // simple string representation.
                    preference.setSummary(stringValue);
                }
                return true;
            }
        };

        /**
         * Binds a preference's summary to its value. More specifically, when the
         * preference's value is changed, its summary (line of name below the
         * preference title) is updated to reflect the value. The summary is also
         * immediately updated upon calling this method. The exact display format is
         * dependent on the type of preference.
         *
         * @see #sBindPreferenceSummaryToValueListener
         */
        private static void bindPreferenceSummaryToValue(Preference preference) {
            // Set the listener to watch for value changes.
            preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }

        private void setupModEnabledPreference(final Preference preference) {
            preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Toast.makeText(getActivity(), R.string.restart_hangouts_toast, Toast.LENGTH_LONG).show();
                    return true;
                }
            });
        }

        private void setupVersionPreference(final Preference preference) {
            final Context ctx = getActivity();
            preference.setTitle(ctx.getString(R.string.pref_title_about_version, BuildConfig.VERSION_NAME));
            String gHangoutsVerName = "unknown";
            int gHangoutsVerCode = 0;
            try {
                PackageInfo pi = ctx.getPackageManager().getPackageInfo(XHangouts.HANGOUTS_PKG_NAME, 0);
                gHangoutsVerName = pi.versionName;
                gHangoutsVerCode = pi.versionCode;

            } catch (PackageManager.NameNotFoundException ex) {
                //
            }
            String loaded = XApp.isActive() ? ctx.getString(R.string.pref_title_about_version_loaded) :
                    ctx.getString(R.string.pref_title_about_version_notloaded);
            preference.setSummary(ctx.getString(R.string.pref_desc_about_version, gHangoutsVerName, gHangoutsVerCode, loaded));
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ctx.getString(R.string.xda_thread))));
                    return false;
                }
            });
        }

        static void updateMmsScaleSummary(final Preference preference, final int width, final int height) {
            preference.setSummary(preference.getContext().getString(R.string.pref_desc_mms_scale, width, height));
        }

        static void updateMmsTypeQualitySummary(final Preference preference, final Setting.ImageFormat format, final int quality) {
            Context ctx = preference.getContext();
            String strQuality = format == Setting.ImageFormat.PNG ? ctx.getString(R.string.dialog_mms_type_quality_lossless) : String.valueOf(quality);
            preference.setSummary(ctx.getString(R.string.pref_desc_mms_image_type, format.toString(), strQuality.toLowerCase()));
        }

        static void updateMmsApnConfigSummary(final Preference preference, final Setting.ApnPreset preset) {
            preference.setSummary(preset.toString());
        }
    }
}
