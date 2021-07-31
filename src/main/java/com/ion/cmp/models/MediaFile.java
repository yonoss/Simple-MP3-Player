package com.ion.cmp.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MediaFile {
    private String name;
    private String path;
    private String hash;
    @JsonIgnore
    private int index;

    public MediaFile() {}

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getHash() {
        return hash;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
