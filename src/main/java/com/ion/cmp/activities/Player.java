package com.ion.cmp.activities;


import android.app.ActionBar;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TableRow;

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.ion.cmp.R;
import com.ion.cmp.models.MediaFile;
import com.ion.cmp.models.OnKeyboardVisibilityListener;
import com.ion.cmp.models.PlayList;
import com.ion.cmp.utils.Constants;
import com.ion.cmp.utils.DirectoryChooserDialog;
import com.ion.cmp.utils.FileUtils;
import com.ion.cmp.utils.Filewalker;
import com.ion.cmp.utils.Session;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;


public class Player extends BaseActivity implements OnKeyboardVisibilityListener {

    MediaFile previouslyPlayedFile;
    MediaFile currentPlayingFile;
    LibVLC libVLC = null;
    MediaPlayer player;
    Set<String> playedFiles = new HashSet<>();
    int entryHeight;
    String searchedFile;
    boolean isSoftKeyboarVisible = false;
    Handler hdlr = new Handler();
    SeekBar songPrgs;
    boolean songPrgsActive;
    EditText searchText;
    Button addFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        readPlayListFromFileSystem();
        songPrgs = (SeekBar)findViewById(R.id.seekBar);
        songPrgs.setClickable(false);
        songPrgs.setEnabled(false);
        searchText = findViewById(R.id.search);
        addFiles = findViewById(R.id.addFiles);
        addFiles.requestFocus();

        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvv");
        libVLC = new LibVLC(this, args);

        searchText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                searchedFile = s.toString();
                if (searchedFile == null || searchedFile.equals("")) {
                    searchedFile = null;
                    searchText.clearFocus();
                }
                refreshView();
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        setKeyboardVisibilityListener(this);

