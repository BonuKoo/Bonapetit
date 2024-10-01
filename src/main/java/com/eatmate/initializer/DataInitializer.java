package com.eatmate.initializer;

import com.eatmate.dao.repository.account.AccountRepository;
import com.eatmate.dao.repository.notice.NoticeRepository;
import com.eatmate.domain.entity.notice.Notice;
import com.eatmate.domain.entity.user.Account;
import com.eatmate.map.vo.MapVo;
import com.eatmate.post.service.PostJpaService;
import com.eatmate.post.vo.PostForm;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer
        //implements CommandLineRunner
{   /*
    private final PostJpaService teamService;
    private final AccountRepository accountRepository;

    private final NoticeRepository noticeRepository;
*/  
    /*
    //@Override
    public void run(String... args) throws Exception {
        // 초기화 로직 실행

        //createTeamsAndChatRooms();
        //createNoticeData();
    }*/
    
        
    private void createTeamsAndChatRooms() {

        List<PostForm> postForms = generatePostForms();  // 팀 정보가 담긴 리스트
        List<MapVo> mapVos = generateMapVos();  // 장소 정보가 담긴 리스트

        for (int i = 0; i < 300; i++) {
            PostForm postForm = postForms.get(i);
            MapVo mapVo = mapVos.get(i);

            // 팀과 채팅방을 생성
            teamService.createChatRoomAndTeamWhenWriteThePost(postForm, mapVo);
        }



    }

    // PostForm을 생성하는 예시 (원하는 데이터로 교체 가능)
    private List<PostForm> generatePostForms() {
        //Account account = accountRepository.findByOauth2id("3683546464");
        List<PostForm> postForms = new ArrayList<>();
        for (int i = 1; i <= 300; i++) {
            postForms.add(PostForm.builder()
                    .teamName("Team " + i)
                    .description("Description for team " + i)
                    .author("3683546464")
                    .build());
        }
        return postForms;
    }

    // MapVo를 생성하는 예시 (원하는 데이터로 교체 가능)
    private List<MapVo> generateMapVos() {
        List<MapVo> mapVos = new ArrayList<>();
        for (int i = 1; i <= 300; i++) {
            MapVo mapVo = new MapVo();
            mapVo.setMapId("MapId" + i);
            mapVo.setAddressName("Address" + i);
            mapVo.setPhone("010-1234-" + i);
            mapVo.setPlaceName("Place" + i);
            mapVo.setPlaceUrl("http://place" + i + ".com");
            mapVo.setRoadAddressName("RoadAddress" + i);
            mapVo.setX(String.valueOf(126.0 + i));
            mapVo.setY(String.valueOf(37.0 + i));

            mapVos.add(mapVo);
        }
        return mapVos;
    }
  
}
