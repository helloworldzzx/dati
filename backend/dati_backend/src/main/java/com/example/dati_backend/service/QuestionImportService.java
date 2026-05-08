package com.example.dati_backend.service;

import com.example.dati_backend.dto.QuestionOptionRequest;
import com.example.dati_backend.dto.QuestionImportResult;
import com.example.dati_backend.dto.QuestionRequest;
import com.example.dati_backend.entity.QuestionCategory;
import com.example.dati_backend.entity.QuestionImportBatch;
import com.example.dati_backend.entity.QuestionImportError;
import com.example.dati_backend.mapper.QuestionCategoryMapper;
import com.example.dati_backend.mapper.QuestionImportBatchMapper;
import com.example.dati_backend.mapper.QuestionImportErrorMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class QuestionImportService {
    private static final Pattern OPTION_MARKER_PATTERN = Pattern.compile(
            "(?<![A-Za-z])([A-Z])\\s*[\\.．、\\)）:：]"
    );

    private final QuestionImportBatchMapper batchMapper;
    private final QuestionImportErrorMapper errorMapper;
    private final QuestionCategoryMapper categoryMapper;
    private final QuestionService questionService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final DataFormatter dataFormatter = new DataFormatter(Locale.CHINA);

    @Transactional
    public QuestionImportBatch createBatch(String fileName, Long importedBy) {
        QuestionImportBatch batch = new QuestionImportBatch();
        batch.setFileName(fileName);
        batch.setTotalCount(0);
        batch.setSuccessCount(0);
        batch.setFailCount(0);
        batch.setStatus("PROCESSING");
        batch.setImportedBy(importedBy);
        batchMapper.insert(batch);
        return batch;
    }

    public QuestionImportResult importQuestions(MultipartFile file, Long importedBy) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is required");
        }

        QuestionImportBatch batch = createBatch(file.getOriginalFilename(), importedBy);
        int totalCount = 0;
        int successCount = 0;
        int failCount = 0;
        boolean foundImportSheet = false;

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("Excel sheet is empty");
            }

            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                HeaderMap header = readHeader(sheet);
                if (!header.isImportSheet()) {
                    continue;
                }
                foundImportSheet = true;

                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (isEmptyRow(row, header)) {
                        continue;
                    }
                    totalCount++;
                    try {
                        transactionTemplate.executeWithoutResult(status -> importRow(row, header, importedBy));
                        successCount++;
                    } catch (Exception exception) {
                        failCount++;
                        saveError(
                                batch.getId(),
                                rowIndex + 1,
                                sheet.getSheetName() + ": " + exceptionMessage(exception),
                                rawRowJson(row, header)
                        );
                    }
                }
            }

            if (!foundImportSheet) {
                throw new IllegalArgumentException("No import sheet found");
            }
        } catch (Exception exception) {
            failCount++;
            saveError(batch.getId(), 0, exceptionMessage(exception), "[]");
        }

        batch.setTotalCount(totalCount);
        batch.setSuccessCount(successCount);
        batch.setFailCount(failCount);
        batch.setStatus(resolveStatus(successCount, failCount));
        batchMapper.updateResult(batch);
        return buildResult(batch.getId());
    }

    public byte[] buildTemplate(String templateType) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            createGuideSheet(workbook);
            switch (normalizeTemplateType(templateType)) {
                case "choice" -> createChoiceSheet(workbook);
                case "judge" -> createJudgeSheet(workbook);
                case "analysis" -> createAnalysisSheet(workbook);
                case "all" -> {
                    createChoiceSheet(workbook);
                    createJudgeSheet(workbook);
                    createAnalysisSheet(workbook);
                }
                default -> throw new IllegalArgumentException("Unsupported template type: " + templateType);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to build Excel template", exception);
        }
    }

    public String templateFileName(String templateType) {
        return switch (normalizeTemplateType(templateType)) {
            case "choice" -> "choice-question-import-template.xlsx";
            case "judge" -> "judge-question-import-template.xlsx";
            case "analysis" -> "analysis-question-import-template.xlsx";
            case "all" -> "question-import-template.xlsx";
            default -> "question-import-template.xlsx";
        };
    }

    public QuestionImportResult buildResult(Long batchId) {
        QuestionImportBatch batch = batchMapper.findById(batchId);
        return QuestionImportResult.from(batch, errorMapper.listByBatchId(batchId));
    }

    @Transactional
    public void saveError(Long batchId, Integer rowNo, String message, String rawData) {
        QuestionImportError error = new QuestionImportError();
        error.setBatchId(batchId);
        error.setRowNo(rowNo);
        error.setErrorMessage(limitText(message, 500));
        error.setRawData(rawData);
        errorMapper.insert(error);
    }

    private String exceptionMessage(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return StringUtils.hasText(message) ? message : current.getClass().getSimpleName();
    }

    private String limitText(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private void importRow(Row row, HeaderMap header, Long importedBy) {
        Long categoryId = resolveCategoryId(row, header);
        String type = normalizeQuestionType(cellString(row, header.typeColumn));
        String title = cellString(row, header.titleColumn);
        if (!StringUtils.hasText(title)) {
            throw new IllegalArgumentException("Question title is required");
        }

        String correctAnswer = normalizeCorrectAnswer(type, cellString(row, header.correctAnswerColumn));
        if (!"ANALYSIS".equals(type) && !StringUtils.hasText(correctAnswer)) {
            throw new IllegalArgumentException("Correct answer is required");
        }

        List<QuestionOptionRequest> options = parseOptions(row, header, type, correctAnswer);
        if (("SINGLE".equals(type) || "MULTIPLE".equals(type)) && options.size() < 2) {
            throw new IllegalArgumentException("Choice questions require at least two options");
        }

        QuestionRequest request = new QuestionRequest(
                categoryId,
                type,
                title,
                correctAnswer,
                cellString(row, header.analysisColumn),
                cellString(row, header.sourceFileColumn),
                StringUtils.hasText(cellString(row, header.statusColumn))
                        ? cellString(row, header.statusColumn).toUpperCase(Locale.ROOT)
                        : "ENABLED",
                importedBy,
                options
        );
        questionService.createQuestion(request);
    }

    private Long resolveCategoryId(Row row, HeaderMap header) {
        Long parentId = null;
        Long categoryId = null;
        boolean metBlank = false;
        List<Integer> categoryColumns = List.of(
                header.categoryLevel1Column,
                header.categoryLevel2Column,
                header.categoryLevel3Column
        );

        for (int index = 0; index < categoryColumns.size(); index++) {
            String name = cellString(row, categoryColumns.get(index));
            if (!StringUtils.hasText(name)) {
                metBlank = true;
                continue;
            }
            if (metBlank) {
                throw new IllegalArgumentException("Category levels cannot be skipped");
            }
            QuestionCategory category = getOrCreateCategory(parentId, name, index + 1);
            parentId = category.getId();
            categoryId = category.getId();
        }

        if (categoryId == null) {
            throw new IllegalArgumentException("At least one category is required");
        }
        return categoryId;
    }

    private QuestionCategory getOrCreateCategory(Long parentId, String name, int level) {
        String normalizedName = name.trim();
        QuestionCategory existing = categoryMapper.findByParentAndName(parentId, normalizedName);
        if (existing != null) {
            return existing;
        }
        QuestionCategory category = new QuestionCategory();
        category.setParentId(parentId);
        category.setName(normalizedName);
        category.setLevel(level);
        category.setSortNo(0);
        category.setStatus("ENABLED");
        categoryMapper.insert(category);
        return category;
    }

    private List<QuestionOptionRequest> parseOptions(Row row, HeaderMap header, String type, String correctAnswer) {
        if ("ANALYSIS".equals(type)) {
            return List.of();
        }
        if ("JUDGE".equals(type)) {
            return List.of(
                    new QuestionOptionRequest("TRUE", "\u6B63\u786E", "TRUE".equals(correctAnswer), 0),
                    new QuestionOptionRequest("FALSE", "\u9519\u8BEF", "FALSE".equals(correctAnswer), 1)
            );
        }

        String mergedOptions = cellString(row, header.optionsColumn);
        if (StringUtils.hasText(mergedOptions)) {
            return parseMergedOptions(mergedOptions, correctAnswer);
        }

        List<String> correctKeys = splitAnswer(correctAnswer);
        return header.optionColumns.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String content = cellString(row, entry.getValue());
                    if (!StringUtils.hasText(content)) {
                        return null;
                    }
                    int sortNo = entry.getKey().charAt(0) - 'A';
                    return new QuestionOptionRequest(
                            entry.getKey(),
                            content,
                            correctKeys.contains(entry.getKey()),
                            sortNo
                    );
                })
                .filter(option -> option != null)
                .toList();
    }

    private List<QuestionOptionRequest> parseMergedOptions(String optionsText, String correctAnswer) {
        List<String> correctKeys = splitAnswer(correctAnswer);
        String normalized = optionsText
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .strip();

        Matcher matcher = OPTION_MARKER_PATTERN.matcher(normalized);
        List<OptionMarker> markers = new ArrayList<>();
        while (matcher.find()) {
            markers.add(new OptionMarker(matcher.group(1), matcher.start(), matcher.end()));
        }

        List<QuestionOptionRequest> options = new ArrayList<>();
        for (int i = 0; i < markers.size(); i++) {
            OptionMarker current = markers.get(i);
            int contentEnd = i + 1 < markers.size() ? markers.get(i + 1).markerStart() : normalized.length();
            String optionKey = current.optionKey();
            String content = normalized.substring(current.contentStart(), contentEnd).strip();
            if (!StringUtils.hasText(content)) {
                continue;
            }
            options.add(new QuestionOptionRequest(
                    optionKey,
                    content,
                    correctKeys.contains(optionKey),
                    optionKey.charAt(0) - 'A'
            ));
        }
        return options;
    }

    private String normalizeQuestionType(String value) {
        String type = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
        return switch (type) {
            case "SINGLE", "\u5355\u9009", "\u5355\u9009\u9898" -> "SINGLE";
            case "MULTIPLE", "\u591A\u9009", "\u591A\u9009\u9898" -> "MULTIPLE";
            case "JUDGE", "\u5224\u65AD", "\u5224\u65AD\u9898" -> "JUDGE";
            case "ANALYSIS", "\u5206\u6790", "\u5206\u6790\u9898", "\u95EE\u7B54", "\u95EE\u7B54\u9898" -> "ANALYSIS";
            default -> throw new IllegalArgumentException("Unsupported question type: " + value);
        };
    }

    private String normalizeTemplateType(String value) {
        if (!StringUtils.hasText(value)) {
            return "all";
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "choice", "choices", "single", "multiple", "\u9009\u62E9\u9898", "\u5355\u9009", "\u591A\u9009" -> "choice";
            case "judge", "judgement", "truefalse", "\u5224\u65AD\u9898", "\u5224\u65AD" -> "judge";
            case "analysis", "essay", "\u5206\u6790\u9898", "\u5206\u6790", "\u95EE\u7B54" -> "analysis";
            case "all", "\u5168\u90E8" -> "all";
            default -> value.trim().toLowerCase(Locale.ROOT);
        };
    }

    private String normalizeCorrectAnswer(String type, String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if ("ANALYSIS".equals(type)) {
            return value.trim();
        }
        String answer = value.trim().toUpperCase(Locale.ROOT);
        if (!"JUDGE".equals(type)) {
            return String.join(",", splitAnswer(answer));
        }
        return switch (answer) {
            case "TRUE", "T", "1", "YES", "Y", "\u6B63\u786E", "\u5BF9", "A" -> "TRUE";
            case "FALSE", "F", "0", "NO", "N", "\u9519\u8BEF", "\u9519", "B" -> "FALSE";
            default -> throw new IllegalArgumentException("Unsupported judge answer: " + value);
        };
    }

    private List<String> splitAnswer(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.trim().toUpperCase(Locale.ROOT).split("[,;\\uFF0C\\uFF1B\\u3001\\s]+"))
                .filter(StringUtils::hasText)
                .map(String::trim)
                .sorted()
                .toList();
    }

    private HeaderMap readHeader(Sheet sheet) {
        Row row = sheet.getRow(0);
        HeaderMap header = new HeaderMap();
        if (row == null) {
            return header;
        }

        for (int col = 0; col < row.getLastCellNum(); col++) {
            String label = normalizeHeader(cellString(row, col));
            if (!StringUtils.hasText(label)) {
                continue;
            }
            header.lastColumn = Math.max(header.lastColumn, col);
            switch (label) {
                case "\u4E00\u7EA7\u5206\u7C7B", "\u4E00\u7EA7\u76EE\u5F55" -> header.categoryLevel1Column = col;
                case "\u4E8C\u7EA7\u5206\u7C7B", "\u4E8C\u7EA7\u76EE\u5F55" -> header.categoryLevel2Column = col;
                case "\u4E09\u7EA7\u5206\u7C7B", "\u4E09\u7EA7\u76EE\u5F55" -> header.categoryLevel3Column = col;
                case "\u9898\u578B" -> header.typeColumn = col;
                case "\u9898\u5E72", "\u9898\u76EE" -> header.titleColumn = col;
                case "\u9009\u9879", "\u9009\u9879\u5185\u5BB9", "\u9009\u62E9\u9898\u9009\u9879" -> header.optionsColumn = col;
                case "\u6B63\u786E\u7B54\u6848", "\u53C2\u8003\u7B54\u6848", "\u7B54\u6848" -> header.correctAnswerColumn = col;
                case "\u89E3\u6790", "\u7B54\u6848\u89E3\u6790" -> header.analysisColumn = col;
                case "\u6765\u6E90\u6587\u4EF6", "\u6765\u6E90" -> header.sourceFileColumn = col;
                case "\u72B6\u6001" -> header.statusColumn = col;
                default -> readOptionColumn(header, label, col);
            }
        }
        return header;
    }

    private void readOptionColumn(HeaderMap header, String label, int col) {
        String optionKey = null;
        if (label.matches("[A-Z]")) {
            optionKey = label;
        } else if (label.matches("\u9009\u9879[A-Z]")) {
            optionKey = label.substring(2);
        } else if (label.matches("OPTION[A-Z]")) {
            optionKey = label.substring("OPTION".length());
        }

        if (optionKey != null) {
            header.optionColumns.put(optionKey, col);
        }
    }

    private String normalizeHeader(String value) {
        return StringUtils.hasText(value)
                ? value.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT)
                : "";
    }

    private boolean isEmptyRow(Row row, HeaderMap header) {
        if (row == null) {
            return true;
        }
        for (int col = 0; col <= header.lastColumn; col++) {
            if (StringUtils.hasText(cellString(row, col))) {
                return false;
            }
        }
        return true;
    }

    private String rawRowJson(Row row, HeaderMap header) {
        try {
            List<String> values = new ArrayList<>();
            for (int col = 0; col <= header.lastColumn; col++) {
                values.add(cellString(row, col));
            }
            return objectMapper.writeValueAsString(values);
        } catch (Exception exception) {
            return "[]";
        }
    }

    private String cellString(Row row, Integer col) {
        if (row == null || col == null || col < 0) {
            return "";
        }
        Cell cell = row.getCell(col);
        return cell == null ? "" : dataFormatter.formatCellValue(cell).trim();
    }

    private String resolveStatus(int successCount, int failCount) {
        if (successCount > 0 && failCount == 0) {
            return "SUCCESS";
        }
        if (successCount > 0) {
            return "PARTIAL_SUCCESS";
        }
        return "FAILED";
    }

    private void createGuideSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("\u5BFC\u5165\u8BF4\u660E");
        List<String> lines = List.of(
                "\u8BF7\u6309\u9898\u578B\u5206\u522B\u5728\u3010\u9009\u62E9\u9898\u3011\u3010\u5224\u65AD\u9898\u3011\u3010\u5206\u6790\u9898\u3011\u5DE5\u4F5C\u8868\u586B\u5199\u3002",
                "\u9009\u62E9\u9898\u7684\u9009\u9879\u653E\u5728\u4E00\u4E2A\u5355\u5143\u683C\uFF0C\u6BCF\u4E2A\u9009\u9879\u4EE5 A. / B. / C. \u5F00\u5934\uFF0C\u9009\u9879\u6570\u91CF\u4E0D\u9650\u3002",
                "\u591A\u9009\u9898\u6B63\u786E\u7B54\u6848\u7528 A,C,F \u8FD9\u79CD\u683C\u5F0F\uFF0C\u4E5F\u652F\u6301 A\u3001C\u3001F\u3002",
                "\u5224\u65AD\u9898\u6B63\u786E\u7B54\u6848\u53EF\u586B\uFF1A\u6B63\u786E\u3001\u9519\u8BEF\u3001\u5BF9\u3001\u9519\u3001TRUE\u3001FALSE\u3002",
                "\u7CFB\u7EDF\u4F1A\u6309\u4E00\u7EA7/\u4E8C\u7EA7/\u4E09\u7EA7\u5206\u7C7B\u81EA\u52A8\u521B\u5EFA\u76EE\u5F55\u3002"
        );
        for (int i = 0; i < lines.size(); i++) {
            sheet.createRow(i).createCell(0).setCellValue(lines.get(i));
        }
        sheet.setColumnWidth(0, 12000);
    }

    private void createChoiceSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("\u9009\u62E9\u9898");
        writeHeader(sheet, List.of(
                "\u4E00\u7EA7\u5206\u7C7B",
                "\u4E8C\u7EA7\u5206\u7C7B",
                "\u4E09\u7EA7\u5206\u7C7B",
                "\u9898\u578B",
                "\u9898\u5E72",
                "\u9009\u9879",
                "\u6B63\u786E\u7B54\u6848",
                "\u89E3\u6790",
                "\u6765\u6E90\u6587\u4EF6",
                "\u72B6\u6001"
        ));
        writeRow(sheet, 1, List.of(
                "\u601D\u60F3\u653F\u6CBB\u6559\u80B2",
                "\u9AD8\u6821\u8F85\u5BFC\u5458",
                "\u961F\u4F0D\u5EFA\u8BBE",
                "\u5355\u9009",
                "\u8F85\u5BFC\u5458\u5E94\u5F53\u52AA\u529B\u6210\u4E3A\u5B66\u751F\u6210\u957F\u6210\u624D\u7684\u548C\u5065\u5EB7\u751F\u6D3B\u7684\u77E5\u5FC3\u670B\u53CB\u3002",
                "A. \u7231\u56FD\u5B88\u6CD5\nB. \u656C\u4E1A\u7231\u751F\nC. \u80B2\u4EBA\u4E3A\u672C\nD. \u7EC8\u8EAB\u5B66\u4E60\nE. \u4E3A\u4EBA\u5E08\u8868",
                "B",
                "\u6B63\u786E\u7B54\u6848\u4E3A B\u3002",
                "\u666E\u901A\u9AD8\u7B49\u5B66\u6821\u8F85\u5BFC\u5458\u961F\u4F0D\u5EFA\u8BBE\u89C4\u5B9A",
                "ENABLED"
        ));
        writeRow(sheet, 2, List.of(
                "\u601D\u60F3\u653F\u6CBB\u6559\u80B2",
                "\u7EFC\u5408\u77E5\u8BC6",
                "",
                "\u591A\u9009",
                "\u4EE5\u4E0B\u5C5E\u4E8E\u793A\u4F8B\u591A\u9009\u9879\u7684\u6709\u54EA\u4E9B\uFF1F",
                "A. \u9009\u9879\u4E00\nB. \u9009\u9879\u4E8C\nC. \u9009\u9879\u4E09\nD. \u9009\u9879\u56DB\nE. \u9009\u9879\u4E94\nF. \u9009\u9879\u516D",
                "A,C,F",
                "\u591A\u9009\u7B54\u6848\u7528\u82F1\u6587\u9017\u53F7\u5206\u9694\u3002",
                "\u793A\u4F8B\u6765\u6E90",
                "ENABLED"
        ));
        finishSheet(sheet);
    }

    private void createJudgeSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("\u5224\u65AD\u9898");
        writeHeader(sheet, List.of(
                "\u4E00\u7EA7\u5206\u7C7B",
                "\u4E8C\u7EA7\u5206\u7C7B",
                "\u4E09\u7EA7\u5206\u7C7B",
                "\u9898\u578B",
                "\u9898\u5E72",
                "\u6B63\u786E\u7B54\u6848",
                "\u89E3\u6790",
                "\u6765\u6E90\u6587\u4EF6",
                "\u72B6\u6001"
        ));
        writeRow(sheet, 1, List.of(
                "\u601D\u60F3\u653F\u6CBB\u6559\u80B2",
                "\u5224\u65AD\u793A\u4F8B",
                "",
                "\u5224\u65AD",
                "\u8FD9\u662F\u4E00\u9053\u5224\u65AD\u9898\u793A\u4F8B\u3002",
                "\u6B63\u786E",
                "\u53EF\u586B\u6B63\u786E/\u9519\u8BEF\u6216 TRUE/FALSE\u3002",
                "\u793A\u4F8B\u6765\u6E90",
                "ENABLED"
        ));
        finishSheet(sheet);
    }

    private void createAnalysisSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("\u5206\u6790\u9898");
        writeHeader(sheet, List.of(
                "\u4E00\u7EA7\u5206\u7C7B",
                "\u4E8C\u7EA7\u5206\u7C7B",
                "\u4E09\u7EA7\u5206\u7C7B",
                "\u9898\u578B",
                "\u9898\u5E72",
                "\u53C2\u8003\u7B54\u6848",
                "\u89E3\u6790",
                "\u6765\u6E90\u6587\u4EF6",
                "\u72B6\u6001"
        ));
        writeRow(sheet, 1, List.of(
                "\u601D\u60F3\u653F\u6CBB\u6559\u80B2",
                "\u5206\u6790\u793A\u4F8B",
                "",
                "\u5206\u6790",
                "\u8BF7\u7ED3\u5408\u6750\u6599\u8FDB\u884C\u5206\u6790\u3002",
                "\u8FD9\u91CC\u586B\u5199\u53C2\u8003\u7B54\u6848\u3002",
                "\u8FD9\u91CC\u586B\u5199\u7B54\u9898\u601D\u8DEF\u6216\u8BC4\u5206\u8981\u70B9\u3002",
                "\u793A\u4F8B\u6765\u6E90",
                "ENABLED"
        ));
        finishSheet(sheet);
    }

    private void writeHeader(Sheet sheet, List<String> headers) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            row.createCell(i).setCellValue(headers.get(i));
        }
    }

    private void writeRow(Sheet sheet, int rowIndex, List<String> values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.size(); i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(values.get(i));
            if (values.get(i).contains("\n")) {
                CellStyle style = sheet.getWorkbook().createCellStyle();
                style.setWrapText(true);
                cell.setCellStyle(style);
                row.setHeightInPoints(90);
            }
        }
    }

    private void finishSheet(Sheet sheet) {
        for (int i = 0; i < 10; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(Math.max(sheet.getColumnWidth(i), 2800), 12000));
        }
        sheet.createFreezePane(0, 1);
    }

    private static class HeaderMap {
        private Integer categoryLevel1Column;
        private Integer categoryLevel2Column;
        private Integer categoryLevel3Column;
        private Integer typeColumn;
        private Integer titleColumn;
        private Integer optionsColumn;
        private Integer correctAnswerColumn;
        private Integer analysisColumn;
        private Integer sourceFileColumn;
        private Integer statusColumn;
        private int lastColumn;
        private final Map<String, Integer> optionColumns = new LinkedHashMap<>();

        private boolean isImportSheet() {
            return categoryLevel1Column != null
                    && typeColumn != null
                    && titleColumn != null
                    && correctAnswerColumn != null;
        }
    }

    private record OptionMarker(String optionKey, int markerStart, int contentStart) {
    }
}
