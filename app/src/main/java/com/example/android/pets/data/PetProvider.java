package com.example.android.pets.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import static com.example.android.pets.data.PetContract.CONTENT_AUTHORITY;
import static com.example.android.pets.data.PetContract.PATH_PETS;
import static com.example.android.pets.data.PetContract.PetEntry;

public class PetProvider extends ContentProvider {

    //Tag for log messages
    private static final String LOG_TAG = PetProvider.class.getSimpleName();

    private static final int PETS = 100;

    private static final int PET_ID = 101;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(CONTENT_AUTHORITY, PATH_PETS, PETS);
        uriMatcher.addURI(CONTENT_AUTHORITY, PATH_PETS + "/#", PET_ID);
    }

    private PetDbHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = new PetDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {

        SQLiteDatabase database = dbHelper.getReadableDatabase();

        Cursor cursor;

        int match = uriMatcher.match(uri);

        switch (match) {
            case PETS:
                cursor = database.query(PetEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;

            case PET_ID:
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(PetEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;

            default:
                throw new IllegalArgumentException("Cannot query unknown Uri : " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        int match = uriMatcher.match(uri);

        switch (match) {
            case PETS:
                return PetEntry.CONTENT_LIST_TYPE;

            case PET_ID:
                return PetEntry.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {

        Uri retUri = null;
        int match = uriMatcher.match(uri);
        switch (match) {
            case PETS:
                Long id = insertPet(uri, values);
                if (id == -1)
                    Log.e(LOG_TAG, "Failed to insert row for " + uri);
                else
                    retUri = ContentUris.withAppendedId(PetEntry.CONTENT_URI, id);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown Uri : " + uri);
        }
        return retUri;
    }

    private long insertPet(Uri uri, @Nullable ContentValues values) {

        // Check that the name is not null
        String name = values.getAsString(PetEntry.COLUMN_PET_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Pet requires a name");
        }

        Integer weight = values.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);
        if (weight != null && weight <= 0) {
            throw new IllegalArgumentException("Pet requires valid weight");
        }

        Integer gender = values.getAsInteger(PetEntry.COLUMN_PET_GENDER);
        if (gender == null || !PetEntry.isValidGender(gender)) {
            throw new IllegalArgumentException("Pet requires a gender");
        }

        SQLiteDatabase database = dbHelper.getWritableDatabase();
        Long new_row_id = database.insert(PetEntry.TABLE_NAME, null, values);
        if (new_row_id != -1)
            getContext().getContentResolver().notifyChange(uri, null);

        return new_row_id;
    }


    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        int match = uriMatcher.match(uri);
        switch (match) {
            case PETS:
                return deletePet(uri, selection, selectionArgs);
            case PET_ID:
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return deletePet(uri, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Cannot query unknown Uri : " + uri);
        }
    }

    private int deletePet(Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        int rowsDeleted = database.delete(PetEntry.TABLE_NAME, selection, selectionArgs);
        if (rowsDeleted != 0)
            getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }


    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String
            selection, @Nullable String[] selectionArgs) {

        int match = uriMatcher.match(uri);

        if (values.size() == 0)
            return 0;

        switch (match) {
            case PETS:
                return updatePets(uri, values, selection, selectionArgs);

            case PET_ID:
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updatePets(uri, values, selection, selectionArgs);

            default:
                throw new IllegalArgumentException("Cannot query unknown Uri : " + uri);
        }
    }

    private int updatePets(Uri uri, @Nullable ContentValues values, @Nullable String
            selection, @Nullable String[] selectionArgs) {

        SQLiteDatabase database = dbHelper.getWritableDatabase();

        if (values.containsKey(PetEntry.COLUMN_PET_NAME)) {
            String name = values.getAsString(PetEntry.COLUMN_PET_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Pet requires a name");
            }
        }

        if (values.containsKey(PetEntry.COLUMN_PET_WEIGHT)) {
            Integer weight = values.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);
            if (weight != null && weight <= 0) {
                throw new IllegalArgumentException("Pet requires valid weight");
            }
        }

        if (values.containsKey(PetEntry.COLUMN_PET_GENDER)) {
            Integer gender = values.getAsInteger(PetEntry.COLUMN_PET_GENDER);
            if (gender == null || !PetEntry.isValidGender(gender)) {
                throw new IllegalArgumentException("Pet requires a gender");
            }
        }

        int rowsUpdated = database.update(PetEntry.TABLE_NAME, values, selection, selectionArgs);
        if (rowsUpdated != 0)
            getContext().getContentResolver().notifyChange(uri, null);

        return rowsUpdated;

    }
}
