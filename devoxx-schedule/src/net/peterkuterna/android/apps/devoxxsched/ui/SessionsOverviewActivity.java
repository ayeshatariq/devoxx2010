/*
 * Copyright 2010 Peter Kuterna
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.peterkuterna.android.apps.devoxxsched.ui;

import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Rooms;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Sessions;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Tags;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Tracks;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Types;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;

public class SessionsOverviewActivity extends TabActivity {

	private static final String TAG = "SessionsOverviewActivity";

    public static final String TAG_TRACK = "track";
    public static final String TAG_TYPE = "type";
    public static final String TAG_TAG = "tag";
    public static final String TAG_ROOM = "room";
    public static final String TAG_ALL = "all";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sessions_overview);

        ((TextView) findViewById(R.id.title_text)).setText(getTitle());
        
        setupTrackTab();
        setupTypesTab();
        setupTagTab();
        setupRoomTab();
        setupAllTab();
    }

    public void onHomeClick(View v) {
        UIUtils.goHome(this);
    }

    public void onSearchClick(View v) {
        UIUtils.goSearch(this);
    }
    
    /** Build and add "track" tab. */
    private void setupTrackTab() {
        final TabHost host = getTabHost();

        final Intent intent = new Intent(Intent.ACTION_VIEW, Tracks.CONTENT_URI);
        intent.addCategory(Intent.CATEGORY_TAB);

        host.addTab(host.newTabSpec(TAG_TRACK)
                .setIndicator(buildIndicator(R.string.sessions_track))
                .setContent(intent));
    }

    /** Build and add "types" tab. */
    private void setupTypesTab() {
        final TabHost host = getTabHost();

        final Intent intent = new Intent(Intent.ACTION_VIEW, Types.CONTENT_URI);
        intent.addCategory(Intent.CATEGORY_TAB);

        host.addTab(host.newTabSpec(TAG_TYPE)
                .setIndicator(buildIndicator(R.string.sessions_type))
                .setContent(intent));
    }

    /** Build and add "tags" tab. */
    private void setupTagTab() {
        final TabHost host = getTabHost();

        final Intent intent = new Intent(Intent.ACTION_VIEW, Tags.CONTENT_URI);
        intent.addCategory(Intent.CATEGORY_TAB);

        host.addTab(host.newTabSpec(TAG_TAG)
                .setIndicator(buildIndicator(R.string.sessions_tag))
                .setContent(intent));
    }

    /** Build and add "rooms" tab. */
    private void setupRoomTab() {
        final TabHost host = getTabHost();

        final Intent intent = new Intent(Intent.ACTION_VIEW, Rooms.CONTENT_URI);
        intent.addCategory(Intent.CATEGORY_TAB);

        host.addTab(host.newTabSpec(TAG_ROOM)
                .setIndicator(buildIndicator(R.string.sessions_room))
                .setContent(intent));
    }

    /** Build and add "all" tab. */
    private void setupAllTab() {
        final TabHost host = getTabHost();

        final Intent intent = new Intent(Intent.ACTION_VIEW, Sessions.CONTENT_URI);
        intent.addCategory(Intent.CATEGORY_TAB);
        intent.putExtra(SessionsActivity.EXTRA_FAST_SCROLL, true);
        intent.putExtra(SessionsActivity.EXTRA_FOCUS_CURRENT_NEXT_SESSION, true);

        host.addTab(host.newTabSpec(TAG_ALL)
                .setIndicator(buildIndicator(R.string.sessions_all))
                .setContent(intent));
    }
    
    /**
     * Build a {@link View} to be used as a tab indicator, setting the requested
     * string resource as its label.
     */
    private View buildIndicator(int textRes) {
        final TextView indicator = (TextView) getLayoutInflater().inflate(R.layout.tab_indicator,
                getTabWidget(), false);
        indicator.setText(textRes);
        return indicator;
    }
    
}
