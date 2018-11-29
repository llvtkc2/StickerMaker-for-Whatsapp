/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.idoideas.stickermaker.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.idoideas.stickermaker.BuildConfig;
import com.idoideas.stickermaker.utils.DataArchiver;
import com.idoideas.stickermaker.R;
import com.idoideas.stickermaker.utils.StickerBook;
import com.idoideas.stickermaker.models.StickerPack;
import com.idoideas.stickermaker.adapter.StickerPreviewAdapter;
import com.idoideas.stickermaker.utils.WhitelistCheck;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class StickerPackDetailsActivity extends BaseActivity {

    /**
     * Do not change below values of below 3 lines as this is also used by WhatsApp
     */
    public static final String EXTRA_STICKER_PACK_ID = "sticker_pack_id";
    public static final String EXTRA_STICKER_PACK_AUTHORITY = "sticker_pack_authority";
    public static final String EXTRA_STICKER_PACK_NAME = "sticker_pack_name";

    public static final int ADD_PACK = 200;
    public static final String EXTRA_STICKER_PACK_WEBSITE = "sticker_pack_website";
    public static final String EXTRA_STICKER_PACK_EMAIL = "sticker_pack_email";
    public static final String EXTRA_STICKER_PACK_PRIVACY_POLICY = "sticker_pack_privacy_policy";
    public static final String EXTRA_STICKER_PACK_TRAY_ICON = "sticker_pack_tray_icon";
    public static final String EXTRA_SHOW_UP_BUTTON = "show_up_button";
    public static final String EXTRA_STICKER_PACK_DATA = "sticker_pack";
    private static final String TAG = "StickerPackDetails";

    private RecyclerView recyclerView;
    private GridLayoutManager layoutManager;
    private StickerPreviewAdapter stickerPreviewAdapter;
    private int numColumns;
    private View addButton;
    private View alreadyAddedText;
    private StickerPack stickerPack;
    private View divider;
    private WhiteListCheckAsyncTask whiteListCheckAsyncTask;
    private FrameLayout shareButton;
    private FrameLayout deleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sticker_pack_details);


        boolean showUpButton = getIntent().getBooleanExtra(EXTRA_SHOW_UP_BUTTON, false);
        stickerPack = StickerBook.getStickerPackById(getIntent().getStringExtra(EXTRA_STICKER_PACK_DATA));
        TextView packNameTextView = findViewById(R.id.pack_name);
        TextView packPublisherTextView = findViewById(R.id.author);
        ImageView packTrayIcon = findViewById(R.id.tray_image);

        addButton = findViewById(R.id.add_to_whatsapp_button);
        shareButton = findViewById(R.id.share_pack_button);
        deleteButton = findViewById(R.id.delete_pack_button);
        alreadyAddedText = findViewById(R.id.already_added_text);
        layoutManager = new GridLayoutManager(this, 1);
        recyclerView = findViewById(R.id.sticker_list);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(pageLayoutListener);
        recyclerView.addOnScrollListener(dividerScrollListener);
        divider = findViewById(R.id.divider);
        if (stickerPreviewAdapter == null) {
            stickerPreviewAdapter = new StickerPreviewAdapter(getLayoutInflater(), R.drawable.sticker_error, getResources().getDimensionPixelSize(R.dimen.sticker_pack_details_image_size), getResources().getDimensionPixelSize(R.dimen.sticker_pack_details_image_padding), stickerPack);
            recyclerView.setAdapter(stickerPreviewAdapter);
        }
        packNameTextView.setText(stickerPack.name);
        packPublisherTextView.setText(stickerPack.publisher);
        packTrayIcon.setImageURI(stickerPack.getTrayImageUri());
        findViewById(R.id.add_sticker_to_pack).setOnClickListener(view -> openFile());

        addButton.setOnClickListener(v -> {
            if(stickerPack.getStickers().size()>=3) {
                StickerPackDetailsActivity.this.addStickerPackToWhatsApp(stickerPack);
            } else {
                AlertDialog alertDialog = new AlertDialog.Builder(StickerPackDetailsActivity.this)
                        .setNegativeButton("Ok", (dialogInterface, i) -> dialogInterface.dismiss()).create();
                alertDialog.setTitle("Invalid Action");
                alertDialog.setMessage("In order to be applied to WhatsApp, the sticker pack must have at least 3 stickers. Please add more stickers first.");
                alertDialog.show();
            }
        });

        shareButton.setOnClickListener(view -> DataArchiver.createZipFileFromStickerPack(stickerPack, StickerPackDetailsActivity.this));

        deleteButton.setOnClickListener(view -> {
            AlertDialog alertDialog = new AlertDialog.Builder(StickerPackDetailsActivity.this)
                    .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                    .setPositiveButton("Confirm", (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                        StickerBook.deleteStickerPackById(stickerPack.getIdentifier());
                        finish();
                        Intent intent = new Intent(StickerPackDetailsActivity.this, StickerPackListActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        Toast.makeText(StickerPackDetailsActivity.this, "Sticker Pack deleted", Toast.LENGTH_SHORT).show();
                    }).create();
            alertDialog.setTitle("Are you sure?");
            alertDialog.setMessage("Deleting this package will also remove it from your WhatsApp app.");
            alertDialog.show();
        });
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(showUpButton);
            getSupportActionBar().setTitle(showUpButton ? R.string.title_activity_sticker_pack_details_multiple_pack : R.string.title_activity_sticker_pack_details_single_pack);
        }
    }


    private void openFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        i.setType("image/*");
        startActivityForResult(i, 3000);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }


    private void addStickerPackToWhatsApp(StickerPack sp) {
        Intent intent = new Intent();
        intent.setAction("com.whatsapp.intent.action.ENABLE_STICKER_PACK");
        Log.w("IS IT A NEW IDENTIFIER?", sp.getIdentifier());
        intent.putExtra(EXTRA_STICKER_PACK_ID, sp.getIdentifier());
        intent.putExtra(EXTRA_STICKER_PACK_AUTHORITY, BuildConfig.CONTENT_PROVIDER_AUTHORITY);
        intent.putExtra(EXTRA_STICKER_PACK_NAME, sp.getName());
        try {
            startActivityForResult(intent, 200);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.error_adding_sticker_pack, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_PACK) {
            if (resultCode == Activity.RESULT_CANCELED && data != null) {
                final String validationError = data.getStringExtra("validation_error");
                if (validationError != null) {
                    if (BuildConfig.DEBUG) {
                        //validation error should be shown to developer only, not users.
                        MessageDialogFragment.newInstance(R.string.title_validation_error, validationError).show(getSupportFragmentManager(), "validation error");
                    }
                    Log.e(TAG, "Validation failed:" + validationError);
                }
            }
        } else if(requestCode == 3000){
            if(data!=null){
                if(data.getClipData()!=null){
                    ClipData clipData = data.getClipData();
                    for(int i = 0; i < clipData.getItemCount(); i++)
                    {
                        ClipData.Item path = clipData.getItemAt(i);
                        Uri uri = path.getUri();
                        getContentResolver().takePersistableUriPermission(Objects.requireNonNull(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        stickerPack.addSticker(uri, this);
                    }
                } else {
                    Uri uri = data.getData();
                    getContentResolver().takePersistableUriPermission(Objects.requireNonNull(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    stickerPack.addSticker(uri, this);
                }
                finish();
                startActivity(getIntent());
            }
        }
    }

    private final ViewTreeObserver.OnGlobalLayoutListener pageLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            setNumColumns(recyclerView.getWidth() / recyclerView.getContext().getResources().getDimensionPixelSize(R.dimen.sticker_pack_details_image_size));
        }
    };

    private void setNumColumns(int numColumns) {
        if (this.numColumns != numColumns) {
            layoutManager.setSpanCount(numColumns);
            this.numColumns = numColumns;
            if (stickerPreviewAdapter != null) {
                stickerPreviewAdapter.notifyDataSetChanged();
            }
        }
    }

    private final RecyclerView.OnScrollListener dividerScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(@NonNull final RecyclerView recyclerView, final int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            updateDivider(recyclerView);
        }

        @Override
        public void onScrolled(@NonNull final RecyclerView recyclerView, final int dx, final int dy) {
            super.onScrolled(recyclerView, dx, dy);
            updateDivider(recyclerView);
        }

        private void updateDivider(RecyclerView recyclerView) {
            boolean showDivider = recyclerView.computeVerticalScrollOffset() > 0;
            if (divider != null) {
                divider.setVisibility(showDivider ? View.VISIBLE : View.INVISIBLE);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        whiteListCheckAsyncTask = new WhiteListCheckAsyncTask(this);
        whiteListCheckAsyncTask.execute(stickerPack);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (whiteListCheckAsyncTask != null && !whiteListCheckAsyncTask.isCancelled()) {
            whiteListCheckAsyncTask.cancel(true);
        }
    }

    private void updateAddUI(Boolean isWhitelisted) {
        if (isWhitelisted) {
            addButton.setVisibility(View.GONE);
            alreadyAddedText.setVisibility(View.VISIBLE);
        } else {
            addButton.setVisibility(View.VISIBLE);
            alreadyAddedText.setVisibility(View.GONE);
        }
    }

    static class WhiteListCheckAsyncTask extends AsyncTask<StickerPack, Void, Boolean> {
        private final WeakReference<StickerPackDetailsActivity> stickerPackDetailsActivityWeakReference;

        WhiteListCheckAsyncTask(StickerPackDetailsActivity stickerPackListActivity) {
            this.stickerPackDetailsActivityWeakReference = new WeakReference<>(stickerPackListActivity);
        }

        @Override
        protected final Boolean doInBackground(StickerPack... stickerPacks) {
            StickerPack stickerPack = stickerPacks[0];
            final StickerPackDetailsActivity stickerPackDetailsActivity = stickerPackDetailsActivityWeakReference.get();
            //noinspection SimplifiableIfStatement
            if (stickerPackDetailsActivity == null) {
                return false;
            }
            return WhitelistCheck.isWhitelisted(stickerPackDetailsActivity, stickerPack.identifier);
        }

        @Override
        protected void onPostExecute(Boolean isWhitelisted) {
            final StickerPackDetailsActivity stickerPackDetailsActivity = stickerPackDetailsActivityWeakReference.get();
            if (stickerPackDetailsActivity != null) {
                stickerPackDetailsActivity.updateAddUI(isWhitelisted);
            }
        }
    }
}
