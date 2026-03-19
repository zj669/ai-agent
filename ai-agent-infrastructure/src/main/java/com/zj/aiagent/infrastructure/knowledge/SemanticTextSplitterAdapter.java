package com.zj.aiagent.infrastructure.knowledge;

import com.zj.aiagent.domain.knowledge.valobj.ChunkingConfig;
import com.zj.aiagent.domain.knowledge.valobj.ChunkingStrategy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 规则驱动的语义分块实现。
 * 首版优先按段落、句子边界切分，并结合长度与简单词汇相似度控制块合并。
 */
@Slf4j
@Component
public class SemanticTextSplitterAdapter implements ChunkingStrategySplitter {

    private static final Pattern PARAGRAPH_SPLIT = Pattern.compile("\\R{2,}");
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[。！？!?；;\\.])");
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsIdeographic}]+");

    @Override
    public boolean supports(ChunkingStrategy strategy) {
        return strategy == ChunkingStrategy.SEMANTIC;
    }

    @Override
    public List<String> split(List<String> texts, ChunkingConfig config) {
        ChunkingConfig normalized = config.normalized();
        int minChunkSize = normalized.getMinChunkSize();
        int maxChunkSize = normalized.getMaxChunkSize();
        int overlap = normalized.getChunkOverlap();
        double threshold = normalized.getSimilarityThreshold();
        boolean mergeSmallChunks = Boolean.TRUE.equals(normalized.getMergeSmallChunks());

        List<String> units = new ArrayList<>();
        for (String text : texts) {
            if (text == null || text.isBlank()) {
                continue;
            }
            units.addAll(extractSemanticUnits(text, maxChunkSize, overlap));
        }

        List<String> chunks = new ArrayList<>();
        List<String> currentUnits = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String unit : units) {
            if (unit.isBlank()) {
                continue;
            }

            if (current.isEmpty()) {
                current.append(unit);
                currentUnits.add(unit);
                continue;
            }

            int candidateLength = current.length() + 1 + unit.length();
            double similarity = lexicalSimilarity(currentUnits.get(currentUnits.size() - 1), unit);
            boolean shouldMerge = current.length() < minChunkSize || similarity >= threshold;

            if (candidateLength <= maxChunkSize && shouldMerge) {
                current.append('\n').append(unit);
                currentUnits.add(unit);
                continue;
            }

            chunks.add(current.toString().trim());
            current.setLength(0);
            current.append(unit);
            currentUnits.clear();
            currentUnits.add(unit);
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }

        if (!mergeSmallChunks || chunks.size() <= 1) {
            return chunks;
        }

        return mergeTinyChunks(chunks, minChunkSize, maxChunkSize);
    }

    private List<String> extractSemanticUnits(String text, int maxChunkSize, int overlap) {
        List<String> units = new ArrayList<>();
        String[] paragraphs = PARAGRAPH_SPLIT.split(text.trim());
        for (String paragraph : paragraphs) {
            String normalized = normalizeWhitespace(paragraph);
            if (normalized.isBlank()) {
                continue;
            }

            if (normalized.length() <= maxChunkSize) {
                units.add(normalized);
                continue;
            }

            String[] sentences = SENTENCE_SPLIT.split(normalized);
            for (String sentence : sentences) {
                String trimmed = normalizeWhitespace(sentence);
                if (trimmed.isBlank()) {
                    continue;
                }

                if (trimmed.length() <= maxChunkSize) {
                    units.add(trimmed);
                } else {
                    units.addAll(splitLongUnit(trimmed, maxChunkSize, overlap));
                }
            }
        }
        return units;
    }

    private List<String> splitLongUnit(String text, int maxChunkSize, int overlap) {
        List<String> parts = new ArrayList<>();
        int safeOverlap = Math.max(0, Math.min(overlap, Math.max(0, maxChunkSize - 1)));
        int step = Math.max(1, maxChunkSize - safeOverlap);
        for (int start = 0; start < text.length(); start += step) {
            int end = Math.min(text.length(), start + maxChunkSize);
            parts.add(text.substring(start, end).trim());
            if (end >= text.length()) {
                break;
            }
        }
        return parts;
    }

    private List<String> mergeTinyChunks(List<String> chunks, int minChunkSize, int maxChunkSize) {
        List<String> merged = new ArrayList<>();
        for (String chunk : chunks) {
            if (merged.isEmpty()) {
                merged.add(chunk);
                continue;
            }

            String previous = merged.get(merged.size() - 1);
            if (chunk.length() < minChunkSize && previous.length() + 1 + chunk.length() <= maxChunkSize) {
                merged.set(merged.size() - 1, previous + "\n" + chunk);
            } else {
                merged.add(chunk);
            }
        }
        return merged;
    }

    private double lexicalSimilarity(String left, String right) {
        Set<String> leftTokens = tokenize(left);
        Set<String> rightTokens = tokenize(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0d;
        }

        int intersection = 0;
        for (String token : leftTokens) {
            if (rightTokens.contains(token)) {
                intersection++;
            }
        }

        int union = leftTokens.size() + rightTokens.size() - intersection;
        return union == 0 ? 0d : (double) intersection / union;
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String raw : TOKEN_SPLIT.split(text.toLowerCase(Locale.ROOT))) {
            if (raw == null || raw.isBlank()) {
                continue;
            }

            if (raw.length() == 1) {
                tokens.add(raw);
                continue;
            }

            tokens.add(raw);
            if (raw.codePoints().allMatch(Character::isIdeographic)) {
                raw.codePoints()
                        .mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                        .forEach(tokens::add);
            }
        }
        return tokens;
    }

    private String normalizeWhitespace(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }
}
