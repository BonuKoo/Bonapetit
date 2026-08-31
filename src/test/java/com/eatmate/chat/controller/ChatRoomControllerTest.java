package com.eatmate.chat.controller;

import com.eatmate.chat.dto.ChatHistoryResponse;
import com.eatmate.chat.dto.ChatMessageResponse;
import com.eatmate.chat.redisDao.ChatRoomRedisRepository;
import com.eatmate.chat.service.ChatHistoryService;
import com.eatmate.dao.repository.chatroom.ChatRoomRepository;
import com.eatmate.domain.entity.chat.ChatMessage;
import com.eatmate.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatRoomController.class)
class ChatRoomControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ChatHistoryService chatHistoryService;
    @MockBean private ChatRoomRedisRepository chatRoomRedisRepository;
    @MockBean private ChatRoomRepository chatRoomRepository;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(username = "oauth-1")
    @DisplayName("내역 조회 응답 JSON 구조를 확인한다")
    void returnsHistoryJson() throws Exception {
        given(chatHistoryService.getHistory(eq("room-1"), isNull(), isNull(), eq("oauth-1")))
                .willReturn(new ChatHistoryResponse(
                        List.of(new ChatMessageResponse(1L, "테스터", "안녕하세요",
                                ChatMessage.MessageType.TALK, LocalDateTime.of(2026, 8, 31, 10, 0))),
                        1L,
                        true));

        mockMvc.perform(get("/chat/room/{roomId}/messages", "room-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].id").value(1))
                .andExpect(jsonPath("$.messages[0].sender").value("테스터"))
                .andExpect(jsonPath("$.messages[0].message").value("안녕하세요"))
                .andExpect(jsonPath("$.messages[0].type").value("TALK"))
                .andExpect(jsonPath("$.nextCursor").value(1))
                .andExpect(jsonPath("$.hasMore").value(true));
    }

    @Test
    @WithMockUser(username = "oauth-1")
    @DisplayName("before/size 파라미터가 서비스로 그대로 전달된다")
    void passesCursorParameters() throws Exception {
        given(chatHistoryService.getHistory(anyString(), anyLong(), anyInt(), anyString()))
                .willReturn(new ChatHistoryResponse(List.of(), null, false));

        mockMvc.perform(get("/chat/room/{roomId}/messages", "room-1")
                        .param("before", "42")
                        .param("size", "20"))
                .andExpect(status().isOk());

        verify(chatHistoryService).getHistory("room-1", 42L, 20, "oauth-1");
    }

    @Test
    @WithMockUser(username = "outsider")
    @DisplayName("멤버가 아니면 403이다")
    void nonMemberGetsForbidden() throws Exception {
        given(chatHistoryService.getHistory(anyString(), isNull(), isNull(), anyString()))
                .willThrow(new AccessDeniedException("이 채팅방의 멤버가 아닙니다."));

        mockMvc.perform(get("/chat/room/{roomId}/messages", "room-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증되지 않으면 조회할 수 없다")
    void unauthenticatedIsRejected() throws Exception {
        mockMvc.perform(get("/chat/room/{roomId}/messages", "room-1"))
                .andExpect(status().is3xxRedirection());
    }
}
