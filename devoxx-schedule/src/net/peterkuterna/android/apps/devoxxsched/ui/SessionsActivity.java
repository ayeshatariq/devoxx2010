/*
 * Copyright 2010 Google Inc.
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

/*
 * Modified by Peter Kuterna to support the Devoxx conference.
 * Added headers with track color.
 * Added highlighting of sessions at the same time.
 */
package net.peterkuterna.android.apps.devoxxsched.ui;

import static net.peterkuterna.android.apps.devoxxsched.util.UIUtils.buildStyledSnippet;
import static net.peterkuterna.android.apps.devoxxsched.util.UIUtils.formatSessionSubtitle;

import java.util.ArrayList;

import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Blocks;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Rooms;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.SessionCounts;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Sessions;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Tracks;
import net.peterkuterna.android.apps.devoxxsched.ui.widget.PinnedHeaderListView;
import net.peterkuterna.android.apps.devoxxsched.util.Lists;
import net.peterkuterna.android.apps.devoxxsched.util.NotifyingAsyncQueryHandler;
import net.peterkuterna.android.apps.devoxxsched.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import net.peterkuterna.android.apps.devoxxsched.util.UriUtils;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.BaseColumns;
import android.text.Spannable;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class SessionsActivity extends ListActivity implements AsyncQueryListener {

    private static final String TAG = "SessionsActivity";
    
    public static final String EXTRA_TRACK_COLOR = "net.peterkuterna.android.apps.devoxxsched.extra.TRACK_COLOR";
    public static final String EXTRA_NO_WEEKDAY_HEADER = "net.peterkuterna.android.apps.devoxxsched.extra.NO_WEEKDAY_HEADER";
    public static final String EXTRA_HIHGLIGHT_PARALLEL_STARRED = "net.peterkuterna.android.apps.devoxxsched.extra.HIGHLIGHT_PARALLEL_STARRED";
    public static final String EXTRA_FOCUS_CURRENT_NEXT_SESSION = "net.peterkuterna.android.apps.devoxxsched.extra.FOCUS_CURRENT_NEXT_SESSION";

    private static final int DAY_FLAGS = DateUtils.FORMAT_SHOW_WEEKDAY;

    private CursorAdapter mAdapter;

    private NotifyingAsyncQueryHandler mHandler;
    private Handler mMessageQueueHandler = new Handler();
    private boolean mHighlightParallelStarred = false;
    private boolean mFocusCurrentNextSession = false;
    private boolean mGrayOutSessions = true;
    private boolean mShowWeekdays = false;
    
    private int mTrackColor= -1;
    private int mPinnedHeaderBackgroundColor;
    
    private static final String SESSIONS_SORT = Sessions.BLOCK_START + " ASC," + Rooms.NAME + " ASC";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!getIntent().hasCategory(Intent.CATEGORY_TAB)) {
            setContentView(R.layout.activity_sessions);

            final String customTitle = getIntent().getStringExtra(Intent.EXTRA_TITLE);
            ((TextView) findViewById(R.id.title_text)).setText(
                    customTitle != null ? customTitle : getTitle());

        } else {
            setContentView(R.layout.activity_sessions_content);
        }

        final Intent intent = getIntent();
        final Uri sessionsUri = intent.getData();

        final SharedPreferences settingsPrefs = getSharedPreferences(SettingsActivity.SETTINGS_NAME, MODE_PRIVATE);
        final boolean prefFocusCurrentNextSession = settingsPrefs.getBoolean(getString(R.string.focus_session_during_conference_key), true);

        mTrackColor = intent.getIntExtra(EXTRA_TRACK_COLOR, -1);
        mHighlightParallelStarred = intent.getBooleanExtra(EXTRA_HIHGLIGHT_PARALLEL_STARRED, false);
        mFocusCurrentNextSession = prefFocusCurrentNextSession && intent.getBooleanExtra(EXTRA_FOCUS_CURRENT_NEXT_SESSION, false);
        mGrayOutSessions = settingsPrefs.getBoolean(getString(R.string.gray_out_passed_sessions_key), true);
        mShowWeekdays = UriUtils.readBooleanQueryParameter(sessionsUri, SessionCounts.SESSION_INDEX_EXTRAS, false);
               
        if (mTrackColor != -1) UIUtils.setTitleBarColor(findViewById(R.id.title_container), mTrackColor);

        String[] projection;
        String sort;
        if (!Sessions.isSearchUri(sessionsUri)) {
            mAdapter = new SessionsAdapter(this);
            projection = SessionsQuery.PROJECTION;
            sort = SESSIONS_SORT;
        } else {
           	mAdapter = new SearchAdapter(this);
            projection = SearchQuery.PROJECTION;
            sort = Sessions.DEFAULT_SORT;
        }

        setListAdapter(mAdapter);
        
        setupListView(getIntent());
        
        // Start background query to load sessions
        mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
        mHandler.startQuery(sessionsUri, projection, sort);
    }

	private void setupListView(Intent intent) {
    	if (mAdapter instanceof SessionsAdapter) {
        	final PinnedHeaderListView list = (PinnedHeaderListView) getListView();
			mPinnedHeaderBackgroundColor = mTrackColor != - 1 ? 
					UIUtils.lightenColor(mTrackColor) 
					: getResources().getColor(R.color.header_background);
			View pinnedHeader = getLayoutInflater().inflate(R.layout.list_item_header, list, false);
			if (mTrackColor != -1) {
				UIUtils.setHeaderColor(pinnedHeader, mTrackColor);
			}
			list.setPinnedHeaderView(pinnedHeader);
			list.setDividerHeight(0);
			list.setOnScrollListener((SessionsAdapter) mAdapter);
    	}
    }

    /** {@inheritDoc} */
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
		startManagingCursor(cursor);
		mAdapter.changeCursor(cursor);
    	
    	if (mAdapter instanceof SessionsAdapter && mFocusCurrentNextSession) {
    		final SessionsAdapter adapter = (SessionsAdapter) mAdapter;
	    	getListView().post(new Runnable() {
				@Override
				public void run() {
					final Cursor cursor = mAdapter.getCursor();
					if (cursor != null && !cursor.isClosed()) {
						int scrollPos = getScrollPosition(mAdapter.getCursor());
						final int height = (int) TypedValue.applyDimension(
								TypedValue.COMPLEX_UNIT_DIP, 
								30.0f, 
								getResources().getDisplayMetrics());
						final boolean firstItemOfDay = (scrollPos == 0) 
							|| (scrollPos > 0 
									&& adapter.getSectionForPosition(scrollPos) != adapter.getSectionForPosition(scrollPos - 1)); 
						if (scrollPos != -1) getListView().setSelectionFromTop(scrollPos, firstItemOfDay ? 0 : height);
					}
				}
			});
    	}
    }
    
    private int getScrollPosition(Cursor cursor) {
    	int scrollPos = -1;
    	
    	final long currentTime = System.currentTimeMillis();
    	
        if (currentTime > UIUtils.CONFERENCE_START_MILLIS &&
        		currentTime < UIUtils.CONFERENCE_END_MILLIS) {
	    	for (int i = 0; i < cursor.getCount(); i++) {
	    		cursor.moveToPosition(i);
	    		long blockStart = cursor.getLong(SessionsQuery.BLOCK_START);
	    		long blockEnd = cursor.getLong(SessionsQuery.BLOCK_END);
	    		if ((currentTime >= blockStart && currentTime <= blockEnd)
	    				|| currentTime <= blockEnd) {
	    			scrollPos = i;
	    			break;
	    		}
	    	}
        }
    	
    	return scrollPos;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMessageQueueHandler.post(mRefreshSessionsRunnable);
    }

    @Override
    protected void onPause() {
        mMessageQueueHandler.removeCallbacks(mRefreshSessionsRunnable);
        super.onPause();
    }

    /** {@inheritDoc} */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // Launch viewer for specific session, passing along any track knowledge
        // that should influence the title-bar.
        final Cursor cursor = (Cursor) mAdapter.getItem(position);
        final String sessionId = cursor.getString(cursor.getColumnIndex(Sessions.SESSION_ID));
        final Uri sessionUri = Sessions.buildSessionUri(sessionId);
        final Intent intent = new Intent(Intent.ACTION_VIEW, sessionUri);
        startActivity(intent);
    }

    /** Handle "home" title-bar action. */
    public void onHomeClick(View v) {
        UIUtils.goHome(this);
    }

    /** Handle "search" title-bar action. */
    public void onSearchClick(View v) {
        UIUtils.goSearch(this);
    }

    public static final class SessionItemViews {
        View headerView;
        TextView headerTextView;
        View dividerView;
        TextView titleView;
        TextView subtitleView;
        View trackView;
        CheckBox starButton;
    }
    
    final static class PinnedHeaderCache {
        public TextView titleView;
        public ColorStateList textColor;
        public Drawable background;
    }
    
    private abstract class BaseAdapter extends CursorAdapter {

		public BaseAdapter(Context context) {
			super(context, null, true);
		}
    	
	    protected void findAndCacheViews(View view) {
	        // Get the views to bind to
	        SessionItemViews views = new SessionItemViews();
	        views.headerView = view.findViewById(R.id.header);
	        views.headerTextView = (TextView) view.findViewById(R.id.header_text);
	        views.dividerView = view.findViewById(R.id.session_divider);
	        views.titleView = (TextView) view.findViewById(R.id.session_title);
	        views.subtitleView = (TextView) view.findViewById(R.id.session_subtitle);
	        views.starButton = (CheckBox) view.findViewById(R.id.star_button);
	        views.trackView = view.findViewById(R.id.session_track);
	        view.setTag(views);
	    }

    }
    
    private final class SessionsAdapter extends BaseAdapter
	    implements SectionIndexer, OnScrollListener, PinnedHeaderListView.PinnedHeaderAdapter {

    	private SectionIndexer mIndexer;
    	private boolean mDisplaySectionHeaders = true;
	
		private final Context mContext;
	
		public SessionsAdapter(Context context) {
		    super(context);
		    
		    this.mContext = context;
		}
	
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
		    if (!getCursor().moveToPosition(position)) {
		        throw new IllegalStateException("couldn't move cursor to position " + position);
		    }
		
		    boolean newView;
		    View v;
		    if (convertView == null || convertView.getTag() == null) {
		        newView = true;
		        v = newView(mContext, getCursor(), parent);
		    } else {
		        newView = false;
		        v = convertView;
		    }
		    bindView(v, mContext, getCursor());
		    bindSectionHeader(v, position, mDisplaySectionHeaders);
		    return v;
		}
		
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View v = getLayoutInflater().inflate(R.layout.list_item_session, parent,false);
			findAndCacheViews(v);
			if (mTrackColor != -1) {
				View headerView = ((SessionItemViews) v.getTag()).headerView;
				UIUtils.setHeaderColor(headerView, mTrackColor);
			}
			return v;
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			SessionItemViews views = (SessionItemViews) view.getTag();
	        if (mTrackColor == -1) {
	        	views.trackView.setBackgroundColor(cursor.getInt(SessionsQuery.TRACK_COLOR));
	        } else {
	        	views.trackView.setVisibility(View.GONE);
	        }
	
	        views.titleView.setText(cursor.getString(SessionsQuery.TITLE));
	
	        // Format time block this session occupies
	        final long blockStart = cursor.getLong(SessionsQuery.BLOCK_START);
	        final long blockEnd = cursor.getLong(SessionsQuery.BLOCK_END);
	        final String roomName = cursor.getString(SessionsQuery.ROOM_NAME);
	        final String subtitle = formatSessionSubtitle(blockStart, blockEnd, roomName, context);
	
	        views.subtitleView.setText(subtitle);
	
	        final boolean starred = cursor.getInt(SessionsQuery.STARRED) != 0;
	        views.starButton.setVisibility(starred ? View.VISIBLE : View.INVISIBLE);
	        views.starButton.setChecked(starred);
	        
	        if (mHighlightParallelStarred) {
	            final int parallelStarredCount = cursor.getInt(SessionsQuery.STARRED_IN_BLOCK_COUNT);
	        	if (starred && parallelStarredCount > 1) {
	            	view.setBackgroundColor(0x20ff0000);
	        	} else {
	            	view.setBackgroundColor(0x00000000);
	        	}
	        } else {
	        	view.setBackgroundColor(0x00000000);
	        }
	        
	        // Possibly indicate that the session has occurred in the past.
	        if (mGrayOutSessions) {
	        	UIUtils.setSessionTitleColor(blockStart, blockEnd, views.titleView, views.subtitleView);
	        }
		}
		
		private void bindSectionHeader(View view, int position, boolean displaySectionHeaders) {
			SessionItemViews views = (SessionItemViews) view.getTag();
		    if (!displaySectionHeaders) {
		    	views.headerView.setVisibility(View.GONE);
		    	views.dividerView.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
		    } else {
		        final int section = getSectionForPosition(position);
		        if (getPositionForSection(section) == position) {
		            String title = (String) mIndexer.getSections()[section];
		            views.headerTextView.setText(title);
			    	views.headerView.setVisibility(View.VISIBLE);
			    	views.dividerView.setVisibility(View.GONE);
		        } else {
		        	views.headerView.setVisibility(View.GONE);
			    	views.dividerView.setVisibility(View.VISIBLE);
		        }
		
		        // move the divider for the last item in a section
		        if (getPositionForSection(section + 1) - 1 == position) {
			    	views.dividerView.setVisibility(View.GONE);
		        } else {
			    	views.dividerView.setVisibility(View.VISIBLE);
		        }
		    }
		}
	
		@Override
		public void notifyDataSetChanged() {
			if (mShowWeekdays) {
				final Cursor cursor = getCursor();
				
				final ArrayList<String> tmpWeekdays = Lists.newArrayList();
				final ArrayList<Integer> tmpCounts = Lists.newArrayList();
				
				String currentWeekday = null;
				int count = 0;
				if (cursor != null && !cursor.isClosed()) {
					cursor.moveToPosition(-1);
					while (cursor.moveToNext()) {
		                long millis = cursor.getLong(SessionsQuery.BLOCK_START);
		                String weekday = DateUtils.formatDateTime(mContext, millis, DAY_FLAGS);
		                if (!weekday.equals(currentWeekday)) {
		                	if (count > 0) {
		                		tmpWeekdays.add(currentWeekday);
		                		tmpCounts.add(count);
		                	}
		                	count = 1;
		                	currentWeekday = weekday;
		                } else {
		                	count++;
		                }
					}
	            	if (count > 0) {
	            		tmpWeekdays.add(currentWeekday);
	            		tmpCounts.add(count);
	            	}
	            	
	            	String[] weekdays = new String[tmpWeekdays.size()];
	            	int[] counts = new int[tmpWeekdays.size()];
	            	for (int i = 0; i < tmpWeekdays.size(); i++) {
	            		weekdays[i] = tmpWeekdays.get(i);
	            		counts[i] = tmpCounts.get(i);
	            	}
	            	
	            	updateIndexer(weekdays, counts);
				} else {
					updateIndexer(null, null);
				}
			}

			super.notifyDataSetChanged();
		}

		@Override
		public void notifyDataSetInvalidated() {
			updateIndexer(null, null);

			super.notifyDataSetInvalidated();
		}

		private void updateIndexer(String[] sections, int[] counts) {
			if (sections == null || counts == null) {
				mIndexer = null;
				return;
			}
			
			mIndexer = new SessionsSectionIndexer(sections, counts);
		}
	
		public Object [] getSections() {
		    if (mIndexer == null) {
		        return new String[] { " " };
		    }
		    
		    return mIndexer.getSections();
		}
		
		public int getPositionForSection(int sectionIndex) {
		    if (mIndexer == null) {
		        return -1;
		    }
		
		    return mIndexer.getPositionForSection(sectionIndex);
		}
		
		public int getSectionForPosition(int position) {
		    if (mIndexer == null) {
		        return -1;
		    }
		
		    return mIndexer.getSectionForPosition(position);
		}

		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
	        int totalItemCount) {
		    if (view instanceof PinnedHeaderListView) {
		        ((PinnedHeaderListView) view).configureHeaderView(firstVisibleItem);
		    }
		}
	
		public void onScrollStateChanged(AbsListView view, int scrollState) {
		}
	
		/**
		 * Computes the state of the pinned header.  It can be invisible, fully
		 * visible or partially pushed up out of the view.
		 */
		public int getPinnedHeaderState(int position) {
			final Cursor cursor = getCursor();
		    if (mIndexer == null || cursor == null || cursor.getCount() == 0) {
		        return PINNED_HEADER_GONE;
		    }
		
		    if (position < 0) {
		        return PINNED_HEADER_GONE;
		    }
		
		    // The header should get pushed up if the top item shown
		    // is the last item in a section for a particular letter.
		    int section = getSectionForPosition(position);
		    int nextSectionPosition = getPositionForSection(section + 1);
		    if (nextSectionPosition != -1 && position == nextSectionPosition - 1) {
		        return PINNED_HEADER_PUSHED_UP;
		    }
		
		    return PINNED_HEADER_VISIBLE;
		}
	
		/**
		 * Configures the pinned header by setting the appropriate text label
		 * and also adjusting color if necessary.  The color needs to be
		 * adjusted when the pinned header is being pushed up from the view.
		 */
		public void configurePinnedHeader(View header, int position, int alpha) {
		    PinnedHeaderCache cache = (PinnedHeaderCache) header.getTag();
	
		    if (cache == null) {
		        cache = new PinnedHeaderCache();
		        cache.titleView = (TextView) header.findViewById(R.id.header_text);
		        cache.textColor = cache.titleView.getTextColors();
		        cache.background = header.getBackground();
		        header.setTag(cache);
		    }
		
		    int section = getSectionForPosition(position);
		    
		    if (section != -1) {
			    String title = (String) mIndexer.getSections()[section];
			    cache.titleView.setText(title);
			
			    if (alpha == 255) {
			        // Opaque: use the default background, and the original text color
			        header.setBackgroundDrawable(cache.background);
			        cache.titleView.setTextColor(cache.textColor);
			    } else {
			        // Faded: use a solid color approximation of the background, and
			        // a translucent text color
			    	final int diffAlpha = 255 - alpha;
			    	final int red = Color.red(mPinnedHeaderBackgroundColor);
			    	final int diffRed = 255 - red;
			    	final int green = Color.green(mPinnedHeaderBackgroundColor);
			    	final int diffGreen = 255 - green;
			    	final int blue = Color.blue(mPinnedHeaderBackgroundColor);
			    	final int diffBlue = 255 - blue;
			        header.setBackgroundColor(Color.rgb(
			        		red + (diffRed * diffAlpha / 255),
			        		green + (diffGreen * diffAlpha / 255),
			        		blue + (diffBlue * diffAlpha / 255)));
			
			        int textColor = cache.textColor.getDefaultColor();
			        cache.titleView.setTextColor(Color.argb(alpha,
			                Color.red(textColor), Color.green(textColor), Color.blue(textColor)));
			    }
		    }
		}

    }
    
    /**
     * {@link CursorAdapter} that renders a {@link SearchQuery}.
     */
    private class SearchAdapter extends BaseAdapter implements PinnedHeaderListView.PinnedHeaderAdapter {
    
    	public SearchAdapter(Context context) {
            super(context);
        }

        @Override
		public int getPinnedHeaderState(int position) {
			return 0;
		}

		@Override
		public void configurePinnedHeader(View header, int position, int alpha) {
		}

		/** {@inheritDoc} */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View v = getLayoutInflater().inflate(R.layout.list_item_session, parent,false);
			findAndCacheViews(v);
			return v;
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
        	SessionItemViews views = (SessionItemViews) view.getTag();
        	views.trackView.setBackgroundColor(cursor.getInt(SearchQuery.TRACK_COLOR));
        	views.titleView.setText(cursor.getString(SearchQuery.TITLE));
            final String snippet = cursor.getString(SearchQuery.SEARCH_SNIPPET);
            final Spannable styledSnippet = buildStyledSnippet(snippet);
            views.subtitleView.setText(styledSnippet);
            final boolean starred = cursor.getInt(SearchQuery.STARRED) != 0;
            views.starButton.setVisibility(starred ? View.VISIBLE : View.INVISIBLE);
            views.starButton.setChecked(starred);
            views.dividerView.setVisibility(View.GONE);
            views.headerView.setVisibility(View.GONE);
        }
    }

    private Runnable mRefreshSessionsRunnable = new Runnable() {
        public void run() {
            if (mAdapter != null) {
                // This is used to refresh session title colors.
                mAdapter.notifyDataSetChanged();
            }

            // Check again on the next quarter hour, with some padding to account for network
            // time differences.
            long nextQuarterHour = (SystemClock.uptimeMillis() / 900000 + 1) * 900000 + 5000;
            mMessageQueueHandler.postAtTime(mRefreshSessionsRunnable, nextQuarterHour);
        }
    };

    /** {@link Sessions} query parameters. */
    private interface SessionsQuery {
    	String[] PROJECTION = {
                BaseColumns._ID,
                Sessions.SESSION_ID,
                Sessions.TITLE,
                Sessions.STARRED,
                Blocks.BLOCK_START,
                Blocks.BLOCK_END,
                Rooms.NAME,
                Tracks.TRACK_COLOR,
                Sessions.STARRED_IN_BLOCK_COUNT,
        };

        int _ID = 0;
        int SESSION_ID = 1;
        int TITLE = 2;
        int STARRED = 3;
        int BLOCK_START = 4;
        int BLOCK_END = 5;
        int ROOM_NAME = 6;
        int TRACK_COLOR = 7;
        int STARRED_IN_BLOCK_COUNT = 8;
    }

	/** {@link Sessions} search query parameters. */
    private interface SearchQuery {
    	String[] PROJECTION = {
                BaseColumns._ID,
                Sessions.SESSION_ID,
                Sessions.TITLE,
                Sessions.SEARCH_SNIPPET,
                Sessions.STARRED,
                Tracks.TRACK_COLOR,
        };

        int _ID = 0;
        int SESSION_ID = 1;
        int TITLE = 2;
        int SEARCH_SNIPPET = 3;
        int STARRED = 4;
        int TRACK_COLOR = 5;
     
    }
    
}
