package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class mainactivity extends AppCompatActivity {
    Button submit;
    EditText add;
    ImageView imageView;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri; // To hold the image URI
    private Uri pdfuri;
    private static final int PICK_PDF_REQUEST = 2;
    Button downpdf;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        submit = findViewById(R.id.submit);
        add = findViewById(R.id.add);
        imageView = findViewById(R.id.myImageView);
        Button uploadImage = findViewById(R.id.uploadImage);
        uploadImage.setOnClickListener(view -> openFileChooser());
        Button uploadpdf = findViewById(R.id.uploadpdf);
        downpdf = findViewById(R.id.devapdf);
        downpdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getpdfurl();
            }
        });
       uploadpdf.setOnClickListener(view -> openpdfchooser());
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateData();
            }

            private void validateData() {
                String data = add.getText().toString().trim();
                if (data.isEmpty()) {
                    Toast.makeText(mainactivity.this, "Please enter data", Toast.LENGTH_SHORT).show();
                } else {
                    addCategory(data); // Pass the entered data to the method
                }
            }
        });
    }

        private void getpdfurl() {
            DatabaseReference df = FirebaseDatabase.getInstance().getReference("category").child("operating systems").child("Books");
            df.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String url = snapshot.child("pdfUrl").getValue(String.class);
                    pdfdownload(url);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(mainactivity.this ,"NO URL PRESENT",Toast.LENGTH_LONG).show();
                }
            });
        }

    private void pdfdownload(String url) {
        if (url != null && !url.isEmpty()) {
            // Ensure the URL is well-formed
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), "application/pdf");  // Correct MIME type for PDFs
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Use a chooser to let the user pick a PDF viewer
            Intent chooser = Intent.createChooser(intent, "Open PDF with");
            startActivity(chooser);
        } else {
            Toast.makeText(mainactivity.this, "No valid URL", Toast.LENGTH_SHORT).show();
        }
    }

    private void openpdfchooser() {
        Intent pdfintent = new Intent(Intent.ACTION_GET_CONTENT);
        pdfintent.setType("pdf/*");
        startActivityForResult(Intent.createChooser(pdfintent, "Select PDF"), PICK_PDF_REQUEST);
    }


    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            imageView.setImageURI(imageUri); // Set the image in the ImageView
            upImage();
        }
        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            pdfuri = data.getData(); // Get the PDF URI
            upPDF(); // Call method to handle PDF upload
        }
    }

    private void upPDF() {
        if (pdfuri != null) {
            StorageReference ref = FirebaseStorage.getInstance().getReference("uploads").child("Operating systems" + System.currentTimeMillis() +".pdf");
            ref.putFile(pdfuri).addOnSuccessListener(taskSnapshot -> {
                ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    savePdfUrlToDatabase(downloadUrl);
                });

            }).addOnFailureListener(e -> {
                Toast.makeText(mainactivity.this, "PDF upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
        else{
            Toast.makeText(mainactivity.this,"PDf failed" , Toast.LENGTH_SHORT).show();
        }}
    private void savePdfUrlToDatabase(String downloadUrl) {
        DatabaseReference ref = database.getReference("category").child("operating systems").child("Books");
       ref.addListenerForSingleValueEvent(new ValueEventListener() {
           @Override
           public void onDataChange(@NonNull DataSnapshot snapshot) {
               long count = snapshot.getChildrenCount();
               String pdfnum = "pdf_" + (count + 1);
               ref.child(pdfnum).setValue(downloadUrl).addOnCompleteListener(task -> {
                   if (task.isSuccessful()) {
                       Toast.makeText(mainactivity.this, "PDF URL saved successfully", Toast.LENGTH_SHORT).show();
                   } else {
                       Toast.makeText(mainactivity.this, "Failed to save PDF URL", Toast.LENGTH_SHORT).show();
                   }
               });
           }

           @Override
           public void onCancelled(@NonNull DatabaseError error) {

           }
       });


    }


    private void upImage() {
        if (imageUri != null) {
            StorageReference fileReference = FirebaseStorage.getInstance().getReference("uploads").child(System.currentTimeMillis() + ".jpg");
            fileReference.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
                fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    // Now save this URL in the Realtime Database under "qp"
                    saveImageUrlToDatabase(downloadUrl);
                });
            }).addOnFailureListener(e -> {
                Toast.makeText(mainactivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImageUrlToDatabase(String downloadUrl) {
        DatabaseReference qpReference = database.getReference("category").child("operating systems").child("qp"); // Adjust as needed
        qpReference.child("imageUrl").setValue(downloadUrl).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(mainactivity.this, "Image URL saved successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mainactivity.this, "Failed to save URL", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addCategory(String categoryName) {
        DatabaseReference categoriesRef = database.getReference("category");
        String[] insideCategory = {"qp", "links", "Study materials", "Books"};
        DatabaseReference subjectRef = categoriesRef.child(categoryName);

        // Loop through each inside category
        for (String s : insideCategory) {
            subjectRef.child(s).setValue("pending").addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(mainactivity.this, "Submitted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mainactivity.this, "Submission failed", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
