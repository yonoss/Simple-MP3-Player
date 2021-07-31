package com.ion.cmp.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableRow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ion.cmp.R;
import com.ion.cmp.models.PlayList;
import com.ion.cmp.models.MediaList;
import com.ion.cmp.utils.Constants;
import com.ion.cmp.utils.FileUtils;
import com.ion.cmp.utils.Session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends BaseActivity  {

    List<MediaList> mediaLists = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getPermissions();
        readListFromFileSystem();
    }

    private void getPermissions() {

        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_PERMISSION_STORAGE = 100;
            String[] permissions = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };

            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    this.requestPermissions(permissions, REQUEST_CODE_PERMISSION_STORAGE);
                    return;
                }
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        refreshView();
    }

    private void refreshView() {
        if (mediaLists.size() >= 2) {
            Collections.sort(mediaLists, new Comparator<MediaList>() {
                @Override
                public int compare(MediaList m1, MediaList m2) {
                    if (m1.getName().equals(m2.getName())) {
                        return 0;
                    }
                    return m1.getName().compareTo(m2.getName());
                }
            });
        }

        clearList();
        positionElements();
        createListsView();
    }

    private void clearList() {
        LinearLayout mediaList = findViewById(R.id.scrollList);
        mediaList.removeAllViews();
    }

    private void createListsView() {
        for(MediaList list : mediaLists) {
            createListEntry(list);
        }
    }

    private void createListEntry(final MediaList listInstance) {
        Button btn = new Button(this);
        btn.setText(listInstance.getName()+"\n" + getTracksNumber(listInstance.getId()) + " tracks");
        int tColor = Color.parseColor(Constants.TEXT_COLOR);
        btn.setTextColor(tColor);
        btn.setTypeface(Typeface.SANS_SERIF, 1);
        btn.setTextSize(24);
        TableRow.LayoutParams params = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);
        params.setMargins(0, 0, 0, border/2);
        btn.setLayoutParams(params);
        btn.setGravity(Gravity.CENTER);
        btn.setTransformationMethod(null);
        int color = Color.parseColor(Constants.WHITE_COLOR);
        btn.setBackgroundColor(color);
        final AppCompatActivity activity = this;
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Session.mediaList = listInstance;
                Intent i = new Intent(activity, Player.class);
                startActivity(i);
            }
        });

        btn.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                openDeleteListDialog(listInstance);
                return true;
            }
        });
        LinearLayout mediaList = findViewById(R.id.scrollList);
        mediaList.addView(btn);
    }

    private void positionElements() {
        Button addList = findViewById(R.id.addList);
        int addButtonHeight = addList.getHeight();

        ScrollView scrollView = findViewById(R.id.scrollView);
        scrollView.setPadding(border,border,border,2 * border + addButtonHeight);

        LinearLayout scrollList = findViewById(R.id.scrollList);
        scrollList.setPadding(0,0,0,0);

        addList.setX(border);
        addList.setY(viewHeight - border - addButtonHeight);
    }

    public void clickAddNewList(View v) {
        openCreateListDialog();
    }

    private void openCreateListDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        alertDialogBuilder.setView(input);
        alertDialogBuilder.setMessage("Type below the new playlist name:");
        alertDialogBuilder.setPositiveButton("Add",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        try {
                            addNewList(input.getText().toString());
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }
                });

        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void removeList(MediaList listToDelete) {
        for(int i=0; i < mediaLists.size(); i++) {
            if(mediaLists.get(i).getId().equals(listToDelete.getId())) {
                FileUtils.deleteFile(mediaLists.get(i).getName() + Constants.playListFileExtension, getApplicationContext());
                mediaLists.remove(i);
                refreshView();
                saveListToFileSystem();
                return;
            }
        }
    }

    public void addNewList(String listName) throws JsonProcessingException  {
        MediaList mediaList = new MediaList();
        mediaList.setName(listName);
        mediaList.setId("list_" + System.currentTimeMillis());
        mediaLists.add(mediaList);
        refreshView();
        saveListToFileSystem();
        PlayList playList = new PlayList();
        playList.setId(mediaList.getId());
        savePlayListToFileSystem(playList);
    }

    private void openDeleteListDialog(final MediaList listToDelete) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("Are you sure you want to remove the playlist: " + listToDelete.getName() + " ?");
        alertDialogBuilder.setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        removeList(listToDelete);
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

    private void saveListToFileSystem() {
        try {
            FileUtils.writeObjectToFile(Constants.listsFile, mediaLists, getApplicationContext());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private void readListFromFileSystem() {
        try {
            mediaLists = FileUtils.readListFromFile(Constants.listsFile, getApplicationContext(), MediaList.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getTracksNumber(String id) {
        try {
            PlayList playList = (PlayList)FileUtils.readObjectFromFile(id + Constants.playListFileExtension, getApplicationContext(), PlayList.class);
            if (playList != null) {
                return playList.getMediaFiles().size();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
