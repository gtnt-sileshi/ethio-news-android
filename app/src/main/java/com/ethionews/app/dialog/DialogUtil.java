package com.ethionews.app.dialog;

import static android.view.WindowManager.LayoutParams.MATCH_PARENT;
import static android.view.WindowManager.LayoutParams.WRAP_CONTENT;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

import java.util.Objects;

import com.ethionews.app.R;
import com.ethionews.app.activity.BillingSubscribeActivity;
import com.ethionews.app.callback.Callback;
import com.ethionews.app.utils.ApplicationUtil;


public class DialogUtil {

    private static Dialog dialog;

    private DialogUtil() {
        throw new IllegalStateException("Utility class");
    }

    // Dialog
    public static void exitDialog(Activity activity) {
        if (dialog != null){
            dialog = null;
        }
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_app);

        ImageView icon = dialog.findViewById(R.id.iv_dialog_icon);
        icon.setImageResource(R.drawable.ic_exit);

        TextView title = dialog.findViewById(R.id.tv_dialog_title);
        title.setText(R.string.exit);

        TextView msg = dialog.findViewById(R.id.tv_dialog_msg);
        msg.setText(R.string.sure_exit);

        dialog.findViewById(R.id.iv_dialog_close).setOnClickListener(view -> dialog.dismiss());

