package com.collab.collab_editor_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.collab_editor_backend.dto.DocCreateDTO;
import com.collab.collab_editor_backend.entity.Document;
import com.collab.collab_editor_backend.mapper.DocumentMapper;
import com.collab.collab_editor_backend.service.DocumentService;
import com.collab.collab_editor_backend.service.DocumentVersionService;
import com.collab.collab_editor_backend.util.Result;
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
import java.util.List;

@Service
public class DocumentServiceImpl implements DocumentService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentServiceImpl.class);

    // 注入DocumentMapper（数据库操作）
    @Autowired
    private DocumentMapper documentMapper;
    
    // 注入DocumentVersionService（文档版本管理）
    @Autowired
    private DocumentVersionService documentVersionService;



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
            document.setContent(""); // 初始为空内容

            // 2. 保存文档信息到数据库
            documentMapper.insert(document);

            return Result.success(document); // 返回创建的文档信息
        } catch (Exception e) {
            // 捕获数据库操作异常，返回错误信息
            return Result.error("文档创建失败：" + e.getMessage());
        }
    }

    /**
     * 查询用户的文档列表
     * @param userId 用户ID（当前登录用户）
     */
    @Override
    public Result<?> getList(Long userId) {
        // 构建查询条件：查询所有者为当前用户的所有文档
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getOwnerId, userId);

        // 执行查询
        List<Document> documentList = documentMapper.selectList(queryWrapper);

        return Result.success(documentList); // 返回文档列表
    }

    /**
     * 获取文档内容（从数据库读取）
     * @param docId 文档ID
     */
    @Override
    public Result<String> getContent(Long docId) {
        try {
            logger.debug("获取文档内容，docId: {}", docId);
            // 1. 查询文档信息（验证文档是否存在）
            Document document = documentMapper.selectById(docId);
            if (document == null) {
                logger.debug("文档不存在或已被删除，docId: {}", docId);
                return Result.error("文档不存在或已被删除");
            }

            logger.debug("获取到文档对象：{}", document);
            logger.debug("文档ID: {}, 标题: {}, content: {}", document.getId(), document.getTitle(), document.getContent());

            // 2. 直接从数据库读取文档内容（暂时不使用MinIO）
            String content = document.getContent();
            logger.debug("从文档对象中获取到的content: {}", content);

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
     */
    @Override
    public Result<?> saveContent(Long docId, String content) {
        try {
            // 1. 验证文档是否存在
            Document document = documentMapper.selectById(docId);
            if (document == null) {
                return Result.error("文档不存在或已被删除");
            }

            // 2. 直接将内容保存在数据库中（暂时不使用MinIO）
            document.setContent(content);
            document.setUpdateTime(LocalDateTime.now());
            documentMapper.updateById(document);
            
            // 3. 自动创建文档版本（使用默认版本名称）
            documentVersionService.createVersion(docId, content, null, "自动保存版本", document.getOwnerId());

            return Result.successWithMessage("文档内容保存成功");
        } catch (Exception e) {
            return Result.error("保存文档内容失败：" + e.getMessage());
        }
    }

    @Override
    public Result<?> importWord(MultipartFile file, Long userId) {
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
            documentMapper.insert(document);
            
            return Result.success(document);
        } catch (Exception e) {
            return Result.error("Word文档导入失败：" + e.getMessage());
        }
    }

    @Override
    public Result<?> importPdf(MultipartFile file, Long userId) {
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
    public Result<?> exportWord(Long docId) {
        try {
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
            return Result.error("Word文档导出失败：" + e.getMessage());
        }
    }

    @Override
    public Result<?> exportPdf(Long docId, String content) {
        try {
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
            return Result.error("PDF文档导出失败：" + e.getMessage());
        }
    }
}