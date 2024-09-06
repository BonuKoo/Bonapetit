package com.eatmate.dao.repository.map;

import com.eatmate.domain.entity.map.KakaoMap;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MapRepository extends JpaRepository<KakaoMap,String> {

}
