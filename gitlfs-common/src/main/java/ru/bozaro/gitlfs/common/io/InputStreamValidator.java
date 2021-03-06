package ru.bozaro.gitlfs.common.io;

import ru.bozaro.gitlfs.common.data.Meta;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Wrapper for validating hash and size of uploading object.
 *
 * @author Artem V. Navrotskiy
 */
public class InputStreamValidator extends InputStream {
  @Nonnull
  private static final char[] hexDigits = "0123456789abcdef".toCharArray();
  @Nonnull
  private final MessageDigest digest;
  @Nonnull
  private final InputStream stream;
  @Nonnull
  private final Meta meta;
  private boolean eof;
  private long totalSize;

  public InputStreamValidator(@Nonnull InputStream stream, @Nonnull Meta meta) throws IOException {
    try {
      this.digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IOException(e);
    }
    this.stream = stream;
    this.meta = meta;
    this.eof = false;
    this.totalSize = 0;
  }

  @Override
  public int read() throws IOException {
    if (eof) {
      return -1;
    }
    final int data = stream.read();
    if (data >= 0) {
      digest.update((byte) data);
      checkSize(1);
    } else {
      checkSize(-1);
    }
    return data;
  }

  private void checkSize(int size) throws IOException {
    if (size > 0) {
      totalSize += size;
    }
    if ((meta.getSize() > 0 && totalSize > meta.getSize())) {
      throw new IOException("Input stream too big");
    }
    if (size < 0) {
      eof = true;
      if ((meta.getSize() >= 0) && (totalSize != meta.getSize())) {
        throw new IOException("Unexpected end of stream");
      }
      final String hash = toHexString(digest.digest());
      if (!meta.getOid().equals(hash)) {
        throw new IOException("Invalid stream hash");
      }
    }
  }

  @Nonnull
  private static String toHexString(@Nonnull byte[] bytes) {
    StringBuilder sb = new StringBuilder(2 * bytes.length);
    for (byte b : bytes) {
      sb.append(hexDigits[(b >> 4) & 0xf]).append(hexDigits[b & 0xf]);
    }
    return sb.toString();
  }

  @Override
  public int read(@Nonnull byte[] buffer, int off, int len) throws IOException {
    if (eof) {
      return -1;
    }
    final int size = stream.read(buffer, off, len);
    if (size > 0) {
      digest.update(buffer, off, size);
    }
    checkSize(size);
    return size;
  }

  @Override
  public void close() throws IOException {
    stream.close();
  }
}
