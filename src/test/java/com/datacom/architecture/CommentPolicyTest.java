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

    private static final Set<String> COMMENTAIRES_DE_STRUCTURE =
            Set.of("Arrange", "Act", "Assert", "Given", "When", "Then");

    private static final ParserConfiguration CONFIGURATION = new ParserConfiguration()
            .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);

    @Test
    void leCodeDeProductionNeContientAucunCommentaire() {
        assertThat(violations(Path.of("src/main/java"), commentaire -> false)).isEmpty();
    }

    @Test
    void lesTestsNeContiennentQueDesCommentairesDeStructure() {
        assertThat(violations(Path.of("src/test/java"), CommentPolicyTest::estUnCommentaireDeStructure)).isEmpty();
    }

    private static boolean estUnCommentaireDeStructure(Comment commentaire) {
        return commentaire.isLineComment() && COMMENTAIRES_DE_STRUCTURE.contains(commentaire.getContent().trim());
    }

    private static List<String> violations(Path racine, Predicate<Comment> autorise) {
        try (Stream<Path> fichiers = Files.walk(racine)) {
            return fichiers
                    .filter(fichier -> fichier.toString().endsWith(".java"))
                    .flatMap(fichier -> commentairesDe(fichier).stream()
                            .filter(commentaire -> !autorise.test(commentaire))
                            .map(commentaire -> fichier + ":" + ligne(commentaire)))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Comment> commentairesDe(Path fichier) {
        try {
            ParseResult<CompilationUnit> resultat = new JavaParser(CONFIGURATION).parse(fichier);
            return resultat.getCommentsCollection()
                    .map(CommentsCollection::getComments)
                    .map(List::copyOf)
                    .orElse(List.of());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static int ligne(Comment commentaire) {
        return commentaire.getRange().map(plage -> plage.begin.line).orElse(0);
    }
}
