package jp.co.sss.lms.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import jp.co.sss.lms.dto.AttendanceManagementDto;
import jp.co.sss.lms.entity.TStudentAttendance;

/**
 * 勤怠情報（受講生入力）テーブルマッパー
 * 
 * @author 東京ITスクール
 */
@Mapper
public interface TStudentAttendanceMapper {

	/**
	 * 勤怠情報（受講生入力）取得（LMSユーザーID）
	 * 
	 * @param lmsUserId
	 * @param deleteFlg
	 * @return 勤怠情報（受講生入力）エンティティ
	 */
	List<TStudentAttendance> findByLmsUserId(@Param("lmsUserId") Integer lmsUserId,
			@Param("deleteFlg") Short deleteFlg);

	/**
	 * 勤怠情報（受講生入力）取得（LMSユーザーID＆日付）
	 * 
	 * @param lmsUserId
	 * @param trainingDate
	 * @param deleteFlg
	 * @return 勤怠情報（受講生入力）エンティティ
	 */
	TStudentAttendance findByLmsUserIdAndTrainingDate(@Param("lmsUserId") Integer lmsUserId,
			@Param("trainingDate") Date trainingDate, @Param("deleteFlg") Short deleteFlg);

	/**
	 * 勤怠管理画面用DTOリスト取得
	 * 
	 * @param courseId
	 * @param lmsUserId
	 * @param deleteFlg
	 * @return 勤怠管理画面用DTOリスト
	 */
	List<AttendanceManagementDto> getAttendanceManagement(@Param("courseId") Integer courseId,
			@Param("lmsUserId") Integer lmsUserId, @Param("deleteFlg") Short deleteFlg);

	/**
	 * 勤怠情報（受講生入力）登録
	 * 
	 * @param tStudentAttendance
	 * @return 登録結果
	 */
	Boolean insert(TStudentAttendance tStudentAttendance);

	/**
	 * 勤怠情報（受講生入力）更新
	 * 
	 * @param tStudentAttendance
	 * @return 更新結果
	 */
	Boolean update(TStudentAttendance tStudentAttendance);
	
	/**
     * 過去日に入力されていない勤怠情報の件数を取得する
     * @param lmsUserId LMSユーザID
     * @param deleteFlg 削除フラグ
     * @param currentDateStr 現在日付け（'yyyyMMdd'形式）
     * @return 未入力件数
     */
    int countMissingEntry(@Param("lmsUserId") Integer lmsUserId,
                          @Param("deleteFlg") Short deleteFlg,
                          @Param("currentDateStr") String currentDateStr);
	
}
