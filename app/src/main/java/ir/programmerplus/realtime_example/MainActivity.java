package ir.programmerplus.realtime_example;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Choreographer;

import java.text.SimpleDateFormat;
import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import ir.programmerplus.realtime.RealTime;
import ir.programmerplus.realtime_example.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private Choreographer.FrameCallback frameCallback;
    private boolean frameCallbackActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestLocationPermission();

        binding.btnClearCache.setOnClickListener(v -> RealTime.clearCachedInfo());

        setupTimeDisplay();
    }

    @SuppressLint("SetTextI18n")
    private void setupTimeDisplay() {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);

        frameCallback = new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                if (RealTime.isInitialized()) {
                    binding.txtDateTime.setText(isoFormat.format(RealTime.now()));
                    binding.txtStatus.setText("GPS SYNCED");
                } else {
                    binding.txtDateTime.setText("--:--:--");
                    binding.txtStatus.setText("Waiting for GPS...");
                }
                Choreographer.getInstance().postFrameCallback(this);
            }
        };
        Choreographer.getInstance().postFrameCallback(frameCallback);
        frameCallbackActive = true;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (frameCallbackActive) {
            Choreographer.getInstance().removeFrameCallback(frameCallback);
            frameCallbackActive = false;
        }
    }
}
