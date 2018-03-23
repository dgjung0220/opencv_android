/*
 * Mobile Communication Company, LG ELECTRONICS INC., SEOUL, KOREA
 * Copyright(c) 2017 by LG Electronics Inc.
 *
 * All rights reserved. No part of this work may be reproduced, stored in a
 * retrieval system, or transmitted by any means without prior written
 * Permission of LG Electronics Inc.
 */
package com.lge.project1;

import android.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NaverLensXmlParser extends DocParserBase {
    private final static String TAG = "NaverSIXmlParser";

    private static final int STEP_SIZE = 100;
    private static final int STEP_W = 1000;
    private static final int STEP_H = 10000;
    private static final int STEP_LINK = 100000;

    private static final int STEP_NOTHING = 0;
    private static final int STEP_RESULT = 1;
    private static final int STEP_STATUS = 2;
    private static final int STEP_BASE_SIZE = STEP_SIZE + STEP_RESULT;
    private static final int STEP_BASE_SIZE_W = STEP_W + STEP_BASE_SIZE;
    private static final int STEP_BASE_SIZE_H = STEP_H + STEP_BASE_SIZE;
    private static final int STEP_OBJECTS = 3;
    private static final int STEP_OBJECT = 4;
    private static final int STEP_ROI = 5;
    private static final int STEP_ROI_X = 6;
    private static final int STEP_ROI_Y = 7;
    private static final int STEP_ROI_W = STEP_W + STEP_ROI;
    private static final int STEP_ROI_H = STEP_H + STEP_ROI;
    private static final int STEP_IMAGES = 8;
    private static final int STEP_IMAGE = 9;
    private static final int STEP_IMAGE_SRC = 10;
    private static final int STEP_IMAGE_LINK = STEP_LINK + STEP_IMAGE;
    private static final int STEP_IMAGE_SECTION = 11;
    private static final int STEP_IMAGE_SUB_SECTION = 12;
    private static final int STEP_IMAGE_TITLE = 13;
    private static final int STEP_IMAGE_SIZE = STEP_SIZE + STEP_IMAGE;
    private static final int STEP_IMAGE_SIZE_W = STEP_W + STEP_IMAGE_SIZE;
    private static final int STEP_IMAGE_SIZE_H = STEP_H + STEP_IMAGE_SIZE;
    private static final int STEP_KEYWORDS = 14;
    private static final int STEP_KEYWORD = 15;
    private static final int STEP_KEYWORD_TEXT = 16;
    private static final int STEP_KEYWORD_LINK = STEP_LINK + STEP_KEYWORD;
    private static final int STEP_REP_COLORS = 17;
    private static final int STEP_COLOR = 18;
    private static final int STEP_REP_OBJECT_INDEX = 19;

    private int mRepObjectIndex = 0;
    private List<List<NaverLensImage>> mNaverSIImageList = new ArrayList<List<NaverLensImage>>();
    private List<List<NaverLensKeyword>> mNaverSIKeywordList = new ArrayList<List<NaverLensKeyword>>();

    private HashMap<String, Integer> mParentStep = new HashMap<String, Integer>();
    private HashMap<String, Integer> mStep = new HashMap<String, Integer>();

    public NaverLensXmlParser() {
        mStep.put("result", STEP_RESULT);
        mStep.put("status", STEP_STATUS);
        mStep.put("size", STEP_SIZE);
        mStep.put("w", STEP_W);
        mStep.put("h", STEP_H);
        mStep.put("objects", STEP_OBJECTS);
        mStep.put("object", STEP_OBJECT);
        mStep.put("roi", STEP_ROI);
        mStep.put("x", STEP_ROI_X);
        mStep.put("y", STEP_ROI_Y);
        mStep.put("images", STEP_IMAGES);
        mStep.put("image", STEP_IMAGE);
        mStep.put("src", STEP_IMAGE_SRC);
        mStep.put("link", STEP_LINK);
        mStep.put("section", STEP_IMAGE_SECTION);
        mStep.put("subsection", STEP_IMAGE_SUB_SECTION);
        mStep.put("title", STEP_IMAGE_TITLE);
        mStep.put("keywords", STEP_KEYWORDS);
        mStep.put("keyword", STEP_KEYWORD);
        mStep.put("text", STEP_KEYWORD_TEXT);
        mStep.put("repColors", STEP_REP_COLORS);
        mStep.put("color", STEP_COLOR);
        mStep.put("repObjectIndex", STEP_REP_OBJECT_INDEX);
    }

    @Override
    public boolean parseResult(String response) {
        String status = "";

        int currentStep = STEP_NOTHING;
        int parentStep = STEP_NOTHING;
        int objIdx = 0;

        NaverLensImage naverLensImage = new NaverLensImage();
        NaverLensKeyword naverLensKeyword = new NaverLensKeyword();

        InputStream input = null;

        try {
            input = new ByteArrayInputStream(response.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {
            XmlPullParserFactory factory = null;
            factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new InputStreamReader(input, "UTF-8"));

            int eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        String startTag = parser.getName();

                        mParentStep.put(startTag, currentStep);
                        parentStep = mParentStep.get(startTag);
                        currentStep = mStep.get(startTag);

                        if (currentStep == STEP_OBJECT) {
                            Log.d(TAG, "STEP_OBJECT");
                            mNaverSIImageList.add(new ArrayList<NaverLensImage>());
                            mNaverSIKeywordList.add(new ArrayList<NaverLensKeyword>());
                            objIdx = mNaverSIImageList.size() - 1;
                        }

                        if (currentStep == STEP_SIZE || currentStep == STEP_W || currentStep == STEP_H || currentStep == STEP_LINK) {
                            currentStep += parentStep;
                        }

                        break;
                    case XmlPullParser.TEXT:
                        String value = parser.getText();

                        switch (currentStep) {
                            case STEP_STATUS:
                                status = value;
                                break;
                            case STEP_BASE_SIZE:
                            case STEP_BASE_SIZE_W:
                            case STEP_BASE_SIZE_H:
                                // Do nothing
                                break;
                            case STEP_ROI_X:
                                break;
                            case STEP_ROI_Y:
                                break;
                            case STEP_ROI_W:
                                break;
                            case STEP_ROI_H:
                                break;
                            case STEP_IMAGE_SRC:
                                naverLensImage.imageUrl = value;
                                break;
                            case STEP_IMAGE_LINK:
                                naverLensImage.link = value;
                                break;
                            case STEP_IMAGE_SECTION:
                                naverLensImage.section = value;
                                break;
                            case STEP_IMAGE_SUB_SECTION:
                                naverLensImage.subSection = value;
                                break;
                            case STEP_IMAGE_TITLE:
                                naverLensImage.title = value;
                                break;
                            case STEP_IMAGE_SIZE_W:
                                naverLensImage.width = Integer.parseInt(value);
                                break;
                            case STEP_IMAGE_SIZE_H:
                                naverLensImage.height = Integer.parseInt(value);
                                break;
                            case STEP_KEYWORD_TEXT:
                                naverLensKeyword.text = value;
                                break;
                            case STEP_KEYWORD_LINK:
                                naverLensKeyword.link = value;
                                break;
                            case STEP_COLOR:
                                break;
                            case STEP_REP_OBJECT_INDEX:
                                mRepObjectIndex = Integer.parseInt(value);
                                break;
                            default:
                                Log.d(TAG, "Invalid step = " + currentStep);
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        String endTag = parser.getName();

                        if (currentStep == STEP_IMAGE) {
                            mNaverSIImageList.get(objIdx).add(naverLensImage);
                            naverLensImage = new NaverLensImage();
                        } else if (currentStep == STEP_KEYWORD) {
                            mNaverSIKeywordList.get(objIdx).add(naverLensKeyword);
                            naverLensKeyword = new NaverLensKeyword();
                        }

                        currentStep = mParentStep.get(endTag);
                        mParentStep.remove(endTag);

                        break;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "ok".equals(status) ? true : false;
    }

    public List<List<NaverLensImage>> getImageList() {
        return mNaverSIImageList;
    }

    public List<List<NaverLensKeyword>> getSIKeywordList() {
        return mNaverSIKeywordList;
    }

    public List<NaverLensImage> getRepImageDataList() {
        if (mNaverSIImageList.size() != 0 && mRepObjectIndex >= 0) {
            return mNaverSIImageList.get(mRepObjectIndex);
        }
        return null;
    }

    public List<NaverLensKeyword> getRepKeywordDataList() {
        if (mNaverSIKeywordList.size() != 0 && mRepObjectIndex >= 0) {
            return mNaverSIKeywordList.get(mRepObjectIndex);
        }
        return null;
    }
}
