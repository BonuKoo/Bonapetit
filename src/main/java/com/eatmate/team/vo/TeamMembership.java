package com.eatmate.team.vo;

/**
 * 인가 검사 결과.
 *
 * 검사에 필요한 것은 "이 사람이 이 모임에서 누구이고 개설자인가" 둘뿐이다.
 * AccountTeam 엔티티를 그대로 돌려주면 account·team 연관이 EAGER라 검사 한 번에
 * 부가 SELECT가 3건 더 붙는다(측정: {@code TeamAccessQueryCountTest}).
 * 필요한 두 값만 프로젝션해 1쿼리로 끝낸다.
 *
 * @param accountId 세션 주체의 계정 식별자. 요청 파라미터로 들어온 account_id 대신 이 값을 쓴다
 * @param leader    개설자 여부
 */
public record TeamMembership(Long accountId, boolean leader) {
}
