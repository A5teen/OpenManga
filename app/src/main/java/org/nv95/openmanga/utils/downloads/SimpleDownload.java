package org.nv95.openmanga.utils.downloads;

import org.nv95.openmanga.providers.staff.MangaProviderManager;
import org.nv95.openmanga.utils.NoSSLv3SocketFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import javax.net.ssl.HttpsURLConnection;

import info.guardianproject.netcipher.NetCipher;

/**
 * Created by nv95 on 12.02.16.
 */
public class SimpleDownload extends Download {


    public SimpleDownload(String url, File destination) {
        super(url, destination);
    }

    @Override
    public void download() {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            connection = NetCipher.getHttpURLConnection(mSourceUrl);
            if (connection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) connection).setSSLSocketFactory(NoSSLv3SocketFactory.getInstance());
            }
            MangaProviderManager.prepareConnection(connection);
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return;
            }
            input = connection.getInputStream();
            output = new FileOutputStream(mDestinationFile);
            byte data[] = new byte[4096];
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
            }
        } catch (Exception e) {
            if (mDestinationFile.exists()) {
                mDestinationFile.delete();
            }
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }
            if (connection != null)
                connection.disconnect();
        }
    }
}
