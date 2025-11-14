/**
 * 勤怠一括登録
 * 
 * @author 東京ITスクール
 */
$(function(){
	  $('.copyAttendanceDaily').click(function(){
			const index = $(this).val();
			let startTimeCompanyAry = $('.startTimeCompany' + index);
			let endTimeCompanyAry = $('.endTimeCompany'  + index);
			let startTimeCompanyRoundedAry = $('.startTimeCompanyRounded' + index);
			let endTimeCompanyRoundedAry = $('.endTimeCompanyRounded' + index);
			let idxI, $startTimeCompany, $endTimeCompany, $startTimeCompanyRounded, $endTimeCompanyRounded;
			for (idxI = 0; idxI < startTimeCompanyAry.length; idxI++) {
				  $startTimeCompany = $(startTimeCompanyAry[idxI]);
				  $endTimeCompany = $(endTimeCompanyAry[idxI]);
				  $startTimeCompanyRounded = $(startTimeCompanyRoundedAry[idxI]);
				  $endTimeCompanyRounded = $(endTimeCompanyRoundedAry[idxI]);
				if ($startTimeCompany.val() === '' && !($startTimeCompanyRounded.val() === '')) {
					$startTimeCompany.val($startTimeCompanyRounded.val());
					$startTimeCompany.addClass('warnInput');
				}
				if ($endTimeCompany.val() === '' && !($endTimeCompanyRounded.val() === '')) {
					$endTimeCompany.val($endTimeCompanyRounded.val());
					$endTimeCompany.addClass('warnInput');
				}
			}
	  });
});

function setTargetIndex(index) {
	// エレメントを作成
	var element = document.createElement('input');
	// データを設定
	element.setAttribute('type', 'hidden');
	element.setAttribute('name', 'targetIndex');
	element.setAttribute('value', index);
	// 要素を追加
	document.bulkRegistForm.appendChild(element);
	return true;
}

/**
 * 過去未入力チェックダイアログ表示
 * サーバーから渡されたフラグに基づき、確認ダイアログを表示する。
 */
$(function(){
    // Spring Modelから渡されたフラグを取得（JSPなどのテンプレートエンジンに依存）
    // このファイルがHTML/JSPに直接埋め込まれているか、またはロードされている必要があります。
    var isMissingEntry = "${isMissingEntry}"; // ★JSP/EL式での値取得を想定★

    // ログ出力（デバッグ用）
    console.log('過去未入力チェック: isMissingEntry = ' + isMissingEntry);

    if (isMissingEntry === 'true') {
        var confirmMessage = "過去日に入力されていない勤怠情報があります。\n確認しますか？";
        
        if (confirm(confirmMessage)) {
            // 「はい」が押された場合の処理
            console.log('ユーザーは「はい」を選択。編集画面へ遷移します。');
            // 例: window.location.href = '/lms/attendance/update'; 
            // ※ 遷移先のパスを適切なものに修正してください
        } else {
            // 「いいえ」が押された場合の処理
            console.log('ユーザーは「いいえ」を選択。');
        }
    }
});
