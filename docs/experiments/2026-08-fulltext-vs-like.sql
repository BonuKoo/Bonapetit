-- Full-Text Index vs LIKE 검색 재현용 진단 스크립트
--
-- 배경: 60만 건 상태에서 LIKE '%keyword%'가 Full-Text MATCH...AGAINST보다 약 150배 빠른
-- 역전 결과가 나왔다("60만 건에서 Full-Text Index가 LIKE보다 150배 느렸던 이유"). 그런데
-- 그 측정 당시 EXPLAIN/COUNT 결과를 파일로 남기지 않아 원인(정렬 filesort, 결과 집합 크기
-- 차이, 한글 토큰화 등)을 확정하지 못한 채 재현 조건 자체가 사라진 상태였다.
--
-- 이 파일은 그 재현 조건을 코드(저장소)에 남겨, 다음에 60만 건 규모의 실제/스테이징 DB에서
-- 그대로 실행할 수 있게 하기 위한 것이다. 로컬 소규모 DB에서 실행해도 문법은 확인되지만,
-- 결과 해석(어느 쪽이 빠른지)은 데이터 규모에 따라 달라지므로 반드시 대용량 상태에서
-- 재실행해야 의미가 있다.
--
-- 대상 테이블: team (team_name, place_name, address_name, road_address_name, created_at)
-- 검색어 예시는 원본 측정에 쓰였던 '대구 편의점', '미용실'을 그대로 사용한다.

-- ============================================================
-- 1) 두 쿼리의 실행 계획 비교 — Using filesort 유무와 rows를 확인한다.
--    (정렬이 Full-Text 경로를 느리게 만든 원인인지 첫 번째로 확인할 지점)
-- ============================================================

EXPLAIN SELECT *
FROM team t
WHERE t.team_name LIKE '%대구 편의점%'
   OR t.place_name LIKE '%대구 편의점%'
   OR t.address_name LIKE '%대구 편의점%'
   OR t.road_address_name LIKE '%대구 편의점%'
ORDER BY t.created_at DESC
LIMIT 0, 10;

EXPLAIN SELECT *
FROM team t
WHERE MATCH(t.team_name, t.place_name, t.address_name, t.road_address_name)
      AGAINST ('대구 편의점' IN NATURAL LANGUAGE MODE)
ORDER BY t.created_at DESC
LIMIT 0, 10;

-- ============================================================
-- 2) 두 쿼리가 같은 건수를 매칭하는지 확인 — 결과 집합 크기가 다르면
--    실행 시간 비교 자체가 공정하지 않다.
-- ============================================================

SELECT COUNT(*) AS like_match_count
FROM team t
WHERE t.team_name LIKE '%대구 편의점%'
   OR t.place_name LIKE '%대구 편의점%'
   OR t.address_name LIKE '%대구 편의점%'
   OR t.road_address_name LIKE '%대구 편의점%';

SELECT COUNT(*) AS fulltext_match_count
FROM team t
WHERE MATCH(t.team_name, t.place_name, t.address_name, t.road_address_name)
      AGAINST ('대구 편의점' IN NATURAL LANGUAGE MODE);

-- ============================================================
-- 3) 두 번째 검색어로도 동일하게 재현 (원본 측정의 '미용실' 케이스)
-- ============================================================

EXPLAIN SELECT *
FROM team t
WHERE t.team_name LIKE '%미용실%'
   OR t.place_name LIKE '%미용실%'
   OR t.address_name LIKE '%미용실%'
   OR t.road_address_name LIKE '%미용실%'
ORDER BY t.created_at DESC
LIMIT 0, 10;

EXPLAIN SELECT *
FROM team t
WHERE MATCH(t.team_name, t.place_name, t.address_name, t.road_address_name)
      AGAINST ('미용실' IN NATURAL LANGUAGE MODE)
ORDER BY t.created_at DESC
LIMIT 0, 10;

SELECT COUNT(*) AS like_match_count
FROM team t
WHERE t.team_name LIKE '%미용실%'
   OR t.place_name LIKE '%미용실%'
   OR t.address_name LIKE '%미용실%'
   OR t.road_address_name LIKE '%미용실%';

SELECT COUNT(*) AS fulltext_match_count
FROM team t
WHERE MATCH(t.team_name, t.place_name, t.address_name, t.road_address_name)
      AGAINST ('미용실' IN NATURAL LANGUAGE MODE);

-- ============================================================
-- 4) 실행 시간 직접 측정 (MySQL 8용 — 세션 프로파일링)
--    각 SELECT 앞뒤로 실행해 Query_time을 확인한다. EXPLAIN만으로는
--    실제 소요 시간을 알 수 없으므로 반드시 이 스텝도 함께 실행할 것.
-- ============================================================

SET profiling = 1;

-- 위 1)~3)의 SELECT 문(EXPLAIN 없이)을 그대로 다시 실행

SHOW PROFILES;
-- 각 Query_ID의 SHOW PROFILE FOR QUERY <id>; 로 세부 단계별 시간도 확인 가능

-- ============================================================
-- 5) 현재 인덱스 상태 확인 — 운영 DB에 실험용으로 만든 인덱스가
--    남아 있는지, 남아 있다면 이 쿼리에 실제로 쓰이는지 확인
-- ============================================================

SHOW INDEX FROM team;
