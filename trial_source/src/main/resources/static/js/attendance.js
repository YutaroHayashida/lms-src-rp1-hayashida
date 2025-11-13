$(document).ready(function() {
    var table = $('.dataTable').DataTable({
        scrollY: "650px",   // テーブルの縦スクロール高さ
        scrollCollapse: true,
        paging: false       // ページング不要なら false
    });

    // ヘッダー固定
    new $.fn.dataTable.FixedHeader(table);
});
