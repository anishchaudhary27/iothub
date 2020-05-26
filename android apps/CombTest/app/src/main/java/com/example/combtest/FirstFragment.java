package com.example.combtest;

import android.annotation.SuppressLint;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FirstFragment extends Fragment {
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());
        cameraExecutor = Executors.newSingleThreadExecutor();
        previewView = view.findViewById(R.id.viewFinder);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            }
            catch (ExecutionException | InterruptedException e) {
                Log.e("my",e.toString());
            }
        }, ContextCompat.getMainExecutor(getContext()));

        view.findViewById(R.id.captureBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scan();
            }
        });
    }

    private void scan() {
        File file;
        try {
            file = File.createTempFile("filename", null, getContext().getCacheDir());
            FirebaseVisionBarcodeDetectorOptions options =
                    new FirebaseVisionBarcodeDetectorOptions.Builder()
                            .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
                            .build();
            FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance()
                    .getVisionBarcodeDetector(options);
            ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
            imageCapture.takePicture(outputFileOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    try {
                        FirebaseVisionImage visionImage = FirebaseVisionImage.fromFilePath(getContext(),Uri.fromFile(file));
                        detector.detectInImage(visionImage).addOnSuccessListener(firebaseVisionBarcodes -> {
                            if(firebaseVisionBarcodes.size() > 0) {
                                Bundle b = new Bundle();
                                b.putString("qrCode",firebaseVisionBarcodes.get(0).getRawValue());
                                Navigation.findNavController(getView()).navigate(R.id.action_FirstFragment_to_optionsFragment,b);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Log.i("my",exception.toString());
                }
            });
        }
        catch (IOException e) {

        }

    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        imageCapture = new ImageCapture.Builder()
                        .setTargetRotation(getView().getDisplay().getRotation())
                        .build();
        Camera camera;
        camera = cameraProvider.bindToLifecycle(this, cameraSelector,imageCapture, preview);

        preview.setSurfaceProvider(previewView.createSurfaceProvider(camera.getCameraInfo()));
    }
}
