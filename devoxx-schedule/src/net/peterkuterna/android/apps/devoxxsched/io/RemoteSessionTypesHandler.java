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

import static net.peterkuterna.android.apps.devoxxsched.util.ParserUtils.sanitizeId;

import java.util.ArrayList;
import java.util.HashSet;

import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract;
import net.peterkuterna.android.apps.devoxxsched.provider.ScheduleContract.Types;
import net.peterkuterna.android.apps.devoxxsched.util.Lists;
import net.peterkuterna.android.apps.devoxxsched.util.Sets;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.util.Log;


/**
 * Handle a remote {@link JSONArray} that defines a set of {@link Types}
 * entries.
 */
public class RemoteSessionTypesHandler extends JSONHandler {
	
    private static final String TAG = "SessionTypesHandler";

    public RemoteSessionTypesHandler() {
		super(ScheduleContract.CONTENT_AUTHORITY);
	}

	@Override
	public ArrayList<ContentProviderOperation> parse(ArrayList<JSONArray> entries, ContentResolver resolver) throws JSONException {
		final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
		final HashSet<String> typeIds = Sets.newHashSet();
		
		int nrEntries = 0;
		for (JSONArray types : entries) {
			Log.d(TAG, "Retrieved " + types.length() + " presentation types entries.");
			nrEntries += types.length();
	
	        for (int i=0; i < types.length(); i++) {
	            JSONObject type = types.getJSONObject(i);
	            String id = type.getString("id");
	            
	            final String typeId = sanitizeId(id);
	            typeIds.add(typeId);
	            ContentProviderOperation.Builder builder;
	            builder = ContentProviderOperation.newInsert(Types.CONTENT_URI);
	            builder.withValue(Types.TYPE_ID, typeId);
			    builder.withValue(Types.TYPE_NAME, type.getString("name"));
			    builder.withValue(Types.TYPE_DESCRIPTION, type.getString("description"));
			    
	        	batch.add(builder.build());
	        }
		}
        
        return batch;
	}

}
