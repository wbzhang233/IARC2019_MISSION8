/*
 * Copyright 2016 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dji.P4MissionsDemo.tensorflow;

import android.Manifest;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;

import com.dji.P4MissionsDemo.tensorflow.env.Logger;

public abstract class DetectorFeeder {
    private static final Logger LOGGER = new Logger();

    private static final int PERMISSIONS_REQUEST = 1;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    private boolean debug = false;

    private Handler handler;
    private HandlerThread handlerThread;
    private boolean useCamera2API;
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private Bitmap rgbBitmap = null;
    private int yRowStride;
    private int rotation_get;

    protected int previewWidth = 0;
    protected int previewHeight = 0;

    private Runnable postInferenceCallback;
    private Runnable imageConverter;
    private OverlayView overlay;


    protected Bitmap getRgbBitmap(){
        return rgbBitmap;
    }

    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    protected byte[] getLuminance() {
        return yuvBytes[0];
    }

    public void onFeed(final byte[] bytes, final Size size) {
        if (isProcessingFrame) {
            LOGGER.w("Dropping frame!");
            return;
        }

        try {
            // Initialize the storage bitmaps once when the resolution is known.
            if (rgbBytes == null) {
                previewHeight = size.getHeight();
                previewWidth = size.getWidth();
                rgbBytes = new int[previewWidth * previewHeight];
                onPreviewSizeChosen(new Size(size.getWidth(), size.getHeight()), 90);
            }
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            return;
        }

        isProcessingFrame = true;
        yuvBytes[0] = bytes;
        yRowStride = previewWidth;

        imageConverter =
                new Runnable() {
                    @Override
                    public void run() {
//                        ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
                        rgbBytes =  byteArrayTointArray(bytes);
                    }
                };

        postInferenceCallback =
                new Runnable() {
                    @Override
                    public void run() {
                        isProcessingFrame = false;
                    }
                };
        processImage();
    }
    public void onFeed(final Bitmap bitmap, final Size size) {
        if (isProcessingFrame) {
            LOGGER.w("Dropping frame!");
            return;
        }

        try {
            // Initialize the storage bitmaps once when the resolution is known.
            if (rgbBytes == null) {
                previewHeight = size.getHeight();
                previewWidth = size.getWidth();
                rgbBytes = new int[previewWidth * previewHeight];
                onPreviewSizeChosen(new Size(size.getWidth(), size.getHeight()), 90);
            }
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            return;
        }

        isProcessingFrame = true;
        rgbBitmap = Bitmap.createBitmap(bitmap);

        postInferenceCallback =
                new Runnable() {
                    @Override
                    public void run() {
                        isProcessingFrame = false;
                    }
                };
        processImage();
    }

    public synchronized void onStart() {
        LOGGER.d("onResume " + this);

        if(handlerThread == null || handler == null) {
            handlerThread = new HandlerThread("inference");
            handlerThread.start();
            handler = new Handler(handlerThread.getLooper());
        }
    }

    public synchronized void onPause() {
        LOGGER.d("onPause " + this);

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    public boolean isDebug() {
        return debug;
    }

    public void DetectorFeederblend (OverlayView olv,int rotation){
        overlay = olv;
        rotation_get = rotation;
    }

    public void requestRender() {
//        final OverlayView overlay = (OverlayView) findViewById(R.id.debug_overlay);
        if (overlay != null) {
            overlay.postInvalidate();
        }
        else {

        }
    }

    public void addCallback(final OverlayView.DrawCallback callback) {

//        final OverlayView overlay = (OverlayView) findViewById(R.id.debug_overlay);
        if (overlay != null) {
            overlay.addCallback(callback);
        }
        else {

        }
    }


    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    protected int getScreenOrientation() {
        switch (rotation_get) {     //getWindowManager().getDefaultDisplay().getRotation()
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    public static int[] byteArrayTointArray(byte[] btarr) {
        if (btarr.length % 4 != 0) {
            return null;
        }
        int[] intarr = new int[btarr.length / 4];
        int i1, i2, i3, i4;
        for (int j = 0, k = 0; j < intarr.length; j++, k += 4)//j循环int		k循环byte数组
        {
            i1 = btarr[k];
            i2 = btarr[k + 1];
            i3 = btarr[k + 2];
            i4 = btarr[k + 3];
            if (i1 < 0) {
                i1 += 256;
            }
            if (i2 < 0) {
                i2 += 256;
            }
            if (i3 < 0) {
                i3 += 256;
            }
            if (i4 < 0) {
                i4 += 256;
            }
            intarr[j] = (i1 << 24) + (i2 << 16) + (i3 << 8) + (i4 << 0);//保存Int数据类型转换
        }
        return intarr;
    }
    protected abstract void processImage();

    protected abstract void onPreviewSizeChosen(final Size size, final int rotation);
}
