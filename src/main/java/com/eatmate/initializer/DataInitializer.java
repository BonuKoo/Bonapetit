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
        implements CommandLineRunner
{
    private final PostJpaService teamService;
    private final AccountRepository accountRepository;
    /*
    private final NoticeRepository noticeRepository;
    */



    @Override
    public void run(String... args) throws Exception {
        // 초기화 로직 실행
        createTeamsAndChatRooms();
    }

    // 이 메서드는 ApplicationReadyEvent나 필요한 시점에서 호출 가능
    public void createTeamsAndChatRooms() {
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

    /*
    @Override
    public void run(String... args) throws Exception {

        List<Notice> notices = new ArrayList<>();

        Account account = accountRepository.findById(1L).orElseThrow();

        // 6만 건의 데이터를 생성하여 리스트에 추가
        for (int i = 1; i <= 60000; i++) {
            Notice notice = Notice.builder()
                    .title("Title " + i)
                    .content("Content " + i)
                    .account(account)
                    .build();
            notices.add(notice);

            // 1000건씩 저장 (Batch 처리)
            if (i % 1000 == 0) {
                noticeRepository.saveAll(notices);
                notices.clear();  // 리스트 비우기
            }
        }

        // 남은 데이터 저장
        if (!notices.isEmpty()) {
            noticeRepository.saveAll(notices);
        }

        System.out.println("6만 건의 데이터가 성공적으로 저장되었습니다.");
    }*/
}
