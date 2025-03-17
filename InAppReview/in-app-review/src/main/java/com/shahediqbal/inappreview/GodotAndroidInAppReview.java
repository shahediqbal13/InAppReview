package com.shahediqbal.inappreview;

import android.app.Activity;
import android.os.Build;
import android.util.Log;

import androidx.annotation.ChecksSdkIntAtLeast;
import androidx.annotation.NonNull;
import androidx.collection.ArraySet;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.review.testing.FakeReviewManager;

import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;
import org.godotengine.godot.plugin.UsedByGodot;

import java.util.Set;

public class GodotAndroidInAppReview extends GodotPlugin {
    private static final String TAG = "godot";
    private static final String PLUGIN_NAME = "GodotAndroidInAppReview";

    private final Activity activity;

    public GodotAndroidInAppReview(Godot godot) {
        super(godot);
        activity = getActivity();
    }

    @NonNull
    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @NonNull
    @Override
    public Set<SignalInfo> getPluginSignals() {
        Set<SignalInfo> signals = new ArraySet<>();
        signals.add(new SignalInfo(Signals.IN_APP_REVIEW_FAILED_TO_SHOW, String.class));
        signals.add(new SignalInfo(Signals.IN_APP_REVIEW_FLOW_COMPLETED));
        return signals;
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.LOLLIPOP)
    @UsedByGodot
    public boolean isInAppReviewSupportedOs() {
        int version = android.os.Build.VERSION.SDK_INT;
        Log.d(TAG, PLUGIN_NAME + ": android sdk version: " + version);
        return version >= android.os.Build.VERSION_CODES.LOLLIPOP;
    }

    @UsedByGodot
    public void showInAppReview(boolean isFake) {
        ReviewManager manager = isFake ? new FakeReviewManager(activity) : ReviewManagerFactory.create(activity);

        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // We can get the ReviewInfo object
                ReviewInfo reviewInfo = task.getResult();
                Task<Void> flow = manager.launchReviewFlow(activity, reviewInfo);
                flow.addOnCompleteListener(task2 -> {
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even whether the review dialog was shown. Thus, no
                    // matter the result, we continue our app flow.
                    Log.i(TAG, PLUGIN_NAME + ": Review flow completed");
                    emitSignal(Signals.IN_APP_REVIEW_FLOW_COMPLETED);
                });

            } else {
                // There was some problem, log or handle the error code.
                String errorMessage = task.getException().getMessage();
                Log.e(TAG, PLUGIN_NAME + ": " + errorMessage);
                emitSignal(Signals.IN_APP_REVIEW_FAILED_TO_SHOW, errorMessage);
            }
        });
    }
}
