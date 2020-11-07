package com.example.android.pets;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.android.pets.data.PetContract.PetEntry;

public class PetCursorAdapter extends CursorAdapter {

    public PetCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, true);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        TextView nameTextView = view.findViewById(R.id.name);
        TextView summaryTextView = view.findViewById(R.id.summary);

        nameTextView.setText(cursor.getString(cursor.getColumnIndex(PetEntry.COLUMN_PET_NAME)));
        String breed = cursor.getString(cursor.getColumnIndex(PetEntry.COLUMN_PET_BREED));
        if (TextUtils.isEmpty(breed))
            summaryTextView.setText("Unknown Breed");
        else
            summaryTextView.setText(breed);
    }
}
