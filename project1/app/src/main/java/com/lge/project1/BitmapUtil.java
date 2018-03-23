/*
 * Mobile Communication Company, LG ELECTRONICS INC., SEOUL, KOREA
 * Copyright(c) 2017 by LG Electronics Inc.
 *
 * All rights reserved. No part of this work may be reproduced, stored in a
 * retrieval system, or transmitted by any means without prior written
 * Permission of LG Electronics Inc.
 */
package com.lge.project1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.VectorDrawable;
import android.media.ExifInterface;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BitmapUtil {

    public static Bitmap getBitmapFromByteArray(byte[] stream, int scrW, int srcH,
                                                int resizeW, int resizeH, Matrix matrix) {

        Bitmap bm = Bitmap.createBitmap(scrW, srcH, Bitmap.Config.ARGB_8888);
        ColorConverter.byteArrayToBitmap(bm, stream);

        if (matrix == null) {
            return bm;
        }

        Bitmap bmRotate = Bitmap.createBitmap(bm, 0, 0, scrW, srcH, matrix, true);
        if (!bm.isRecycled()) {
            bm.recycle();
            bm = null;
        }

        if (resizeW == 0 || resizeH == 0) {
            return bmRotate;
        }

        Bitmap bmResize = Bitmap.createScaledBitmap(bmRotate, resizeW, resizeW, true);
        if (!bmRotate.isRecycled()) {
            bmRotate.recycle();
            bmRotate = null;
        }
        return bmResize;
    }

    public static Bitmap getBitmapFromFilePath(String filePath) {
        Bitmap bmp = BitmapFactory.decodeFile(filePath);
        Bitmap rotated = null;
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (exif == null) {
            return bmp;
        }
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        int orientationDegree = 0;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                orientationDegree = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                orientationDegree = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                orientationDegree = 270;
                break;
            default:
                break;
        }
        if (orientationDegree != 0) {
            rotated = rotateImage(bmp, orientationDegree);
            if (rotated != null && !rotated.isRecycled()) {
                bmp.recycle();
                bmp = rotated;
            }
        }
        return bmp;
    }

    public static Bitmap rotateImage(Bitmap bitmap, int degrees) {
        if (degrees != 0 && bitmap != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) bitmap.getWidth() / 2,
                    (float) bitmap.getHeight() / 2);

            try {
                Bitmap converted = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), m, true);
                if (bitmap != converted) {
                    bitmap.recycle();
                    bitmap = converted;
                }
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            }
        }

        return bitmap;
    }

    public static Bitmap cropImage(Bitmap image, RectF cropRect) {
        if (image == null || image.isRecycled()) {
            Log.d("BitmapUtil", "cropRect : " + cropRect);
            return null;
        }

        if (cropRect == null || cropRect.isEmpty()) {
            return image;
        }

        Bitmap cropped;
        try {
            cropped = Bitmap.createBitmap(image,
                    (int) cropRect.left, (int) cropRect.top,
                    (int) cropRect.width(), (int) cropRect.height());
        } catch (Exception e) {
            e.printStackTrace();
            cropped = null;
        }

        return cropped;
    }

    public static int[] pointFArrayToIntArray(PointF[] p) {
        int[] array = new int[p.length * 2];
        for (int i = 0; i < p.length; ++i) {
            array[i * 2] = (int) p[i].x;
            array[i * 2 + 1] = (int) p[i].y;
        }

        return array;
    }

    public static Bitmap flipVertical(Bitmap bm) {
        Matrix matrixFlip = new Matrix();
        matrixFlip.preScale(-1.0f, 1.0f);
        Bitmap bmFlip = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrixFlip, true);
        return bmFlip;
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff000000;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = bitmap.getWidth() / 10;
        final float roundPxh = bitmap.getHeight() / 10;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);

        canvas.drawRoundRect(rectF, roundPx, roundPxh, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(roundPx);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        return output;
    }

    public static Bitmap getRotateBitmap(Bitmap orgBitmap, int x, int y, int width, int height, int degree) {
        Matrix matrix = new Matrix();
        matrix.preRotate(degree);
        Bitmap outputBitmap = Bitmap.createBitmap(orgBitmap, x, y, width, height, matrix, true);
        return outputBitmap;
    }

    public static Bitmap getBitmapFromFile(final String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        int bitmapWidth = 0;
        int bitmapHeight = 0;

        options.inJustDecodeBounds = false;
        if (options.outWidth * options.outHeight > 1920 * 1440) {
            float ratioW = (float) 1440 / (float) options.outWidth;
            float ratioH = (float) 1920 / (float) options.outHeight;
            float contentRatio = (float) options.outWidth / (float) options.outHeight;

            if (ratioW < ratioH) {
                bitmapWidth = 1440;
                bitmapHeight = (int) ((float) bitmapWidth / contentRatio);
            } else {
                bitmapHeight = 1920;
                bitmapWidth = (int) ((float) bitmapHeight * contentRatio);
            }

            Bitmap temp = BitmapFactory.decodeFile(path, options);
            Bitmap outBitmap = Bitmap.createScaledBitmap(temp, bitmapWidth, bitmapHeight, true);
            if (temp != null && !temp.isRecycled()) {
                temp.recycle();
                temp = null;
            }
            return outBitmap;
        } else {
            return BitmapFactory.decodeFile(path, options);
        }
    }

    public static Bitmap getBitmapFromURL(final String src_url) {

        HttpURLConnection connection = null;
        try {
            URL url = new URL(src_url);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            if (input != null) {
                input.close();
                input = null;
            }
            return myBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static int getImageOrientation(String path) {
        int degree = 0;
        try {
            ExifInterface exif = new ExifInterface(path);
            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (rotation == ExifInterface.ORIENTATION_ROTATE_90) {
                degree = 90;
            } else if (rotation == ExifInterface.ORIENTATION_ROTATE_180) {
                degree = 180;
            } else if (rotation == ExifInterface.ORIENTATION_ROTATE_270) {
                degree = 270;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    public static Bitmap getPinterestBitmap(String path) {
        final int minLength = 256;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        int sampleW = options.outWidth;
        int sampleH = options.outHeight;

        options.inJustDecodeBounds = false;
        Bitmap temp = BitmapFactory.decodeFile(path, options);

        Matrix matrix = new Matrix();
        matrix.postScale((float) minLength / (float) sampleW, (float) minLength / (float) sampleH);
        matrix.preRotate(getImageOrientation(path));
        Bitmap outBitmap = Bitmap.createBitmap(temp, 0, 0, sampleW, sampleH, matrix, true);
        if (outBitmap != null && !outBitmap.sameAs(temp)) {
            if (temp != null && !temp.isRecycled()) {
                temp.recycle();
                temp = null;
            }
        }
        return outBitmap;
    }

    public static Bitmap getNaverBitmap(String path) {
        final int minLength = 800;
        int newSampleW = 0;
        int newSampleH = 0;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        int sampleW = options.outWidth;
        int sampleH = options.outHeight;

        if (sampleW > sampleH) {
            newSampleW = Math.min(sampleW, minLength);
            float ratio = (float) newSampleW / (float) sampleW;
            newSampleH = (int) (sampleH * ratio);
        } else {
            newSampleH = Math.min(sampleH, minLength);
            float ratio = (float) newSampleH / (float) sampleH;
            newSampleW = (int) (sampleW * ratio);
        }

        options.inJustDecodeBounds = false;
        Bitmap temp = BitmapFactory.decodeFile(path, options);

        Matrix matrix = new Matrix();
        matrix.postScale((float) newSampleW / (float) sampleW, (float) newSampleH / (float) sampleH);
        matrix.preRotate(getImageOrientation(path));
        Bitmap outBitmap = Bitmap.createBitmap(temp, 0, 0, sampleW, sampleH, matrix, true);
        if (outBitmap != null && !outBitmap.sameAs(temp)) {
            if (temp != null && !temp.isRecycled()) {
                temp.recycle();
                temp = null;
            }
        }
        return outBitmap;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static Bitmap getBitmapFromVector(Context context, int id) {
        VectorDrawable vc = ((VectorDrawable) context.getResources().getDrawable(id, null));
        Bitmap bitmap = Bitmap.createBitmap(vc.getIntrinsicWidth(),
                vc.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        vc.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vc.draw(canvas);
        return bitmap;
    }
}
