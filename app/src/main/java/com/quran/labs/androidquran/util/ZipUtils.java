package com.quran.labs.androidquran.util;

import androidx.annotation.VisibleForTesting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import timber.log.Timber;

public class ZipUtils {

  private static final int BUFFER_SIZE = 512;
  private static final int MAX_FILES = 12000; // Max number of files

  @VisibleForTesting
  static int MAX_UNZIPPED_SIZE = 0x1f400000; // Max size of unzipped data, 500MB

  /**
   * Unzip a file given the file, an item, and the listener
   * Does similar checks to those shown in rule 0's IDS04-J rule from:
   * https://www.securecoding.cert.org/confluence/display/java
   * @param zipFile the path to the zip file
   * @param destDirectory the directory to extract the file in
   * @param item any data object passed back to the listener
   * @param listener a progress listener
   * @param <T> the type of the item passed in
   * @return a boolean representing whether we succeeded to unzip the file or not
   */
  public static <T> boolean unzipFile(File zipFile,
      File destDirectory, T item, ZipListener<T> listener){
    File stagingDirectory = null;
    File backupDirectory = null;
    List<MoveRecord> moveRecords = new ArrayList<>();
    try {
      Timber.d("unzipping %s, size: %d", zipFile, zipFile.length());

      File destinationRoot = destDirectory.getCanonicalFile();
      if ((!destinationRoot.exists() && !destinationRoot.mkdirs()) ||
          !destinationRoot.isDirectory()) {
        throw new IOException("Unable to create destination directory.");
      }

      stagingDirectory = createStagingDirectory(destinationRoot);

      try (ZipFile zip = new ZipFile(zipFile, ZipFile.OPEN_READ)) {
        int numberOfFiles = zip.size();
        if (numberOfFiles > MAX_FILES) {
          throw new IllegalStateException("Invalid zip file.");
        }
        Enumeration<? extends ZipEntry> entries = zip.entries();

        long total = 0;
        int processedFiles = 0;
        byte[] buf = new byte[BUFFER_SIZE];
        while (entries.hasMoreElements()) {
          processedFiles++;
          ZipEntry entry = entries.nextElement();

          File destinationEntry = new File(destinationRoot, entry.getName()).getCanonicalFile();
          File stagingEntry = new File(stagingDirectory, entry.getName()).getCanonicalFile();
          if (!isChildOf(destinationRoot, destinationEntry) ||
              !isChildOf(stagingDirectory, stagingEntry)) {
            throw new IllegalStateException("Invalid zip file.");
          }

          if (entry.isDirectory()) {
            if (!stagingEntry.exists() && !stagingEntry.mkdirs()) {
              throw new IOException("Unable to create zip entry directory.");
            }
          } else {
            File parent = stagingEntry.getParentFile();
            if (parent == null || (!parent.exists() && !parent.mkdirs())) {
              throw new IOException("Unable to create zip entry parent directory.");
            }

            try (InputStream is = zip.getInputStream(entry);
                 FileOutputStream ostream = new FileOutputStream(stagingEntry)) {
              int size;
              while ((size = is.read(buf)) != -1) {
                if (size > MAX_UNZIPPED_SIZE - total) {
                  throw new IllegalStateException("Invalid zip file.");
                }
                ostream.write(buf, 0, size);
                total += size;
              }
            }
          }

          if (listener != null) {
            listener.onProcessingProgress(item, processedFiles, numberOfFiles);
          }
        }
      }

      backupDirectory = createStagingDirectory(destinationRoot);
      moveStagingContents(
          stagingDirectory,
          destinationRoot,
          destinationRoot,
          backupDirectory,
          moveRecords);
      return true;
    }
    catch (IOException | IllegalStateException | SecurityException error) {
      Timber.e(error, "Error unzipping file");
      rollbackMoves(moveRecords);
      return false;
    } finally {
      if (stagingDirectory != null && !deleteRecursively(stagingDirectory)) {
        Timber.w("Unable to completely remove zip staging directory: %s", stagingDirectory);
      }
      if (backupDirectory != null && !deleteRecursively(backupDirectory)) {
        Timber.w("Unable to completely remove zip backup directory: %s", backupDirectory);
      }
    }
  }

