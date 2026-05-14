package com.example.dati_backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.example.dati_backend.dto.RankingItem;
import com.example.dati_backend.dto.PageResult;
import com.example.dati_backend.mapper.UserAnswerStatMapper;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RankingService {
    private static final Duration RANKING_CACHE_TTL = Duration.ofSeconds(10);
    private static final String RANKING_CACHE_PREFIX = "dati:ranking:";
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserAnswerStatMapper userAnswerStatMapper;
    private final RedisJsonCacheService cacheService;

    public List<RankingItem> ranking(Integer limit) {
        return ranking(limit, null);
    }

    public List<RankingItem> ranking(Integer limit, String sort) {
        int safeLimit = limit == null || limit < 1 ? 50 : Math.min(limit, 200);
        String safeSort = normalizeSort(sort);
        String cacheKey = rankingCacheKey(safeLimit, safeSort);
        TypeReference<List<RankingItem>> typeReference = new TypeReference<>() {};
        return cacheService.get(cacheKey, typeReference)
                .orElseGet(() -> {
                    List<RankingItem> items = switch (safeSort) {
                        case "answerCount" -> userAnswerStatMapper.listRankingByAnswerCount(safeLimit);
                        case "accuracy" -> userAnswerStatMapper.listRankingByAccuracy(safeLimit);
                        default -> userAnswerStatMapper.listRanking(safeLimit);
                    };
                    cacheService.set(cacheKey, items, RANKING_CACHE_TTL);
                    return items;
                });
    }

    public PageResult<RankingItem> pageRanking(Integer page, Integer size) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        String cacheKey = rankingPageCacheKey(safePage, safeSize);
        TypeReference<PageResult<RankingItem>> typeReference = new TypeReference<>() {};
        return cacheService.get(cacheKey, typeReference)
                .orElseGet(() -> {
                    PageResult<RankingItem> pageResult = new PageResult<>(
                            userAnswerStatMapper.listRankingPage(safeSize, (safePage - 1) * safeSize),
                            userAnswerStatMapper.countRanking(),
                            safePage,
                            safeSize
                    );
                    cacheService.set(cacheKey, pageResult, RANKING_CACHE_TTL);
                    return pageResult;
                });
    }

    public byte[] buildRankingExport() {
        List<RankingItem> items = userAnswerStatMapper.listRankingExport();
        try (Workbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("排行榜");
            writeRow(sheet, 0, List.of("排名", "账号", "姓名", "答题数", "正确数", "错误数", "正确率", "最近答题"));
            for (int i = 0; i < items.size(); i++) {
                RankingItem item = items.get(i);
                writeRow(sheet, i + 1, List.of(
                        i + 1,
                        valueOrEmpty(item.getUsername()),
                        valueOrEmpty(item.getRealName()),
                        valueOrZero(item.getAnswerCount()),
                        valueOrZero(item.getCorrectCount()),
                        valueOrZero(item.getWrongCount()),
                        item.getAccuracyRate() == null ? "0%" : item.getAccuracyRate() + "%",
                        item.getLastAnsweredAt() == null ? "" : item.getLastAnsweredAt().format(EXPORT_TIME_FORMATTER)
                ));
            }
            for (int i = 0; i < 8; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.min(Math.max(sheet.getColumnWidth(i), 2600), 9000));
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to build ranking export", exception);
        }
    }

    public void clearRankingCache() {
        cacheService.deleteByPattern(RANKING_CACHE_PREFIX + "*");
    }

    private void writeRow(Sheet sheet, int rowIndex, List<?> values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            if (value instanceof Number number) {
                row.createCell(i).setCellValue(number.doubleValue());
            } else {
                row.createCell(i).setCellValue(value == null ? "" : String.valueOf(value));
            }
        }
    }

    private String valueOrEmpty(String value) {
        return StringUtils.hasText(value) ? value : "";
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalizeSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return "correct";
        }
        return switch (sort.trim().toLowerCase(Locale.ROOT)) {
            case "answer", "answers", "answer_count", "answercount", "count" -> "answerCount";
            case "accuracy", "accuracy_rate", "accuracyrate", "rate" -> "accuracy";
            default -> "correct";
        };
    }

    private String rankingCacheKey(int limit, String sort) {
        return RANKING_CACHE_PREFIX + "list:sort:" + sort + ":limit:" + limit;
    }

    private String rankingPageCacheKey(int page, int size) {
        return RANKING_CACHE_PREFIX + "admin:page:" + page + ":size:" + size;
    }
}
