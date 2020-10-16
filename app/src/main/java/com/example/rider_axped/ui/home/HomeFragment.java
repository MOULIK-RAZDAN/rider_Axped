package com.example.rider_axped.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.example.rider_axped.Callback.IFirebaseDriverInfoListener;
import com.example.rider_axped.Callback.IFirebaseFailedListner;
import com.example.rider_axped.Common.Common;
import com.example.rider_axped.Common.DriverGeoModel;
import com.example.rider_axped.Model.DriverInfoModel;
import com.example.rider_axped.Model.GeoQueryModel;
import com.example.rider_axped.R;
import com.example.rider_axped.ui.slideshow.SlideshowFragment;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.internal.ICameraUpdateFactoryDelegate;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class HomeFragment extends Fragment implements OnMapReadyCallback, IFirebaseFailedListner, IFirebaseDriverInfoListener {


    @BindView(R.id.activity_main)
    SlidingUpPanelLayout slidingUpPanelLayout;
    @BindView(R.id.txt_welcome)
    TextView txt_welcome;

    private AutocompleteSupportFragment autocompleteSupportFragment;

    private HomeViewModel homeViewModel;

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;


    //location
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    //load driver
    private double distance = 1.0;//in km
    private static final double LIMIT_RANGE = 10.0;
    private Location previousLocation, currentLocation; //use to calculate distance

    private boolean firstTime = true;
    //Listener
    IFirebaseDriverInfoListener iFirebaseDriverInfoListener;
    IFirebaseFailedListner iFirebaseFailedListner;
    private String cityName;

    @Override
    public void onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        super.onDestroy();
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        init();
        initViews(root);

        mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
        return root;
    }

    private void initViews(View root) {
        ButterKnife.bind(this,root);
        Common.setWelcomeMessage(txt_welcome);
    }

    private void init() {

        Places.initialize(getContext(),getString(R.string.google_maps_key));
        autocompleteSupportFragment = (AutocompleteSupportFragment)getChildFragmentManager()
                .findFragmentById(R.id.autocomplete_fragment);
        autocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.ADDRESS, Place.Field.NAME, Place.Field.LAT_LNG));
        autocompleteSupportFragment.setHint(getString(R.string.where_to));
        autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                Snackbar.make(getView(), ""+place.getLatLng(), Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull Status status) {
                 Snackbar.make(getView(), ""+status.getStatusMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });

        iFirebaseFailedListner = this;
        iFirebaseDriverInfoListener = this;

        locationRequest = new LocationRequest();
        locationRequest.setSmallestDisplacement(10f);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            //
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                LatLng newPosition = new LatLng(locationResult.getLastLocation().getLatitude(),
                        locationResult.getLastLocation().getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPosition, 18f));


                //If user has changed location ,calculate and load driver again
                if (firstTime) {

                    previousLocation = currentLocation = locationResult.getLastLocation();
                    firstTime = false;

                    setRestrictPlacesInCountry(locationResult.getLastLocation());


                } else {
                    previousLocation = currentLocation;
                    currentLocation = locationResult.getLastLocation();
                }

                if (previousLocation.distanceTo(currentLocation) / 1000 <= LIMIT_RANGE)
                    loadAvailableDrivers();
                else {

                }

            }
        };
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

        //add in  init at end
        loadAvailableDrivers();
    }

    private void setRestrictPlacesInCountry(Location location) {
        try {
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            List<Address> addressesList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(),1);
            if (addressesList.size() > 0)
               autocompleteSupportFragment.setCountry(addressesList.get(0).getCountryCode());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("StringFormatInvalid")
    private void loadAvailableDrivers() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(getView(), getString(R.string.permission_require), Snackbar.LENGTH_SHORT).show();

           return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnFailureListener(e -> Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show()).addOnSuccessListener(location -> {
                    //load all driver in city
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            List<Address> addressList;
            try {
                addressList = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
                if (addressList.size() > 0)
                     cityName = addressList.get(0).getLocality();

                if(!TextUtils.isEmpty(cityName)) {

                    //Query
                    DatabaseReference driver_location_ref = FirebaseDatabase.getInstance()
                            .getReference(Common.DRIVERS_LOCATION_REFERENCES)
                            .child(cityName);
                    GeoFire gf = new GeoFire(driver_location_ref);
                    GeoQuery geoQuery = gf.queryAtLocation(new GeoLocation(location.getLatitude(),
                            location.getLongitude()), distance);
                    geoQuery.removeAllListeners();

                    geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                        @Override
                        public void onKeyEntered(String key, GeoLocation location) {
                            Common.driversFound.add(new DriverGeoModel(key, location));

                        }

                        @Override
                        public void onKeyExited(String key) {

                        }

                        @Override
                        public void onKeyMoved(String key, GeoLocation location) {

                        }

                        @Override
                        public void onGeoQueryReady() {
                            if (distance <= LIMIT_RANGE) {

                                distance++;
                                loadAvailableDrivers();//continue search in new distance
                            } else {
                                distance = 1.0; //Reset it
                                addDriverMaker();
                            }

                        }

                        @Override
                        public void onGeoQueryError(DatabaseError error) {

                            Snackbar.make(getView(), error.getMessage(), Snackbar.LENGTH_SHORT).show();
                        }
                    });

                    // Listen to new driver in city and range
                    driver_location_ref.addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                            //have new driver
                            GeoQueryModel geoqueryModel = snapshot.getValue(GeoQueryModel.class);
                            GeoLocation geoLocation = new GeoLocation(geoqueryModel.getL().get(0),
                                    geoqueryModel.getL().get(1));
                            DriverGeoModel driverGeoModel = new DriverGeoModel(snapshot.getKey(),
                                    geoLocation);
                            Location newDriverLocation = new Location("");
                            newDriverLocation.setLatitude(geoLocation.latitude);
                            newDriverLocation.setLongitude(geoLocation.longitude);
                            float newDistance = location.distanceTo(newDriverLocation) / 1000;  // in KM
                            if (newDistance <= LIMIT_RANGE)
                                findDriverByKey(driverGeoModel); // If driver in range, add to map
                        }

                        @Override
                        public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                        }

                        @Override
                        public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                        }

                        @Override
                        public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

                }
                else
                    Snackbar.make(getView(), getString(R.string.city_name_empty), Snackbar.LENGTH_LONG).show();


            } catch (IOException e) {
                e.printStackTrace();
                Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show();
            }

        });
    }

    private void addDriverMaker() {
        if(Common.driversFound.size() >0)
        {
            Observable.fromIterable(Common.driversFound)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(driverGeoModel -> {

                        //on next
                        findDriverByKey(driverGeoModel);
                        }, throwable -> {

                        Snackbar.make(getView(),throwable.getMessage(), Snackbar.LENGTH_SHORT).show();

                    },()->{


                    } );



        }
        else
        {
            Snackbar.make(getView(),getString(R.string.drivers_not_found),Snackbar.LENGTH_SHORT).show();
        }


    }

    private void findDriverByKey(DriverGeoModel driverGeoModel) {

        FirebaseDatabase.getInstance()
                .getReference(Common.DRIVERS_INFO_REFERENCE)
                .child(driverGeoModel.getKey())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.hasChildren())
                        {
                           driverGeoModel.setDriverInfoModel(snapshot.getValue(DriverInfoModel.class));
                            iFirebaseDriverInfoListener.onDriverInfoLoadSuccess(driverGeoModel);
                        }
                        else
                            iFirebaseFailedListner.onFirebaseLoadFailed(getString(R.string.not_found_key)+driverGeoModel.getKey());

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        iFirebaseFailedListner.onFirebaseLoadFailed(error.getMessage());

                    }
                });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //request permission to add location
        mMap = googleMap;
        Dexter.withContext(getContext())
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {

                        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        mMap.setMyLocationEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(true);


                        mMap.setOnMyLocationButtonClickListener(() -> {
                            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                    && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                return false;
                            }
                            fusedLocationProviderClient.getLastLocation()
                                    .addOnFailureListener(e -> Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_SHORT)
                                            .show())
                                    .addOnSuccessListener(location -> {

                                        LatLng userLatLng = new LatLng(location.getLatitude(),location.getLongitude());
                                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng,18f));

                                    });
                            return true;
                        });
                        //layout button
                        View locationButton = ((View)mapFragment.getView().findViewById(Integer.parseInt("1")).getParent())
                                .findViewById(Integer.parseInt("2"));
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
                        //right bottom
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP,0);
                        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE);
                        params.setMargins(0,0,0,250);



                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {

                        Snackbar.make(getView(),permissionDeniedResponse.getPermissionName() + " need enable",
                                Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {

                    }
                })
        .check();


        mMap.getUiSettings().setZoomControlsEnabled(true);

        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(),
                    R.raw.uber_maps_style));
            if(!success)
                Snackbar.make(getView(), "Load map style failed", Snackbar.LENGTH_SHORT).show();
        }catch (Exception e)
        {
            Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onFirebaseLoadFailed(String message) {
        Snackbar.make(getView(),message,Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onDriverInfoLoadSuccess(DriverGeoModel driverGeoModel) {
        if(!Common.markerList.containsKey(driverGeoModel.getKey()))
            Common.markerList.put(driverGeoModel.getKey(),
                    mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(driverGeoModel.getGeoLocation().latitude,
                            driverGeoModel.getGeoLocation().longitude))
                    .flat(true)
                    .title(Common.buildName(driverGeoModel.getDriverInfoModel().getFirstName(),
                            driverGeoModel.getDriverInfoModel().getLastName()))
                    .snippet(driverGeoModel.getDriverInfoModel().getPhoneNumber())
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.bike_512))
                    ));


        if(!TextUtils.isEmpty(cityName)){

            DatabaseReference driverLocation = FirebaseDatabase.getInstance()
                    .getReference(Common.DRIVERS_LOCATION_REFERENCES)
                    .child(cityName)
                    .child(driverGeoModel.getKey());
            driverLocation.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(!snapshot.hasChildren()){
                        if(Common.markerList.get(driverGeoModel.getKey()) != null)
                            Common.markerList.get(driverGeoModel.getKey()).remove();//Remove maker
                        Common.markerList.remove(driverGeoModel.getKey());// Remove marker info from hash map
                        driverLocation.removeEventListener(this);//remove event listner

                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Snackbar.make(getView(), error.getMessage(),Snackbar.LENGTH_SHORT).show();

                }
            });

        }
    }
}