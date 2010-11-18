package net.peterkuterna.android.apps.devoxxsched.io;

import android.content.ContentValues;
import android.net.Uri;

public class SyncOperation {

    public final static int TYPE_INSERT = 1;
    public final static int TYPE_UPDATE = 2;
    public final static int TYPE_DELETE = 3;

    private final static String TAG = "SyncOperation";

    private final int mType;
    private final Uri mUri;
    private final ContentValues mValues;

    public SyncOperation(Builder builder) {
        mType = builder.mType;
        mUri = builder.mUri;
        mValues = builder.mValues;
    }
    
    public static Builder newInsert(Uri uri) {
    	return new Builder(uri, TYPE_INSERT);
    }

    public static Builder newUpdate(Uri uri) {
    	return new Builder(uri, TYPE_UPDATE);
    }

    public static Builder newDelete(Uri uri) {
    	return new Builder(uri, TYPE_DELETE);
    }

    public int getType() {
		return mType;
	}

	public Uri getUri() {
		return mUri;
	}

	public ContentValues getValues() {
		return mValues;
	}
	
	public boolean isInsertOperation() {
		return mType == TYPE_INSERT;
	}

	public boolean isUpdateOperation() {
		return mType == TYPE_UPDATE;
	}

	public boolean isDeleteOperation() {
		return mType == TYPE_DELETE;
	}

	public static class Builder {
		
		private final int mType;
		private final Uri mUri;
		private ContentValues mValues;
		
		public Builder(Uri uri, int type) {
			this.mUri = uri;
			this.mType = type;
		}
		
		public SyncOperation build() {
			return new SyncOperation(this);
		}
		
        public Builder withValue(String key, Object value) {
            if (mValues == null) {
                mValues = new ContentValues();
            }
            if (value == null) {
                mValues.putNull(key);
            } else if (value instanceof String) {
                mValues.put(key, (String) value);
            } else if (value instanceof Byte) {
                mValues.put(key, (Byte) value);
            } else if (value instanceof Short) {
                mValues.put(key, (Short) value);
            } else if (value instanceof Integer) {
                mValues.put(key, (Integer) value);
            } else if (value instanceof Long) {
                mValues.put(key, (Long) value);
            } else if (value instanceof Float) {
                mValues.put(key, (Float) value);
            } else if (value instanceof Double) {
                mValues.put(key, (Double) value);
            } else if (value instanceof Boolean) {
                mValues.put(key, (Boolean) value);
            } else if (value instanceof byte[]) {
                mValues.put(key, (byte[]) value);
            }
            return this;
        }

	}
	
}
