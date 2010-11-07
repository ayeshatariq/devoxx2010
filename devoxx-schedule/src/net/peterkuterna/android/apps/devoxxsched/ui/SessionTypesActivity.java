package net.peterkuterna.android.apps.devoxxsched.ui;

import net.peterkuterna.android.apps.devoxxsched.R;
import net.peterkuterna.android.apps.devoxxsched.R.id;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Types;
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
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class SessionTypesActivity extends ListActivity implements AsyncQueryListener {

    private TypesAdapter mAdapter;

    private NotifyingAsyncQueryHandler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!getIntent().hasCategory(Intent.CATEGORY_TAB)) {
            setContentView(R.layout.activity_sessiontypes);

            final String customTitle = getIntent().getStringExtra(Intent.EXTRA_TITLE);
            ((TextView) findViewById(R.id.title_text)).setText(
                    customTitle != null ? customTitle : getTitle());
        } else {
            setContentView(R.layout.activity_sessiontypes_content);
        }

        mAdapter = new TypesAdapter(this);
        setListAdapter(mAdapter);

        final Intent intent = getIntent();
        final Uri typesUri = intent.getData();

        // Start background query to load types
        mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
        mHandler.startQuery(typesUri, TypesQuery.PROJECTION_WITH_SESSIONS_COUNT, Types.SESSIONS_COUNT + ">0", null, Types.DEFAULT_SORT);
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
        final String typeId = cursor.getString(TypesQuery.TYPE_ID);
        final String typeName = cursor.getString(TypesQuery.TYPE_NAME);
        final String typeTitle = "Sessions for '" + typeName + "'";
        final Uri sessionUri = Types.buildSessionsDirUri(typeId);
        final Intent intent = new Intent(Intent.ACTION_VIEW, sessionUri);
        intent.putExtra(Intent.EXTRA_TITLE, typeTitle);
        intent.putExtra(SessionsActivity.EXTRA_FAST_SCROLL, true);
        startActivity(intent);
    }

    /**
     * {@link CursorAdapter} that renders a {@link TypesQuery}.
     */
    private class TypesAdapter extends CursorAdapter {
    	
        public TypesAdapter(Context context) {
            super(context, null);
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getLayoutInflater().inflate(R.layout.list_item_sessiontype, parent, false);
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final TextView typeView = (TextView) view.findViewById(id.sessiontype_name);
            typeView.setText(cursor.getString(TypesQuery.TYPE_NAME));
            final TextView descrView = (TextView) view.findViewById(id.sessiontype_description);
            descrView.setText(cursor.getString(TypesQuery.TYPE_DESCRIPTION));
            final TextView countView = (TextView) view.findViewById(id.sessiontype_count);
            countView.setText(cursor.getString(TypesQuery.SESSION_COUNT));
        }
    }

    /** {@link Types} query parameters. */
    private interface TypesQuery {
        String[] PROJECTION_WITH_SESSIONS_COUNT = {
                BaseColumns._ID,
                Types.TYPE_ID,
                Types.TYPE_NAME,
                Types.TYPE_DESCRIPTION,
                Types.SESSIONS_COUNT,
        };

        int _ID = 0;
        int TYPE_ID = 1;
        int TYPE_NAME = 2;
        int TYPE_DESCRIPTION = 3;
        int SESSION_COUNT = 4;
    }

}
