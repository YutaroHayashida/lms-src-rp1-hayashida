package jp.co.sss.lms.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.dto.LoginUserDto;
import jp.co.sss.lms.entity.TStudentAttendance;
import jp.co.sss.lms.enums.AttendanceStatusEnum;
import jp.co.sss.lms.form.AttendanceForm;
import jp.co.sss.lms.form.DailyAttendanceForm;
import jp.co.sss.lms.mapper.TStudentAttendanceMapper;
import jp.co.sss.lms.util.AttendanceUtil;
import jp.co.sss.lms.util.Constants;
import jp.co.sss.lms.util.DateUtil;
import jp.co.sss.lms.util.LoginUserUtil;
import jp.co.sss.lms.util.MessageUtil;
import jp.co.sss.lms.util.TrainingTime;

/**
 * 勤怠情報（受講生入力）サービス
 * 
 * @author 東京ITスクール
 */
@Service
public class StudentAttendanceService {

	@Autowired
	private DateUtil dateUtil;
	@Autowired
	private AttendanceUtil attendanceUtil;
	@Autowired
	private MessageUtil messageUtil;
	@Autowired
	private LoginUserUtil loginUserUtil;
	@Autowired
	private LoginUserDto loginUserDto;

	@Autowired
	private TStudentAttendanceMapper tStudentAttendanceMapper;

	/**
	 * 勤怠一覧情報取得
	 * 
	 * @param courseId
	 * @param lmsUserId
	 * @return 勤怠管理画面用DTOリスト
	 */
	public List<AttendanceManagementDto> getAttendanceManagement(Integer courseId,
			Integer lmsUserId) {

		// 勤怠管理リストの取得
		List<AttendanceManagementDto> attendanceManagementDtoList = tStudentAttendanceMapper
				.getAttendanceManagement(courseId, lmsUserId, Constants.DB_FLG_FALSE);
		for (AttendanceManagementDto dto : attendanceManagementDtoList) {
			// 中抜け時間を設定
			if (dto.getBlankTime() != null) {
				TrainingTime blankTime = attendanceUtil.calcBlankTime(dto.getBlankTime());
				dto.setBlankTimeValue(String.valueOf(blankTime));
			}
			// 遅刻早退区分判定
			AttendanceStatusEnum statusEnum = AttendanceStatusEnum.getEnum(dto.getStatus());
			if (statusEnum != null) {
				dto.setStatusDispName(statusEnum.name);
			}
		}

		return attendanceManagementDtoList;
	}
	

	/**
	 * 出退勤更新前のチェック
	 * 
	 * @param attendanceType
	 * @return エラーメッセージ
	 */
	public String punchCheck(Short attendanceType) {
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 権限チェック
		if (!loginUserUtil.isStudent()) {
			return messageUtil.getMessage(Constants.VALID_KEY_AUTHORIZATION);
		}
		// 研修日チェック
		if (!attendanceUtil.isWorkDay(loginUserDto.getCourseId(), trainingDate)) {
			return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_NOTWORKDAY);
		}
		// 登録情報チェック
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		switch (attendanceType) {
		case Constants.CODE_VAL_ATWORK:
			if (tStudentAttendance != null
					&& !tStudentAttendance.getTrainingStartTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			break;
		case Constants.CODE_VAL_LEAVING:
			if (tStudentAttendance == null
					|| tStudentAttendance.getTrainingStartTime().equals("")) {
				// 出勤情報がないため退勤情報を入力出来ません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHINEMPTY);
			}
			if (!tStudentAttendance.getTrainingEndTime().equals("")) {
				// 本日の勤怠情報は既に入力されています。直接編集してください。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_PUNCHALREADYEXISTS);
			}
			TrainingTime trainingStartTime = new TrainingTime(
					tStudentAttendance.getTrainingStartTime());
			TrainingTime trainingEndTime = new TrainingTime();
			if (trainingStartTime.compareTo(trainingEndTime) > 0) {
				// 退勤時刻は出勤時刻より後でなければいけません。
				return messageUtil.getMessage(Constants.VALID_KEY_ATTENDANCE_TRAININGTIMERANGE);
			}
			break;
		}
		return null;
	}

