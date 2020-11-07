/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.example.android.pets;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.android.pets.data.PetContract.PetEntry;

/**
 * Allows user to create a new pet or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * EditText field to enter the pet's name
     */
    private EditText mNameEditText;

    /**
     * EditText field to enter the pet's breed
     */
    private EditText mBreedEditText;

    /**
     * EditText field to enter the pet's weight
     */
    private EditText mWeightEditText;

    /**
     * EditText field to enter the pet's gender
     */
    private Spinner mGenderSpinner;

    /**
     * Gender of the pet. The possible valid values are in the PetContract.java file:
     * {@link PetEntry#GENDER_UNKNOWN}, {@link PetEntry#GENDER_MALE}, or
     * {@link PetEntry#GENDER_FEMALE}.
     */
    private int mGender = PetEntry.GENDER_UNKNOWN;

    private ArrayAdapter<CharSequence> genderSpinnerAdapter;

    private Uri currentPetUri;

    private boolean petHasChanged = false;

    private OnTouchListener onTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            petHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Find all relevant views that we will need to read user input from
        mNameEditText = findViewById(R.id.edit_pet_name);
        mBreedEditText = findViewById(R.id.edit_pet_breed);
        mWeightEditText = findViewById(R.id.edit_pet_weight);
        mGenderSpinner = findViewById(R.id.spinner_gender);
        setupSpinner();

        mNameEditText.setOnTouchListener(onTouchListener);
        mWeightEditText.setOnTouchListener(onTouchListener);
        mBreedEditText.setOnTouchListener(onTouchListener);
        mGenderSpinner.setOnTouchListener(onTouchListener);

        Intent intent = getIntent();
//        String uri = intent.getStringExtra("Uri");

        currentPetUri = intent.getData();
        if (currentPetUri != null) {
            setTitle(R.string.editor_activity_title_edit_pet);
            getLoaderManager().initLoader(1, null, this);
        } else {
            setTitle(R.string.editor_activity_title_new_pet);
            invalidateOptionsMenu();
        }
    }

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListner) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListner);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        if (!petHasChanged) {
            super.onBackPressed();
            return;
        }
        DialogInterface.OnClickListener discardButtonOnClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        };
        showUnsavedChangesDialog(discardButtonOnClickListener);
    }

    /**
     * Setup the dropdown spinner that allows the user to select the gender of the pet.
     */
    private void setupSpinner() {
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        genderSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_gender_options, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        genderSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mGenderSpinner.setAdapter(genderSpinnerAdapter);

        // Set the integer mSelected to the constant values
        mGenderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.gender_male))) {
                        mGender = PetEntry.GENDER_MALE;
                    } else if (selection.equals(getString(R.string.gender_female))) {
                        mGender = PetEntry.GENDER_FEMALE;
                    } else {
                        mGender = PetEntry.GENDER_UNKNOWN;
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mGender = PetEntry.GENDER_UNKNOWN;
            }
        });
    }

    /**
     * Get user input from editor and save new pet into database.
     */
    private void savePet() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String nameString = mNameEditText.getText().toString().trim();
        String breedString = mBreedEditText.getText().toString().trim();
        String weightString = mWeightEditText.getText().toString().trim();
        if (TextUtils.isEmpty(nameString) || TextUtils.isEmpty(weightString) || mGender == PetEntry.GENDER_UNKNOWN) {
            return;
        } else {
            int weight = Integer.parseInt(weightString);

            // Create a ContentValues object where column names are the keys,
            // and pet attributes from the editor are the values.
            ContentValues values = new ContentValues();
            values.put(PetEntry.COLUMN_PET_NAME, nameString);
            values.put(PetEntry.COLUMN_PET_BREED, breedString);
            values.put(PetEntry.COLUMN_PET_GENDER, mGender);
            values.put(PetEntry.COLUMN_PET_WEIGHT, weight);


            if (currentPetUri == null) {
                Uri uri = getContentResolver().insert(PetEntry.CONTENT_URI, values);
                if (uri == null)
                    Toast.makeText(this, getString(R.string.editor_insert_pet_failed), Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, getString(R.string.editor_insert_pet_successful), Toast.LENGTH_SHORT).show();
            } else {
                int rowsUpdated = getContentResolver().update(currentPetUri, values, null, null);
                if (rowsUpdated == 0)
                    Toast.makeText(this, getString(R.string.editor_update_pet_failed), Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, getString(R.string.editor_update_pet_successful), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        if (currentPetUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save pet to database
                savePet();
                // Exit activity
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Do nothing for now
                confirmDeleteDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                if (!petHasChanged) {
                    // Navigate back to parent activity (CatalogActivity)
                    NavUtils.navigateUpFromSameTask(this);
                    return true;
                }
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = new String[]{
                PetEntry._ID,
                PetEntry.COLUMN_PET_NAME,
                PetEntry.COLUMN_PET_BREED,
                PetEntry.COLUMN_PET_WEIGHT,
                PetEntry.COLUMN_PET_GENDER
        };
        return new CursorLoader(this, currentPetUri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        if (cursor.moveToNext()) {
            mNameEditText.setText(cursor.getString(cursor.getColumnIndex(PetEntry.COLUMN_PET_NAME)));
            mBreedEditText.setText(cursor.getString(cursor.getColumnIndex(PetEntry.COLUMN_PET_BREED)));
            mWeightEditText.setText(cursor.getString(cursor.getColumnIndex(PetEntry.COLUMN_PET_WEIGHT)));
            int gender = cursor.getInt(cursor.getColumnIndex(PetEntry.COLUMN_PET_GENDER));

            switch (gender) {
                case PetEntry.GENDER_MALE:
                    mGenderSpinner.setSelection(genderSpinnerAdapter.getPosition(getString(R.string.gender_male)));
                    break;
                case PetEntry.GENDER_FEMALE:
                    mGenderSpinner.setSelection(genderSpinnerAdapter.getPosition(getString(R.string.gender_female)));
                    break;
                default:
                    mGenderSpinner.setSelection(genderSpinnerAdapter.getPosition(getString(R.string.gender_unknown)));
                    break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private void deletePet() {
        int rowsDeleted = getContentResolver().delete(currentPetUri, null, null);
        if (rowsDeleted == 0)
            Toast.makeText(this, R.string.editor_delete_pet_failed, Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, R.string.editor_delete_pet_successful, Toast.LENGTH_SHORT).show();
    }

    private void confirmDeleteDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(R.string.delete_dialog_msg);

        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deletePet();
                finish();
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (dialog != null)
                    dialog.dismiss();
            }
        });

        builder.create().show();

    }
}