package com.quran.labs.androidquran.util

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipUtilsTest {

  companion object {
    private const val PRODUCTION_MAX_UNZIPPED_SIZE = 0x1f400000
    private const val TEST_MAX_UNZIPPED_SIZE = 5 * 1024 * 1024 // 5 mb
    private const val CLI_ROOT_DIRECTORY = "src/test/resources"
  }

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  private lateinit var destinationDirectory: File
  private val listener =
    ZipUtils.ZipListener { _: Any?, _: Int, _: Int -> }

  @Before
  fun setup() {
    ZipUtils.MAX_UNZIPPED_SIZE = PRODUCTION_MAX_UNZIPPED_SIZE
    destinationDirectory = temporaryFolder.newFolder("destination")
  }

  @After
  fun cleanup() {
    ZipUtils.MAX_UNZIPPED_SIZE = PRODUCTION_MAX_UNZIPPED_SIZE
  }

  @Test
  fun testFileWithSmallMaxUnzipSize() {
    // max size is 500mb - make it 5mb for the test
    ZipUtils.MAX_UNZIPPED_SIZE = TEST_MAX_UNZIPPED_SIZE
    // thanks to https://github.com/commonsguy/cwac-security for this zip file
    val result = ZipUtils.unzipFile(File(CLI_ROOT_DIRECTORY, "zip_file_100mb.zip"),
      destinationDirectory, null, listener)

    assertThat(result).isFalse()
    assertThat(destinationDirectory.listFiles()).isEmpty()
  }

  @Test
  fun testNormalFile() {
    // thanks to https://github.com/commonsguy/cwac-security for this zip file
    val result = ZipUtils.unzipFile(File(CLI_ROOT_DIRECTORY, "zip_file_100mb.zip"),
      destinationDirectory, null, listener)

    assertThat(result).isTrue()
    assertThat(destinationDirectory.listFiles()).isNotEmpty()
  }

  @Test
  fun testZipFileWritesOutside() {
    // thanks to https://github.com/commonsguy/cwac-security for this zip file
    val result = ZipUtils.unzipFile(File(CLI_ROOT_DIRECTORY, "zip_file_writes_outside.zip"),
      destinationDirectory, null, listener)

    assertThat(result).isFalse()
    assertThat(destinationDirectory.listFiles()).isEmpty()
  }

  @Test
  fun rejectsSiblingDirectoryWithMatchingPrefix() {
    val zip = createZip(
      "sibling-prefix.zip",
      "../destination-sibling/escaped.txt" to "escaped".toByteArray()
    )

    val result = ZipUtils.unzipFile(zip, destinationDirectory, null, listener)

    assertThat(result).isFalse()
    assertThat(File(destinationDirectory.parentFile, "destination-sibling/escaped.txt").exists())
      .isFalse()
    assertThat(destinationDirectory.listFiles()).isEmpty()
  }

  @Test
  fun rejectsParentDirectoryTraversal() {
    val zip = createZip(
      "parent-traversal.zip",
      "../escaped.txt" to "escaped".toByteArray()
    )

    val result = ZipUtils.unzipFile(zip, destinationDirectory, null, listener)

    assertThat(result).isFalse()
    assertThat(File(destinationDirectory.parentFile, "escaped.txt").exists()).isFalse()
    assertThat(destinationDirectory.listFiles()).isEmpty()
  }

  @Test
  fun acceptsArchiveAtExactUncompressedSizeLimit() {
    ZipUtils.MAX_UNZIPPED_SIZE = 1024
    val zip = createZip("exact-limit.zip", "payload.bin" to ByteArray(1024) { 1 })

    val result = ZipUtils.unzipFile(zip, destinationDirectory, null, listener)

    assertThat(result).isTrue()
    assertThat(File(destinationDirectory, "payload.bin").length()).isEqualTo(1024)
  }

  @Test
  fun rejectsArchiveOverSizeLimitWithoutPartialFiles() {
    ZipUtils.MAX_UNZIPPED_SIZE = 1024
    val zip = createZip("over-limit.zip", "payload.bin" to ByteArray(1025) { 1 })

    val result = ZipUtils.unzipFile(zip, destinationDirectory, null, listener)

    assertThat(result).isFalse()
    assertThat(File(destinationDirectory, "payload.bin").exists()).isFalse()
    assertThat(destinationDirectory.listFiles()).isEmpty()
    assertNoStagingDirectories()
  }

  @Test
  fun extractsNestedFileWithoutDirectoryEntries() {
    val content = "nested content".toByteArray()
    val zip = createZip("nested.zip", "nested/path/file.txt" to content)

    val result = ZipUtils.unzipFile(zip, destinationDirectory, null, listener)

    assertThat(result).isTrue()
    assertThat(File(destinationDirectory, "nested/path/file.txt").readBytes())
      .isEqualTo(content)
  }

  @Test
  fun invalidLaterEntryDoesNotLeaveEarlierExtractedFile() {
    val zip = createZip(
      "invalid-after-valid.zip",
      "valid.txt" to "valid".toByteArray(),
      "../escaped.txt" to "escaped".toByteArray()
    )

    val result = ZipUtils.unzipFile(zip, destinationDirectory, null, listener)

    assertThat(result).isFalse()
    assertThat(File(destinationDirectory, "valid.txt").exists()).isFalse()
    assertThat(destinationDirectory.listFiles()).isEmpty()
    assertNoStagingDirectories()
  }

  @Test
  fun mergeFailureRestoresReplacedFilesAndRemovesNewFiles() {
    val existingFile = File(destinationDirectory, "existing.txt").apply {
      writeText("original")
    }
    File(destinationDirectory, "zz-conflict").writeText("keep me")
    val zip = createZip(
      "merge-failure.zip",
      "existing.txt" to "replacement".toByteArray(),
      "new.txt" to "new".toByteArray(),
      "zz-conflict/child.txt" to "cannot merge".toByteArray()
    )

    val result = ZipUtils.unzipFile(zip, destinationDirectory, null, listener)

    assertThat(result).isFalse()
    assertThat(existingFile.readText()).isEqualTo("original")
    assertThat(File(destinationDirectory, "new.txt").exists()).isFalse()
    assertThat(File(destinationDirectory, "zz-conflict").readText()).isEqualTo("keep me")
    assertNoStagingDirectories()
  }

  private fun assertNoStagingDirectories() {
    val stagingDirectories = destinationDirectory.parentFile?.listFiles().orEmpty()
      .filter { it.name.startsWith(".quran-unzip-") }
    assertThat(stagingDirectories).isEmpty()
  }

  private fun createZip(name: String, vararg entries: Pair<String, ByteArray>): File {
    val zip = temporaryFolder.newFile(name)
    ZipOutputStream(zip.outputStream().buffered()).use { output ->
      entries.forEach { (entryName, content) ->
        output.putNextEntry(ZipEntry(entryName))
        output.write(content)
        output.closeEntry()
      }
    }

    return zip
  }
}
