//function updatePlaceholder() {
//    var searchType = document.getElementById("searchType").value;
//    var searchInput = document.getElementById("searchKeyword");
//
//    switch (searchType) {
//        case "location":
//            searchInput.placeholder = "Search by location...";
//            break;
//        case "title":
//            searchInput.placeholder = "Search by title...";
//            break;
//        case "date":
//            searchInput.type = "date";
//            break;
//        case "author":
//            searchInput.placeholder = "Search by author...";
//            searchInput.type = "text";
//            break;
//        case "teamName":
//            searchInput.placeholder = "Search by team name...";
//            searchInput.type = "text";
//            break;
//        case "foodKind":
//            searchInput.placeholder = "Search by food kind...";
//            searchInput.type = "text";
//            break;
//        default:
//            searchInput.placeholder = "Search...";
//    }
//}
//
//// 페이지 로드 시 초기화
//document.addEventListener("DOMContentLoaded", function() {
//    updatePlaceholder();
//});

// 리스트 검색
function searchTeam() {
    document.getElementById('keyword').value = document.getElementById('keyword-area').value;
    document.getElementById('page').value = 1;
    document.getElementById('searchForm').submit();
}

// 페이지 번호 눌렀을 때
function changePage(page) {
    var pageNumber = page.getAttribute('data-page'); // 페이지 번호 가져오기

    document.getElementById('page').value = pageNumber; // searchForm의 <input type="hidden" name="page" id="page">에 페이지 번호 저장

    document.getElementById('searchForm').submit(); // 전송
}