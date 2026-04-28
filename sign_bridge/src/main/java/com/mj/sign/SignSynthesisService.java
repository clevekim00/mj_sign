package com.mj.sign;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class SignSynthesisService {
    public static final String PROTOCOL_VERSION = "signbridge-synthesis-v1";

    private static final int DEFAULT_FPS = 12;
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

    public SignSynthesisResult synthesize(SignSynthesisRequest request) {
        SignSynthesisRequest safeRequest = request == null
                ? new SignSynthesisRequest(null, null, null, null, null, null, null, null, null, null)
                : request;

        String sourceType = defaultIfBlank(safeRequest.source_type(), "text").toLowerCase(Locale.ROOT);
        String text = resolveInputText(safeRequest, sourceType);
        String locale = defaultIfBlank(safeRequest.locale(), InferenceContext.defaults().locale());
        String signLanguage = defaultIfBlank(safeRequest.sign_language(), defaultSignLanguage(locale));
        String modelProfile = defaultIfBlank(safeRequest.model_profile(), defaultModelProfile(signLanguage));
        String protocolVersion = normalizeProtocolVersion(safeRequest.protocol_version());
        String sessionId = defaultIfBlank(safeRequest.session_id(), "synthesis-" + UUID.randomUUID());

        List<String> glosses = buildGlosses(text, locale, signLanguage);
        List<String> nonManualMarkers = buildNonManualMarkers(text);
        SignSynthesisPlan signPlan = new SignSynthesisPlan(
                glosses,
                nonManualMarkers,
                grammarNoteFor(signLanguage)
        );
        SignSynthesisMotion motion = buildMotion(glosses);

        return new SignSynthesisResult(
                sessionId,
                "synthesis_result",
                sourceType,
                text,
                locale,
                signLanguage,
                modelProfile,
                protocolVersion,
                signPlan,
                motion,
                true,
                sourceType.equals("speech") ? 0.76 : 0.82,
                null
        );
    }

    private String resolveInputText(SignSynthesisRequest request, String sourceType) {
        String text = firstNonBlank(request.text(), request.transcript());
        if (text != null) {
            return text;
        }
        if ("speech".equals(sourceType) && !isBlank(request.audio_b64())) {
            return "mock speech input";
        }
        throw new IllegalArgumentException("text or transcript is required for sign synthesis.");
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

    private SignSynthesisMotion buildMotion(List<String> glosses) {
        int frameCount = Math.max(18, Math.min(48, glosses.size() * 8));
        List<SignSynthesisFrame> frames = new ArrayList<>(frameCount);
        for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
            long timestampMs = Math.round(frameIndex * (1000.0 / DEFAULT_FPS));
            double progress = frameIndex / (double) Math.max(1, frameCount - 1);
            frames.add(new SignSynthesisFrame(
                    timestampMs,
                    buildPoints(21, progress, 0.36, 0.48, 0.0),
                    buildPoints(21, progress, 0.64, 0.48, 1.1),
                    buildPoints(8, progress, 0.50, 0.62, 2.2),
                    buildPoints(12, progress, 0.50, 0.24, 3.3)
            ));
        }
        return new SignSynthesisMotion("landmark-frames", DEFAULT_FPS, frameCount, List.copyOf(frames));
    }

    private List<SignSynthesisPoint> buildPoints(
            int count,
            double progress,
            double centerX,
            double centerY,
            double phaseOffset
    ) {
        List<SignSynthesisPoint> points = new ArrayList<>(count);
        for (int pointIndex = 0; pointIndex < count; pointIndex++) {
            double phase = (progress * Math.PI * 2.0) + phaseOffset + (pointIndex * 0.19);
            double spread = 0.07 + ((pointIndex % 5) * 0.012);
            double x = clamp(centerX + Math.sin(phase) * spread + ((pointIndex % 4) - 1.5) * 0.012);
            double y = clamp(centerY + Math.cos(phase * 0.8) * spread + ((pointIndex / 4) * 0.01));
            double z = Math.cos(phase) * 0.06;
            points.add(new SignSynthesisPoint(round(x), round(y), round(z)));
        }
        return List.copyOf(points);
    }

    private String defaultSignLanguage(String locale) {
        String language = locale.toLowerCase(Locale.ROOT).split("[-_]")[0];
        return switch (language) {
            case "en" -> "asl";
            case "ja" -> "jsl";
            case "zh" -> "csl";
            case "fr" -> "lsf";
            case "de" -> "dgs";
            case "es" -> "lse";
            default -> "ksl";
        };
    }

    private String defaultModelProfile(String signLanguage) {
        if (signLanguage.equalsIgnoreCase("asl")) {
            return "sign-gemma";
        }
        return "sign-gemma-" + signLanguage.toLowerCase(Locale.ROOT);
    }

    private String normalizeProtocolVersion(String value) {
        if (isBlank(value)) {
            return PROTOCOL_VERSION;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String firstNonBlank(String first, String second) {
        if (!isBlank(first)) {
            return first.trim();
        }
        if (!isBlank(second)) {
            return second.trim();
        }
        return null;
    }

    private String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
