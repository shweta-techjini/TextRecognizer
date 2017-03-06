package techjini.com.textrecognizer;

import android.Manifest;
import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Patterns;
import android.util.SparseArray;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;


public class FindTextOnImage extends Activity implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {
    private ArrayList<Text> lines;
    private String personName;
    private String selectedImagePath;
    private ArrayList<String> phoneNumbers, emailIds, webSites;
    private LinearLayout contactDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_text_on_image);

        Uri selectedImage = getIntent().getData();
        if (selectedImage != null) {
            selectedImagePath = PathUtils.getPath(this, selectedImage);
        }

        TextRecognizer textRecognizer = new TextRecognizer.Builder(this).build();

        Bitmap imageWithTextBitmap = BitmapFactory.decodeFile(selectedImagePath);
        imageWithTextBitmap = correctImagerotation(imageWithTextBitmap);
        ((ImageView) findViewById(R.id.image)).setImageBitmap(imageWithTextBitmap);
        contactDetails = (LinearLayout) findViewById(R.id.contact_details);

        Frame frame = new Frame.Builder().setBitmap(imageWithTextBitmap).build();
        SparseArray<TextBlock> items = textRecognizer.detect(frame);
        StringBuilder stringBuilder = new StringBuilder();

        lines = new ArrayList<>();
        for (int i = 0; i < items.size(); ++i) {
            TextBlock item = items.valueAt(i);
            if (item != null && item.getValue() != null) {
                stringBuilder.append(item.getValue());
                ArrayList<Text> texts = (ArrayList) item.getComponents();

                for (int j = 0; j < texts.size(); j++) {
                    lines.add(texts.get(j));
                }

                // Since the identified text blocks are not arranged in order we can sort them based on the Rect. Rect(left, top, right, bottom)
            }
        }

        Log.d("FindTextOnImage", "" + stringBuilder);
        findViewById(R.id.create_contact).setOnClickListener(this);
        phoneNumbers = new ArrayList<>();
        emailIds = new ArrayList<>();
        webSites = new ArrayList<>();

        // Once we have all text, need to use pattern matching to identify phone number from business card.
        // for each line use pattern matching

        for (int k = 0; k < lines.size(); k++) {
            Text text = lines.get(k);
            Log.d("FindTextOnImage", "identify pattern in text :: " + text.getValue());
            String value = text.getValue();
            if (value.contains("@@")) {
                value = value.replace("@@", "@");
            }
            if (value.contains(". ")) {
                value = value.replace(". ", ".");
            }


            Matcher phoneMatcher = Patterns.PHONE.matcher(text.getValue());
            Log.d("FindTextOnImage", "phone matcher");
            while (phoneMatcher.find()) {
                Log.d("FindTextOnImage", "start index::" + phoneMatcher.start());
                Log.d("FindTextOnImage", "end index::" + phoneMatcher.end());
                Log.d("FindTextOnImage", "matcher group::" + phoneMatcher.group());
                String phoneNumber = phoneMatcher.group();
                if (phoneNumber.length() > 9) {
                    phoneNumbers.add(phoneNumber);

                    int position = k;

                    if ((position - 1) == 0) {
                        // TODO: position-1 can be name
                        Text txtName = lines.get((position - 1));
                        personName = txtName.getValue();
                        Log.d("FindTextOnImage", "name is ::" + personName);
                        ((EditText) findViewById(R.id.name)).setText(personName);
                    }
                    if ((position - 1) > 0) {
                        // TODO: position-1 can be job title
                        Text txtJob = lines.get((position - 1));
                        Log.d("FindTextOnImage", "job title is ::" + txtJob.getValue());

                        Text txtName = lines.get(position - 2);
                        // TODO: position-2 can be name
                        Log.d("FindTextOnImage", "name is with job title ::" + txtName.getValue());
                        ((EditText) findViewById(R.id.name)).setText(txtName.getValue());
                    }
                }
            }


            Matcher emailMatcher = Patterns.EMAIL_ADDRESS.matcher(value);

            Log.d("FindTextOnImage", "email matcher");
            while (emailMatcher.find()) {
                Log.d("FindTextOnImage", "start index::" + emailMatcher.start());
                Log.d("FindTextOnImage", "end index::" + emailMatcher.end());
                Log.d("FindTextOnImage", "matcher group::" + emailMatcher.group());
                String emailAddress = emailMatcher.group();
                emailIds.add(emailAddress);
            }

            Matcher websiteMatcher = Patterns.WEB_URL.matcher(text.getValue());

            Log.d("FindTextOnImage", "websiteMatcher matcher");
            while (websiteMatcher.find()) {
                Log.d("FindTextOnImage", "start index::" + websiteMatcher.start());
                Log.d("FindTextOnImage", "end index::" + websiteMatcher.end());
                Log.d("FindTextOnImage", "matcher group::" + websiteMatcher.group());
                String website = websiteMatcher.group();
                webSites.add(website);
            }
        }

        String phoneNum = "";
        for (int i = 0; i < phoneNumbers.size(); i++) {
            if (i == 0) {
                TextView phoneText = new TextView(this);
                phoneText.setText("Phone Number");
                contactDetails.addView(phoneText);
            }
            phoneNum = phoneNum + phoneNumbers.get(i) + ",";
            Log.d("FindTextOnImage", "phone number " + i + " ::" + phoneNumbers.get(i));
            EditText phone1 = new EditText(this);
            phone1.setText(phoneNumbers.get(i));
            phone1.setTag("phone" + i);
            contactDetails.addView(phone1);
        }

        String emailId = "";
        for (int i = 0; i < emailIds.size(); i++) {
            if (i == 0) {
                TextView emailText = new TextView(this);
                emailText.setText("Email Id");
                contactDetails.addView(emailText);
            }
            emailId = emailId + emailIds.get(i) + ",";
            Log.d("FindTextOnImage", "email ids " + i + " ::" + emailIds.get(i));
            EditText emailEditText = new EditText(this);
            emailEditText.setTag("email" + i);
            emailEditText.setText(emailIds.get(i));
            contactDetails.addView(emailEditText);
        }

        String webSite = "";
        for (int i = 0; i < webSites.size(); i++) {
            if (i == 0) {
                TextView webText = new TextView(this);
                webText.setText("Website");
                contactDetails.addView(webText);
            }
            webSite = webSite + webSites.get(i) + ",";
            Log.d("FindTextOnImage", "website number " + i + " ::" + webSites.get(i));
            EditText websiteEditText = new EditText(this);
            websiteEditText.setTag("website" + i);
            websiteEditText.setText(webSites.get(i));
            contactDetails.addView(websiteEditText);
        }
    }

    private Bitmap correctImagerotation(Bitmap bitmap) {
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(selectedImagePath);
        } catch (IOException e) {

            e.printStackTrace();
        }
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);
        Log.i("RotateImage", "Exif orientation: " + orientation);
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
    }

    @Override
    public void onClick(View view) {
        Log.d("Find", "Create contact button clicked :: ");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            Log.d("Find", "Create contact permission received ");
            createContact();
        } else {
            Log.d("Find", "request for permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_CONTACTS}, 102);
        }
    }

    private void createContact() {
        ArrayList<ContentProviderOperation> cntProOper = new ArrayList<ContentProviderOperation>();
        int contactIndex = cntProOper.size();//ContactSize

        //Newly Inserted contact
        // A raw contact will be inserted ContactsContract.RawContacts table in contacts database.
        cntProOper.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)//Step1
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build());

        String name = ((EditText) findViewById(R.id.name)).getText().toString();
        //Display name will be inserted in ContactsContract.Data table
        cntProOper.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)//Step2
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name) // Name of the contact
                .build());

        for (int i = 0; i < phoneNumbers.size(); i++) {
            int phoneType;
            String phoneNumber = ((EditText) contactDetails.findViewWithTag("phone" + i)).getText().toString();
            Log.d("findText", "After edit Phone number " + phoneNumber);
            if (i == 0) {
                phoneType = ContactsContract.CommonDataKinds.Phone.TYPE_WORK;
            } else if (i == 1) {
                phoneType = ContactsContract.CommonDataKinds.Phone.TYPE_HOME;
            } else if (i == 2) {
                phoneType = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;
            } else {
                phoneType = ContactsContract.CommonDataKinds.Phone.TYPE_OTHER;
            }

            //Mobile number will be inserted in ContactsContract.Data table
            cntProOper.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)//Step 3
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber) // Number to be added
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType).build()); //Type like HOME, MOBILE etc
        }

        for (int i = 0; i < emailIds.size(); i++) {
            int emailType;
            String emailId = ((EditText) contactDetails.findViewWithTag("email" + i)).getText().toString();
            Log.d("findText", "After edit email id " + emailId);
            if (i == 0) {
                emailType = ContactsContract.CommonDataKinds.Email.TYPE_WORK;
            } else if (i == 1) {
                emailType = ContactsContract.CommonDataKinds.Email.TYPE_HOME;
            } else if (i == 2) {
                emailType = ContactsContract.CommonDataKinds.Email.TYPE_MOBILE;
            } else {
                emailType = ContactsContract.CommonDataKinds.Email.TYPE_OTHER;
            }

            cntProOper.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)//Step 4
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, emailId) // Email to be added
                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, emailType).build());
        }

        for (int i = 0; i < webSites.size(); i++) {
            int websiteType;
            String website = ((EditText) contactDetails.findViewWithTag("website" + i)).getText().toString();
            Log.d("findText", "After edit website " + website);
            if (i == 0) {
                websiteType = ContactsContract.CommonDataKinds.Website.TYPE_WORK;
            } else if (i == 1) {
                websiteType = ContactsContract.CommonDataKinds.Website.TYPE_HOME;
            } else if (i == 2) {
                websiteType = ContactsContract.CommonDataKinds.Website.TYPE_PROFILE;
            } else {
                websiteType = ContactsContract.CommonDataKinds.Website.TYPE_OTHER;
            }
            cntProOper.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)//Step 5
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Website.URL, website) // Email to be added
                    .withValue(ContactsContract.CommonDataKinds.Website.TYPE, websiteType).build());
        }

        try {
            // We will do batch operation to insert all above data
            //Contains the output of the app of a ContentProviderOperation.
            //It is sure to have exactly one of uri or count set
            ContentProviderResult[] contentProresult = null;
            contentProresult = getContentResolver().applyBatch(ContactsContract.AUTHORITY, cntProOper); //apply above data insertion into contacts list
            Log.d("Find", "inserted contact detail is :: " + contentProresult.toString());
        } catch (RemoteException exp) {
            //logs;
            exp.printStackTrace();
        } catch (OperationApplicationException exp) {
            //logs
            exp.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 102:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    createContact();
                } else {
                    boolean shouldShowRational = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    if (shouldShowRational) {
                        // Show rational message or directly ask for the request.
                        Toast.makeText(this, "show rational message", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }
}
