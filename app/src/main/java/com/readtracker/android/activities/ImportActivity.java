package com.readtracker.android.activities;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.readtracker.R;
import com.readtracker.android.ReadTrackerApp;
import com.readtracker.android.db.export.ImportError;
import com.readtracker.android.db.export.JSONImporter;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class ImportActivity extends BaseActivity {

  public static final String KEY_PICKED_FILE = "picked-file";
  private static final String TAG = ImportActivity.class.getSimpleName();

  @InjectView(R.id.current_folder_text) TextView currentFolderText;
  @InjectView(R.id.file_list) ListView fileList;

  private FileBrowserAdapter fileBrowseAdapter;
  private static Comparator<? super File> fileListComparator = new Comparator<File>() {
    @Override public int compare(File first, File second) {
      if(first.isDirectory() && !second.isDirectory()) return -1;
      if(second.isDirectory() && !first.isDirectory()) return 1;
      return first.getName().compareTo(second.getName());
    }
  };

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_import_data);
    ButterKnife.inject(this);

    fileBrowseAdapter = new FileBrowserAdapter(this);
    fileList.setAdapter(fileBrowseAdapter);
    fileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        File clickedFile = fileBrowseAdapter.getItem(position);
        if(clickedFile != null && clickedFile.canRead()) {
          if(clickedFile.isDirectory()) {
            setCurrentDirectory(clickedFile);
          } else if(clickedFile.isFile()) {
            importDataFromFile(clickedFile);
          }
        }
      }
    });

    File defaultDir = Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_DOWNLOADS
    );
    setCurrentDirectory(defaultDir);
  }

  @OnClick(R.id.current_folder_text) void onCurrentFolderClick() {
    final File parentDir = (File) currentFolderText.getTag();
    if(parentDir != null) {
      setCurrentDirectory(parentDir);
    }
  }

  private void importDataFromFile(File importFile) {
    if(importFile.exists() && importFile.canRead()) {
      Log.i(TAG, "Importing from file " + importFile.getAbsolutePath());
      JSONImporter importer = new JSONImporter(ReadTrackerApp.from(this).getDatabaseManager());
      try {
        importer.importFile(importFile);
        Log.i(TAG, "Imported file " + importFile);
        setResult(RESULT_OK);
        finish();
      } catch(ImportError importError) {
        Log.e(TAG, "Error while importing file", importError);
        String msg = getString(R.string.error_import_failed_broken_file, importFile.getName());
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
      } catch(IOException e) {
        Log.e(TAG, "Error while importing file", e);
        Toast.makeText(this, R.string.error_import_failed_read_file, Toast.LENGTH_SHORT).show();
      }
    } else {
      Log.w(TAG, "Invalid file selected: " + importFile);
    }
  }

  private void setCurrentDirectory(File dir) {
    // Set title
    currentFolderText.setText(dir.getAbsolutePath());
    currentFolderText.setTag(dir.getParentFile());

    File[] files = dir.listFiles();
    fileBrowseAdapter.clear();
    for(File file : files) {
      fileBrowseAdapter.add(file);
    }
    fileBrowseAdapter.sort(fileListComparator);
    fileBrowseAdapter.notifyDataSetChanged();
  }

  /**
   * Simple array adapter that presents a list of files in a directory.
   */
  protected static class FileBrowserAdapter extends ArrayAdapter<File> {
    public FileBrowserAdapter(ImportActivity importActivity) {
      super(importActivity, R.layout.file_list_entry);
      setNotifyOnChange(false);
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
      if(convertView == null) {
        convertView = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.file_list_entry, parent, false);
      }

      File file = getItem(position);

      final TextView entryView = (TextView) convertView;
      entryView.setText(file.getName());

      int iconResource, textColorResource;
      if(file.isFile()) {
        iconResource = R.drawable.icon_file;
        textColorResource = R.color.text_color_primary;
      } else {
        iconResource = R.drawable.icon_folder;
        textColorResource = R.color.text_color_secondary;
      }

      entryView.setCompoundDrawablesWithIntrinsicBounds(iconResource, 0, 0, 0);
      entryView.setTextColor(getContext().getResources().getColor(textColorResource));

      return entryView;
    }
  }
}
