package com.copilotbackend.askapi.service.impl;

import com.copilotbackend.askapi.config.SearchProperties;
import com.copilotbackend.askapi.dto.AskResponse;
import com.copilotbackend.askapi.exception.FileSearchException;
import com.copilotbackend.askapi.exception.InvalidBaseDirectoryException;
import com.copilotbackend.askapi.service.AskService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AskServiceImpl implements AskService {

    private static final Logger logger = LoggerFactory.getLogger(AskServiceImpl.class);
    private static final List<String> DEFAULT_KEYWORDS = List.of("Application", "Controller", "Service");

    private final SearchProperties searchProperties;

    public AskServiceImpl(SearchProperties searchProperties) {
        this.searchProperties = searchProperties;
    }

    @Override
    public AskResponse ask(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }

        Path baseDirectory = resolveBaseDirectory();
        String keyword = resolvePrimaryKeyword(question);

        List<String> files;
        if (keyword != null) {
            files = findFilesByKeywords(baseDirectory, List.of(keyword));
            if (files.isEmpty()) {
                logger.debug("No files found for keyword '{}'. Falling back to defaults.", keyword);
                files = findDefaultFiles(baseDirectory);
            }
        } else {
            files = findDefaultFiles(baseDirectory);
        }

        List<String> sources = files.stream()
                .map(path -> Path.of(path).getFileName().toString())
                .distinct()
                .toList();

        String answer = buildAnswer(question, sources);

        logger.info("Question='{}' produced {} source file(s)", question, sources.size());
        return new AskResponse(question, answer, sources);
    }

    private String buildAnswer(String question, List<String> sources) {
        Set<String> insights = new LinkedHashSet<>();

        String normalized = question.toLowerCase(Locale.ROOT);
        if (normalized.contains("spring boot")) {
            insights.add("Bu repo Spring Boot tabanli bir REST API uygulamasi.");
        }
        if (normalized.contains("api") || normalized.contains("endpoint") || normalized.contains("controller")) {
            insights.add("API giris noktasi AskController uzerinden /api/ask endpointidir.");
        }
        if (normalized.contains("hata") || normalized.contains("error") || normalized.contains("exception")) {
            insights.add("Hata yonetimi GlobalExceptionHandler sinifi ile merkezi olarak yapiliyor.");
        }
        if (normalized.contains("service") || normalized.contains("is mantigi") || normalized.contains("business")) {
            insights.add("Is mantigi AskServiceImpl icinde dosya arama ve sonuclari derleme adimlarindan olusuyor.");
        }

        if (insights.isEmpty()) {
            insights.add("Soruya gore repoda ilgili kaynaklari taradim ve bu modul bir soru alip kod tabaninda ilgili bolumleri analiz ediyor.");
        }

        String sourceText = sources.isEmpty()
                ? "Kaynak dosya bulunamadi."
                : "Incelenen kaynaklar: " + String.join(", ", sources) + ".";

        return String.join(" ", insights) + " " + sourceText;
    }

    private String resolvePrimaryKeyword(String question) {
        String normalized = question.toLowerCase(Locale.ROOT);
        if (normalized.contains("mfa")) {
            return "Mfa";
        }
        if (normalized.contains("login")) {
            return "Login";
        }
        return null;
    }

    private List<String> findDefaultFiles(Path baseDirectory) {
        List<String> defaultMatches = findFilesByKeywords(baseDirectory, DEFAULT_KEYWORDS);
        if (!defaultMatches.isEmpty()) {
            return defaultMatches;
        }
        return listFirstFiles(baseDirectory);
    }

    private List<String> findFilesByKeywords(Path baseDirectory, List<String> keywords) {
        Set<String> loweredKeywords = keywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        try (Stream<Path> stream = Files.walk(baseDirectory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> fileNameMatches(path, loweredKeywords))
                    .map(this::toNormalizedAbsolutePath)
                    .filter(this::exists)
                    .sorted()
                    .distinct()
                    .limit(searchProperties.getMaxResults())
                    .toList();
        } catch (IOException exception) {
            throw new FileSearchException("Failed to search files under: " + baseDirectory, exception);
        }
    }

    private List<String> listFirstFiles(Path baseDirectory) {
        try (Stream<Path> stream = Files.walk(baseDirectory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(this::toNormalizedAbsolutePath)
                    .filter(this::exists)
                    .sorted()
                    .limit(searchProperties.getMaxResults())
                    .toList();
        } catch (IOException exception) {
            throw new FileSearchException("Failed to list fallback files under: " + baseDirectory, exception);
        }
    }

    private boolean fileNameMatches(Path path, Set<String> loweredKeywords) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        for (String keyword : loweredKeywords) {
            if (fileName.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String toNormalizedAbsolutePath(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }

    private boolean exists(String absolutePath) {
        return Files.exists(Path.of(absolutePath));
    }

    private Path resolveBaseDirectory() {
        Path baseDirectory = Path.of(searchProperties.getBaseDirectory()).toAbsolutePath().normalize();
        if (!Files.exists(baseDirectory) || !Files.isDirectory(baseDirectory)) {
            throw new InvalidBaseDirectoryException(
                    "Configured base directory does not exist or is not a directory: " + baseDirectory);
        }

        logger.debug("Using base directory: {}", baseDirectory);
        return baseDirectory;
    }
}
