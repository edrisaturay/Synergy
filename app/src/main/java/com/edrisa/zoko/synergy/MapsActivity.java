package com.edrisa.zoko.synergy;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import com.edrisa.zoko.synergy.models.PlaceAutoCompleteAdapter;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "Maps Activity";

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private FusedLocationProviderClient mFusedLocationClient;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private LatLng sourceLocation;
    private LatLng destinationLocation;

    private List<Address> addresses;
    private PlaceAutoCompleteAdapter mPlaceAutocompleteAdapter;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(29.57, -1.48), new LatLng(35.03, 4.22)
    );

    private int radius = 1;

    private Boolean delivierFound = false;
    private String delivierFoundId;
    private boolean isSourceAvailable = false, isDestinationAvailable = false;
    //Views
    TextView mSourceAddress;
    AutoCompleteTextView mDestinationAddress;
    Button mRequest;
    ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        GeoDataClient mGeoDataClient = Places.getGeoDataClient(this, null);
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        mDestinationAddress = findViewById(R.id.et_delivery_address);
        mSourceAddress = findViewById(R.id.tv_current_address);
        mRequest = findViewById(R.id.btn_request);
        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSourceAvailable && isDestinationAvailable) {
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference("user_requests").child(currentUser.getUid());
                    GeoFire geoFire = new GeoFire(reference);
                    geoFire.setLocation("source", new GeoLocation(sourceLocation.latitude, sourceLocation.longitude), new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            Log.i(TAG, "Source Location Set Successfully");
                        }
                    });
                    geoFire.setLocation("destination", new GeoLocation(destinationLocation.latitude, destinationLocation.longitude), new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            Log.i(TAG, "Destination Location Set Successfully");
                        }
                    });
                    progressDialog = new ProgressDialog(MapsActivity.this);
                    progressDialog.setMessage("Finding Your Deliverer");
                    progressDialog.setCancelable(false);
                    progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    progressDialog.show();
                    Timer timer = new Timer();
                    TimerTask findAvailableDelivierer = new TimerTask() {
                        @Override
                        public void run() {
                            getClosestDelivierer();
                        }
                    };
                    timer.schedule(findAvailableDelivierer, 1000, 5000 );

                }
            }
        });
        mDestinationAddress.setOnItemClickListener(mAutoCompleteClickListener);
        mPlaceAutocompleteAdapter = new PlaceAutoCompleteAdapter(MapsActivity.this, mGeoDataClient, LAT_LNG_BOUNDS, new AutocompleteFilter.Builder().setCountry("UG").build());
        mDestinationAddress.setAdapter(mPlaceAutocompleteAdapter);
        mDestinationAddress.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEARCH
                        || event.getAction() == event.ACTION_DOWN || event.getAction() == event.KEYCODE_ENTER) {
                    Log.i(TAG, "Geolocating");
                    String searchString = mDestinationAddress.getText().toString();
                    Geocoder geocoder = new Geocoder(MapsActivity.this);
                    List<Address> list = new ArrayList<>();
                    try {
                        list = geocoder.getFromLocationName(searchString, 1);
                    } catch (Exception e) {
                        Log.e(TAG, "Geo Locate IO Exception" + e.getMessage());
                    }
                    if (list.size() > 0) {
                        Address address = list.get(0);
                        Log.d(TAG, "Geolocate Found A Location: " + address.toString());
                    }
                }
                return false;
            }
        });
    }


    private void getClosestDelivierer(){
        DatabaseReference reference = firebaseDatabase.getReference().child("deliverers_available");
        GeoFire geoFire = new GeoFire(reference);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(sourceLocation.latitude, sourceLocation.longitude), radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!delivierFound) {
                    delivierFound = true;
                    delivierFoundId = key;
                    Log.i(TAG, "Driver Found ID: " + key);
                    if(progressDialog.isShowing()){
                        progressDialog.dismiss();
                    }
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!delivierFound){
                    radius++;
                    Log.i(TAG, "Radius: " + radius);
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        currentUser = mAuth.getCurrentUser();
        updateUi(currentUser);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null){
                    CameraUpdate center = CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 14f);
                    mMap.animateCamera(center);
                    String address = "";
                    Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
                    List<Address> addresses = new ArrayList<>();
                    try {
                        addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(addresses.size() > 0){
                        address = addresses.get(0).getAddressLine(0);
                        sourceLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mSourceAddress.setText(address);
                        isSourceAvailable = true;
                    }
                }
            }
        });
    }

    private AdapterView.OnItemClickListener mAutoCompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            hideSoftKeyboard(MapsActivity.this);
            final AutocompletePrediction prediction = mPlaceAutocompleteAdapter.getItem(position);
            String placeId = "";
            if(prediction != null){
                placeId = prediction.getPlaceId();
            }
            PendingResult<PlaceBuffer> placeBufferPendingResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId);
            placeBufferPendingResult.setResultCallback(mUpdatePlaceDetailsCallback);
        }
    };

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback =   new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if(!places.getStatus().isSuccess()){
                Log.d(TAG, "onResult: Place did not complete successfully: " + places.getStatus().toString());
                places.release();
                return;
            }
            final Place place =  places.get(0);
            destinationLocation = new LatLng (place.getLatLng().latitude, place.getLatLng().longitude);
            String address = place.getName().toString() + ", " + place.getAddress();
            mDestinationAddress.setText(address);
            mDestinationAddress.clearFocus();
            isDestinationAvailable = true;
        }
    };
    private void hideSoftKeyboard(Activity activity){
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "Google API Connection Failed...");
    }
    private void updateUi(FirebaseUser user){
        if(user == null){
            startActivity(new Intent(MapsActivity.this, LoginActivity.class));
            finish();
        }
    }
}
