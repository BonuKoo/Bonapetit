package com.eatmate.team.vo;

import java.time.LocalDateTime;

/**
 * 모임 목록 한 행. 저장소가 쿼리 한 번으로 채워 준다.
 *
 * <p>화면이 필요로 하는 값만 담는다. 엔티티를 돌려주면 목록을 그리는 데 쓰지도 않는
 * 연관들이 따라온다 — {@code AccountTeam}의 {@code @ManyToOne}이 fetch 미지정이라
 * EAGER이고, 인원수를 세려고 {@code members} 컬렉션까지 통째로 로드된다.
 * 그래서 10건짜리 페이지 한 장에 쿼리가 42건 나갔다.
 *
 * <p>{@code createdAt}을 포맷된 문자열이 아니라 {@link LocalDateTime}으로 두는 이유는,
 * 표시 형식은 화면의 관심사이지 저장소의 관심사가 아니기 때문이다. 변환은
 * {@code TeamJpaService}가 한다.
 *
 * @param memberCount 참여자 수. JPQL {@code size()}가 상관 서브쿼리로 번역되어
 *                    같은 쿼리 안에서 계산된다
 */
public record TeamListRow(
        Long teamId,
        String teamName,
        String addressName,
        String roadAddressName,
        String placeName,
        String author,
        int memberCount,
        LocalDateTime createdAt
) {
}
