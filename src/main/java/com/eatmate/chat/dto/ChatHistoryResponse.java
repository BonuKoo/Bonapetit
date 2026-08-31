package com.eatmate.chat.dto;

import java.util.List;

/**
 * 커서 기반 채팅 내역 조회 응답.
 *
 * @param messages   오래된 것 -> 최신 순. 조회는 id 내림차순이지만 화면 렌더링은
 *                   시간순이므로 서버에서 뒤집어 내려준다.
 * @param nextCursor 이 페이지에서 가장 오래된 메시지의 id. 다음 요청의 before에
 *                   그대로 넣으면 그 이전 구간을 받는다. 결과가 없으면 null.
 * @param hasMore    더 과거 메시지가 있는지. size+1건을 조회해 판정하므로
 *                   count 쿼리가 필요 없다.
 */
public record ChatHistoryResponse(
        List<ChatMessageResponse> messages,
        Long nextCursor,
        boolean hasMore
) {
}
