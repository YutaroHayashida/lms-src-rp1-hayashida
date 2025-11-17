document.addEventListener("DOMContentLoaded", function () {
    const flag = document.getElementById("hasUnenteredPast")?.value;

    if (flag === "true") {
        alert("過去日に未入力の勤怠があります。確認してください。");
    }
});