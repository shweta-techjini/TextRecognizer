package techjini.com.textrecognizer;

import android.Manifest;
import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.util.ArrayList;
import java.util.regex.Matcher;

import static techjini.com.textrecognizer.R.id.phone;


public class FindTextOnImage extends Activity implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {
    private ArrayList<Text> lines;
    private String phoneNumber, website, emailAddress, personName;
    private String selectedImagePath;
    private ArrayList<String> phoneNumbers, emailIds, webSites;

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
        ((ImageView) findViewById(R.id.image)).setImageBitmap(imageWithTextBitmap);

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
                phoneNumber = phoneMatcher.group();
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


            Matcher emailMatcher = Patterns.EMAIL_ADDRESS.matcher(value);

            Log.d("FindTextOnImage", "email matcher");
            while (emailMatcher.find()) {
                Log.d("FindTextOnImage", "start index::" + emailMatcher.start());
                Log.d("FindTextOnImage", "end index::" + emailMatcher.end());
                Log.d("FindTextOnImage", "matcher group::" + emailMatcher.group());
                emailAddress = emailMatcher.group();
                emailIds.add(emailAddress);
            }

            Matcher websiteMatcher = Patterns.WEB_URL.matcher(text.getValue());

            Log.d("FindTextOnImage", "websiteMatcher matcher");
            while (websiteMatcher.find()) {
                Log.d("FindTextOnImage", "start index::" + websiteMatcher.start());
                Log.d("FindTextOnImage", "end index::" + websiteMatcher.end());
                Log.d("FindTextOnImage", "matcher group::" + websiteMatcher.group());
                website = websiteMatcher.group();
                webSites.add(website);
            }
        }

        String phoneNum = "";
        for (int i = 0; i < phoneNumbers.size(); i++) {
            phoneNum = phoneNum + phoneNumbers.get(i) + ",";
            Log.d("FindTextOnImage", "phone number " + i + " ::" + phoneNumbers.get(i));
        }
        ((EditText) findViewById(phone)).setText(phoneNum);

        String emailId = "";
        for (int i = 0; i < emailIds.size(); i++) {
            emailId = emailId + emailIds.get(i) + ",";
            Log.d("FindTextOnImage", "email ids " + i + " ::" + emailIds.get(i));
        }
        ((EditText) findViewById(R.id.email)).setText(emailId);

        String webSite = "";
        for (int i = 0; i < webSites.size(); i++) {
            webSite = webSite + webSites.get(i) + ",";
            Log.d("FindTextOnImage", "website number " + i + " ::" + webSites.get(i));
        }
        ((EditText) findViewById(R.id.website)).setText(webSite);
    }

    @Override
    public void onClick(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            ArrayList<ContentProviderOperation> cntProOper = new ArrayList<ContentProviderOperation>();
            int contactIndex = cntProOper.size();//ContactSize

            //Newly Inserted contact
            // A raw contact will be inserted ContactsContract.RawContacts table in contacts database.
            cntProOper.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)//Step1
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build());

            //Display name will be inserted in ContactsContract.Data table
            cntProOper.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)//Step2
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, "Business Card Reader") // Name of the contact
                    .build());
            //Mobile number will be inserted in ContactsContract.Data table
            cntProOper.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)//Step 3
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber) // Number to be added
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_WORK).build()); //Type like HOME, MOBILE etc

            cntProOper.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)//Step 4
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, emailAddress) // Email to be added
                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK).build());

            cntProOper.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)//Step 5
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Website.URL, website) // Email to be added
                    .withValue(ContactsContract.CommonDataKinds.Website.TYPE, ContactsContract.CommonDataKinds.Website.TYPE_WORK).build());

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
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_CONTACTS}, 102);
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }
}
