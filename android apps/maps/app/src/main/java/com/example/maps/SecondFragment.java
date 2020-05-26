package com.example.maps;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.Map;

public class SecondFragment extends Fragment {
    private String dId;
    private FirebaseFunctions mFunctions;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dId = getArguments().getString("dId");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_second, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        mFunctions = FirebaseFunctions.getInstance();
        super.onViewCreated(view, savedInstanceState);
        TextView deviceName = view.findViewById(R.id.deviceid_textview);
        deviceName.setText(dId);
        final TextView onlinetv = view.findViewById(R.id.onlineState);
        final Switch modeSwitch = view.findViewById(R.id.mode_switch);
        final Switch onoffSwitch = view.findViewById(R.id.onoff_switch);
        final TextView fwversiontv = view.findViewById(R.id.firmwareversion);
        final Button configureButton = view.findViewById(R.id.configureButton);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("devices").document(dId).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    fwversiontv.setText(doc.get("fwCurrent").toString());

                    if (Integer.parseInt(doc.get("online").toString()) == 1) onlinetv.setText("Online");
                    else  onlinetv.setText("Offline");

                    if (Integer.parseInt(doc.get("state").toString()) == 1) onoffSwitch.setChecked(true);
                    else onoffSwitch.setChecked(false);

                    if(Integer.parseInt(doc.get("mode").toString()) == 1) modeSwitch.setChecked(true);
                    else modeSwitch.setChecked(false);
                }
            }
        });
        configureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int state = 0;
                configureButton.setEnabled(false);
                if(onoffSwitch.isChecked()) {
                    state = 1;
                }
                int mode = 0;
                if(modeSwitch.isChecked()) {
                    mode = 1;
                }
                addMessage(state,mode).addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        Navigation.findNavController(getView()).navigate(R.id.action_SecondFragment_to_FirstFragment);
                    }
                });
            }
        });
    }
    private Task<String> addMessage(int state,int mode) {
        Map<String, Object> data = new HashMap<>();
        data.put("state", state);
        data.put("mode", mode);
        data.put("deviceId", dId);
        data.put("registryId", "registery_1");
        data.put("cloudRegion", "us-central1");

        return mFunctions
                .getHttpsCallable("updateDeviceConfig")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        String result = (String) task.getResult().getData();
                        return result;
                    }
                });
    }
}
