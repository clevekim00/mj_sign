package com.mj.sign;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class DefaultSignPlanner implements SignPlanner {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern PUNCTUATION = Pattern.compile("[\\p{Punct}、。！？，；：]");

    private static final Map<String, String> KOREAN_GLOSS_MAP = Map.ofEntries(
            Map.entry("안녕하세요", "안녕"),
            Map.entry("안녕", "안녕"),
            Map.entry("감사합니다", "감사"),
            Map.entry("고맙습니다", "감사"),
            Map.entry("내일", "내일"),
            Map.entry("오늘", "오늘"),
            Map.entry("어제", "어제"),
            Map.entry("병원", "병원"),
            Map.entry("학교", "학교"),
            Map.entry("집", "집"),
            Map.entry("도움", "도움"),
            Map.entry("필요", "필요"),
            Map.entry("가야", "가다"),
            Map.entry("갑니다", "가다"),
            Map.entry("가요", "가다"),
            Map.entry("먹다", "먹다"),
            Map.entry("먹어요", "먹다"),
            Map.entry("아프다", "아프다"),
            Map.entry("아파요", "아프다")
    );

    private static final Map<String, String> ENGLISH_GLOSS_MAP = Map.ofEntries(
            Map.entry("hello", "HELLO"),
            Map.entry("hi", "HELLO"),
            Map.entry("thank", "THANK-YOU"),
            Map.entry("thanks", "THANK-YOU"),
            Map.entry("you", "YOU"),
            Map.entry("i", "I"),
            Map.entry("me", "I"),
            Map.entry("need", "NEED"),
            Map.entry("help", "HELP"),
            Map.entry("tomorrow", "TOMORROW"),
            Map.entry("today", "TODAY"),
            Map.entry("hospital", "HOSPITAL"),
            Map.entry("school", "SCHOOL"),
            Map.entry("home", "HOME"),
            Map.entry("go", "GO"),
            Map.entry("want", "WANT"),
            Map.entry("please", "PLEASE")
    );

    @Override
    public SignSynthesisPlan plan(SignSynthesisContext context) {
        return new SignSynthesisPlan(
                buildGlosses(context.text(), context.locale(), context.sign_language()),
                buildNonManualMarkers(context.text()),
                grammarNoteFor(context.sign_language())
        );
    }

    private List<String> buildGlosses(String text, String locale, String signLanguage) {
        boolean english = locale.toLowerCase(Locale.ROOT).startsWith("en")
                || signLanguage.equalsIgnoreCase("asl");
        String cleaned = PUNCTUATION.matcher(text).replaceAll(" ").trim();
        String[] rawTokens = WHITESPACE.split(cleaned);
        List<String> glosses = new ArrayList<>();

        for (String rawToken : rawTokens) {
            String token = rawToken.trim();
            if (token.isBlank()) {
                continue;
            }
            String gloss = english ? englishGloss(token) : koreanGloss(token);
            if (!gloss.isBlank() && !glosses.contains(gloss)) {
                glosses.add(gloss);
            }
            if (glosses.size() >= 12) {
                break;
            }
        }

        if (glosses.isEmpty()) {
            glosses.add(english ? "MESSAGE" : "메시지");
        }
        return List.copyOf(glosses);
    }

    private String koreanGloss(String token) {
        String normalized = token
                .replaceAll("(은|는|이|가|을|를|에|에서|으로|로|와|과)$", "")
                .replaceAll("(합니다|해요|해야|했어요|합니다)$", "하다")
                .trim();
        if (normalized.endsWith("가야") || normalized.endsWith("갑니다") || normalized.endsWith("가요")) {
            normalized = "가다";
        }
        if (normalized.endsWith("필요해요") || normalized.endsWith("필요합니다")) {
            normalized = "필요";
        }
        if (normalized.equals("하다")) {
            return "";
        }
        return KOREAN_GLOSS_MAP.getOrDefault(normalized, normalized);
    }

    private String englishGloss(String token) {
        String normalized = token.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9'-]", "");
        return ENGLISH_GLOSS_MAP.getOrDefault(normalized, normalized.toUpperCase(Locale.ROOT));
    }

    private List<String> buildNonManualMarkers(String text) {
        List<String> markers = new ArrayList<>();
        markers.add("neutral");
        if (text.contains("?") || text.contains("？")) {
            markers.add("question-eyebrows");
        }
        if (text.contains("!") || text.contains("！")) {
            markers.add("emphasis");
        }
        return List.copyOf(markers);
    }

    private String grammarNoteFor(String signLanguage) {
        if (signLanguage.equalsIgnoreCase("asl")) {
            return "Mock ASL-compatible gloss order. Replace with a language-specific planner before production.";
        }
        if (signLanguage.equalsIgnoreCase("ksl")) {
            return "Mock KSL-compatible gloss order. Replace with a language-specific planner before production.";
        }
        return "Mock language-neutral gloss order. Attach a language-specific planner for production.";
    }
}
