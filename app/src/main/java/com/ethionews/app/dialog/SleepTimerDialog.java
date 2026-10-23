package com.ethionews.app.dialog;

import static android.view.WindowManager.LayoutParams.MATCH_PARENT;
import static android.view.WindowManager.LayoutParams.WRAP_CONTENT;

import android.app.Activity;
import android.app.Dialog;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.Objects;

import com.ethionews.app.R;

/**
 * Sleep timer dialog — lets the user choose a countdown after which playback stops.
 *
 * Usage:
 *   SleepTimerDialog dialog = new SleepTimerDialog(activity, this::stopPlayback);
 *   dialog.showDialog();
 *
 * The {@link OnTimerExpiredListener} fires on the main thread when the timer expires.
 * Call {@link #cancelTimer()} from onDestroy to avoid leaks.
 */
public class SleepTimerDialog {

    public interface OnTimerExpiredListener {
        void onTimerExpired();
    }

    private static final int[] PRESET_MINUTES = {15, 30, 45, 60};

    // Tick interval for updating the "cancel" button countdown — every second
    private static final long TICK_MS = 1_000L;

    private final Activity ctx;
    private final OnTimerExpiredListener listener;
    private Dialog dialog;

    private final Handler handler = new Handler(Looper.getMainLooper());

    // Sleep-timer state
    private Runnable timerRunnable;
    private long     timerEndMs = -1; // -1 = no active timer

    // Countdown tick while the dialog is open
    private Runnable tickRunnable;
    private TextView tvCancelTimer; // kept so the tick can update it

    // Remember the last selection across dialog opens
    private int lastCheckedId = R.id.rd_30;

    public SleepTimerDialog(@NonNull Activity ctx, @NonNull OnTimerExpiredListener listener) {
        this.ctx      = ctx;
        this.listener = listener;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void showDialog() {
        dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_sleep_timer);

        RadioGroup   rg          = dialog.findViewById(R.id.rg_sleep_timer);
        LinearLayout llCustom    = dialog.findViewById(R.id.ll_custom_input);
        EditText     etCustom    = dialog.findViewById(R.id.et_custom_minutes);
                     tvCancelTimer = dialog.findViewById(R.id.tv_cancel_timer);
        TextView     tvSet       = dialog.findViewById(R.id.tv_sleep_set);
        TextView     tvCancel    = dialog.findViewById(R.id.tv_sleep_cancel);
        View         ivClose     = dialog.findViewById(R.id.iv_sleep_close);

        // Restore last-used preset
        rg.check(lastCheckedId);
        if (lastCheckedId == R.id.rd_custom) {
            llCustom.setVisibility(View.VISIBLE);
        }

        // Show live countdown if a timer is already running
        if (isTimerActive()) {
            tvCancelTimer.setVisibility(View.VISIBLE);
            updateCancelButtonText();
            startDialogTick();
        }

        // Show/hide custom input
        rg.setOnCheckedChangeListener((group, checkedId) -> {
            lastCheckedId = checkedId;
            llCustom.setVisibility(checkedId == R.id.rd_custom ? View.VISIBLE : View.GONE);
        });

        // Cancel active timer
        tvCancelTimer.setOnClickListener(v -> {
            cancelTimer();
            Toast.makeText(ctx, R.string.sleep_timer_cancelled, Toast.LENGTH_SHORT).show();
            dismissDialog();
        });

        // Set timer
        tvSet.setOnClickListener(v -> {
            int minutes = getSelectedMinutes(rg, etCustom);
            if (minutes <= 0) {
                Toast.makeText(ctx, R.string.sleep_timer_invalid, Toast.LENGTH_SHORT).show();
                return;
            }
            startTimer(minutes);
            Toast.makeText(ctx,
                    ctx.getString(R.string.sleep_timer_set_toast, String.valueOf(minutes)),
                    Toast.LENGTH_SHORT).show();
            dismissDialog();
        });

        tvCancel.setOnClickListener(v -> dismissDialog());
        ivClose.setOnClickListener(v -> dismissDialog());

        dialog.setOnDismissListener(d -> stopDialogTick());

        Objects.requireNonNull(dialog.getWindow())
                .setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
        dialog.show();
        dialog.getWindow().setLayout(MATCH_PARENT, WRAP_CONTENT);
    }

    /** Cancel any active sleep timer. Call this from onDestroy to avoid handler leaks. */
    public void cancelTimer() {
        if (timerRunnable != null) {
            handler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
        timerEndMs = -1;
        stopDialogTick();
    }

    /** Returns true if a sleep timer is currently counting down. */
    public boolean isTimerActive() {
        return timerRunnable != null && timerEndMs > System.currentTimeMillis();
    }

    /**
     * Returns a formatted remaining-time string like "14:32" for use in external
     * UI (e.g. a toolbar subtitle or status text).  Returns empty string if no
     * timer is active.
     */
    public String getRemainingTimeString() {
        if (!isTimerActive()) return "";
        long remainingMs = Math.max(0, timerEndMs - System.currentTimeMillis());
        return formatMs(remainingMs);
    }

    // ── Timer logic ───────────────────────────────────────────────────────────

    private void startTimer(int minutes) {
        cancelTimer();
        long delayMs = minutes * 60_000L;
        timerEndMs   = System.currentTimeMillis() + delayMs;
        timerRunnable = () -> {
            timerRunnable = null;
            timerEndMs    = -1;
            listener.onTimerExpired();
        };
        handler.postDelayed(timerRunnable, delayMs);
    }

    // ── Dialog live-countdown tick ────────────────────────────────────────────

    /** Starts a 1-second tick that refreshes the cancel-button text while dialog is open. */
    private void startDialogTick() {
        stopDialogTick();
        tickRunnable = new Runnable() {
            @Override
            public void run() {
                if (dialog == null || !dialog.isShowing()) return;
                if (!isTimerActive()) {
                    // Timer expired while dialog was open — close it
                    dismissDialog();
                    return;
                }
                updateCancelButtonText();
                handler.postDelayed(this, TICK_MS);
            }
        };
        handler.postDelayed(tickRunnable, TICK_MS);
    }

    private void stopDialogTick() {
        if (tickRunnable != null) {
            handler.removeCallbacks(tickRunnable);
            tickRunnable = null;
        }
    }

    /** Updates the cancel-button text with the current remaining time. */
    private void updateCancelButtonText() {
        if (tvCancelTimer == null) return;
        long remainingMs  = Math.max(0, timerEndMs - System.currentTimeMillis());
        String formatted  = formatMs(remainingMs);
        tvCancelTimer.setText(ctx.getString(R.string.sleep_timer_cancel_active, formatted));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int getSelectedMinutes(RadioGroup rg, EditText etCustom) {
        int id = rg.getCheckedRadioButtonId();
        if (id == R.id.rd_15)     return PRESET_MINUTES[0];
        if (id == R.id.rd_30)     return PRESET_MINUTES[1];
        if (id == R.id.rd_45)     return PRESET_MINUTES[2];
        if (id == R.id.rd_60)     return PRESET_MINUTES[3];
        if (id == R.id.rd_custom) {
            String raw = etCustom.getText() != null
                    ? etCustom.getText().toString().trim() : "";
            if (TextUtils.isEmpty(raw)) return 0;
            try {
                int val = Integer.parseInt(raw);
                return (val > 0 && val <= 600) ? val : 0; // max 10 hours
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /** Formats milliseconds as "mm:ss", e.g. 905_000 ms → "15:05". */
    private static String formatMs(long ms) {
        long totalSeconds = ms / 1_000L;
        long minutes      = totalSeconds / 60;
        long seconds      = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
