package com.ethionews.app.fragment.player;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.squareup.picasso.Picasso;

import com.ethionews.app.R;
import com.ethionews.app.utils.helper.SPHelper;

public class EmbeddedHLSFragment extends Fragment {

    @NonNull
    public static EmbeddedHLSFragment newInstance(String streamUrl, String imageCover,
                                                  String playerType, String streamName,
                                                  String userAgentName, boolean isUserAgent) {
        EmbeddedHLSFragment f = new EmbeddedHLSFragment();
        Bundle args = new Bundle();
        args.putString("streamUrl", streamUrl);
        args.putString("imageCover", imageCover);
        args.putString("player_type", playerType);
        args.putString("streamName", streamName);
        args.putString("userAgent", userAgentName);
        args.putBoolean("isUserAgent", isUserAgent);
        f.setArguments(args);
        return f;
    }

    private String streamUrl;
    private String imageUrl;
    private String streamName;
    private String userAgent;
    private String playerType;
    private boolean isUserAgent;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_embedded_image, container, false);

        SPHelper spHelper = new SPHelper(getActivity());

        if (getArguments() != null) {
            streamUrl = getArguments().getString("streamUrl");
            imageUrl = getArguments().getString("imageCover");

            streamName = getArguments().getString("streamName");
            userAgent = getArguments().getString("userAgent");
            playerType = getArguments().getString("player_type");
            isUserAgent = getArguments().getBoolean("isUserAgent");
        }

        ImageView imageCover = rootView.findViewById(R.id.imageCover);
        ImageView imagePlay = rootView.findViewById(R.id.imagePlay);

        Picasso.get().load(imageUrl).into(imageCover);

        imagePlay.setOnClickListener(v -> {
            if (!streamUrl.isEmpty()) {
                try {
                    Intent sendIntent = getSendIntent(spHelper.getHLSVideoPlayer());
                    startActivity(sendIntent);
                } catch (Exception e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id="
                                    + spHelper.getHLSVideoPlayer()))
                    );
                }
            } else {
                Toast.makeText(getActivity(), getString(R.string.stream_not_found), Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }

    @NonNull
    private Intent getSendIntent(String data) {
        Intent sendIntent = new Intent(Intent.ACTION_SEARCH);
        sendIntent.setPackage(data);
        sendIntent.putExtra("video_title", streamName);
        sendIntent.putExtra("video_url", streamUrl);
        if (isUserAgent){
            sendIntent.putExtra("video_agent", userAgent);
        } else {
            sendIntent.putExtra("video_agent", "");
        }
        // videoType = normal or youtube or webview
        sendIntent.putExtra("video_type", playerType);
        sendIntent.setType("text/plain");
        sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return sendIntent;
    }
}
