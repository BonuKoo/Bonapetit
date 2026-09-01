package com.eatmate.notice.controller;

import com.eatmate.notice.service.NoticeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 공지 삭제는 GET으로 실행되면 안 된다.
 *
 * <p>상태를 바꾸는 작업이 GET에 있으면 링크 프리페치나 {@code <img src="...">} 같은 것으로도
 * 실행된다. CSRF가 꺼져 있어 더 그렇다. 관리자 인증이 걸려 있어 아무나 부르지는 못하지만,
 * <b>관리자가</b> 그런 페이지를 열기만 해도 삭제가 일어난다.
 *
 * <p>ISS-11 문서에는 "크롤러 접근만으로 삭제가 발생할 수 있다"고 적혀 있었는데 그건 과장이다.
 * {@code @PreAuthorize("hasRole('ROLE_ADMIN')")}가 걸려 있어 크롤러는 삭제하지 못한다.
 * 실제 위험 대상은 관리자 세션이다.
 */
@WebMvcTest(NoticeController.class)
@Import(NoticeDeleteMethodTest.MethodSecurityForSlice.class)
class NoticeDeleteMethodTest {

    /**
     * 슬라이스 테스트에서 {@code @PreAuthorize}를 켠다.
     *
     * <p>이 프로젝트는 {@code @EnableMethodSecurity}를 {@code SecurityConfig}에 두는데
     * {@code @WebMvcTest}는 그 설정을 로드하지 않는다. 그래서 <b>슬라이스에서는
     * {@code @PreAuthorize}가 아예 동작하지 않는다.</b> 이것을 모르고 권한 테스트를 짜면
     * 막혀야 할 요청이 통과하는데도 테스트가 통과한다.
     */
    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityForSlice {
    }

    @Autowired private MockMvc mockMvc;

    @MockBean private NoticeService noticeService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET으로는 삭제되지 않는다")
    void deleteIsNotReachableByGet() throws Exception {
        mockMvc.perform(get("/notice/delete/{id}", 1L))
                .andExpect(status().isMethodNotAllowed());

        verify(noticeService, never()).removeNotice(anyLong());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("관리자는 POST로 삭제할 수 있다")
    void adminCanDeleteByPost() throws Exception {
        mockMvc.perform(post("/notice/delete/{id}", 1L).with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(noticeService).removeNotice(1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("관리자가 아니면 삭제할 수 없다")
    void nonAdminCannotDelete() throws Exception {
        mockMvc.perform(post("/notice/delete/{id}", 1L).with(csrf()))
                .andExpect(status().isForbidden());

        verify(noticeService, never()).removeNotice(anyLong());
    }
}
