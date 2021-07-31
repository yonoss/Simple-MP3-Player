package com.ion.cmp.models;

import java.util.ArrayList;
import java.util.List;

public class PlayList {
    private String id;
    private boolean isShuffleOn;
    private int volumeLevel = 50;
    private List<MediaFile> mediaFiles = new ArrayList<>();

    public PlayList() {}

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setMediaFiles(List<MediaFile> mediaFiles) {
            this.mediaFiles = mediaFiles;
    }

    public List<MediaFile> getMediaFiles() {
        return mediaFiles;
    }

    public void setShuffleOn(boolean isShuffleOn) {
        this.isShuffleOn = isShuffleOn;
    }

    public boolean getShuffleOn() {
        return isShuffleOn;
    }

    public void setVolumeLevel(int volumeLevel) {
        this.volumeLevel = volumeLevel;
    }

    public int getVolumeLevel() {
        return volumeLevel;
    }
}
