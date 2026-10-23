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

public class ExternalImageFragment extends Fragment {

    @NonNull
    public static ExternalImageFragment newInstance(String streamUrl, String imageCover,
                                                    boolean isPlayVisible, String external) {
        ExternalImageFragment f = new ExternalImageFragment();
        Bundle args = new Bundle();
        args.putString("streamUrl", streamUrl);
        args.putString("imageCover", imageCover);
        args.putBoolean("isPlayVisible", isPlayVisible);
        args.putString("external", external);
        f.setArguments(args);
        return f;
    }

    private String streamUrl;
    private String imageUrl;
    private String external;
    private boolean isPlayVisible;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_embedded_image, container, false);

        if (getArguments() != null) {
            streamUrl = getArguments().getString("streamUrl");
            imageUrl = getArguments().getString("imageCover");
            isPlayVisible = getArguments().getBoolean("isPlayVisible");
            external = getArguments().getString("external");
        }

        ImageView imageCover = rootView.findViewById(R.id.imageCover);
        ImageView imagePlay = rootView.findViewById(R.id.imagePlay);

        imagePlay.setVisibility(isPlayVisible ? View.VISIBLE : View.GONE);
        Picasso.get().load(imageUrl).into(imageCover);

        imagePlay.setOnClickListener(v -> {
            if (!streamUrl.isEmpty()) {
                if (external.equals("browser")){
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(streamUrl));
                    startActivity(intent);

                } else if (external.equals("hls_player")){
                    final String application = new SPHelper(requireActivity()).getHLSVideoPlayer();
                    try {
                        Intent sendIntent = getSendIntent(application);
                        startActivity(sendIntent);
                    } catch (Exception e) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + application)));
                    }
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(streamUrl), "video/*");
                    startActivity(Intent.createChooser(intent, "live"));
                }
            } else {
                Toast.makeText(getActivity(), getString(R.string.stream_not_found), Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }

    @NonNull
    private static Intent getSendIntent(String application) {
        Intent sendIntent = new Intent(Intent.ACTION_SEARCH);
        sendIntent.setPackage(application);
        sendIntent.putExtra("video_title", "7UP Madras Gig");
        sendIntent.putExtra("video_url", "https://example.com/sample.mp4");
        sendIntent.putExtra("video_agent", "");
        // videoType = normal or youtube or  webview
        sendIntent.putExtra("video_type", "normal");
        sendIntent.setType("text/plain");
        sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return sendIntent;
    }
}
