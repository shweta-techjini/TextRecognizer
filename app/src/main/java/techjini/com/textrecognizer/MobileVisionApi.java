package techjini.com.textrecognizer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;

public class MobileVisionApi extends Activity implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    public static final int REQUEST_FILE = 101;
    public static final int READ_PERMISSION_REQUESTED = 105;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile_vision_api);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.button3:
                //textonimage
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        // Explain to the user why we need to read the contacts
                        showRationaleDialog();
                    } else {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                READ_PERMISSION_REQUESTED);
                    }
                } else {
                    openFileProvider();
                }

                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_FILE && data != null) {
                // only one photo is selected
                Uri uri = data.getData();
                // start phototag activity and pass data
                startFindTextOnImage(uri);
            }
        }
    }

    private void startFindTextOnImage(Uri selectedImageUri) {
        Intent intent = new Intent(this, FindTextOnImage.class);
        intent.setData(selectedImageUri);
        startActivity(intent);
    }

    private void showRationaleDialog() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle(getString(R.string.permission_denied));
        alertBuilder.setMessage(getString(R.string.rational_storage_permission));
        DialogInterface.OnClickListener positive = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ActivityCompat.requestPermissions(MobileVisionApi.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        READ_PERMISSION_REQUESTED);
                dialogInterface.dismiss();
            }
        };
        DialogInterface.OnClickListener negative = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        };
        alertBuilder.setPositiveButton(R.string.retry, positive);
        alertBuilder.setNegativeButton(R.string.i_m_sure, negative);
        AlertDialog alertDialog = alertBuilder.create();
        alertDialog.show();
    }

    private void openFileProvider() {
        try {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, getString(R.string.intent_chooser)), REQUEST_FILE);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case READ_PERMISSION_REQUESTED:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openFileProvider();
                } else {
                    boolean shouldShowRational = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    if (shouldShowRational) {
                        // Show rational message or directly ask for the request.
                        showRationaleDialog();
                    }
                }
                break;
        }
    }
}
