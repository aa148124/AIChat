package com.example.aidemo.ai.memory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息实体类
 */
@Data
@TableName("chat_message")
public class ChatMessageEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID (memoryId/userId)
     */
    private String memoryId;

    /**
     * 消息类型: USER / ASSISTANT / SYSTEM
     */
    private String messageType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息顺序索引
     */
    @TableField("`index`")
    private Integer index;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
