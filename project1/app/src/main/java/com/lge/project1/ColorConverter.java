/*
 * Mobile Communication Company, LG ELECTRONICS INC., SEOUL, KOREA
 * Copyright(c) 2017 by LG Electronics Inc.
 *
 * All rights reserved. No part of this work may be reproduced, stored in a
 * retrieval system, or transmitted by any means without prior written
 * Permission of LG Electronics Inc.
 */
package com.lge.project1;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;

public class ColorConverter {
    // version : ColorConverter_Native v1.1.0
    static {
        System.loadLibrary("ColorConverter");
    }

    public static final native void yuvToRgbArrayWithResize(byte[] outRgb, byte[] inYuv420sp,
                                                            int orgInputW, int orgInputH,
                                                            int resizedInputW, int resizedInputH);

    public static final native void nv21ToRgbArrayWithResize(byte[] outRgb, ByteBuffer y, ByteBuffer vu,
                                                             int orgInputW, int orgInputH,
                                                             int resizedInputW, int resizedInputH, int rowStride);

    public static native void byteArrayToBitmap(Bitmap outBitmap, byte[] inByteArray);
}
