package com.lge.project1;

/**
 * Created by dg.jung on 2018-03-16.
 */

public class NaverLensKeyword {
    public String text;
    public String link;

    public NaverLensKeyword() {
        this.text = null;
        this.link = null;
    }

    public NaverLensKeyword(String text, String link) {
        this.text = text;
        this.link = link;
    }
}
