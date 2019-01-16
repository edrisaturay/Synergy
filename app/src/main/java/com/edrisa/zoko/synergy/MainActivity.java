package com.edrisa.zoko.synergy;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Main Activity";
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    FirebaseDatabase firebaseDatabase;
    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        if (
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)){

            }else{
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                }else{
                    Toast.makeText(this, "Please Allow Location Permission", Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        currentUser = mAuth.getCurrentUser();
        updateUi(currentUser);
    }

    @Override
    protected void onResume() {
        super.onResume();
        DatabaseReference reference = firebaseDatabase.getReference("users").child("customers").child(currentUser.getUid()).child("userStatus");
        reference.addValueEventListener(statusChangeListener);
    }

    ValueEventListener statusChangeListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            try {
                String status = dataSnapshot.getValue().toString();
                if(status.equals("idle")){
                    startActivity(new Intent(MainActivity.this, MapsActivity.class));
                    finish();
                }else if(status.equals("on-trip")){
                    startActivity(new Intent(MainActivity.this, DeliveryTrackerActivity.class));
                    finish();
                }else if(status.equals("trip-complete")){
                    startActivity(new Intent(MainActivity.this, DelivererFeedbackActivity.class));
                    finish();
                }else if(status.equals("waiting-delivierer")){
                    startActivity(new Intent(MainActivity.this, WaitingDeliviererActivity.class));
                    finish();
                }
            }catch (Exception e){
                Log.e(TAG, "Exception Caught " + e);
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };
    private void updateUi(FirebaseUser user){
        if(user == null){
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }
    }
}


