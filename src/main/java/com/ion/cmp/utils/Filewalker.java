package com.ion.cmp.utils;

import com.ion.cmp.models.MediaFile;

import java.io.File;

public class Filewalker {

    public void walk(File root) {
        File[] list = {root};
        if (root.isDirectory()) {
            list = root.listFiles();
        }

        for (File f : list) {
            if (f.isDirectory()) {
                walk(f);
            } else {
                String fileExtension = getFileExtension(f);
                if (fileExtension.equalsIgnoreCase(Constants.MP3_EXTENSION)) {
                    importFile(stripExtension(f.getName()), f.getPath());
                }
            }
        }
    }

    public static String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".") + 1;
        if (lastIndexOf == -1) {
            return null;
        }
        return name.substring(lastIndexOf);
    }

    public void importFile(String fileName, String filePath) {
        if (!isFileAvailable(fileName, filePath)) {
            addMP3FileToPlayList(fileName, filePath);
        }
    }

    public void addMP3FileToPlayList(String name, String path) {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setName(name);
        mediaFile.setPath(path);
        String hash = DigestUtils.hashText(name + path);
        mediaFile.setHash(hash);
        Session.playList.getMediaFiles().add(mediaFile);
    }

    private boolean isFileAvailable(String fileName, String filePath) {
        for (MediaFile file : Session.playList.getMediaFiles()) {
            if (file.getName().equals(fileName) && file.getPath().equals(filePath)) {
                return true;
            }
        }
        return false;
    }

    private String stripExtension(String str) {
        if (str == null) {
            return null;
        }
        int pos = str.lastIndexOf(".");
        if (pos == -1) {
            return str;
        }
        return str.substring(0, pos);
    }
}