        dialog.findViewById(R.id.tv_dialog_no).setOnClickListener(view -> dialog.dismiss());
        dialog.findViewById(R.id.tv_dialog_yes).setOnClickListener(view -> {
            dialog.dismiss();
            activity.finish();
        });

        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null){
            window.setLayout(MATCH_PARENT, WRAP_CONTENT);
        }
    }

    public static void maintenanceDialog(Activity activity) {
        if (dialog != null){
            dialog = null;
        }
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_app);

        ImageView icon = dialog.findViewById(R.id.iv_dialog_icon);
        icon.setImageResource(R.drawable.ic_error);

        TextView title = dialog.findViewById(R.id.tv_dialog_title);
        title.setText(R.string.maintenance);

        TextView msg = dialog.findViewById(R.id.tv_dialog_msg);
        msg.setText(R.string.we_are_performing_scheduled);

        // VISIBLE
        TextView titleSub = dialog.findViewById(R.id.tv_dialog_title_sub);
        titleSub.setVisibility(View.VISIBLE);
        titleSub.setText(R.string.temporarily_down_for_maintenance);

        ImageView iconBg = dialog.findViewById(R.id.iv_dialog_icon_bg);
        iconBg.setVisibility(View.VISIBLE);
        iconBg.setImageResource(R.drawable.ic_maintenance);

        dialog.findViewById(R.id.iv_dialog_close).setOnClickListener(view -> {
            dialog.dismiss();
            activity.finish();
        });

        TextView no = dialog.findViewById(R.id.tv_dialog_no);
        no.setText(R.string.cancel);
        no.setOnClickListener(view -> {
            dialog.dismiss();
            activity.finish();
        });

        TextView yes = dialog.findViewById(R.id.tv_dialog_yes);
        yes.setVisibility(View.GONE);

        View view = dialog.findViewById(R.id.vw_dialog_bar);
        view.setVisibility(View.GONE);

        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null){
            window.setLayout(MATCH_PARENT, WRAP_CONTENT);
        }
    }

    public static void upgradeDialog(Activity activity, CancelListener listener) {
        if (dialog != null){
            dialog = null;
        }
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_app);

        ImageView icon = dialog.findViewById(R.id.iv_dialog_icon);
        icon.setImageResource(R.drawable.ic_error);

        TextView title = dialog.findViewById(R.id.tv_dialog_title);
        title.setText(R.string.upgrade);

        TextView msg = dialog.findViewById(R.id.tv_dialog_msg);
        msg.setText(R.string.its_time_to_upgrade);

        // VISIBLE
        TextView titleSub = dialog.findViewById(R.id.tv_dialog_title_sub);
        titleSub.setVisibility(View.VISIBLE);
        titleSub.setText(R.string.upgrade);

        ImageView iconBg = dialog.findViewById(R.id.iv_dialog_icon_bg);
        iconBg.setVisibility(View.VISIBLE);
        iconBg.setImageResource(R.drawable.ic_upgrade_svg);

        dialog.findViewById(R.id.iv_dialog_close).setOnClickListener(view -> {
            dialog.dismiss();
            listener.onCancel();
        });

        TextView no = dialog.findViewById(R.id.tv_dialog_no);
        no.setText(R.string.cancel);
        no.setOnClickListener(view -> {
            dialog.dismiss();
            listener.onCancel();
        });

        TextView yes = dialog.findViewById(R.id.tv_dialog_yes);
        yes.setText(R.string.do_it_now);
        yes.setOnClickListener(view -> {
            dialog.dismiss();
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Callback.getAppRedirectUrl())));
        });

        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null){
            window.setLayout(MATCH_PARENT, WRAP_CONTENT);
        }
    }

    public static void dModeDialog(Activity activity) {
        if (dialog != null){
            dialog = null;
        }
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_app);

        ImageView icon = dialog.findViewById(R.id.iv_dialog_icon);
        icon.setImageResource(R.drawable.ic_error);

        TextView title = dialog.findViewById(R.id.tv_dialog_title);
        title.setText(R.string.developer_mode);

        TextView msg = dialog.findViewById(R.id.tv_dialog_msg);
        msg.setText(R.string.turn_off_developer_mode);

        // VISIBLE
        TextView titleSub = dialog.findViewById(R.id.tv_dialog_title_sub);
        titleSub.setVisibility(View.VISIBLE);
        titleSub.setText(R.string.developer_mode);

        ImageView iconBg = dialog.findViewById(R.id.iv_dialog_icon_bg);
        iconBg.setVisibility(View.VISIBLE);
        iconBg.setImageResource(R.drawable.ic_coding_development);

        dialog.findViewById(R.id.iv_dialog_close).setOnClickListener(view -> {
            dialog.dismiss();
            activity.finish();
        });

        TextView no = dialog.findViewById(R.id.tv_dialog_no);
        no.setText(R.string.try_again_later);
        no.setOnClickListener(view -> {
            dialog.dismiss();
            activity.finish();
        });

        TextView yes = dialog.findViewById(R.id.tv_dialog_yes);
        yes.setVisibility(View.GONE);

        View view = dialog.findViewById(R.id.vw_dialog_bar);
        view.setVisibility(View.GONE);

        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null){
            window.setLayout(MATCH_PARENT, WRAP_CONTENT);
        }
    }

    public static void vpnDialog(Activity activity) {
        if (dialog != null){
            dialog = null;
        }
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_app);

        ImageView icon = dialog.findViewById(R.id.iv_dialog_icon);
        icon.setImageResource(R.drawable.ic_error);

        TextView title = dialog.findViewById(R.id.tv_dialog_title);
        title.setText(R.string.sniffing_detected);

        TextView msg = dialog.findViewById(R.id.tv_dialog_msg);
        msg.setText(R.string.turn_off_all_sniffers_tools);

        // VISIBLE
        TextView titleSub = dialog.findViewById(R.id.tv_dialog_title_sub);
        titleSub.setVisibility(View.VISIBLE);
        titleSub.setText(R.string.sniffing_detected);

        ImageView iconBg = dialog.findViewById(R.id.iv_dialog_icon_bg);
        iconBg.setVisibility(View.VISIBLE);
        iconBg.setImageResource(R.drawable.ic_vpn_network);

        dialog.findViewById(R.id.iv_dialog_close).setOnClickListener(view -> {
            dialog.dismiss();
            activity.finish();
        });

        TextView no = dialog.findViewById(R.id.tv_dialog_no);
        no.setText(R.string.cancel);
        no.setOnClickListener(view -> {
            dialog.dismiss();
            activity.finish();
        });

        TextView yes = dialog.findViewById(R.id.tv_dialog_yes);
        yes.setVisibility(View.GONE);

        View view = dialog.findViewById(R.id.vw_dialog_bar);
        view.setVisibility(View.GONE);

        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null){
            window.setLayout(MATCH_PARENT, WRAP_CONTENT);
        }
    }

    public static void trashDialog(Activity activity, DeleteListener listener) {
        if (dialog != null){
            dialog = null;
        }
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_app);

        ImageView icon = dialog.findViewById(R.id.iv_dialog_icon);
        icon.setImageResource(R.drawable.ic_trash);

        TextView title = dialog.findViewById(R.id.tv_dialog_title);
        title.setText(R.string.delete);

        TextView msg = dialog.findViewById(R.id.tv_dialog_msg);
        msg.setText(R.string.sure_delete);

        dialog.findViewById(R.id.iv_dialog_close).setOnClickListener(view -> {
            dialog.dismiss();
            listener.onCancel();
        });

        dialog.findViewById(R.id.tv_dialog_no).setOnClickListener(view -> {
            dialog.dismiss();
            listener.onCancel();
        });
        dialog.findViewById(R.id.tv_dialog_yes).setOnClickListener(view -> {
            dialog.dismiss();
            listener.onDelete();
        });

        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null){
            window.setLayout(MATCH_PARENT, WRAP_CONTENT);
        }
    }

    public static void verifyDialog(Activity activity, String titleData, String message, CancelListener listener) {
        if (dialog != null){
            dialog = null;
        }
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_app);

        ImageView icon = dialog.findViewById(R.id.iv_dialog_icon);
        icon.setImageResource(R.drawable.ic_notification);

        TextView title = dialog.findViewById(R.id.tv_dialog_title);
        title.setText(titleData);

        TextView msg = dialog.findViewById(R.id.tv_dialog_msg);
        msg.setText(message);

        // VISIBLE
        dialog.findViewById(R.id.iv_dialog_close).setOnClickListener(view -> {
            dialog.dismiss();
            listener.onCancel();
        });

        TextView no = dialog.findViewById(R.id.tv_dialog_no);
        no.setText(R.string.ok);
        no.setOnClickListener(view -> {
            dialog.dismiss();
            listener.onCancel();
        });

        TextView yes = dialog.findViewById(R.id.tv_dialog_yes);
        yes.setVisibility(View.GONE);

        View view = dialog.findViewById(R.id.vw_dialog_bar);
        view.setVisibility(View.GONE);

        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null){
            window.setLayout(MATCH_PARENT, WRAP_CONTENT);
        }
    }

    public static void premiumDialog(Activity activity) {
        if (dialog != null){
            dialog = null;
        }
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_app_premium);
        dialog.findViewById(R.id.iv_dialog_close).setOnClickListener(view -> dialog.dismiss());
        dialog.findViewById(R.id.tv_dialog_no).setOnClickListener(view -> {
            dialog.dismiss();
            activity.startActivity(new Intent(activity, BillingSubscribeActivity.class));
        });
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null){
            window.setLayout(MATCH_PARENT, WRAP_CONTENT);
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    public static void dialogPlayerInfo(Activity ctx, ExoPlayer exoPlayer) {
        if (exoPlayer != null){
            if (dialog != null){
                dialog = null;
            }
            dialog = new Dialog(ctx);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_media_info);
            dialog.findViewById(R.id.iv_close_vw).setOnClickListener(v -> dialog.dismiss());
            dialog.findViewById(R.id.iv_back_player_info).setOnClickListener(v -> dialog.dismiss());

            String infoVideo = ApplicationUtil.getInfoVideo(exoPlayer);
            TextView mediaVideo = dialog.findViewById(R.id.tv_info_video);
            mediaVideo.setText(infoVideo);

            String infoAudio = ApplicationUtil.getInfoAudio(exoPlayer);
            TextView mediaAudio = dialog.findViewById(R.id.tv_info_audio);
            mediaAudio.setText(infoAudio);

            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
            dialog.show();
            Window window = dialog.getWindow();
            if (window != null){
                window.setLayout(MATCH_PARENT, WRAP_CONTENT);
            }
        }
    }

    // Listener
    public interface CancelListener {
        void onCancel();
    }

    public interface DeleteListener {
        void onDelete();
        void onCancel();
    }
}
