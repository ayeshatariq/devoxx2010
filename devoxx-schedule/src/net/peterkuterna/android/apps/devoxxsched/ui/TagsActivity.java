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
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Tags;
import net.peterkuterna.android.apps.devoxxsched.util.NotifyingAsyncQueryHandler;
import net.peterkuterna.android.apps.devoxxsched.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AlphabetIndexer;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

/**
 * {@link ListActivity} that displays a set of {@link Tags}
 */
public class TagsActivity extends ListActivity implements AsyncQueryListener {

    private TagsAdapter mAdapter;

    private NotifyingAsyncQueryHandler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!getIntent().hasCategory(Intent.CATEGORY_TAB)) {
            setContentView(R.layout.activity_tags);

            final String customTitle = getIntent().getStringExtra(Intent.EXTRA_TITLE);
            ((TextView) findViewById(R.id.title_text)).setText(
                    customTitle != null ? customTitle : getTitle());
        } else {
            setContentView(R.layout.activity_tags_content);
        }

        mAdapter = new TagsAdapter(this);
        setListAdapter(mAdapter);

        final Intent intent = getIntent();
        final Uri tagsUri = intent.getData();

        // Start background query to load tags
        mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
        mHandler.startQuery(tagsUri, TagsQuery.PROJECTION_WITH_SESSIONS_COUNT, Tags.SESSIONS_COUNT + ">0", null, Tags.DEFAULT_SORT);
    }

    /** {@inheritDoc} */
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        startManagingCursor(cursor);
        mAdapter.changeCursor(cursor);
    }

    /** Handle "home" title-bar action. */
    public void onHomeClick(View v) {
        UIUtils.goHome(this);
    }

    /** Handle "search" title-bar action. */
    public void onSearchClick(View v) {
        UIUtils.goSearch(this);
    }

    /** {@inheritDoc} */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // Launch viewer for specific tag
        final Cursor cursor = (Cursor) mAdapter.getItem(position);
        final String tagId = cursor.getString(TagsQuery.TAG_ID);
        final String tagName = cursor.getString(TagsQuery.TAG_NAME);
        final String tagTitle = "Sessions for '" + tagName + "'";
        final Uri sessionUri = Tags.buildSessionsDirUri(tagId);
        final Intent intent = new Intent(Intent.ACTION_VIEW, sessionUri);
        intent.putExtra(Intent.EXTRA_TITLE, tagTitle);
        intent.putExtra(SessionsActivity.EXTRA_FOCUS_CURRENT_NEXT_SESSION, true);
        startActivity(intent);
    }

    /**
     * {@link CursorAdapter} that renders a {@link TagsQuery}.
     */
    private class TagsAdapter extends CursorAdapter implements SectionIndexer {
    	
    	private AlphabetIndexer mIndexer;
    	
        public TagsAdapter(Context context) {
            super(context, null);

            mIndexer = new AlphabetIndexer(null, TagsQuery.TAG_NAME, "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        }

		@Override
		public void changeCursor(Cursor cursor) {
			super.changeCursor(cursor);
			
			mIndexer.setCursor(cursor);
		}

		@Override
		public int getPositionForSection(int section) {
			return mIndexer.getPositionForSection(section);
		}

		@Override
		public int getSectionForPosition(int position) {
			return mIndexer.getSectionForPosition(position);
		}

		@Override
		public Object[] getSections() {
			return mIndexer.getSections();
		}
          
        /** {@inheritDoc} */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getLayoutInflater().inflate(R.layout.list_item_tag, parent, false);
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final TextView tagView = (TextView) view.findViewById(android.R.id.text1);
            tagView.setText(cursor.getString(TagsQuery.TAG_NAME));
            final TextView tagCountView = (TextView) view.findViewById(android.R.id.text2);
            tagCountView.setText(cursor.getString(TagsQuery.SESSION_COUNT));
        }
    }

    /** {@link Tags} query parameters. */
    private interface TagsQuery {
        String[] PROJECTION_WITH_SESSIONS_COUNT = {
                BaseColumns._ID,
                Tags.TAG_ID,
                Tags.TAG_NAME,
                Tags.SESSIONS_COUNT,
        };

        int _ID = 0;
        int TAG_ID = 1;
        int TAG_NAME = 2;
        int SESSION_COUNT = 3;
    }

}
