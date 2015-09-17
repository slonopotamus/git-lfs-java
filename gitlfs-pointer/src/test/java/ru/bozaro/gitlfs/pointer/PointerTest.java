package ru.bozaro.gitlfs.pointer;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Pointer parser test.
 *
 * @author Artem V. Navrotskiy <bozaro@users.noreply.github.com>
 */
public class PointerTest {
  @DataProvider(name = "parseValidProvider")
  public static Object[][] parseValidProvider() {
    return new Object[][]{
        new Object[]{
            "pointer-valid-01.dat",
            ImmutableMap.builder()
                .put("version", "https://git-lfs.github.com/spec/v1")
                .put("oid", "sha256:4d7a214614ab2935c943f9e0ff69d22eadbb8f32b1258daaa5e2ca24d17e2393")
                .put("size", "12345")
                .build()
        },
        new Object[]{
            "pointer-valid-02.dat",
            ImmutableMap.builder()
                .put("version", "https://git-lfs.github.com/spec/v1")
                .put("oid", "sha256:4d7a214614ab2935c943f9e0ff69d22eadbb8f32b1258daaa5e2ca24d17e2393")
                .put("name", "Текст в UTF-8")
                .put("size", "12345")
                .build()
        },
    };
  }

  @Test(dataProvider = "parseValidProvider")
  public void parseValid(@NotNull String fileName, @NotNull Map<String, String> expected) throws IOException {
    try (InputStream stream = getClass().getResourceAsStream(fileName)) {
      Assert.assertNotNull(stream);
      Assert.assertEquals(Pointer.parsePointer(stream), expected);
    }
  }

  @DataProvider(name = "parseInvalidProvider")
  public static Object[][] parseInvalidProvider() {
    return new Object[][]{
        new Object[]{"pointer-invalid-01.dat", "Version is not in first line"},
        new Object[]{"pointer-invalid-02.dat", "Two empty lines at end of file"},
        new Object[]{"pointer-invalid-03.dat", "Size not found"},
        new Object[]{"pointer-invalid-04.dat", "Oid not found"},
        new Object[]{"pointer-invalid-05.dat", "Version not found"},
        new Object[]{"pointer-invalid-06.dat", "Invalid items order"},
        new Object[]{"pointer-invalid-07.dat", "Non utf-8"},
        new Object[]{"pointer-invalid-08.dat", "Size is not number"},
        new Object[]{"pointer-invalid-09.dat", "Duplicate line"},
    };
  }

  @Test(dataProvider = "parseInvalidProvider")
  public void parseInvalid(@NotNull String fileName, @NotNull String commit) throws IOException {
    try (InputStream stream = getClass().getResourceAsStream(fileName)) {
      Assert.assertNotNull(stream);
      Assert.assertNull(Pointer.parsePointer(stream));
    }
  }
}
