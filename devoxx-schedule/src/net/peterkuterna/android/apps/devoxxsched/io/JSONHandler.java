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

package net.peterkuterna.android.apps.devoxxsched.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import net.peterkuterna.android.apps.devoxxsched.util.Sets;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;


/**
 * Abstract class that handles reading and parsing an {@link JSONArray} into
 * a set of {@link ContentProviderOperation}. It catches exceptions and 
 * rethrows them as {@link JSONHandlerException}. Any local
 * {@link ContentProvider} exceptions are considered unrecoverable.
 * <p>
 * This class is only designed to handle simple one-way synchronization.
 */
public abstract class JSONHandler extends BaseHandler {
	
	private static final String TAG = "JSONHandler"; 
	
    public JSONHandler(String mAuthority, int syncType) {
		super(mAuthority, syncType);
	}

	/**
     * Parse the given {@link JSONArray}, turning into a series of
     * {@link ContentProviderOperation} that are immediately applied using the
     * given {@link ContentResolver}.
     */
    public void parseAndApply(JSONArray entries, ContentResolver resolver) throws JSONHandlerException {
    	try {
	        final ArrayList<ContentProviderOperation> batch = parse(entries, resolver);
	        resolver.applyBatch(getAuthority(), batch);
        } catch (JSONException e) {
            throw new JSONHandlerException("Problem parsing JSON response", e);
        } catch (RemoteException e) {
            throw new RuntimeException("Problem applying batch operation", e);
        } catch (OperationApplicationException e) {
            throw new RuntimeException("Problem applying batch operation", e);
        }
    }

    /**
     * Parse the given {@link JSONHandler}, returning a set of
     * {@link ContentProviderOperation} that will bring the
     * {@link ContentProvider} into sync with the parsed data.
     */
    public abstract ArrayList<ContentProviderOperation> parse(JSONArray entries, ContentResolver resolver) throws JSONException;

	protected static boolean isRowExisting(Uri uri, String [] projection, ContentResolver resolver) {
		final Cursor cursor = resolver.query(uri, projection, null, null, null);
		try {
			if (!cursor.moveToFirst()) return false;
		} finally {
			cursor.close();
		}
		return true;
	}

	/**
	 * Returns those id's from a {@link Uri} that were not found in a given set. 
	 */
    protected static HashSet<String> getLostIds(Set<String> ids, Uri uri, String [] projection, int idColumnIndex, ContentResolver resolver) {
        final HashSet<String> lostIds = Sets.newHashSet();
        
        final Cursor cursor = resolver.query(uri, projection, null, null, null);
        try {
        	while (cursor.moveToNext()) {
        		final String id = cursor.getString(idColumnIndex);
        		if (!ids.contains(id)) {
        			lostIds.add(id);
        		}
        	}
        } finally {
            cursor.close();
        }
        
        if (!lostIds.isEmpty()) {
        	Log.d(TAG, "Found " + lostIds.size() + " for " + uri.toString() + " that need to be removed.");
        }
        
        return lostIds;
    }

	/**
     * General {@link IOException} that indicates a problem occured while
     * parsing or applying an {@link JSONArray}.
     */
    public static class JSONHandlerException extends IOException {
        public JSONHandlerException(String message) {
            super(message);
        }

        public JSONHandlerException(String message, Throwable cause) {
            super(message);
            initCause(cause);
        }

        @Override
        public String toString() {
            if (getCause() != null) {
                return getLocalizedMessage() + ": " + getCause();
            } else {
                return getLocalizedMessage();
            }
        }
    }
    
}
