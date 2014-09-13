package com.readtracker.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.readtracker.R;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class ImportActivity extends BaseActivity {

  public static final String KEY_PICKED_FILE = "picked-file";

  @InjectView(R.id.current_folder_text) TextView currentFolderText;
  @InjectView(R.id.file_list) ListView fileList;

  private FileBrowserAdapter fileBrowseAdapter;

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
            Intent result = new Intent();
            result.putExtra(KEY_PICKED_FILE, clickedFile.getAbsolutePath());
            setResult(RESULT_OK, result);
            finish();
          }
        }
      }
    });

    File defaultDir = Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_DOWNLOADS
    );
    setCurrentDirectory(defaultDir);
  }

  @OnClick(R.id.current_folder_text)
  void onCurrentFolderClick() {
    final File parentDir = (File) currentFolderText.getTag();
    if(parentDir != null) {
      setCurrentDirectory(parentDir);
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
      ViewHolder holder;
      if(convertView == null) {
        convertView = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.file_list_entry, parent, false);
        holder = new ViewHolder(convertView);
        convertView.setTag(holder);
      } else {
        holder = (ViewHolder) convertView.getTag();
      }

      File file = getItem(position);
      holder.filenameText.setText(file.getName());

      int iconResource;
      if(file.isFile()) {
        iconResource = R.drawable.icon_file;
      } else {
        iconResource = R.drawable.icon_folder;
      }
      holder.itemIconImage.setImageDrawable(getContext().getResources().getDrawable(iconResource));

      return convertView;
    }

    protected static class ViewHolder {
      @InjectView(R.id.item_icon_image) ImageView itemIconImage;
      @InjectView(R.id.filename_text) TextView filenameText;

      public ViewHolder(View view) {
        ButterKnife.inject(this, view);
      }
    }
  }
}
