package com.datacom.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.CommentsCollection;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CommentPolicyTest {

    private static final Set<String> STRUCTURING_COMMENTS =
            Set.of("Arrange", "Act", "Assert", "Given", "When", "Then");

    private static final ParserConfiguration CONFIGURATION = new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);

    @Test
    void shouldContainNoComments_whenScanningProductionSources() {
        assertThat(violations(Path.of("src/main/java"), comment -> false)).isEmpty();
    }

    @Test
    void shouldContainOnlyStructuringComments_whenScanningTestSources() {
        assertThat(violations(Path.of("src/test/java"), CommentPolicyTest::isStructuringComment)).isEmpty();
    }

    private static boolean isStructuringComment(Comment comment) {
        return comment.isLineComment() && STRUCTURING_COMMENTS.contains(comment.getContent().trim());
    }

    private static List<String> violations(Path root, Predicate<Comment> allowed) {
        try (Stream<Path> files = Files.walk(root)) {
            return files
                    .filter(file -> file.toString().endsWith(".java"))
                    .flatMap(file -> commentsOf(file).stream()
                            .filter(comment -> !allowed.test(comment))
                            .map(comment -> file + ":" + line(comment)))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Comment> commentsOf(Path file) {
        try {
            ParseResult<CompilationUnit> result = new JavaParser(CONFIGURATION).parse(file);
            return result.getCommentsCollection()
                    .map(CommentsCollection::getComments)
                    .map(List::copyOf)
                    .orElse(List.of());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static int line(Comment comment) {
        return comment.getRange().map(range -> range.begin.line).orElse(0);
    }
}
