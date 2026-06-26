package com.osimatic.core_android.scanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.osimatic.core_android.Audio;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BarcodeScannerFragment extends Fragment {

    public interface OnBarcodeScannedListener {
        void onBarcodeScanned(String value);
    }

    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private OnBarcodeScannedListener listener;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private ProcessCameraProvider cameraProvider;
    // flag pour éviter plusieurs notifications sur le même scan
    private volatile boolean resultHandled = false;

    public void setOnBarcodeScannedListener(OnBarcodeScannedListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        previewView = new PreviewView(requireContext());
        return previewView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onResume() {
        super.onResume();
        resultHandled = false;
        startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopCamera();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (null != cameraExecutor) {
            cameraExecutor.shutdown();
        }
    }

    public void startCamera() {
        if (null == getActivity() || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                bindCamera();
            } catch (ExecutionException | InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void bindCamera() {
        if (null == cameraProvider || null == getActivity()) {
            return;
        }
        cameraProvider.unbindAll();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        BarcodeScanner scanner = BarcodeScanning.getClient(
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build()
        );

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
            if (resultHandled) {
                imageProxy.close();
                return;
            }
            Image mediaImage = imageProxy.getImage();
            if (null == mediaImage) {
                imageProxy.close();
                return;
            }
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            String value = barcode.getRawValue();
                            if (null != value && !resultHandled) {
                                resultHandled = true;
                                stopCamera();
                                if (isAdded() && null != getActivity()) {
                                    Audio.playBeep(requireContext(), Audio.VibrateOption.IF_NOT_SILENT);
                                    if (null != listener) {
                                        requireActivity().runOnUiThread(() -> listener.onBarcodeScanned(value));
                                    }
                                }
                                break;
                            }
                        }
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        });

        cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageAnalysis);
    }

    public void stopCamera() {
        if (null != cameraProvider) {
            cameraProvider.unbindAll();
        }
    }

    public void switchCamera() {
        lensFacing = (lensFacing == CameraSelector.LENS_FACING_BACK)
                ? CameraSelector.LENS_FACING_FRONT
                : CameraSelector.LENS_FACING_BACK;
        bindCamera();
    }
}