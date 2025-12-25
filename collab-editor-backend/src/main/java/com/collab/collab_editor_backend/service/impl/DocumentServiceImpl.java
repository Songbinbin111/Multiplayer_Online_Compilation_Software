package com.collab.collab_editor_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.collab_editor_backend.dto.DocCreateDTO;
import com.collab.collab_editor_backend.entity.Document;
import com.collab.collab_editor_backend.mapper.DocumentMapper;
import com.collab.collab_editor_backend.mapper.DocPermissionMapper;
import com.collab.collab_editor_backend.mapper.UserMapper;
import com.collab.collab_editor_backend.entity.User;
import com.collab.collab_editor_backend.service.DocumentService;
import com.collab.collab_editor_backend.service.DocumentVersionService;
import com.collab.collab_editor_backend.service.DocPermissionService;
import com.collab.collab_editor_backend.service.OperationLogService;
import com.collab.collab_editor_backend.util.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DocumentServiceImpl implements DocumentService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentServiceImpl.class);

    // 注入DocumentMapper（数据库操作）
    @Autowired
    private DocumentMapper documentMapper;
    
    // 注入DocumentVersionService（文档版本管理）
    @Autowired
    private DocumentVersionService documentVersionService;
    
    // 注入DocPermissionService（文档权限管理）
    @Autowired
    private DocPermissionService docPermissionService;
    
    // 注入DocPermissionMapper（文档权限数据库操作）
    @Autowired
    private DocPermissionMapper docPermissionMapper;
    
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OperationLogService operationLogService;
    
    // ObjectMapper用于将操作内容转换为JSON格式
    private final ObjectMapper objectMapper = new ObjectMapper();



    /**
     * 创建文档
     * @param dto 文档创建参数（标题）
     * @param userId 文档所有者ID（当前登录用户）
     */
    @Override
    public Result<?> create(DocCreateDTO dto, Long userId) {
        try {
            // 1. 构建文档实体
            Document document = new Document();
            document.setTitle(dto.getTitle()); // 设置文档标题
            document.setOwnerId(userId); // 设置所有者ID
            document.setContent(dto.getContent() != null ? dto.getContent() : ""); // 设置文档内容
            document.setCategory(dto.getCategory()); // 设置文档分类
            document.setTags(dto.getTags()); // 设置文档标签
            document.setCreateTime(LocalDateTime.now()); // 设置创建时间
            document.setUpdateTime(LocalDateTime.now()); // 设置更新时间

            // 2. 保存文档信息到数据库
            documentMapper.insert(document);
            
            // 3. 记录操作日志
            try {
                String content = objectMapper.writeValueAsString(dto);
                operationLogService.recordLog(
                    userId, // 用户ID
                    "未知用户名", // 用户名
                    "create_document", // 操作类型
                    "创建文档: " + document.getId(), // 操作内容
                    "127.0.0.1", // IP地址
                    "", // User-Agent
                    true, // 操作成功
                    null // 错误信息
                );
            } catch (JsonProcessingException e) {
                logger.error("记录操作日志失败: {}", e.getMessage());
            }

            return Result.success(document); // 返回创建的文档信息
        } catch (Exception e) {
            // 捕获数据库操作异常，返回错误信息
            return Result.error("文档创建失败：" + e.getMessage());
        }
    }

    /**
     * 查询所有文档列表（共享模式，所有用户可见）
     * @param userId 用户ID（当前登录用户，虽然在共享模式下不用于过滤，但可用于日志等）
     */
    @Override
    public Result<?> getList(Long userId) {
        try {
            // 获取所有文档
            LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
            
            // 按更新时间降序排序
            queryWrapper.orderByDesc(Document::getUpdateTime);
            
            List<Document> allDocuments = documentMapper.selectList(queryWrapper);

            return Result.success(allDocuments); // 返回文档列表
        } catch (Exception e) {
            logger.error("获取文档列表失败，用户ID: {}", userId, e);
            return Result.error("获取文档列表失败：" + e.getMessage());
        }
    }

    @Override
    public Result<?> getListByCategory(Long userId, String category) {
        try {
            // 获取指定分类的所有文档
            LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Document::getCategory, category);
            
            // 按更新时间降序排序
            queryWrapper.orderByDesc(Document::getUpdateTime);
            
            List<Document> allDocuments = documentMapper.selectList(queryWrapper);

            return Result.success(allDocuments); // 返回文档列表
        } catch (Exception e) {
            logger.error("获取分类文档列表失败，用户ID: {}, 分类: {}", userId, category, e);
            return Result.error("获取分类文档列表失败：" + e.getMessage());
        }
    }

    @Override
    public Result<?> getCategories(Long userId) {
        try {
            // 获取所有文档的分类
            LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.select(Document::getCategory)
                    .groupBy(Document::getCategory);
                    
            List<String> categories = documentMapper.selectObjs(queryWrapper).stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toList());

            // 按分类名称排序
            categories.sort(String::compareTo);

            return Result.success(categories); // 返回分类列表
        } catch (Exception e) {
            logger.error("获取文档分类失败，用户ID: {}", userId, e);
            return Result.error("获取文档分类失败：" + e.getMessage());
        }
    }

    /**
     * 获取文档内容（从数据库读取）
     * @param docId 文档ID
     * @param userId 用户ID
     */
    @Override
    public Result<String> getContent(Long docId, Long userId) {
        try {
            logger.debug("获取文档内容，docId: {}, userId: {}", docId, userId);
            // 1. 查询文档信息（验证文档是否存在）
            Document document = documentMapper.selectById(docId);
            if (document == null) {
                logger.debug("文档不存在或已被删除，docId: {}", docId);
                return Result.error("文档不存在或已被删除");
            }
            
            // 2. 共享模式下，所有用户都有权限查看
            // if (!docPermissionService.hasViewPermission(docId, userId)) {
            //     logger.debug("用户没有查看权限，docId: {}, userId: {}", docId, userId);
            //     return Result.error("您没有权限查看此文档");
            // }

            logger.debug("获取到文档对象：{}", document);
            logger.debug("文档ID: {}, 标题: {}, content: {}", document.getId(), document.getTitle(), document.getContent());

            // 3. 直接从数据库读取文档内容（暂时不使用MinIO）
            String content = document.getContent();
            logger.debug("从文档对象中获取到的content: {}", content);
            
            // 4. 记录操作日志
            try {
                String logContent = objectMapper.writeValueAsString("{\"action\":\"view\",\"docId\":\"" + docId + "\"}");
                operationLogService.recordLog(
                    userId, // 用户ID
                    "未知用户名", // 用户名
                    "view_document", // 操作类型
                    "查看文档: " + docId, // 操作内容
                    "127.0.0.1", // IP地址
                    "", // User-Agent
                    true, // 操作成功
                    null // 错误信息
                );
            } catch (JsonProcessingException e) {
                logger.error("记录操作日志失败: {}", e.getMessage());
            }

            Result<String> result = Result.success(content);
            logger.debug("返回的Result对象：{}", result);
            logger.debug("Result对象的data字段：{}", result.getData());

            return result; // 返回文档内容
        } catch (Exception e) {
            logger.error("获取文档内容失败：", e);
            return Result.error("获取文档内容失败：" + e.getMessage());
        }
    }

    /**
     * 保存文档内容（直接保存在数据库中）
     * @param docId 文档ID
     * @param content 最新的文档内容
     * @param userId 用户ID
     */
    @Override
    public Result<?> saveContent(Long docId, String content, Long userId) {
        try {
            // 1. 验证文档是否存在
            Document document = documentMapper.selectById(docId);
            if (document == null) {
                return Result.error("文档不存在或已被删除");
            }

            // 2. 共享模式下，所有用户都有权限修改
            // if (!docPermissionService.hasEditPermission(docId, userId) && !docPermissionService.hasAdminPermission(docId, userId)) {
            //     return Result.error("您没有权限修改此文档");
            // }

            // 3. 直接将内容保存在数据库中（暂时不使用MinIO）
            document.setContent(content);
            document.setUpdateTime(LocalDateTime.now());
            documentMapper.updateById(document);
            
            // 4. 自动创建文档版本（使用默认版本名称）
            documentVersionService.createVersion(docId, content, null, "自动保存版本", document.getOwnerId());
            
            // 5. 记录操作日志
            try {
                String logContent = objectMapper.writeValueAsString("{\"action\":\"update_content\",\"docId\":\"" + docId + "\",\"content_length\":" + content.length() + "}");
                operationLogService.recordLog(
                    userId, // 用户ID
                    "未知用户名", // 用户名
                    "update_document", // 操作类型
                    "更新文档内容: " + docId, // 操作内容
                    "127.0.0.1", // IP地址
                    "", // User-Agent
                    true, // 操作成功
                    null // 错误信息
                );
            } catch (JsonProcessingException e) {
                logger.error("记录操作日志失败: {}", e.getMessage());
            }

            return Result.successWithMessage("文档内容保存成功");
        } catch (Exception e) {
            return Result.error("保存文档内容失败：" + e.getMessage());
        }
    }

    @Override
    public Result<?> importWord(MultipartFile file, Long userId, String category) {
        try {
            String content = "";
            String fileName = file.getOriginalFilename();
            
            // 根据文件扩展名选择合适的处理方式
            if (fileName.endsWith(".doc")) {
                // 处理旧版Word文档
                try (HWPFDocument doc = new HWPFDocument(file.getInputStream());
                     WordExtractor extractor = new WordExtractor(doc)) {
                    content = extractor.getText();
                }
            } else if (fileName.endsWith(".docx")) {
                // 处理新版Word文档
                try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
                    StringBuilder sb = new StringBuilder();
                    for (XWPFParagraph paragraph : doc.getParagraphs()) {
                        sb.append(paragraph.getText()).append("\n");
                    }
                    content = sb.toString();
                }
            } else {
                return Result.error("不支持的Word文件格式");
            }
            
            // 创建新文档并保存内容
            Document document = new Document();
            document.setTitle(fileName.replaceFirst(".[^.]+$", ""));
            document.setOwnerId(userId);
            document.setContent(content);
            document.setCategory(category);
            documentMapper.insert(document);
            
            return Result.success(document);
        } catch (Exception e) {
            return Result.error("Word文档导入失败：" + e.getMessage());
        }
    }

    @Override
    public Result<?> importPdf(MultipartFile file, Long userId, String category) {
        try {
            // 使用PDFBox提取PDF内容
            PDDocument document = null;
            try {
                document = Loader.loadPDF(file.getBytes());
                PDFTextStripper stripper = new PDFTextStripper();
                String content = stripper.getText(document);
                
                // 创建新文档
                Document doc = new Document();
                doc.setTitle(file.getOriginalFilename().replaceFirst(".[^.]+$", ""));
                doc.setContent(content);
                doc.setOwnerId(userId);
                doc.setCategory(category);
                doc.setCreateTime(LocalDateTime.now());
                doc.setUpdateTime(LocalDateTime.now());
                
                documentMapper.insert(doc);
                
                return Result.success(doc);
            } finally {
                if (document != null) {
                    document.close();
                }
            }
        } catch (Exception e) {
            return Result.error("PDF文档导入失败：" + e.getMessage());
        }
    }

    @Override
    public Result<?> exportWord(Long docId, Long userId) {
        try {
            // 共享模式下，所有用户都有权限导出
            // if (!docPermissionService.hasViewPermission(docId, userId)) {
            //     logger.error("导出失败：用户无权限查看该文档，文档ID: {}, 用户ID: {}", docId, userId);
            //     return Result.error("无权限查看该文档");
            // }
            
            // 获取文档信息
            Document document = documentMapper.selectById(docId);
            if (document == null) {
                return Result.error("文档不存在或已被删除");
            }
            
            // 创建Word文档
            XWPFDocument doc = new XWPFDocument();
            
            // 添加内容
            XWPFParagraph paragraph = doc.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText(document.getContent());
            
            // 将文档转换为字节数组
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            out.close();
            doc.close();
            
            // 构建结果
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            return Result.successWithMessage(document.getTitle() + ".docx", in);
        } catch (Exception e) {
            logger.error("Word文档导出失败，文档ID: {}", docId, e);
            return Result.error("Word文档导出失败：" + e.getMessage());
        }
    }

    @Override
    public Result<?> exportPdf(Long docId, String content, Long userId) {
        try {
            // 检查用户是否有查看权限
            if (!docPermissionService.hasViewPermission(docId, userId)) {
                logger.error("导出失败：用户无权限查看该文档，文档ID: {}, 用户ID: {}", docId, userId);
                return Result.error("无权限查看该文档");
            }
            
            // 获取文档信息
            Document document = documentMapper.selectById(docId);
            if (document == null) {
                return Result.error("文档不存在或已被删除");
            }
            
            // 如果没有提供内容，使用文档当前内容
            if (content == null) {
                content = document.getContent();
            }
            
            // 由于PDFBox 3.0 API变化较大，这里简化实现
            // 直接返回文本内容，前端可以使用更适合的库来生成PDF
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(content.getBytes());
            out.close();
            
            // 构建结果
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            return Result.successWithMessage(document.getTitle() + ".pdf", in);
        } catch (Exception e) {
            logger.error("PDF文档导出失败，文档ID: {}", docId, e);
            return Result.error("PDF文档导出失败：" + e.getMessage());
        }
    }

    @Override
    public Result<?> deleteDocument(Long docId, Long userId) {
        try {
            // 1. 验证文档是否存在
            Document document = documentMapper.selectById(docId);
            if (document == null) {
                return Result.error("文档不存在或已被删除");
            }

            // 2. 共享模式下，移除删除权限限制
            // if (!document.getOwnerId().equals(userId)) {
            //     return Result.error("您没有权限删除此文档，只有文档所有者可以删除");
            // }

            // 3. 删除文档
            documentMapper.deleteById(docId);
            
            // 4. 记录操作日志
            try {
                operationLogService.recordLog(
                    userId, 
                    "未知用户名", 
                    "delete_document", 
                    "删除文档: " + docId, 
                    "127.0.0.1", 
                    "", 
                    true, 
                    null
                );
            } catch (Exception e) {
                logger.error("记录操作日志失败: {}", e.getMessage());
            }

            return Result.successWithMessage("文档删除成功");
        } catch (Exception e) {
            logger.error("删除文档失败，文档ID: {}", docId, e);
            return Result.error("删除文档失败：" + e.getMessage());
        }
    }

    /**
     * 搜索文档
     * @param userId 用户ID（当前登录用户）
     * @param keyword 搜索关键字
     * @param tags 标签
     * @param author 作者用户名
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param sortField 排序字段
     * @param sortOrder 排序顺序
     */
    @Override
    public Result<?> search(Long userId, String keyword, String tags, String author, LocalDateTime startTime, LocalDateTime endTime, String sortField, String sortOrder) {
        try {
            // 共享模式下，不需要过滤权限
            // 获取用户有权限访问的所有文档ID列表
            // List<Long> permissionDocIds = docPermissionService.getDocIdsByUserId(userId, null); // null表示所有权限类型
            
            // 构建查询条件
            LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
            
            // 共享模式下，不需要权限控制
            // 基本权限控制：(用户是所有者 OR 用户有权限)
            /*
            queryWrapper.and(wrapper -> 
                wrapper.eq(Document::getOwnerId, userId)
                       .or(w -> {
                           if (permissionDocIds != null && !permissionDocIds.isEmpty()) {
                               w.in(Document::getId, permissionDocIds);
                           } else {
                               // 如果没有额外权限文档，为了使OR逻辑正确（Owner OR False），这里不需要做任何事，
                               // 但为了避免生成的SQL错误，加一个永远为假的条件或者什么都不加
                               // MybatisPlus如果in的集合为空，通常会生成 (id IN ()) 这在某些DB是错的，或者直接忽略
                               // 这里的逻辑： (ownerId = userId OR (false)) -> 只要 ownerId = userId
                               // 所以如果permissionDocIds为空，不加这个OR分支即可。
                               // 但上面的写法已经进入了OR分支。
                               // 修正逻辑：
                               w.apply("1=2"); // 永远为假，相当于没有共享文档
                           }
                       })
            );
            */

            // 添加关键字搜索条件
            if (StringUtils.hasText(keyword)) {
                final String searchKeyword = keyword.trim();
                queryWrapper.and(wrapper -> 
                    wrapper.like(Document::getTitle, searchKeyword)
                           .or().like(Document::getContent, searchKeyword)
                );
            }

            // 添加标签搜索
            if (StringUtils.hasText(tags)) {
                final String searchTag = tags.trim();
                queryWrapper.like(Document::getTags, searchTag);
            }

            // 添加作者搜索
            if (StringUtils.hasText(author)) {
                LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
                userQuery.like(User::getUsername, author.trim());
                List<User> users = userMapper.selectList(userQuery);
                if (users.isEmpty()) {
                    return Result.success(new ArrayList<>()); // 找不到作者，直接返回空
                }
                List<Long> authorIds = users.stream().map(User::getId).collect(Collectors.toList());
                queryWrapper.in(Document::getOwnerId, authorIds);
            }

            // 添加时间范围过滤条件
            if (startTime != null && endTime != null) {
                queryWrapper.and(wrapper -> 
                    wrapper.between(Document::getCreateTime, startTime, endTime)
                           .or().between(Document::getUpdateTime, startTime, endTime)
                );
            } else if (startTime != null) {
                queryWrapper.and(wrapper -> 
                    wrapper.ge(Document::getCreateTime, startTime)
                           .or().ge(Document::getUpdateTime, startTime)
                );
            } else if (endTime != null) {
                queryWrapper.and(wrapper -> 
                    wrapper.le(Document::getCreateTime, endTime)
                           .or().le(Document::getUpdateTime, endTime)
                );
            }

            // 排序处理
            boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
            if ("createTime".equals(sortField)) {
                queryWrapper.orderBy(true, isAsc, Document::getCreateTime);
            } else if ("title".equals(sortField)) {
                queryWrapper.orderBy(true, isAsc, Document::getTitle);
            } else {
                // 默认按更新时间排序
                queryWrapper.orderBy(true, isAsc, Document::getUpdateTime);
            }

            // 执行查询
            List<Document> documentList = documentMapper.selectList(queryWrapper);

            return Result.success(documentList);
        } catch (Exception e) {
            return Result.error("文档搜索失败：" + e.getMessage());
        }
    }
}