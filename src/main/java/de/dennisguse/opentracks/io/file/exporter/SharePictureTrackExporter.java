package de.dennisguse.opentracks.io.file.exporter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;

import java.io.OutputStream;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.databinding.TrackSharePictureBinding;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.StringUtils;

/**
 * Create a share picture for a track.
 * Here a layout is inflated and converted to PNG.
 * <p>
 * TODO
 * If this classes is used with non-visual context (e.g., ApplicationContext), it may not work in the future.
 * API31 introduces `ncorrectContextUseViolation`, which may thrown during runtime.
 * This code was tested on API30.
 * Some more information: https://stackoverflow.com/questions/68539083/inflate-layout-from-applicationcontext-i-e-in-a-contentprovider/68567186#68567186
 * <p>
 * Future plan: draw directly on a view.
 */
public class SharePictureTrackExporter implements TrackExporter {

    private static final String TAG = SharePictureTrackExporter.class.getSimpleName();

    private final static int imageWidth = 1024;

    private final Context context;

    private final Bitmap.CompressFormat compressFormat;

    public SharePictureTrackExporter(Context context, Bitmap.CompressFormat compressFormat) {
        this.context = context;
        this.compressFormat = compressFormat;
    }

    @Override
    public boolean writeTrack(Track[] tracks, @NonNull OutputStream outputStream) {
        if (tracks.length > 1) {
            Log.e(TAG, "Not yet implemented.");
            return false;
        }

        return writeTrack(tracks[0], outputStream);
    }

    @Override
    public boolean writeTrack(Track track, @NonNull OutputStream outputStream) {
        int imageHeight = imageWidth / 4 * 3;

        TrackSharePictureBinding viewBinding = TrackSharePictureBinding.inflate(LayoutInflater.from(context));
        viewBinding.sharePictureCategory.setText(track.getCategory());

        boolean metricUnits = PreferencesUtils.isMetricUnits(PreferencesUtils.getSharedPreferences(context), context);
        TrackStatistics trackStatistics = track.getTrackStatistics();

        viewBinding.sharePictureTotalTime.setText(StringUtils.formatElapsedTimeWithHour(track.getTrackStatistics().getTotalTime()));
        viewBinding.sharePictureTotalDistance.setText(StringUtils.formatDistance(context, track.getTrackStatistics().getTotalDistance(), metricUnits));
        viewBinding.sharePictureGain.setText(StringUtils.formatAltitude(context, trackStatistics.getTotalAltitudeGain(), metricUnits));

        // prepare rendering
        View view = viewBinding.getRoot();
        int widthSpec = View.MeasureSpec.makeMeasureSpec(imageWidth, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(imageHeight, View.MeasureSpec.EXACTLY);
        view.measure(widthSpec, heightSpec);
        view.layout(0, 0, imageWidth, imageHeight);

        // draw
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);

        view.draw(canvas);

        // store
        return bitmap.compress(compressFormat, 100, outputStream);
    }
}