	/**
	 * 出勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchIn() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 現在の研修時刻
		TrainingTime trainingStartTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				null);
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		if (tStudentAttendance == null) {
			// 登録処理
			tStudentAttendance = new TStudentAttendance();
			tStudentAttendance.setLmsUserId(loginUserDto.getLmsUserId());
			tStudentAttendance.setTrainingDate(trainingDate);
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setTrainingEndTime("");
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setNote("");
			tStudentAttendance.setAccountId(loginUserDto.getAccountId());
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setFirstCreateDate(date);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendance.setBlankTime(null);
			tStudentAttendanceMapper.insert(tStudentAttendance);
		} else {
			// 更新処理
			tStudentAttendance.setTrainingStartTime(trainingStartTime.toString());
			tStudentAttendance.setStatus(attendanceStatusEnum.code);
			tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
			tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
			tStudentAttendance.setLastModifiedDate(date);
			tStudentAttendanceMapper.update(tStudentAttendance);
		}
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 退勤ボタン処理
	 * 
	 * @return 完了メッセージ
	 */
	public String setPunchOut() {
		// 当日日付
		Date date = new Date();
		// 本日の研修日
		Date trainingDate = attendanceUtil.getTrainingDate();
		// 研修日の勤怠情報取得
		TStudentAttendance tStudentAttendance = tStudentAttendanceMapper
				.findByLmsUserIdAndTrainingDate(loginUserDto.getLmsUserId(), trainingDate,
						Constants.DB_FLG_FALSE);
		// 出退勤時刻
		TrainingTime trainingStartTime = new TrainingTime(
				tStudentAttendance.getTrainingStartTime());
		TrainingTime trainingEndTime = new TrainingTime();
		// 遅刻早退ステータス
		AttendanceStatusEnum attendanceStatusEnum = attendanceUtil.getStatus(trainingStartTime,
				trainingEndTime);
		// 更新処理
		tStudentAttendance.setTrainingEndTime(trainingEndTime.toString());
		tStudentAttendance.setStatus(attendanceStatusEnum.code);
		tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
		tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
		tStudentAttendance.setLastModifiedDate(date);
		tStudentAttendanceMapper.update(tStudentAttendance);
		// 完了メッセージ
		return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}

