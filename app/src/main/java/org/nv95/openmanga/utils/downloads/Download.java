package org.nv95.openmanga.utils.downloads;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.nostra13.universalimageloader.utils.IoUtils;

import java.io.File;

/**
 * Created by admin on 19.07.17.
 */

public abstract class Download implements Runnable {

    protected final String mSourceUrl;
    protected final File mDestinationFile;
    @Nullable
    protected IoUtils.CopyListener mCopyListener = null;

    public Download(String url, File destination) {
        this.mSourceUrl = url;
        this.mDestinationFile = destination;
    }

    public String getSourceUrl() {
        return mSourceUrl;
    }

    public File getDestinationFile() {
        return mDestinationFile;
    }

    public void setCopyListener(@Nullable IoUtils.CopyListener listener) {
        mCopyListener = listener;
    }

    public boolean isSuccess() {
        return mDestinationFile.exists();
    }

    @Override
    public final void run() {
        final long startTime = System.currentTimeMillis();
        download();
        long executionTime = System.currentTimeMillis() - startTime;
        Log.d("TIMING", this.getClass().getName() + ": " + executionTime + "ms");
    }

    protected abstract void download();

    @NonNull
    public static Download getSupported(String url, File destination, int threadsCount) {
        if (threadsCount <= 1 || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return new SimpleDownload(url, destination);
        } else {
            return new ForkJoinDownload(url, destination, threadsCount);
        }
    }
}
