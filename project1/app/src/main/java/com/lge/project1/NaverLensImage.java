/*
 * Mobile Communication Company, LG ELECTRONICS INC., SEOUL, KOREA
 * Copyright(c) 2017 by LG Electronics Inc.
 *
 * All rights reserved. No part of this work may be reproduced, stored in a
 * retrieval system, or transmitted by any means without prior written
 * Permission of LG Electronics Inc.
 */
package com.lge.project1;

public class NaverLensImage {
    public String imageUrl;
    public String link;
    public String title;
    public String section;
    public String subSection;
    public int width;
    public int height;

    public NaverLensImage() {
        this.imageUrl = null;
        this.link = null;
        this.title = null;
        this.section = null;
        this.subSection = null;
        this.width = 0;
        this.height = 0;
    }

    public NaverLensImage(String imageUrl, String link, String title, String section, String subSection, int width, int height) {
        this.imageUrl = imageUrl;
        this.link = link;
        this.title = title;
        this.section = section;
        this.subSection = subSection;
        this.width = width;
        this.height = height;
    }

    public String getImageUrl() {
        return imageUrl;
    }
    public String getLink() {
        return link;
    }
    public String getTitle() {
        return title;
    }
    public String getCaption() {
        return subSection;
    }
    public String getSection() {
        return section;
    }
}
