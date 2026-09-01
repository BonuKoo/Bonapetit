package com.eatmate.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * 전역 예외 처리기.
 *
 * <h3>무엇이 문제였나</h3>
 * 처리기가 없어 <b>상태 코드가 틀렸다.</b> 없는 모임 id로 상세를 열면
 * {@code IllegalArgumentException}이 그대로 올라가 <b>500</b>이 됐다. 서버가 고장 난 것이
 * 아니라 요청이 틀린 것이므로 400이어야 한다. 클라이언트가 재시도해도 소용없는 요청과
 * 서버 장애를 구분하지 못하면, 오류 로그에서 진짜 장애가 묻힌다.
 *
 * <h3>스택 노출은 문제가 아니었다</h3>
 * ISS-11에는 "오류 응답에 스택 정보가 포함될 수 있다"고 적혀 있었으나 재 보니 아니었다.
 * Spring Boot 3의 기본값이 {@code include-stacktrace=never}·{@code include-message=never}이고
 * 이 프로젝트는 그 설정을 바꾸지 않았다({@code ErrorResponseProbeTest}가 고정한다).
 *
 * <h3>왜 sendError 인가</h3>
 * {@link ResponseEntity}로 직접 응답하면 뷰 컨트롤러에서 빈 화면이 나간다. 이 앱은 뷰와
 * REST가 섞여 있다. {@code sendError}는 오류 디스패치로 넘겨 Boot의 오류 처리가 하던 일을
 * 그대로 하게 두되 <b>상태 코드만 바로잡는다.</b> 뷰 요청은 오류 페이지를, REST 요청은
 * 오류 JSON을 그대로 받는다.
 *
 * <h3>여기서 다루지 않는 것</h3>
 * <ul>
 *   <li>{@code AccessDeniedException} — Spring Security의 {@code ExceptionTranslationFilter}가
 *       403으로 처리한다. 여기서 가로채면 그 처리를 망가뜨린다</li>
 *   <li>{@code ResponseStatusException} — 이미 상태 코드를 들고 있다</li>
 *   <li>그 밖의 모든 예외 — Boot 기본 처리(500)가 맞고, 내부 정보도 새지 않는다.
 *       포괄 처리기를 두면 예상 못 한 예외를 조용히 삼킬 위험만 생긴다</li>
 * </ul>
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 잘못된 식별자·형식 등 <b>요청이 틀린</b> 경우. 서버 오류가 아니다.
     *
     * 예: {@code /post/detail/999999} — 존재하지 않는 모임 id
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public void handleBadRequest(IllegalArgumentException e,
                                 HttpServletRequest request,
                                 HttpServletResponse response) throws IOException {
        log.warn("잘못된 요청 {} {} - {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    /**
     * 인자 없는 {@code Optional.orElseThrow()}가 던지는 예외. 대상이 없다는 뜻이다.
     *
     * 예: {@code NoticeService.findDetailById} — 존재하지 않는 공지 id
     */
    @ExceptionHandler(NoSuchElementException.class)
    public void handleNotFound(NoSuchElementException e,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
        log.warn("대상 없음 {} {} - {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}
