package com.collab.collab_editor_backend.handler;

import java.util.List;
import java.util.ArrayList;

/**
 * 操作转换(OT)算法实现
 * 用于解决协作编辑中的并发冲突问题
 */
public class OTAlgorithm {

    /**
     * 操作类型枚举
     */
    public enum OperationType {
        INSERT, DELETE
    }

    /**
     * 编辑操作类
     */
    public static class Operation {
        private final OperationType type;
        private final int position;
        private final String content;
        private final int version;

        public Operation(OperationType type, int position, String content, int version) {
            this.type = type;
            this.position = position;
            this.content = content;
            this.version = version;
        }

        public OperationType getType() {
            return type;
        }

        public int getPosition() {
            return position;
        }

        public String getContent() {
            return content;
        }

        public int getVersion() {
            return version;
        }

        @Override
        public String toString() {
            return "Operation{type=" + type + ", position=" + position + ", content='" + content + "', version=" + version + "}";
        }
    }

    /**
     * 转换操作：将操作op1转换为与操作op2兼容
     * @param op1 要转换的操作
     * @param op2 已经应用的操作
     * @return 转换后的操作
     */
    public static Operation transform(Operation op1, Operation op2) {
        if (op1.getVersion() != op2.getVersion()) {
            throw new IllegalArgumentException("Operations must be on the same version");
        }

        int newPosition = op1.getPosition();
        String newContent = op1.getContent();

        // 处理操作位置的转换
        if (op2.getType() == OperationType.INSERT) {
            // 插入操作会影响后续操作的位置
            if (op2.getPosition() <= op1.getPosition()) {
                newPosition += op2.getContent().length();
            }
        } else if (op2.getType() == OperationType.DELETE) {
            int op2Start = op2.getPosition();
            int op2End = op2Start + op2.getContent().length();
            int op1Start = op1.getPosition();
            int op1End = op1Start;
            
            if (op1.getType() == OperationType.DELETE) {
                op1End += op1.getContent().length();
            }

            // 处理删除操作对当前操作位置的影响
            if (op1Start < op2Start) {
                // 当前操作在删除操作之前，位置不变
            } else if (op1Start >= op2End) {
                // 当前操作在删除操作之后，位置需要调整
                newPosition -= op2.getContent().length();
            } else {
                // 当前操作与删除操作重叠
                if (op1.getType() == OperationType.INSERT) {
                    // 如果是插入操作，需要将位置调整到删除操作之前
                    newPosition = op2Start;
                } else if (op1.getType() == OperationType.DELETE) {
                    // 如果是删除操作，需要调整删除内容
                    if (op1End <= op2End) {
                        // 当前删除操作完全被包含在op2中，整个操作被抵消
                        return new Operation(OperationType.DELETE, newPosition, "", op1.getVersion() + 1);
                    } else {
                        // 当前删除操作部分重叠，只保留不重叠的部分
                        int overlap = op2End - op1Start;
                        newContent = op1.getContent().substring(overlap);
                        newPosition = op2Start;
                    }
                }
            }
        }

        // 返回转换后的操作
        return new Operation(op1.getType(), newPosition, newContent, op1.getVersion() + 1);
    }

    /**
     * 应用操作到内容
     * @param content 当前内容
     * @param operation 要应用的操作
     * @return 应用操作后的新内容
     */
    public static String applyOperation(String content, Operation operation) {
        int contentLength = content.length();
        
        if (operation.getType() == OperationType.INSERT) {
            // 确保插入位置有效
            int insertPos = Math.max(0, Math.min(operation.getPosition(), contentLength));
            String insertContent = operation.getContent();
            
            // 对于空内容或空插入优化
            if (contentLength == 0) {
                return insertContent;
            }
            if (insertContent.isEmpty()) {
                return content;
            }
            
            // 使用StringBuilder进行高效的字符串插入
            StringBuilder sb = new StringBuilder(contentLength + insertContent.length());
            sb.append(content, 0, insertPos);
            sb.append(insertContent);
            if (insertPos < contentLength) {
                sb.append(content, insertPos, contentLength);
            }
            return sb.toString();
        } else if (operation.getType() == OperationType.DELETE) {
            int start = operation.getPosition();
            int deleteLength = operation.getContent().length();
            int end = Math.min(start + deleteLength, contentLength);
            
            // 对于无需删除的情况优化
            if (start >= end || start >= contentLength) {
                return content;
            }
            
            // 使用StringBuilder进行高效的字符串删除
            StringBuilder sb = new StringBuilder(contentLength - (end - start));
            if (start > 0) {
                sb.append(content, 0, start);
            }
            if (end < contentLength) {
                sb.append(content, end, contentLength);
            }
            return sb.toString();
        }
        
        return content;
    }

    /**
     * 批量转换操作列表
     * @param operations 要转换的操作列表
     * @param appliedOperations 已经应用的操作列表
     * @return 转换后的操作列表
     */
    public static List<Operation> transformOperations(List<Operation> operations, List<Operation> appliedOperations) {
        List<Operation> transformed = new ArrayList<>(operations);
        
        for (Operation appliedOp : appliedOperations) {
            for (int i = 0; i < transformed.size(); i++) {
                Operation op = transformed.get(i);
                Operation transformedOp = transform(op, appliedOp);
                
                // 如果转换后的操作是空删除操作，移除它
                if (transformedOp.getType() == OperationType.DELETE && transformedOp.getContent().isEmpty()) {
                    transformed.remove(i);
                    i--;
                } else {
                    transformed.set(i, transformedOp);
                }
            }
        }
        
        return transformed;
    }
}