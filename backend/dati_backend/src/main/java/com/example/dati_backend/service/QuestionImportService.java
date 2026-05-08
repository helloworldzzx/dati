package com.example.dati_backend.service;

import com.example.dati_backend.dto.QuestionOptionRequest;
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
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class QuestionImportService {
    private static final int COL_CATEGORY_LEVEL_1 = 0;
    private static final int COL_CATEGORY_LEVEL_2 = 1;
    private static final int COL_CATEGORY_LEVEL_3 = 2;
    private static final int COL_TYPE = 3;
    private static final int COL_TITLE = 4;
    private static final int COL_OPTION_A = 5;
    private static final int COL_OPTION_E = 9;
    private static final int COL_CORRECT_ANSWER = 10;
    private static final int COL_ANALYSIS = 11;
    private static final int COL_SOURCE_FILE = 12;
    private static final int COL_STATUS = 13;

    private final QuestionImportBatchMapper batchMapper;
    private final QuestionImportErrorMapper errorMapper;
    private final QuestionCategoryMapper categoryMapper;
    private final QuestionService questionService;
    private final ObjectMapper objectMapper;
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

    @Transactional
    public QuestionImportBatch importQuestions(MultipartFile file, Long importedBy) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is required");
        }

        QuestionImportBatch batch = createBatch(file.getOriginalFilename(), importedBy);
        int totalCount = 0;
        int successCount = 0;
        int failCount = 0;

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Excel sheet is empty");
            }

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isEmptyRow(row)) {
                    continue;
                }
                totalCount++;
                try {
                    importRow(row, importedBy);
                    successCount++;
                } catch (Exception exception) {
                    failCount++;
                    saveError(batch.getId(), rowIndex + 1, exception.getMessage(), rawRowJson(row));
                }
            }
        } catch (Exception exception) {
            failCount++;
            saveError(batch.getId(), 0, exception.getMessage(), "[]");
        }

        batch.setTotalCount(totalCount);
        batch.setSuccessCount(successCount);
        batch.setFailCount(failCount);
        batch.setStatus(resolveStatus(successCount, failCount));
        batchMapper.updateResult(batch);
        return batchMapper.findById(batch.getId());
    }

    public byte[] buildTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("questions");
            Row header = sheet.createRow(0);
            List<String> headers = List.of(
                    "\u4E00\u7EA7\u5206\u7C7B",
                    "\u4E8C\u7EA7\u5206\u7C7B",
                    "\u4E09\u7EA7\u5206\u7C7B",
                    "\u9898\u578B",
                    "\u9898\u5E72",
                    "A",
                    "B",
                    "C",
                    "D",
                    "E",
                    "\u6B63\u786E\u7B54\u6848",
                    "\u89E3\u6790",
                    "\u6765\u6E90\u6587\u4EF6",
                    "\u72B6\u6001"
            );
            for (int i = 0; i < headers.size(); i++) {
                header.createCell(i).setCellValue(headers.get(i));
            }

            Row example = sheet.createRow(1);
            example.createCell(0).setCellValue("\u601D\u60F3\u653F\u6CBB\u6559\u80B2");
            example.createCell(1).setCellValue("\u9AD8\u6821\u8F85\u5BFC\u5458");
            example.createCell(2).setCellValue("\u961F\u4F0D\u5EFA\u8BBE");
            example.createCell(3).setCellValue("\u5355\u9009");
            example.createCell(4).setCellValue("\u8F85\u5BFC\u5458\u5E94\u5F53\u52AA\u529B\u6210\u4E3A\u5B66\u751F\u6210\u957F\u6210\u624D\u7684\u548C\u5065\u5EB7\u751F\u6D3B\u7684\u77E5\u5FC3\u670B\u53CB\u3002");
            example.createCell(5).setCellValue("\u7231\u56FD\u5B88\u6CD5");
            example.createCell(6).setCellValue("\u656C\u4E1A\u7231\u751F");
            example.createCell(7).setCellValue("\u80B2\u4EBA\u4E3A\u672C");
            example.createCell(8).setCellValue("\u7EC8\u8EAB\u5B66\u4E60");
            example.createCell(9).setCellValue("\u4E3A\u4EBA\u5E08\u8868");
            example.createCell(10).setCellValue("B");
            example.createCell(11).setCellValue("\u6B63\u786E\u7B54\u6848\u4E3A B\u3002");
            example.createCell(12).setCellValue("\u666E\u901A\u9AD8\u7B49\u5B66\u6821\u8F85\u5BFC\u5458\u961F\u4F0D\u5EFA\u8BBE\u89C4\u5B9A");
            example.createCell(13).setCellValue("ENABLED");

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to build Excel template", exception);
        }
    }

    @Transactional
    public void saveError(Long batchId, Integer rowNo, String message, String rawData) {
        QuestionImportError error = new QuestionImportError();
        error.setBatchId(batchId);
        error.setRowNo(rowNo);
        error.setErrorMessage(message);
        error.setRawData(rawData);
        errorMapper.insert(error);
    }

    private void importRow(Row row, Long importedBy) {
        Long categoryId = resolveCategoryId(row);
        String type = normalizeQuestionType(cellString(row, COL_TYPE));
        String title = cellString(row, COL_TITLE);
        if (!StringUtils.hasText(title)) {
            throw new IllegalArgumentException("Question title is required");
        }

        String correctAnswer = normalizeCorrectAnswer(type, cellString(row, COL_CORRECT_ANSWER));
        if (!"ANALYSIS".equals(type) && !StringUtils.hasText(correctAnswer)) {
            throw new IllegalArgumentException("Correct answer is required");
        }
        List<QuestionOptionRequest> options = parseOptions(row, type, correctAnswer);
        if (("SINGLE".equals(type) || "MULTIPLE".equals(type)) && options.size() < 2) {
            throw new IllegalArgumentException("Objective questions require at least two options");
        }
        QuestionRequest request = new QuestionRequest(
                categoryId,
                type,
                title,
                correctAnswer,
                cellString(row, COL_ANALYSIS),
                cellString(row, COL_SOURCE_FILE),
                StringUtils.hasText(cellString(row, COL_STATUS)) ? cellString(row, COL_STATUS).toUpperCase(Locale.ROOT) : "ENABLED",
                importedBy,
                options
        );
        questionService.createQuestion(request);
    }

    private Long resolveCategoryId(Row row) {
        Long parentId = null;
        Long categoryId = null;
        boolean metBlank = false;

        for (int i = COL_CATEGORY_LEVEL_1; i <= COL_CATEGORY_LEVEL_3; i++) {
            String name = cellString(row, i);
            if (!StringUtils.hasText(name)) {
                metBlank = true;
                continue;
            }
            if (metBlank) {
                throw new IllegalArgumentException("Category levels cannot be skipped");
            }
            int level = i + 1;
            QuestionCategory category = getOrCreateCategory(parentId, name, level);
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

    private List<QuestionOptionRequest> parseOptions(Row row, String type, String correctAnswer) {
        if ("ANALYSIS".equals(type)) {
            return List.of();
        }
        if ("JUDGE".equals(type) && noOptionCells(row)) {
            return List.of(
                    new QuestionOptionRequest("TRUE", "\u6B63\u786E", "TRUE".equals(correctAnswer), 0),
                    new QuestionOptionRequest("FALSE", "\u9519\u8BEF", "FALSE".equals(correctAnswer), 1)
            );
        }

        List<String> correctKeys = splitAnswer(correctAnswer);
        List<QuestionOptionRequest> options = new ArrayList<>();
        for (int col = COL_OPTION_A; col <= COL_OPTION_E; col++) {
            String content = cellString(row, col);
            if (!StringUtils.hasText(content)) {
                continue;
            }
            String optionKey = String.valueOf((char) ('A' + (col - COL_OPTION_A)));
            options.add(new QuestionOptionRequest(
                    optionKey,
                    content,
                    correctKeys.contains(optionKey),
                    col - COL_OPTION_A
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

    private boolean noOptionCells(Row row) {
        for (int col = COL_OPTION_A; col <= COL_OPTION_E; col++) {
            if (StringUtils.hasText(cellString(row, col))) {
                return false;
            }
        }
        return true;
    }

    private boolean isEmptyRow(Row row) {
        if (row == null) {
            return true;
        }
        for (int col = COL_CATEGORY_LEVEL_1; col <= COL_STATUS; col++) {
            if (StringUtils.hasText(cellString(row, col))) {
                return false;
            }
        }
        return true;
    }

    private String rawRowJson(Row row) {
        try {
            List<String> values = new ArrayList<>();
            for (int col = COL_CATEGORY_LEVEL_1; col <= COL_STATUS; col++) {
                values.add(cellString(row, col));
            }
            return objectMapper.writeValueAsString(values);
        } catch (Exception exception) {
            return "[]";
        }
    }

    private String cellString(Row row, int col) {
        if (row == null) {
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
}