	/**
	 * 勤怠フォームへ設定
	 * 
	 * @param attendanceManagementDtoList
	 * @return 勤怠編集フォーム
	 */
	public AttendanceForm setAttendanceForm(
			List<AttendanceManagementDto> attendanceManagementDtoList) {

		AttendanceForm attendanceForm = new AttendanceForm();
		attendanceForm.setAttendanceList(new ArrayList<DailyAttendanceForm>());
		attendanceForm.setLmsUserId(loginUserDto.getLmsUserId());
		attendanceForm.setUserName(loginUserDto.getUserName());
		attendanceForm.setLeaveFlg(loginUserDto.getLeaveFlg());
		attendanceForm.setBlankTimes(attendanceUtil.setBlankTime());
		attendanceForm.setTrainingStartTimeHour(attendanceUtil.getTrainingTimeHourOptions());
		attendanceForm.setTrainingStartTimeMinute(attendanceUtil.getTrainingTimeMinuteOptions());
		attendanceForm.setTrainingEndTimeHour(attendanceUtil.getTrainingTimeHourOptions());
		attendanceForm.setTrainingEndTimeMinute(attendanceUtil.getTrainingTimeMinuteOptions());
		attendanceForm.setTrainingStartTimeMinute(null);
		attendanceForm.setTrainingEndTimeHour(null);
		attendanceForm.setTrainingEndTimeMinute(null);

		// 途中退校している場合のみ設定
		if (loginUserDto.getLeaveDate() != null) {
			attendanceForm
					.setLeaveDate(dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy-MM-dd"));
			attendanceForm.setDispLeaveDate(
					dateUtil.dateToString(loginUserDto.getLeaveDate(), "yyyy年M月d日"));
		}

		// 勤怠管理リストの件数分、日次の勤怠フォームに移し替え
		for (AttendanceManagementDto attendanceManagementDto : attendanceManagementDtoList) {
			DailyAttendanceForm dailyAttendanceForm = new DailyAttendanceForm();
			dailyAttendanceForm
					.setStudentAttendanceId(attendanceManagementDto.getStudentAttendanceId());
			dailyAttendanceForm
					.setTrainingDate(dateUtil.toString(attendanceManagementDto.getTrainingDate()));
			dailyAttendanceForm
					.setTrainingStartTime(attendanceManagementDto.getTrainingStartTime());
			dailyAttendanceForm.setTrainingEndTime(attendanceManagementDto.getTrainingEndTime());
			if (attendanceManagementDto.getBlankTime() != null) {
				dailyAttendanceForm.setBlankTime(attendanceManagementDto.getBlankTime());
				dailyAttendanceForm.setBlankTimeValue(String.valueOf(
						attendanceUtil.calcBlankTime(attendanceManagementDto.getBlankTime())));
			}
			dailyAttendanceForm.setStatus(String.valueOf(attendanceManagementDto.getStatus()));
			dailyAttendanceForm.setNote(attendanceManagementDto.getNote());
			dailyAttendanceForm.setSectionName(attendanceManagementDto.getSectionName());
			dailyAttendanceForm.setIsToday(attendanceManagementDto.getIsToday());
			dailyAttendanceForm.setDispTrainingDate(dateUtil
					.dateToString(attendanceManagementDto.getTrainingDate(), "yyyy年M月d日(E)"));
			dailyAttendanceForm.setStatusDispName(attendanceManagementDto.getStatusDispName());

			attendanceForm.getAttendanceList().add(dailyAttendanceForm);
		}
		//時間プルダウン完了
		// 時プルダウン
		LinkedHashMap<Integer, String> trainingStartTimeHour = new LinkedHashMap<>();
		LinkedHashMap<Integer, String> trainingEndTimeHour = new LinkedHashMap<>();
		for (int h = 0; h < 24; h++) {
		    // キーは Integer (h)、値は String に変換 (String.valueOf(h))
		    String displayValue = String.valueOf(h); 
		    
		    trainingStartTimeHour.put(h, displayValue);
		    trainingEndTimeHour.put(h, displayValue);
		}
		attendanceForm.setTrainingStartTimeHour(trainingStartTimeHour);
		attendanceForm.setTrainingEndTimeHour(trainingEndTimeHour);

		// 分プルダウン
		LinkedHashMap<Integer, String> trainingStartTimeMinute = new LinkedHashMap<>();
		LinkedHashMap<Integer, String> trainingEndTimeMinute = new LinkedHashMap<>();
		for (int m = 0; m < 60; m++) {
		    // キーは Integer (m)、値は String に変換
		    String displayValue = String.valueOf(m); 
		    
		    trainingStartTimeMinute.put(m, displayValue);
		    trainingEndTimeMinute.put(m, displayValue);
		}
		attendanceForm.setTrainingStartTimeMinute(trainingStartTimeMinute);
		attendanceForm.setTrainingEndTimeMinute(trainingEndTimeMinute);

		
		return attendanceForm;
	}



	/**
	 * 勤怠登録・更新処理
	 * 
	 * @param attendanceForm
	 * @return 完了メッセージ
	 * @throws ParseException
	 */
	public String update(AttendanceForm attendanceForm) throws ParseException {

	    Integer lmsUserId = loginUserUtil.isStudent() ? loginUserDto.getLmsUserId()
	            : attendanceForm.getLmsUserId();

	    // 現在の勤怠情報（受講生入力）リストを取得
	    List<TStudentAttendance> tStudentAttendanceList = tStudentAttendanceMapper
	            .findByLmsUserId(lmsUserId, Constants.DB_FLG_FALSE);

	    // 入力された情報を更新用のエンティティに移し替え
	    Date date = new Date();
	    for (DailyAttendanceForm dailyAttendanceForm : attendanceForm.getAttendanceList()) {

	        // 更新用エンティティ作成
	        TStudentAttendance tStudentAttendance = new TStudentAttendance();
	        // 日次勤怠フォームから更新用のエンティティにコピー
	        BeanUtils.copyProperties(dailyAttendanceForm, tStudentAttendance);
	        // 研修日付
	        tStudentAttendance
	                .setTrainingDate(dateUtil.parse(dailyAttendanceForm.getTrainingDate()));
	        // 現在の勤怠情報リストのうち、研修日が同じものを更新用エンティティで上書き
	        for (TStudentAttendance entity : tStudentAttendanceList) {
	            if (entity.getTrainingDate().equals(tStudentAttendance.getTrainingDate())) {
	                tStudentAttendance = entity;
	                break;
	            }
	        }
	        tStudentAttendance.setLmsUserId(lmsUserId);
	        tStudentAttendance.setAccountId(loginUserDto.getAccountId());
	        
	        // =========================================================================
	        // 修正箇所 1: 出勤時刻整形 (時・分を結合し、"HH:MM"形式にする)
	        // =========================================================================
	        TrainingTime trainingStartTime = null;
	        String startHourStr = dailyAttendanceForm.getTrainingStartTime();      
	        String startMinuteStr = dailyAttendanceForm.getTrainingStartMinute();  

	        String trainingStartTimeStr = null;

	        if (startHourStr != null && !startHourStr.isBlank() && 
	            startMinuteStr != null && !startMinuteStr.isBlank()) {
	            
	            try {
	                // 2桁ゼロ埋め整形し、"HH:MM" 形式に結合
	                String formattedHour = String.format("%02d", Integer.parseInt(startHourStr));
	                String formattedMinute = String.format("%02d", Integer.parseInt(startMinuteStr));
	                trainingStartTimeStr = formattedHour + ":" + formattedMinute;

	                // 結合した文字列で TrainingTime を初期化
	                trainingStartTime = new TrainingTime(trainingStartTimeStr);
	                tStudentAttendance.setTrainingStartTime(trainingStartTime.getFormattedString());
	            } catch (NumberFormatException e) {
	                // ここでエラーが発生した場合は、元のエラーとは異なるため、適切に処理する
	                throw new IllegalArgumentException("出勤時刻の数値形式が不正です。", e);
	            }
	        } else {
	            // 未入力の場合は null をセット
	            tStudentAttendance.setTrainingStartTime(null);
	        }
	        // =========================================================================
	        
	        // =========================================================================
	        // 修正箇所 2: 退勤時刻整形 (時・分を結合し、"HH:MM"形式にする)
	        // =========================================================================
	        TrainingTime trainingEndTime = null;
	        String endHourStr = dailyAttendanceForm.getTrainingEndTime();
	        String endMinuteStr = dailyAttendanceForm.getTrainingEndMinute();

	        String trainingEndTimeStr = null;

	        if (endHourStr != null && !endHourStr.isBlank() && 
	            endMinuteStr != null && !endMinuteStr.isBlank()) {
	            
	            try {
	                // 2桁ゼロ埋め整形し、"HH:MM" 形式に結合
	                String formattedHour = String.format("%02d", Integer.parseInt(endHourStr));
	                String formattedMinute = String.format("%02d", Integer.parseInt(endMinuteStr));
	                trainingEndTimeStr = formattedHour + ":" + formattedMinute;

	                // 結合した文字列で TrainingTime を初期化
	                trainingEndTime = new TrainingTime(trainingEndTimeStr);
	                tStudentAttendance.setTrainingEndTime(trainingEndTime.getFormattedString());
	            } catch (NumberFormatException e) {
	                // ここでエラーが発生した場合は、元のエラーとは異なるため、適切に処理する
	                throw new IllegalArgumentException("退勤時刻の数値形式が不正です。", e);
	            }
	        } else {
	            // 未入力の場合は null をセット
	            tStudentAttendance.setTrainingEndTime(null);
	        }
	        // =========================================================================
	        
	        // 中抜け時間
	        tStudentAttendance.setBlankTime(dailyAttendanceForm.getBlankTime());
	        
	        // 遅刻早退ステータス
	        if ((trainingStartTime != null || trainingEndTime != null)
	                && !dailyAttendanceForm.getStatusDispName().equals("欠席")) {
	            // trainingStartTime と trainingEndTime は、nullチェック後に上記で初期化されているものを使用
	            AttendanceStatusEnum attendanceStatusEnum = attendanceUtil
	                    .getStatus(trainingStartTime, trainingEndTime);
	            tStudentAttendance.setStatus(attendanceStatusEnum.code);
	        }
	        // 備考
	        tStudentAttendance.setNote(dailyAttendanceForm.getNote());
	        // 更新者と更新日時
	        tStudentAttendance.setLastModifiedUser(loginUserDto.getLmsUserId());
	        tStudentAttendance.setLastModifiedDate(date);
	        // 削除フラグ
	        tStudentAttendance.setDeleteFlg(Constants.DB_FLG_FALSE);
	        // 登録用Listへ追加
	        tStudentAttendanceList.add(tStudentAttendance);
	    }
	    // 登録・更新処理
	    for (TStudentAttendance tStudentAttendance : tStudentAttendanceList) {
	        if (tStudentAttendance.getStudentAttendanceId() == null) {
	            tStudentAttendance.setFirstCreateUser(loginUserDto.getLmsUserId());
	            tStudentAttendance.setFirstCreateDate(date);
	            tStudentAttendanceMapper.insert(tStudentAttendance);
	        } else {
	            tStudentAttendanceMapper.update(tStudentAttendance);
	        }
	    }
	    // 完了メッセージ
	    return messageUtil.getMessage(Constants.PROP_KEY_ATTENDANCE_UPDATE_NOTICE);
	}
	
	 /**
     * 
     * 現在より過去に未入力があるかをチェック
     * @author 林田悠太朗-Task.25
     * @param loginUser ログインユーザ情報
     * @return boolean（true=過去に未入力がある）
     */
    public boolean hasPastUnentered(LoginUserDto loginUser) {

        // SimpleDateFormatを設定
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // 現在日付を取得
        Date today = new Date();
        try {
            today = sdf.parse(sdf.format(today));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // 過去日の未入力数を取得
        Integer count = tStudentAttendanceMapper.notEnterCount(
                loginUser.getLmsUserId(),
                Constants.DB_FLG_FALSE,        // delete_flg
                today     // 今日より過去
        );

        // 0より大きい場合 true（ダイアログ表示させる）
        return count > 0;
    }

}
