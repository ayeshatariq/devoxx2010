package net.peterkuterna.android.apps.devoxxsched.ui;

import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.R.id;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Rooms;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.SessionCounts;
import net.peterkuterna.android.apps.devoxxsched.util.NotifyingAsyncQueryHandler;
import net.peterkuterna.android.apps.devoxxsched.util.NotifyingAsyncQueryHandler.AsyncQueryListener;
import net.peterkuterna.android.apps.devoxxsched.util.UIUtils;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.NinePatchDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class RoomsActivity extends ListActivity implements AsyncQueryListener {

    private RoomsAdapter mAdapter;

    private NotifyingAsyncQueryHandler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!getIntent().hasCategory(Intent.CATEGORY_TAB)) {
            setContentView(R.layout.activity_rooms);

            final String customTitle = getIntent().getStringExtra(Intent.EXTRA_TITLE);
            ((TextView) findViewById(R.id.title_text)).setText(
                    customTitle != null ? customTitle : getTitle());
        } else {
            setContentView(R.layout.activity_rooms_content);
        }

        mAdapter = new RoomsAdapter(this);
        setListAdapter(mAdapter);

        final Intent intent = getIntent();
        final Uri roomsUri = intent.getData();

        // Start background query to load rooms
        mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
        mHandler.startQuery(roomsUri, RoomsQuery.PROJECTION_WITH_SESSIONS_COUNT, Rooms.SESSIONS_COUNT + ">0", null, Rooms.ROOM_NAME_SORT);
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
        final String roomId = cursor.getString(RoomsQuery.ROOM_ID);
        final String roomName = cursor.getString(RoomsQuery.ROOM_NAME);
        final String roomTitle = "Sessions for '" + roomName + "'";
        final Uri sessionUri = Rooms.buildSessionsDirUri(roomId)
        	.buildUpon()
        	.appendQueryParameter(SessionCounts.SESSION_INDEX_EXTRAS, Boolean.TRUE.toString())
        	.build();
        final Intent intent = new Intent(Intent.ACTION_VIEW, sessionUri);
        intent.putExtra(Intent.EXTRA_TITLE, roomTitle);
        intent.putExtra(SessionsActivity.EXTRA_FOCUS_CURRENT_NEXT_SESSION, true);
        startActivity(intent);
    }

    /**
     * {@link CursorAdapter} that renders a {@link RoomsQuery}.
     */
    private class RoomsAdapter extends CursorAdapter {
    	
        public RoomsAdapter(Context context) {
            super(context, null);
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getLayoutInflater().inflate(R.layout.list_item_room, parent, false);
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final TextView typeView = (TextView) view.findViewById(id.room_name);
            typeView.setText(cursor.getString(RoomsQuery.ROOM_NAME));
            final TextView descrView = (TextView) view.findViewById(id.room_capacity);
            descrView.setText("Capacity of " + cursor.getString(RoomsQuery.ROOM_CAPACITY) + " seats.");
            final TextView countView = (TextView) view.findViewById(id.room_count);
            countView.setText(cursor.getString(RoomsQuery.SESSION_COUNT));
            NinePatchDrawable drawable = (NinePatchDrawable) countView.getBackground();
            drawable.setColorFilter(new LightingColorFilter(getResources().getColor(R.color.foreground1), 1));
        }
    }

    /** {@link Rooms} query parameters. */
    private interface RoomsQuery {
        String[] PROJECTION_WITH_SESSIONS_COUNT = {
                BaseColumns._ID,
                Rooms.ROOM_ID,
                Rooms.NAME,
                Rooms.CAPACITY,
                Rooms.SESSIONS_COUNT,
        };

        int _ID = 0;
        int ROOM_ID = 1;
        int ROOM_NAME = 2;
        int ROOM_CAPACITY = 3;
        int SESSION_COUNT = 4;
    }

}
