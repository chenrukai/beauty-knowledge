package com.beauty.knowledge.infrastructure.dictionary;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BeautyDictionary {

    private static final String DICT_PATH = "dictionary/beauty_terms.txt";

    private volatile List<String> terms = List.of();

    @PostConstruct
    public void init() {
        reload();
    }

    public List<String> match(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        String source = text.trim();
        Set<String> hits = new LinkedHashSet<>();
        for (String term : terms) {
            if (source.contains(term)) {
                hits.add(term);
            }
        }
        return new ArrayList<>(hits);
    }

    public synchronized void reload() {
        List<String> loaded = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource(DICT_PATH).getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String term = line.trim();
                if (!StringUtils.hasText(term) || term.startsWith("#")) {
                    continue;
                }
                loaded.add(term);
            }
        } catch (Exception ex) {
            log.error("Load beauty dictionary failed", ex);
        }
        this.terms = loaded.stream()
                .distinct()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .collect(Collectors.toList());
        log.info("Beauty dictionary loaded, size={}", this.terms.size());
    }
}
