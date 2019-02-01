package com.example.alex.comicscamtest;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResultObject {
    @SerializedName("status")
    @Expose
    private String status;

    @SerializedName("comic")
    @Expose
    private String comic;


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }



    public String getComic() {
        return comic;
    }

    public void setComic(String comic) {
        this.comic = comic;
    }
}