  private static File createStagingDirectory(File destinationRoot) throws IOException {
    File parent = destinationRoot.getParentFile();
    if (parent == null) {
      throw new IOException("Destination directory has no parent.");
    }

    File stagingDirectory = File.createTempFile(".quran-unzip-", ".tmp", parent);
    if (!stagingDirectory.delete() || !stagingDirectory.mkdir()) {
      throw new IOException("Unable to create zip staging directory.");
    }
    return stagingDirectory.getCanonicalFile();
  }

  private static boolean isChildOf(File root, File child) throws IOException {
    String rootPath = root.getCanonicalPath() + File.separator;
    return child.getCanonicalPath().startsWith(rootPath);
  }

  private static void moveStagingContents(File sourceDirectory, File destinationDirectory,
      File destinationRoot, File backupRoot, List<MoveRecord> moveRecords) throws IOException {
    File[] entries = sourceDirectory.listFiles();
    if (entries == null) {
      throw new IOException("Unable to read zip staging directory.");
    }
    Arrays.sort(entries, (left, right) -> left.getName().compareTo(right.getName()));

    for (File source : entries) {
      File destination = new File(destinationDirectory, source.getName()).getCanonicalFile();
      if (!isChildOf(destinationRoot, destination)) {
        throw new IOException("Invalid destination path.");
      }

      if (!destination.exists()) {
        if (!source.renameTo(destination)) {
          throw new IOException("Unable to move extracted zip entry.");
        }
        moveRecords.add(new MoveRecord(destination, null));
      } else if (source.isDirectory() && destination.isDirectory()) {
        moveStagingContents(source, destination, destinationRoot, backupRoot, moveRecords);
        if (!source.delete()) {
          throw new IOException("Unable to remove merged staging directory.");
        }
      } else if (source.isFile() && destination.isFile()) {
        File backup = backupFileFor(destinationRoot, backupRoot, destination);
        File backupParent = backup.getParentFile();
        if (backupParent == null || (!backupParent.exists() && !backupParent.mkdirs())) {
          throw new IOException("Unable to create zip backup directory.");
        }
        if (!destination.renameTo(backup)) {
          throw new IOException("Unable to back up existing zip entry.");
        }
        moveRecords.add(new MoveRecord(destination, backup));
        if (!source.renameTo(destination)) {
          throw new IOException("Unable to replace extracted zip entry.");
        }
      } else {
        throw new IOException("Zip entry conflicts with an existing path.");
      }
    }
  }

  private static File backupFileFor(File destinationRoot, File backupRoot, File destination)
      throws IOException {
    String rootPath = destinationRoot.getCanonicalPath() + File.separator;
    String destinationPath = destination.getCanonicalPath();
    if (!destinationPath.startsWith(rootPath)) {
      throw new IOException("Invalid backup path.");
    }
    File backup = new File(backupRoot, destinationPath.substring(rootPath.length()))
        .getCanonicalFile();
    if (!isChildOf(backupRoot, backup)) {
      throw new IOException("Invalid backup path.");
    }
    return backup;
  }

  private static void rollbackMoves(List<MoveRecord> moveRecords) {
    for (int i = moveRecords.size() - 1; i >= 0; i--) {
      MoveRecord record = moveRecords.get(i);
      if (!deleteRecursively(record.destination)) {
        Timber.e("Unable to remove partially installed zip entry: %s", record.destination);
        continue;
      }
      if (record.backup != null) {
        File destinationParent = record.destination.getParentFile();
        if (destinationParent == null ||
            (!destinationParent.exists() && !destinationParent.mkdirs()) ||
            !record.backup.renameTo(record.destination)) {
          Timber.e("Unable to restore zip entry backup: %s", record.destination);
        }
      }
    }
  }

  private static final class MoveRecord {
    private final File destination;
    private final File backup;

    private MoveRecord(File destination, File backup) {
      this.destination = destination;
      this.backup = backup;
    }
  }

  private static boolean deleteRecursively(File file) {
    if (!file.exists()) {
      return true;
    }

    boolean deletedChildren = true;
    if (file.isDirectory()) {
      File[] children = file.listFiles();
      if (children == null) {
        return false;
      }
      for (File child : children) {
        deletedChildren &= deleteRecursively(child);
      }
    }
    return deletedChildren && file.delete();
  }

  public interface ZipListener<T> {
    void onProcessingProgress(T obj, int processed, int total);
  }
}
