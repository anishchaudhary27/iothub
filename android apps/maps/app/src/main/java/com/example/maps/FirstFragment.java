package com.example.maps;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class FirstFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {

    private MapView mMapView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mMapView = view.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);

        MapsInitializer.initialize(getActivity().getApplicationContext());
        mMapView.getMapAsync(this);

        view.findViewById(R.id.refreshFAB).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //update anchors
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        final GoogleMap mMap = googleMap;
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("admins").document(FirebaseAuth.getInstance().getUid()).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot doc = task.getResult();
                        GeoPoint geoPoint = (GeoPoint) doc.get("location");
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(geoPoint.getLatitude(),geoPoint.getLongitude()),
                                15.0f));
                        ArrayList<String> devices = (ArrayList<String>) doc.get("devices");
                        Log.i("my",devices.toString());
                        for(int i = 0; i< devices.size();i++) {
                            db.collection("devices").document(devices.get(i)).get()
                                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                            DocumentSnapshot doc = task.getResult();
                                            GeoPoint geoPoint = (GeoPoint) doc.get("location");
                                            mMap.addMarker(new MarkerOptions().position(
                                                    new LatLng(geoPoint.getLatitude(),geoPoint.getLongitude()))
                                                    .title(doc.getId())
                                                    .snippet("mode: "+ doc.get("mode") + "   state: " + doc.get("state") + "   online: " + doc.get("online")));
                                        }
                                    });
                        }
                    }
                });
        mMap.setOnInfoWindowClickListener(this);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        Bundle bundle = new Bundle();
        bundle.putString("dId",marker.getTitle());
        Navigation.findNavController(getView()).navigate(R.id.action_FirstFragment_to_SecondFragment,bundle);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

}
