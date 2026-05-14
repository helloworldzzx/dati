package com.example.dati_backend.service;

import com.example.dati_backend.dto.UserImportError;
import com.example.dati_backend.dto.UserImportResult;
import com.example.dati_backend.dto.UserRequest;
import com.example.dati_backend.dto.PageResult;
import com.example.dati_backend.entity.SysUser;
import com.example.dati_backend.mapper.SysUserMapper;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final String DEFAULT_PASSWORD = "123456";

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final DataFormatter dataFormatter = new DataFormatter(Locale.CHINA);

    public List<SysUser> listUsers() {
        return userMapper.listAll();
    }

    public PageResult<SysUser> pageUsers(Integer page, Integer size) {
        int safePage = safePage(page);
        int safeSize = safeSize(size);
        return new PageResult<>(
                userMapper.listPage(safeSize, (safePage - 1) * safeSize),
                userMapper.countAll(),
                safePage,
                safeSize
        );
    }

    public SysUser getUser(Long id) {
        SysUser user = userMapper.findById(id);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        return user;
    }

    @Transactional
    public SysUser createUser(UserRequest request) {
        if (request == null || !StringUtils.hasText(request.username())) {
            throw new IllegalArgumentException("Username is required");
        }

        SysUser user = new SysUser();
        user.setUsername(request.username().trim());
        user.setPhone(trimToNull(request.phone()));
        user.setPasswordHash(passwordEncoder.encode(resolveInitialPassword(request.password())));
        user.setRealName(trimToNull(request.realName()));
        user.setRole(defaultText(request.role(), "USER").toUpperCase());
        user.setStatus(defaultText(request.status(), "ENABLED").toUpperCase());
        user.setMustChangePassword(true);
        userMapper.insert(user);
        return getUser(user.getId());
    }

    @Transactional
    public SysUser updateUser(Long id, UserRequest request) {
        SysUser user = getUser(id);
        if (request == null) {
            return user;
        }
        if (StringUtils.hasText(request.username())) {
            user.setUsername(request.username().trim());
        }
        if (request.phone() != null) {
            user.setPhone(trimToNull(request.phone()));
        }
        if (StringUtils.hasText(request.password())) {
            validatePassword(request.password());
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            user.setMustChangePassword(request.mustChangePassword() == null || request.mustChangePassword());
        } else if (request.mustChangePassword() != null) {
            user.setMustChangePassword(request.mustChangePassword());
        }
        if (request.realName() != null) {
            user.setRealName(trimToNull(request.realName()));
        }
        if (StringUtils.hasText(request.role())) {
            user.setRole(request.role().trim().toUpperCase());
        }
        if (StringUtils.hasText(request.status())) {
            user.setStatus(request.status().trim().toUpperCase());
        }
        userMapper.update(user);
        return getUser(id);
    }

    @Transactional
    public void disableUser(Long id) {
        getUser(id);
        userMapper.updateStatus(id, "DISABLED");
    }

    @Transactional
    public void resetPassword(Long id) {
        getUser(id);
        userMapper.updatePassword(id, passwordEncoder.encode(DEFAULT_PASSWORD), true);
    }

    @Transactional
    public UserImportResult importUsers(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is required");
        }

        int totalCount = 0;
        int successCount = 0;
        List<UserImportError> errors = new ArrayList<>();
        Set<String> importedAccounts = new HashSet<>();
        Set<String> importedPhones = new HashSet<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("Excel sheet is empty");
            }
            Sheet sheet = workbook.getSheetAt(0);
            HeaderMap header = readHeader(sheet);
            if (!header.valid()) {
                throw new IllegalArgumentException("Template header is invalid");
            }

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isEmptyRow(row, header)) {
                    continue;
                }
                totalCount++;
                try {
                    UserRequest request = readUserRequest(row, header, importedAccounts, importedPhones);
                    createUser(request);
                    successCount++;
                } catch (Exception exception) {
                    errors.add(new UserImportError(rowIndex + 1, exception.getMessage()));
                }
            }
        } catch (Exception exception) {
            if (totalCount == 0) {
                errors.add(new UserImportError(0, exception.getMessage()));
            } else {
                errors.add(new UserImportError(0, "Excel parse failed: " + exception.getMessage()));
            }
        }

        return new UserImportResult(totalCount, successCount, errors.size(), errors);
    }

    public byte[] buildImportTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("用户导入");
            writeRow(sheet, 0, List.of("账号", "姓名", "手机号", "初始密码", "角色", "状态", "首次登录修改密码"));
            writeRow(sheet, 1, List.of("student001", "张三", "13800000001", "123456", "USER", "ENABLED", "是"));
            writeRow(sheet, 2, List.of("student002", "李四", "13800000002", "123456", "USER", "ENABLED", "是"));
            writeRow(sheet, 3, List.of("teacher001", "管理员示例", "", "123456", "ADMIN", "ENABLED", "是"));
            for (int i = 0; i < 7; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.min(Math.max(sheet.getColumnWidth(i), 3200), 9000));
            }
            sheet.createFreezePane(0, 1);

            Sheet guide = workbook.createSheet("导入说明");
            List<String> lines = List.of(
                    "账号必填且不能重复。",
                    "初始密码为空时默认使用 123456。",
                    "角色可填：USER/用户、ADMIN/管理员，默认 USER。",
                    "状态可填：ENABLED/启用、DISABLED/禁用，默认 ENABLED。",
                    "首次登录修改密码可填：是/否、TRUE/FALSE、1/0，默认 是。"
            );
            for (int i = 0; i < lines.size(); i++) {
                guide.createRow(i).createCell(0).setCellValue(lines.get(i));
            }
            guide.setColumnWidth(0, 13000);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to build Excel template", exception);
        }
    }

    private UserRequest readUserRequest(
            Row row,
            HeaderMap header,
            Set<String> importedAccounts,
            Set<String> importedPhones
    ) {
        String username = cellString(row, header.usernameColumn);
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("账号不能为空");
        }
        username = username.trim();
        if (!importedAccounts.add(username.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Excel 内账号重复");
        }
        if (userMapper.findByAccount(username) != null) {
            throw new IllegalArgumentException("账号已存在");
        }

        String phone = trimToNull(cellString(row, header.phoneColumn));
        if (phone != null) {
            if (!importedPhones.add(phone)) {
                throw new IllegalArgumentException("Excel 内手机号重复");
            }
            if (userMapper.findByAccount(phone) != null) {
                throw new IllegalArgumentException("手机号已存在");
            }
        }

        return new UserRequest(
                username,
                phone,
                trimToNull(cellString(row, header.passwordColumn)),
                trimToNull(cellString(row, header.realNameColumn)),
                normalizeRole(cellString(row, header.roleColumn)),
                normalizeStatus(cellString(row, header.statusColumn)),
                normalizeMustChangePassword(cellString(row, header.mustChangePasswordColumn))
        );
    }

    private HeaderMap readHeader(Sheet sheet) {
        HeaderMap header = new HeaderMap();
        Row row = sheet.getRow(0);
        if (row == null) {
            return header;
        }

        for (int col = 0; col < row.getLastCellNum(); col++) {
            String label = normalizeHeader(cellString(row, col));
            switch (label) {
                case "账号", "用户名", "USERNAME", "ACCOUNT" -> header.usernameColumn = col;
                case "姓名", "真实姓名", "REALNAME", "NAME" -> header.realNameColumn = col;
                case "手机号", "手机", "PHONE", "MOBILE" -> header.phoneColumn = col;
                case "初始密码", "密码", "PASSWORD" -> header.passwordColumn = col;
                case "角色", "ROLE" -> header.roleColumn = col;
                case "状态", "STATUS" -> header.statusColumn = col;
                case "首次登录修改密码", "首次登录需改密码", "MUSTCHANGEPASSWORD" -> header.mustChangePasswordColumn = col;
                default -> {
                }
            }
            if (StringUtils.hasText(label)) {
                header.lastColumn = Math.max(header.lastColumn, col);
            }
        }
        return header;
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

    private String normalizeRole(String value) {
        if (!StringUtils.hasText(value)) {
            return "USER";
        }
        String role = value.trim().toUpperCase(Locale.ROOT);
        return switch (role) {
            case "ADMIN", "管理员" -> "ADMIN";
            case "USER", "用户", "学生" -> "USER";
            default -> throw new IllegalArgumentException("不支持的角色：" + value);
        };
    }

    private String normalizeStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return "ENABLED";
        }
        String status = value.trim().toUpperCase(Locale.ROOT);
        return switch (status) {
            case "ENABLED", "启用", "正常" -> "ENABLED";
            case "DISABLED", "禁用", "停用" -> "DISABLED";
            default -> throw new IllegalArgumentException("不支持的状态：" + value);
        };
    }

    private Boolean normalizeMustChangePassword(String value) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        String text = value.trim().toUpperCase(Locale.ROOT);
        return switch (text) {
            case "TRUE", "YES", "Y", "1", "是", "需要" -> true;
            case "FALSE", "NO", "N", "0", "否", "不需要" -> false;
            default -> throw new IllegalArgumentException("首次登录修改密码只能填是/否");
        };
    }

    private String normalizeHeader(String value) {
        return StringUtils.hasText(value) ? value.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT) : "";
    }

    private String cellString(Row row, Integer col) {
        if (row == null || col == null || col < 0) {
            return "";
        }
        Cell cell = row.getCell(col);
        return cell == null ? "" : dataFormatter.formatCellValue(cell).trim();
    }

    private void writeRow(Sheet sheet, int rowIndex, List<String> values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.size(); i++) {
            row.createCell(i).setCellValue(values.get(i));
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String resolveInitialPassword(String password) {
        if (!StringUtils.hasText(password)) {
            return DEFAULT_PASSWORD;
        }
        validatePassword(password);
        return password;
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password) || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
    }

    private int safePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int safeSize(Integer size) {
        return size == null || size < 1 ? 20 : Math.min(size, 200);
    }

    private static class HeaderMap {
        private Integer usernameColumn;
        private Integer realNameColumn;
        private Integer phoneColumn;
        private Integer passwordColumn;
        private Integer roleColumn;
        private Integer statusColumn;
        private Integer mustChangePasswordColumn;
        private int lastColumn;

        private boolean valid() {
            return usernameColumn != null;
        }
    }
}
