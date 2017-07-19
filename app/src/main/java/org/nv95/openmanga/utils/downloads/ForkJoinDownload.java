package org.nv95.openmanga.utils.downloads;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.annotation.WorkerThread;

import org.nv95.openmanga.providers.staff.MangaProviderManager;
import org.nv95.openmanga.utils.NetworkUtils;
import org.nv95.openmanga.utils.NoSSLv3SocketFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.HttpsURLConnection;

import info.guardianproject.netcipher.NetCipher;

/**
 * Created by admin on 19.07.17.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ForkJoinDownload extends Download {

    private int mThreadsCount = 2;
    private final ArrayList<PartDownloader> mDownloaders = new ArrayList<PartDownloader>();

    public ForkJoinDownload(String url, File destination) {
        super(url, destination);
    }

    public ForkJoinDownload(String url, File destination, int threadsCount) {
        super(url, destination);
        setThreadsCount(threadsCount);
    }

    public void setThreadsCount(int count) {
        mThreadsCount = count;
    }


    @WorkerThread
    @Override
    public void download() {
        boolean success = true;
        try {
            final long contentLength = NetworkUtils.getContentLength(mSourceUrl);
            ForkJoinPool pool = new ForkJoinPool();
            long lengthPerCopy = contentLength / mThreadsCount;
            long position = 0L;
            new RandomAccessFile(mDestinationFile, "rw").setLength(contentLength);

            for (int i = 0; i < mThreadsCount; i++) {
                PartDownloader downloader;
                if (i == mThreadsCount - 1) {
                    //the last thread
                    downloader = new PartDownloader(mSourceUrl, mDestinationFile, position, contentLength - position);
                } else {
                    downloader = new PartDownloader(mSourceUrl, mDestinationFile, position, lengthPerCopy);
                    position = position + lengthPerCopy + 1;
                }
                mDownloaders.add(downloader);
                pool.execute(downloader);
            }

            do {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                    if (mCopyListener != null) {
                        int progress = getDownloadedSize();
                        if (!mCopyListener.onBytesCopied(progress, (int) contentLength)) {
                            success = false;
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    success = false;
                    e.printStackTrace();
                }
            } while (!isDone()); //when all the thread not been done
            for (PartDownloader o : mDownloaders) {
                success &= o.isSuccess();
            }
            pool.shutdown();
        } catch (IOException e) {
            success = false;
            e.printStackTrace();
        } finally {
            if (!success) {
                mDestinationFile.delete();
            }
        }
    }

    private int getDownloadedSize() {
        int size = 0;
        for (PartDownloader o : mDownloaders) {
            size += o.getCount();
        }
        return size;
    }

    private boolean isDone() {
        boolean res = true;
        for (PartDownloader o : mDownloaders) {
            res &= o.isDone();
        }
        return res;
    }

    private static class PartDownloader extends RecursiveTask<Integer> {

        private final AtomicLong mCount;
        private final AtomicBoolean mSuccess;
        private final HttpURLConnection mConnection;
        private final RandomAccessFile mDestination;

        PartDownloader(String inputUrl, File outputFile, long position, long count) throws IOException {
            mDestination = new RandomAccessFile(outputFile, "rw");
            mDestination.seek(position);
            mCount = new AtomicLong(0);
            mSuccess = new AtomicBoolean(true);
            mConnection = NetCipher.getHttpURLConnection(inputUrl);
            if (mConnection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) mConnection).setSSLSocketFactory(NoSSLv3SocketFactory.getInstance());
            }
            MangaProviderManager.prepareConnection(mConnection);
            mConnection.setRequestProperty("Range", "Bytes=" + position + "-" + (position + count - 1));
            mConnection.setConnectTimeout(15000);
        }

        @Override
        protected Integer compute() {
            int processed = 0;
            try {
                mConnection.connect();
                InputStream inputStream = mConnection.getInputStream();
                byte data[] = new byte[4096];
                int count;
                while ((count = inputStream.read(data)) != -1) {
                    mDestination.write(data, 0, count);
                    processed += count;
                    mCount.addAndGet(count);
                    if (Thread.currentThread().isInterrupted()) {
                        mSuccess.set(false);
                        break;
                    }
                }
            } catch (IOException e) {
                mSuccess.set(false);
                e.printStackTrace();
            } finally {
                mConnection.disconnect();
            }
            return processed;
        }

        boolean isSuccess() {
            return mSuccess.get();
        }

        long getCount() {
            return mCount.get();
        }
    }
}
