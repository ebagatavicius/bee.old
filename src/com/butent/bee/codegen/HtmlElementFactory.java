package com.butent.bee.codegen;

import com.butent.bee.shared.utils.BeeUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.util.ArrayList;
import java.util.List;

public class HtmlElementFactory {

  private final Path elementPackage = Paths.get("src/com/butent/bee/shared/html/builder/elements");
  private final Path elementFactory =
      Paths.get("src/com/butent/bee/shared/html/builder/Factory.java");

  public void generate() throws IOException {
    final List<String> lines = new ArrayList<>();

    lines.add("package com.butent.bee.shared.html.builder;");
    lines.add("import com.butent.bee.shared.html.builder.elements.*;");
    lines.add("public final class Factory {");

    Files.walkFileTree(elementPackage, new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        String name = BeeUtils.getPrefix(file.getFileName().toString(), '.');

        lines.add("public static " + name + " " + name.toLowerCase() + "() {");
        lines.add("return new " + name + "();");
        lines.add("}");

        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        lines.add("private Factory() {");
        lines.add("}");

        lines.add("}");

        try {
          Files.write(elementFactory, lines, Charset.defaultCharset());
        } catch (IOException e) {
          System.out.println(e);
        }

        return FileVisitResult.CONTINUE;
      }
    });
  }
}
