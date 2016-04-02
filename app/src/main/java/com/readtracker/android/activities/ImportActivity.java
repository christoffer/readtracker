package com.readtracker.android.activities;

import android.app.ProgressDialog;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.readtracker.R;
import com.readtracker.android.ReadTrackerApp;
import com.readtracker.android.db.export.ImportException;
import com.readtracker.android.db.export.JSONImporter;

import java.io.File;
import java.util.Comparator;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class ImportActivity extends BaseActivity {

  private static final String TAG = ImportActivity.class.getSimpleName();

  @InjectView(R.id.current_folder_text) TextView currentFolderText;
  @InjectView(R.id.button_common_dirs) TextView commonDirsButton;
  @InjectView(R.id.file_list) ListView fileList;

  private FileBrowserAdapter fileBrowseAdapter;
  private File mHomeDirFile;
  private File mDownloadsDirFile;
  private File mSDCardFile;

  /** Comparator that sorts by type (folders before files) and then by filename */
  private static Comparator<? super File> fileListComparator = new Comparator<File>() {
    @Override public int compare(File first, File second) {
      if(first.isDirectory() && !second.isDirectory()) return -1;
      if(second.isDirectory() && !first.isDirectory()) return 1;
      return first.getName().toLowerCase().compareTo(second.getName().toLowerCase());
    }
  };

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_import_data);
    ButterKnife.inject(this);

    setupFileAdapter();

    mHomeDirFile = getFilesDir();
    mDownloadsDirFile = Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_DOWNLOADS
    );
    mSDCardFile = getExternalFilesDir(null);

    registerForContextMenu(commonDirsButton);

    // NOTE Defaulting to downloads directory because that's the location most likely to contain
    // the users export data.
    File privateCacheDir = this.getFilesDir();
    setCurrentDirectory(privateCacheDir);
  }

  private void setupFileAdapter() {
    fileBrowseAdapter = new FileBrowserAdapter(this);
    fileList.setAdapter(fileBrowseAdapter);
    fileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        File clickedFile = fileBrowseAdapter.getItem(position);
        if(clickedFile.exists() && clickedFile.canRead()) {
          if(clickedFile.isDirectory()) {
            setCurrentDirectory(clickedFile);
          } else if(clickedFile.isFile()) {
            importDataFromFileAsync(clickedFile);
          }
        } else {
          final String msg = getString(
              R.string.error_import_failed_read_file, clickedFile.getName()
          );
          Toast.makeText(ImportActivity.this, msg, Toast.LENGTH_SHORT).show();
        }
      }
    });
  }

  @OnClick(R.id.current_folder_text) void onCurrentFolderClick() {
    final File parentDir = (File) currentFolderText.getTag();
    if(parentDir != null) {
      setCurrentDirectory(parentDir);
    }
  }

  @OnClick(R.id.button_common_dirs) void onCommonDirButtonClick(View view) {
    this.openContextMenu(view);
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.file_list_common_dirs, menu);

    menu.findItem(R.id.file_list_common_home).setVisible(mHomeDirFile != null);
    menu.findItem(R.id.file_list_common_downloads).setVisible(mDownloadsDirFile != null);
    menu.findItem(R.id.file_list_common_sdcard).setVisible(mSDCardFile != null);
  }

  @Override public boolean onContextItemSelected(MenuItem item) {
    switch(item.getItemId()) {
      case R.id.file_list_common_home:
        setCurrentDirectory(mHomeDirFile);
        return true;
      case R.id.file_list_common_downloads:
        setCurrentDirectory(mDownloadsDirFile);
        return true;
      case R.id.file_list_common_sdcard:
        setCurrentDirectory(mSDCardFile);
        return true;

    default:
      return super.onContextItemSelected(item);
    }
  }

  private void importDataFromFileAsync(File importFile) {
    Log.i(TAG, "Importing from file " + importFile.getAbsolutePath());
    fileList.setEnabled(false);
    new ImportTask().execute(importFile);
  }

  private void setCurrentDirectory(File dir) {
    String title = dir.getAbsolutePath();

    // Make the title a little prettier if it's a common dir accessed from the
    // context menu.
    if(dir.equals(mHomeDirFile)) {
      title = getString(R.string.file_list_common_home);
    } else if(dir.equals(mDownloadsDirFile)) {
      title = getString(R.string.file_list_common_downloads);
    } else if(dir.equals(mSDCardFile)) {
      title = getString(R.string.file_list_common_sdcard);
    }

    currentFolderText.setText(title);
    currentFolderText.setTag(dir.getParentFile());

    File[] files = dir.listFiles();
    fileBrowseAdapter.clear();
    if(files != null) {
      for(File file : files) {
        fileBrowseAdapter.add(file);
      }
    }
    fileBrowseAdapter.sort(fileListComparator);
    fileBrowseAdapter.notifyDataSetChanged();
  }

  /**
   * Simple array adapter that presents a list of files in a directory.
   */
  protected static class FileBrowserAdapter extends ArrayAdapter<File> {
    public FileBrowserAdapter(ImportActivity importActivity) {
      super(importActivity, R.layout.file_list_item);
      setNotifyOnChange(false);
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
      TextView fileListItem = (TextView) super.getView(position, convertView, parent);

      File file = getItem(position);

      fileListItem.setText(file.getName());

      int iconResource, textColorResource;
      if(file.isDirectory()) {
        iconResource = R.drawable.icon_folder;
        textColorResource = R.color.text_color_secondary;
      } else if(file.isFile()){
        iconResource = R.drawable.icon_file;
        textColorResource = R.color.text_color_primary;
      } else {
        // Assume unreadable file
        iconResource = R.drawable.icon_file;
        textColorResource = R.color.text_color_secondary_disabled;
      }

      fileListItem.setCompoundDrawablesWithIntrinsicBounds(iconResource, 0, 0, 0);
      if(Build.VERSION.SDK_INT > 23) {
        final Resources.Theme theme = getContext().getTheme();
        fileListItem.setTextColor(getContext().getResources().getColor(textColorResource, theme));
      } else {
        //noinspection deprecation
        fileListItem.setTextColor(getContext().getResources().getColor(textColorResource));
      }

      return fileListItem;
    }
  }

  private class ImportTask extends AsyncTask<File, Void, File> {
    private final JSONImporter importer;

    private ProgressDialog dialog = new ProgressDialog(ImportActivity.this);
    private Exception exception;

    public ImportTask() {
      this.importer = new JSONImporter(
          ReadTrackerApp.from(ImportActivity.this).getDatabaseManager()
      );
    }

    @Override protected void onPreExecute() {
      dialog.setMessage(getString(R.string.importing_files));
      dialog.show();
    }

    @Override protected File doInBackground(File... args) {
      final File importFile = args[0];
      try {
        importer.importFile(importFile);
      } catch(Exception e) {
        Log.e(TAG, "Error while importing file", exception);
        exception = e;
      }

      return importFile;
    }

    @Override protected void onPostExecute(File importFile) {
      dialog.dismiss();
      fileList.setEnabled(true);

      if(exception == null) {
        Log.i(TAG, "Imported file " + importFile);
        setResult(RESULT_OK);
        finish();
        return;
      }

      if(exception instanceof ImportException) {
        String msg = getString(R.string.error_import_failed_broken_file, importFile.getName());
        Toast.makeText(ImportActivity.this, msg, Toast.LENGTH_SHORT).show();
      } else {
        final String msg = getString(
            R.string.error_import_failed_read_file, importFile.getName()
        );
        Toast.makeText(ImportActivity.this, msg, Toast.LENGTH_SHORT).show();
      }
    }
  }
}
