package com.ethionews.app.dialog;

import static android.view.WindowManager.LayoutParams.MATCH_PARENT;
import static android.view.WindowManager.LayoutParams.WRAP_CONTENT;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import android.app.ProgressDialog;

import java.util.Objects;

import com.ethionews.app.R;
import com.ethionews.app.callback.Method;
import com.ethionews.app.executor.GetRating;
import com.ethionews.app.executor.LoadRating;
import com.ethionews.app.interfaces.RatingListener;
import com.ethionews.app.utils.helper.Helper;
import com.ethionews.app.utils.helper.SPHelper;

public class ReviewDialog {

    private final Helper helper;
    private final SPHelper spHelper;
    private Dialog dialog;
    private final Activity ctx;
    private final ProgressDialog progressDialog;
    private final RatingDialogListener listener;

    public ReviewDialog(Activity ctx, RatingDialogListener listener) {
        this.ctx = ctx;
        this.listener = listener;
        helper = new Helper(ctx);
        spHelper = new SPHelper(ctx);
        progressDialog = new ProgressDialog(ctx);
    }

    public void showDialog(String id, String userRating, String userMessage) {
        dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_review);

        final TextView rate = dialog.findViewById(R.id.tv_rate);
        final RatingBar ratingBar = dialog.findViewById(R.id.rb_add);
        final EditText etMessages = dialog.findViewById(R.id.et_messages);
        final ProgressBar pbRate = dialog.findViewById(R.id.pb_rate);

        ratingBar.setStepSize(Float.parseFloat("1"));

        if(spHelper.isLogged()) {
            if (userRating.isEmpty() || userRating.equals("0")) {
                new GetRating(new RatingListener() {
                    @Override
                    public void onStart() {
                        pbRate.setVisibility(View.VISIBLE);
                        ratingBar.setEnabled(false);
                        etMessages.setEnabled(false);
                    }

                    @Override
                    public void onEnd(String success, String rateSuccess, String message, int rating) {
                        ratingBar.setEnabled(true);
                        etMessages.setEnabled(true);
                        pbRate.setVisibility(View.GONE);
                        if (rating > 0) {
                            ratingBar.setRating(rating);
                            etMessages.setText(message);
                            rate.setText(ctx.getString(R.string.thanks_for_rating));
                            listener.onGetRating(String.valueOf(rating),message);
                        } else {
                            ratingBar.setRating(1);
                        }
                    }
                }, helper.getAPIRequest(Method.METHOD_GET_RATINGS, 0, id,"","",
                        "", spHelper.getUserId(),"","","","",
                        "","","",null)).execute();
            } else {
                if (Integer.parseInt(userRating) != 0) {
                    rate.setText(ctx.getString(R.string.thanks_for_rating));
                    etMessages.setText(userMessage);
                    ratingBar.setRating(Integer.parseInt(userRating));
                } else {
                    ratingBar.setRating(1);
                }
            }
        } else {
            ratingBar.setRating(1);
        }
        dialog.findViewById(R.id.tv_cancel).setOnClickListener(view -> dismissDialog());
        dialog.findViewById(R.id.iv_close).setOnClickListener(view -> dismissDialog());
        dialog.findViewById(R.id.tv_submit).setOnClickListener(view -> {
            if (ratingBar.getRating() != 0) {
                if(etMessages.getText().toString().trim().isEmpty()) {
                    etMessages.setError(ctx.getString(R.string.report_message));
                    etMessages.requestFocus();
                } else {
                    if(spHelper.isLogged()) {
                        loadRatingApi(String.valueOf((int) ratingBar.getRating()), etMessages.getText().toString(), id);
                    } else {
                        helper.clickLogin();
                    }
                }
            } else {
                Toast.makeText(ctx, ctx.getString(R.string.select_rating), Toast.LENGTH_SHORT).show();
            }
        });
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
        dialog.show();
        Window window = dialog.getWindow();
        window.setLayout(MATCH_PARENT, WRAP_CONTENT);
    }

    private void loadRatingApi(final String rate, final String report, String id) {
        if (helper.isNetworkAvailable()) {
            LoadRating loadRating = new LoadRating(new RatingListener() {

                @Override
                public void onStart() {
                    progressDialog.show();
                    listener.onShow();
                }

                @Override
                public void onEnd(String success, String rateSuccess, String message, int rating) {
                    listener.onDismiss(success, rateSuccess, message, rating, rate, report);
                    dismissDialog();
                }
            }, helper.getAPIRequest(Method.METHOD_RATINGS, 0, id,"","",
                    report, spHelper.getUserId(),"","","","","",
                    rate,"",null));
            loadRating.execute();
        }else {
            Toast.makeText(ctx, ctx.getString(R.string.err_internet_not_connected), Toast.LENGTH_SHORT).show();
        }
    }
    public void dismissDialog() {
        if (dialog != null && dialog.isShowing()){
            dialog.dismiss();
        }
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    public interface RatingDialogListener {
        void onShow();
        void onGetRating(String rating, String message);
        void onDismiss(String success, String rateSuccess, String message,
                       int rating, String userRating , String userMessage);
    }
}