        songPrgs.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                songPrgsActive = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                player.setTime(seekBar.getProgress()*1000);
                songPrgsActive = false;
            }
        });
        addFiles.requestFocus();
        clearSearch();
    }

    private void setKeyboardVisibilityListener(final OnKeyboardVisibilityListener onKeyboardVisibilityListener) {
        final View parentView = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        parentView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            private boolean alreadyOpen;
            private final int defaultKeyboardHeightDP = 100;
            private final int EstimatedKeyboardDP = defaultKeyboardHeightDP + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? 48 : 0);
            private final Rect rect = new Rect();

            @Override
            public void onGlobalLayout() {
                int estimatedKeyboardHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, EstimatedKeyboardDP, parentView.getResources().getDisplayMetrics());
                parentView.getWindowVisibleDisplayFrame(rect);
                int heightDiff = parentView.getRootView().getHeight() - (rect.bottom - rect.top);
                boolean isShown = heightDiff >= estimatedKeyboardHeight;

                if (isShown == alreadyOpen) {
                    return;
                }
                alreadyOpen = isShown;
                onKeyboardVisibilityListener.onVisibilityChanged(isShown);
            }
        });
    }

    @Override
    public void onVisibilityChanged(boolean visible) {
        isSoftKeyboarVisible = visible;
        if (!visible) {
            clearSearch();
            searchText.setCursorVisible(false);
            scrollToFile(currentPlayingFile);
        } else {
            searchText.setCursorVisible(true);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        refreshView();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        refreshView();
    }

    private void clearSearch() {
        searchedFile = null;
        closeKeyboard(null);
        searchText.setText(searchedFile);
    }

    public void closeKeyboard(View v){
        try {
            InputMethodManager editTextInput = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            editTextInput.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void refreshView() {
        if (Session.playList.getMediaFiles().size() >= 2) {
            Collections.sort(Session.playList.getMediaFiles(), new Comparator<MediaFile>() {
                @Override
                public int compare(MediaFile m1, MediaFile m2) {
                    if (m1.getName().equals(m2.getName())) {
                        return 0;
                    }
                    return m1.getName().compareTo(m2.getName());
                }
            });
        }

        positionElements();
        clearList();
        createPlayList();
        highlightPlayingFile();
    }

    private void clearList() {
        LinearLayout mediaList = findViewById(R.id.scrollFilesList);
        mediaList.removeAllViews();
    }

    private void createPlayList() {
        LinearLayout mediaList = findViewById(R.id.scrollFilesList);
        entryHeight = -1;
        for (int i = 0; i < Session.playList.getMediaFiles().size(); i++) {
            if (searchedFile == null || Session.playList.getMediaFiles().get(i).getName().toLowerCase().indexOf(searchedFile) > -1) {
                Session.playList.getMediaFiles().get(i).setIndex(i);
                Button btn = createListEntry(Session.playList.getMediaFiles().get(i));
                if (entryHeight < 0) {
                    entryHeight = btn.getMeasuredHeight();
                }
                mediaList.addView(btn);
            }
        }
    }

    private Button createListEntry(final MediaFile mediaFile) {
        Button btn = new Button(this);
        btn.setText(mediaFile.getName());
        int tColor = Color.parseColor("#000000");
        btn.setTextColor(tColor);
        btn.setTypeface(Typeface.SANS_SERIF, 1);
        btn.setTextSize(18);
        TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);
        params.setMargins(0, 0, 0, border/2);
        btn.setLayoutParams(params);
        btn.setGravity(Gravity.CENTER);
        btn.setTransformationMethod(null);
        int color = Color.parseColor(Constants.WHITE_COLOR);
        btn.setBackgroundColor(color);
        btn.setId(mediaFile.getIndex());
        btn.setEllipsize(TextUtils.TruncateAt.END);
        btn.setSingleLine();
        btn.setMarqueeRepeatLimit(-1);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (currentPlayingFile !=null && currentPlayingFile.getHash().equals(mediaFile.getHash())) {
                    pausePlay(null);
                } else {
                    playFile(mediaFile);
                }
            }
        });
        btn.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                openDeleteFileDialog(mediaFile);
                return true;
            }
        });

        return btn;
    }

    private void positionElements() {
        // shuffle
        Button shuffleButton = findViewById(R.id.shuffle);
        shuffleButton.setX(border);
        shuffleButton.setY(border);
        int color = Color.parseColor(Constants.WHITE_COLOR);
        if (Session.playList.getShuffleOn()) {
            color = Color.parseColor(Constants.GREEN_COLOR);
        }
        shuffleButton.setBackgroundColor(color);

        // add file
        int addButtonWidth = addFiles.getWidth();
        int addButtonHeight = addFiles.getHeight();
        addFiles.setX(viewWidth - border - addButtonWidth);
        addFiles.setY(border);
        addFiles.setHeight(shuffleButton.getHeight());
        addFiles.setWidth(shuffleButton.getWidth());

        int visibility = View.GONE;
        if (Session.playList.getMediaFiles().size() > 0) {
            visibility = View.VISIBLE;
        }
        // search
        searchText.setY(border);
        searchText.setX(2 * border + shuffleButton.getWidth());
        searchText.setWidth(viewWidth - 4 * border - shuffleButton.getWidth() - addFiles.getWidth());
        searchText.setHeight(shuffleButton.getHeight());

        // play / pause button
        Button playPauseButton = findViewById(R.id.playPause);
        int playPauseButtonWidth = playPauseButton.getWidth();
        int playPauseButtonHeight = playPauseButton.getHeight();
        int playPauseButtonX = viewWidth / 2 - playPauseButtonWidth / 2;
        playPauseButton.setX(playPauseButtonX);
        playPauseButton.setY(viewHeight - playPauseButtonHeight - border);
        playPauseButton.setVisibility(visibility);

        // next
        Button nextButton = findViewById(R.id.next);
        nextButton.setX(viewWidth - border - playPauseButtonWidth);
        nextButton.setY(viewHeight - playPauseButtonHeight - border);
        nextButton.setVisibility(visibility);
        // previous
        Button prevButton = findViewById(R.id.prev);
        prevButton.setX(border);
        prevButton.setY(viewHeight - playPauseButtonHeight - border);
        prevButton.setVisibility(visibility);

        // seek bar
        SeekBar seekBar = findViewById(R.id.seekBar);
        int seekBarHeight = seekBar.getHeight();
        seekBar.setY(viewHeight - playPauseButtonHeight - seekBarHeight - 2 * border);
        //seekBar.setX(border);

        ScrollView scrollView = findViewById(R.id.scrollFilesView);
        scrollView.setY(addFiles.getHeight() + 2 * border);
        scrollView.setPadding(border,0, border, 0);
        scrollView.setLayoutParams(new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT, viewHeight - addButtonHeight - 5 * border - playPauseButtonHeight - seekBarHeight));
    }

    private void readPlayListFromFileSystem() {
        try {
            Session.playList = (PlayList)FileUtils.readObjectFromFile(Session.mediaList.getId() + Constants.playListFileExtension, getApplicationContext(), PlayList.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clickAddFiles(View v) {
        DirectoryChooserDialog directoryChooserDialog =
            new DirectoryChooserDialog(this,
                new DirectoryChooserDialog.ChosenDirectoryListener() {
                    @Override
                    public void onChosenDir(final String chosenDir) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Filewalker fw = new Filewalker();
                                fw.walk(new File(chosenDir));
                                savePlayListToFileSystem(Session.playList);
                                refreshView();
                            }
                        });
                    }
                });
        directoryChooserDialog.setNewFolderEnabled(false);
        directoryChooserDialog.chooseDirectory("");
    }

    private void playFile(MediaFile file) {
        if (file != null) {
            previouslyPlayedFile = currentPlayingFile;
            playedFiles.add(file.getHash());
            try {
                if (player != null && player.isPlaying()) {
                    player.stop();
                    player.release();
                    player = null;
                }
                Uri uri = Uri.fromFile(new File(file.getPath()));
                player = new MediaPlayer(libVLC);
                final Media media = new Media(libVLC, uri);
                player.setMedia(media);
                player.setVolume(100);

                player.setEventListener(new MediaPlayer.EventListener() {
                    @Override
                    public void onEvent(MediaPlayer.Event event) {
                        if (event.type == MediaPlayer.Event.Stopped) {
                            playNext(null);
                        }
                    }
                });

                player.play();
                while(!player.isPlaying()) {
                    Thread.sleep(200);
                }
                int pTime = (int)player.getLength() / 1000;
                int cTime = (int)player.getTime() / 1000;
                songPrgs.setMax(pTime);
                songPrgs.setProgress(cTime);
                songPrgs.setClickable(true);
                songPrgs.setEnabled(true);
                hdlr.postDelayed(UpdateSongTime, 500);

                currentPlayingFile = file;
                scrollToFile(file);
                setPlayPauseIcon("Play");
                highlightPlayingFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void pausePlay(View v) {
        try {
            if (player != null){
                if (player.isPlaying()) {
                    player.pause();
                } else {
                    player.play();
                }
                setPlayPauseIcon(null);
            } else {
                playNext(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setPlayPauseIcon(String text) {
        Button playPauseButton = findViewById(R.id.playPause);
        Drawable playImg = getResources().getDrawable(R.drawable.play);
        Drawable pauseImg = getResources().getDrawable(R.drawable.pause);

        if (text == null) {
            text = playPauseButton.getText().toString();
        }
        if (text.equals("Play")) {
            playPauseButton.setText("Pause");
            playPauseButton.setCompoundDrawablesWithIntrinsicBounds(pauseImg, null, null, null);
            if (currentPlayingFile != null) {
                Button playingFileEntry = findViewById(currentPlayingFile.getIndex());
                int greenColor = Color.parseColor(Constants.GREEN_COLOR);
                playingFileEntry.setBackgroundColor(greenColor);
            }
        } else {
            playPauseButton.setText("Play");
            playPauseButton.setCompoundDrawablesWithIntrinsicBounds(playImg, null, null, null);
            if (currentPlayingFile != null) {
                Button playingFileEntry = findViewById(currentPlayingFile.getIndex());
                int orangeColor = Color.parseColor(Constants.ORANGE_COLOR);
                playingFileEntry.setBackgroundColor(orangeColor);
            }
        }
    }

    public void playPrevious(View v) {
        if (previouslyPlayedFile != null) {
            playFile(previouslyPlayedFile);
        }
    }

    public void playNext(View v) {
        MediaFile nextFile = getNextFile();
        playFile(nextFile);
    }

    public void setShuffle(View v) {
        Session.playList.setShuffleOn(!Session.playList.getShuffleOn());
        savePlayListToFileSystem(Session.playList);

        Button shuffleButton = findViewById(R.id.shuffle);
        int color = Color.parseColor(Constants.WHITE_COLOR);
        if (Session.playList.getShuffleOn()) {
            color = Color.parseColor(Constants.GREEN_COLOR);
        }
        shuffleButton.setBackgroundColor(color);
    }

    private MediaFile getNextFile() {
        if (Session.playList.getShuffleOn()) {
            int range = Session.playList.getMediaFiles().size() - playedFiles.size();
            if (range == 0) {
                playedFiles = new HashSet<>();
                range = Session.playList.getMediaFiles().size() - 1;
            }
            int random = (int)(Math.random() * range);
            int cnt = 0;
            for (int i = 0; i < Session.playList.getMediaFiles().size(); i++) {
                if (!playedFiles.contains(Session.playList.getMediaFiles().get(i).getHash())) {
                    if (cnt == random) {
                        return Session.playList.getMediaFiles().get(i);
                    } else {
                        cnt++;
                    }
                }
            }
        } else {
            if (currentPlayingFile == null) {
                return Session.playList.getMediaFiles().get(0);
            } else {
                for (int i = 0; i < Session.playList.getMediaFiles().size(); i++) {
                    if (Session.playList.getMediaFiles().get(i).getHash().equals(currentPlayingFile.getHash())) {
                        if (i + 1 < Session.playList.getMediaFiles().size()) {
                            return Session.playList.getMediaFiles().get(i + 1);
                        } else {
                            playedFiles = new HashSet<>();
                            return Session.playList.getMediaFiles().get(0);
                        }
                    }
                }
            }
        }
        // failover
        return Session.playList.getMediaFiles().get(0);
    }

    public void removeFileFromList(MediaFile mediaFile) {
        int deleteIndex = -1;
        for (int i = 0; i < Session.playList.getMediaFiles().size(); i++) {
            if (Session.playList.getMediaFiles().get(i).getHash().equals(mediaFile.getHash())) {
                deleteIndex = i;
                break;
            }
        }
        if (deleteIndex >= 0) {
            Session.playList.getMediaFiles().remove(deleteIndex);
            savePlayListToFileSystem(Session.playList);
        }
    }

    private void openDeleteFileDialog(final MediaFile mediaFile) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Are you sure you want to remove the file from the playlist: " + mediaFile.getName() + " ?");
        alertDialogBuilder.setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        removeFileFromList(mediaFile);
                    }
                });

        alertDialogBuilder.setNegativeButton("No",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void scrollToFile(MediaFile mediaFile) {
        if (mediaFile != null) {
            final ScrollView mediaList = findViewById(R.id.scrollFilesView);
            final Button playingFileEntry = findViewById(mediaFile.getIndex());
            mediaList.post(new Runnable() {
                @Override
                public void run() {
                    mediaList.scrollTo(0, playingFileEntry.getTop());
                }
            });
        }
    }

    private void highlightPlayingFile() {
        try {
            if (previouslyPlayedFile != null) {
                Button playingFileEntry = findViewById(previouslyPlayedFile.getIndex());
                int greenColor = Color.parseColor(Constants.WHITE_COLOR);
                playingFileEntry.setBackgroundColor(greenColor);
            }
        } catch(Exception e) {
            // do nothing in case of error
        }
        try {
            if (currentPlayingFile != null) {
                Button playingFileEntry = findViewById(currentPlayingFile.getIndex());
                int color = Color.parseColor(Constants.GREEN_COLOR);
                if (!player.isPlaying()) {
                    color = Color.parseColor(Constants.ORANGE_COLOR);
                }
                playingFileEntry.setBackgroundColor(color);
            }
        } catch(Exception e) {
            // do nothing in case of error
        }
    }

    private Runnable UpdateSongTime = new Runnable() {
        @Override
        public void run() {
            if (player != null && !songPrgsActive) {
                try {
                    int cTime = (int)player.getTime() / 1000;
                    songPrgs.setProgress(cTime);
                } catch (Exception e) {}
            }
            hdlr.postDelayed(this, 500);
        }
    };

    public void onDestroy() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }

        super.onDestroy();
    }
}
