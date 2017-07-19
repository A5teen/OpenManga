package org.nv95.openmanga.components.reader;

import android.graphics.Bitmap;

import com.nostra13.universalimageloader.cache.disc.DiskCache;
import com.nostra13.universalimageloader.utils.IoUtils;

import org.nv95.openmanga.helpers.DirRemoveHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by admin on 19.07.17.
 */

public class PagesCache implements DiskCache {

    private final File mCacheDir;

    public PagesCache(File cacheDir) {
        mCacheDir = cacheDir;
        if (!mCacheDir.exists()) {
            mCacheDir.mkdirs();
        }
    }

    @Override
    public File getDirectory() {
        return mCacheDir;
    }

    @Override
    public File get(String imageUri) {
        return new File(mCacheDir, String.valueOf(imageUri.hashCode()));
    }

    @Override
    public boolean save(String imageUri, InputStream imageStream, IoUtils.CopyListener listener) throws IOException {
        return false;
    }

    @Override
    public boolean save(String imageUri, Bitmap bitmap) throws IOException {
        return false;
    }

    @Override
    public boolean remove(String imageUri) {
        File f = get(imageUri);
        return f.exists() && f.delete();
    }

    @Override
    public void close() {

    }

    @Override
    public void clear() {
        new DirRemoveHelper(mCacheDir).run();
    }
}
