package com.ion.cmp.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Environment;
import android.text.Editable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class DirectoryChooserDialog {
    private boolean m_isNewFolderEnabled = true;
    private String m_sdcardDirectory = "/";
    private Context m_context;
    private TextView m_titleView;

    private String m_dir = "";
    private List<String> m_subdirs = null;
    private ChosenDirectoryListener m_chosenDirectoryListener = null;
    private ArrayAdapter<String> m_listAdapter = null;

    public interface ChosenDirectoryListener {
        public void onChosenDir(String chosenDir);
    }

    public DirectoryChooserDialog(Context context, ChosenDirectoryListener chosenDirectoryListener) {
        m_context = context;
        m_sdcardDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
        m_chosenDirectoryListener = chosenDirectoryListener;

        try {
            m_sdcardDirectory = new File(m_sdcardDirectory).getCanonicalPath();
        } catch (IOException ioe) {
        }
    }

    public void setNewFolderEnabled(boolean isNewFolderEnabled) {
        m_isNewFolderEnabled = isNewFolderEnabled;
    }

    public boolean getNewFolderEnabled()
    {
        return m_isNewFolderEnabled;
    }

    public void chooseDirectory() {
        // Initial directory is sdcard directory
        chooseDirectory(m_sdcardDirectory);
    }

    public void chooseDirectory(String dir) {
        File dirFile = new File(dir);
        if (! dirFile.exists() || ! dirFile.isDirectory()) {
            dir = m_sdcardDirectory;
        }

        try {
            dir = new File(dir).getCanonicalPath();
        } catch (IOException ioe) {
            return;
        }

        m_dir = dir;
        m_subdirs = getDirectories(dir);

        class DirectoryOnClickListener implements DialogInterface.OnClickListener {
            public void onClick(DialogInterface dialog, int item) {
                // Navigate into the sub-directory or select file
                m_dir += "/" + ((AlertDialog) dialog).getListView().getAdapter().getItem(item);
                File dir = new File(m_dir);
                if (dir.exists() && dir.isDirectory()) {
                    updateDirectory();
                } else if (dir.exists() && dir.isFile()) {
                    if (m_chosenDirectoryListener != null) {
                        // Call registered listener supplied with the chosen directory
                        m_chosenDirectoryListener.onChosenDir(m_dir);
                        dialog.dismiss();
                    }
                }
            }
        }

        AlertDialog.Builder dialogBuilder =
                createDirectoryChooserDialog(dir, m_subdirs, new DirectoryOnClickListener());

        dialogBuilder.setPositiveButton("Select current directory", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Current directory chosen
                if (m_chosenDirectoryListener != null) {
                    // Call registered listener supplied with the chosen directory
                    m_chosenDirectoryListener.onChosenDir(m_dir);
                }
            }
        }).setNegativeButton("Cancel", null);

        final AlertDialog dirsDialog = dialogBuilder.create();

        dirsDialog.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (m_dir.equals(m_sdcardDirectory)) {
                        return false;
                    } else {
                        m_dir = new File(m_dir).getParent();
                        updateDirectory();
                    }

                    return true;
                } else {
                    return false;
                }
            }
        });

        dirsDialog.show();
    }

    private boolean createSubDir(String newDir) {
        File newDirFile = new File(newDir);
        if (!newDirFile.exists()) {
            return newDirFile.mkdir();
        }

        return false;
    }

    private List<String> getDirectories(String dir) {
        List<File> files = new ArrayList<>();
        List<String> dirs = new ArrayList<String>();

        try {
            File dirFile = new File(dir);
            if (!dirFile.exists() || !dirFile.isDirectory()) {
                return dirs;
            }

            for (File file : dirFile.listFiles()) {
                if (file.isDirectory() || Filewalker.getFileExtension(file).equalsIgnoreCase(Constants.MP3_EXTENSION)) {
                    boolean currentIsDir = file.isDirectory();
                    boolean added = false;
                    for (int i = 0; i < files.size(); i++ ) {
                        File f = files.get(i);
                        if (f.isDirectory() && currentIsDir && f.getName().compareToIgnoreCase(file.getName()) > 0) {
                            files.add(i, file);
                            added = true;
                            break;
                        } else if (f.isDirectory() && !currentIsDir) {
                            continue;
                        } else if (!f.isDirectory() && currentIsDir) {
                            files.add(i, file);
                            added = true;
                            break;
                        } else if (!f.isDirectory() && !currentIsDir && f.getName().compareToIgnoreCase(file.getName()) > 0) {
                            files.add(i, file);
                            added = true;
                            break;
                        }
                    }
                    if (!added) {
                        files.add(file);
                    }
                }
            }

            for (File f : files) {
                dirs.add(f.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Collections.sort(dirs, new Comparator<String>() {
        //    public int compare(String o1, String o2) {
        //        return o1.compareTo(o2);
        //    }
        //});

        return dirs;
    }

    private AlertDialog.Builder createDirectoryChooserDialog(String title, List<String> listItems,
                                                             DialogInterface.OnClickListener onClickListener) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(m_context);

        LinearLayout titleLayout = new LinearLayout(m_context);
        titleLayout.setOrientation(LinearLayout.VERTICAL);

        m_titleView = new TextView(m_context);
        m_titleView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        m_titleView.setTextAppearance(m_context, android.R.style.TextAppearance_Large);
        m_titleView.setTextColor( m_context.getResources().getColor(android.R.color.white) );
        m_titleView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        m_titleView.setText(title);

        Button backButton = new Button(m_context);
        int tColor = Color.parseColor("#ffffff");
        backButton.setBackgroundColor(tColor);
        backButton.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        backButton.setText(" ../");
        backButton.setGravity(Gravity.LEFT);
        backButton.setTypeface(Typeface.SANS_SERIF, 1);
        backButton.setTextSize(28);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to an upper directory
                File dir = new File(m_dir);
                if (dir.exists() && dir.isDirectory()) {
                    String parent = null;
                    try {
                        parent = dir.getParent();
                    } catch (Exception e) {

                    }
                    if (parent != null) {
                        m_dir = parent;
                        updateDirectory();
                    }
                }
            }
        });

        Button newDirButton = new Button(m_context);
        newDirButton.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
        newDirButton.setText("New folder");
        newDirButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText input = new EditText(m_context);

                // Show new folder name input dialog
                new AlertDialog.Builder(m_context).
                        setTitle("New folder name").
                        setView(input).setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        Editable newDir = input.getText();
                        String newDirName = newDir.toString();
                        // Create new directory
                        if ( createSubDir(m_dir + "/" + newDirName) ) {
                            // Navigate into the new directory
                            m_dir += "/" + newDirName;
                            updateDirectory();
                        } else {
                            Toast.makeText(
                                    m_context, "Failed to create '" + newDirName +
                                            "' folder", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).setNegativeButton("Cancel", null).show();
            }
        });

        if (! m_isNewFolderEnabled) {
            newDirButton.setVisibility(View.GONE);
        }

        titleLayout.addView(m_titleView);
        titleLayout.addView(newDirButton);
        titleLayout.addView(backButton);

        dialogBuilder.setCustomTitle(titleLayout);

        m_listAdapter = createListAdapter(listItems);

        dialogBuilder.setSingleChoiceItems(m_listAdapter, -1, onClickListener);
        dialogBuilder.setCancelable(false);

        return dialogBuilder;
    }

    private void updateDirectory() {
        m_subdirs.clear();
        m_subdirs.addAll(getDirectories(m_dir));
        m_titleView.setText(m_dir);

        m_listAdapter.notifyDataSetChanged();
    }

    private ArrayAdapter<String> createListAdapter(List<String> items) {
        return new ArrayAdapter<String>(m_context,
                android.R.layout.select_dialog_item, android.R.id.text1, items)
        {
            @Override
            public View getView(int position, View convertView,
                                ViewGroup parent)
            {
                View v = super.getView(position, convertView, parent);

                if (v instanceof TextView) {
                    // Enable list item (directory) text wrapping
                    TextView tv = (TextView) v;
                    tv.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
                    tv.setEllipsize(null);
                }
                return v;
            }
        };
    }
}