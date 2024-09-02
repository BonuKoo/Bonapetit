function updatePlaceholder() {
    var searchType = document.getElementById("searchType").value;
    var searchInput = document.getElementById("searchKeyword");

    switch (searchType) {
        case "location":
            searchInput.placeholder = "Search by location...";
            break;
        case "title":
            searchInput.placeholder = "Search by title...";
            break;
        case "date":
            searchInput.type = "date";
            break;
        case "author":
            searchInput.placeholder = "Search by author...";
            searchInput.type = "text";
            break;
        case "teamName":
            searchInput.placeholder = "Search by team name...";
            searchInput.type = "text";
            break;
        case "foodKind":
            searchInput.placeholder = "Search by food kind...";
            searchInput.type = "text";
            break;
        default:
            searchInput.placeholder = "Search...";
    }
}

// 페이지 로드 시 초기화
document.addEventListener("DOMContentLoaded", function() {
    updatePlaceholder();
});