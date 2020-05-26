package com.example.combtest;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.Map;

public class OptionsFragment extends Fragment implements LocationListener {
    private static final String ARG_QR = "qrCode";
    private String qrCode;
    FirebaseFirestore db;
    LocationManager locationManager;

    private FirebaseFunctions mFunctions;


    public OptionsFragment() {
        // Required empty public constructor
    }

    public static OptionsFragment newInstance(String param1, String param2) {
        OptionsFragment fragment = new OptionsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_QR, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            qrCode = getArguments().getString(ARG_QR);
        }
        db = FirebaseFirestore.getInstance();
        mFunctions = FirebaseFunctions.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_options, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Button deployBtn = view.findViewById(R.id.deployBtn);
        Button locBtn = view.findViewById(R.id.locBtn);
        TextView deviceId = view.findViewById(R.id.deviceIdTextView);
        final TextView onlineStatus = view.findViewById(R.id.textView2);
        deviceId.setText(qrCode);
        db.collection("devices").document(qrCode)
                .addSnapshotListener((doc, e) -> {

                    if (!doc.exists()) {
                        Toast.makeText(getContext(), "This device does not exist!!", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(getView()).navigate(R.id.action_optionsFragment_to_FirstFragment);
                    }

                    if (Integer.parseInt(doc.get("deploymentStatus").toString()) == 1 && deployBtn.getText().toString() == "DEPLOY") {
                        deployBtn.setText("REMOVE");
                        Toast.makeText(getContext(),"device deployed!!",Toast.LENGTH_SHORT).show();
                        deployBtn.setEnabled(true);
                    } else if (Integer.parseInt(doc.get("deploymentStatus").toString()) == 0 && deployBtn.getText().toString() == "REMOVE") {
                        deployBtn.setText("DEPLOY");
                        Toast.makeText(getContext(),"device removed!!",Toast.LENGTH_SHORT).show();
                        deployBtn.setEnabled(true);
                    } else if (Integer.parseInt(doc.get("deploymentStatus").toString()) == 0) {
                        deployBtn.setText("DEPLOY");
                    } else if (Integer.parseInt(doc.get("deploymentStatus").toString()) == 1) {
                        deployBtn.setText("REMOVE");
                    }

                    if (Integer.parseInt(doc.get("online").toString()) == 1) {
                        onlineStatus.setText("online");
                    }
                    else {
                        onlineStatus.setText("offline");
                    }
                });

        view.findViewById(R.id.wifiBtn).setOnClickListener(v -> {
            db.collection("devices").document(qrCode)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Toast.makeText(getContext(), "try again!!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String deviceBTName = task.getResult().get("btName").toString();
                        Bundle b = new Bundle();
                        b.putString("btName", deviceBTName);
                        b.putString("qrCode", qrCode);
                        Navigation.findNavController(getView()).navigate(R.id.action_optionsFragment_to_SecondFragment, b);
                    });
        });

        locBtn.setOnClickListener(v -> {
            locBtn.setEnabled(false);
            locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        });

        deployBtn.setOnClickListener(v -> {
            String text = deployBtn.getText().toString();
            if(onlineStatus.getText().toString().equals("offline") && text == "DEPLOY") {
                Toast.makeText(getContext(),"device not online check WiFi!!" ,Toast.LENGTH_SHORT).show();
                return;
            }
            deployBtn.setEnabled(false);
            new AlertDialog.Builder(getContext())
                    .setTitle(text)
                    .setMessage("Are you sure?")
                    .setPositiveButton("yes", (dialog, which) -> {
                        Task<String> result;
                        if(text == "REMOVE") result = remove();
                        else result = deploy();
                        result.addOnCompleteListener(new OnCompleteListener<String>() {
                            @Override
                            public void onComplete(@NonNull Task<String> task) {
                                if(!task.isSuccessful()) {
                                    Toast.makeText(getContext(),"unable to "+ text.toLowerCase()+" device. Try again!!",Toast.LENGTH_SHORT).show();
                                    deployBtn.setEnabled(true);
                                }
                                else {
                                    Toast.makeText(getContext(),text.toLowerCase() + " initiated!!" ,Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    })
                    .setNegativeButton("no", (dialog, which) -> {})
                    .show();
        });

        view.findViewById(R.id.floatingActionButton).setOnClickListener(v -> {
            Navigation.findNavController(getView()).navigate(R.id.action_optionsFragment_to_FirstFragment);
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        locationManager.removeUpdates(this);
        new AlertDialog.Builder(getContext())
                .setTitle("Set Location")
                .setMessage("Are you sure you want to deploy at this location?")
                .setPositiveButton("yes", (dialog, which) -> setLocation(location))
                .setNegativeButton("no", (dialog, which) -> {
                    Toast.makeText(getContext(),"location not settled!!",Toast.LENGTH_SHORT).show();
                    getView().findViewById(R.id.locBtn).setEnabled(true);
                })
                .show();
    }

    private void setLocation(Location location) {
        GeoPoint geoPoint = new GeoPoint(location.getLatitude(),location.getLongitude());
        Map<String,Object> map = new HashMap<>();
        map.put("location",geoPoint);
        db.collection("devices").document(qrCode)
                .update(map)
                .addOnCompleteListener(task -> {
                    getView().findViewById(R.id.locBtn).setEnabled(true);
                    if(!task.isSuccessful()) {
                        Toast.makeText(getContext(),"unable to settle location. Try again!!",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(getContext(),"location settled!",Toast.LENGTH_SHORT).show();
                });
    }

    private Task<String> deploy() {
        // Create the arguments to the callable function.
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", qrCode);

        return mFunctions
                .getHttpsCallable("deployDevice")
                .call(data)
                .continueWith(task -> {
                    // This continuation runs on either success or failure, but if the task
                    // has failed then getResult() will throw an Exception which will be
                    // propagated down.
                    String result = (String) task.getResult().getData();
                    return result;
                });
    }

    private Task<String> remove() {
        // Create the arguments to the callable function.
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", qrCode);

        return mFunctions
                .getHttpsCallable("removeDevice")
                .call(data)
                .continueWith(task -> {
                    // This continuation runs on either success or failure, but if the task
                    // has failed then getResult() will throw an Exception which will be
                    // propagated down.
                    String result = (String) task.getResult().getData();
                    return result;
                });
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.i("my",provider);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.i("my",provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.i("my",provider);
    }
}
