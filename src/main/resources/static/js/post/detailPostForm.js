
var x = document.getElementById('x').value;
var y = document.getElementById('y').value;

var place = {
    id : document.getElementById('mapId').value,
    address_name : document.getElementById('addressName').value,
    phone : document.getElementById('phone').value,
    place_name : document.getElementById('placeName').value,
    place_url : document.getElementById('placeUrl').value,
    road_address_name : document.getElementById('roadAddressName').value,
    x : document.getElementById('x').value,
    y : document.getElementById('y').value
};

// 마커를 클릭했을 때 해당 장소의 상세정보를 보여줄 커스텀오버레이입니다
var placeOverlay = new kakao.maps.CustomOverlay({zIndex:1}),
    contentNode = document.createElement('div'); // 커스텀 오버레이의 컨텐츠 엘리먼트 입니다

var mapContainer = document.getElementById('map'), // 지도를 표시할 div
    mapOption = {
        center: new kakao.maps.LatLng(y, x), // 지도의 중심좌표
        level: 5 // 지도의 확대 레벨
    };

// 지도를 생성합니다
var map = new kakao.maps.Map(mapContainer, mapOption);

// 커스텀 오버레이의 컨텐츠 노드에 css class를 추가합니다
contentNode.className = 'placeinfo_wrap';

// 커스텀 오버레이 컨텐츠를 설정합니다
placeOverlay.setContent(contentNode);

displayPlaces(place);

// 검색 결과 목록과 마커를 표출하는 함수입니다
function displayPlaces(place) {
    // 마커를 생성하고 지도에 표시합니다
    var placePosition = new kakao.maps.LatLng(place.y, place.x),
        marker = addMarker(placePosition, 0);

    displayPlaceInfo(place);
}

// 마커를 생성하고 지도 위에 마커를 표시하는 함수입니다
function addMarker(position, idx, title) {
    var imageSrc = 'https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/marker_number_blue.png', // 마커 이미지 url, 스프라이트 이미지를 씁니다
        imageSize = new kakao.maps.Size(36, 37),  // 마커 이미지의 크기
        imgOptions =  {
            spriteSize : new kakao.maps.Size(36, 691), // 스프라이트 이미지의 크기
            spriteOrigin : new kakao.maps.Point(0, (idx*46)+10), // 스프라이트 이미지 중 사용할 영역의 좌상단 좌표
            offset: new kakao.maps.Point(13, 37) // 마커 좌표에 일치시킬 이미지 내에서의 좌표
        },
        markerImage = new kakao.maps.MarkerImage(imageSrc, imageSize, imgOptions),
            marker = new kakao.maps.Marker({
            position: position, // 마커의 위치
            image: markerImage
        });

    marker.setMap(map); // 지도 위에 마커를 표출합니다
    return marker;
}

// 클릭한 마커에 대한 장소 상세정보를 커스텀 오버레이로 표시하는 함수입니다
function displayPlaceInfo (place) {
    var content = '<div class="placeinfo">' +
                    '   <a class="title" href="' + place.place_url + '" target="_blank" title="' + place.place_name + '">' + place.place_name + '</a>';

    if (place.road_address_name) {
        content += '    <span title="' + place.road_address_name + '">' + place.road_address_name + '</span>' +
                    '  <span class="jibun" title="' + place.address_name + '">(지번 : ' + place.address_name + ')</span>';
    }  else {
        content += '    <span title="' + place.address_name + '">' + place.address_name + '</span>';
    }

    content += '    <span class="tel">' + place.phone + '</span>' +
                '</div>' +
                '<div class="after"></div>';

    contentNode.innerHTML = content;
    placeOverlay.setPosition(new kakao.maps.LatLng(place.y, place.x));
    placeOverlay.setMap(map);
}


function joinTeam(teamId) {
    // 팀 가입을 위한 POST 요청

    axios.post('/team/join/' + teamId)

        .then(response => {

            // 서버로부터 roomId, userNickname, teamName 데이터를 받아옴
            const roomId = response.data.roomId;
            const userNickname = response.data.userNickname;
            const teamName = response.data.teamName;

            // userNickname과 teamName을 localStorage에 저장
            localStorage.setItem('wschat.userNickname', userNickname);
            localStorage.setItem('wschat.teamName', teamName);

            if (roomId) {
                // 채팅방 입장 화면으로 리다이렉트
                window.location.href = "/chat/room/enter/" + roomId;

            } else {
                alert('채팅방 정보를 불러오지 못했습니다.');
            }
        })
        .catch(error => {
            console.error("팀 가입 중 오류가 발생했습니다.", error);
            alert('팀 가입에 실패했습니다.');
        });

}
