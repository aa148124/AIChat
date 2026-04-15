package com.example.aidemo.ai.memory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aidemo.ai.memory.entity.ChatMessageEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 聊天消息Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessageEntity> {
}
