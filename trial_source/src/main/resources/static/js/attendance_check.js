document.addEventListener("DOMContentLoaded", function () {
    const flag = document.getElementById("hasUnenteredPast")?.value;

    if (flag === "true") {
        alert("過去日に未入力の勤怠があります。確認してください。");
    }
});

//$(function() {
//    const dataElement = $('#attendance-page-data');
//    const hasMissing = dataElement.data('missing-flag'); 
//
//    // 1または'1'であれば true
//    const isMissing = (hasMissing === true || hasMissing === 'true');
//
//    // 3. フラグが true の場合にアラートを表示する
//    if (isMissing) {
//        alert("過去日の勤怠に未入力があります。勤怠情報を確認してください。");
//    } 
//});