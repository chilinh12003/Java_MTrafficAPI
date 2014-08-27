package api.process;

import dat.service.ChargeLog;
import dat.service.MOLog;
import dat.sub.Subscriber;
import dat.sub.UnSubscriber;
import db.define.MyTableModel;

/**
 * Chứa các đối tượng đã table mẫu ở Database Các đối tượng này sẽ được load khi
 * chương trình bắt đầu chạy
 * 
 * @author Administrator
 * 
 */
public class TableTemplate
{
	public static synchronized MyTableModel Get_mMOLog() throws Exception
	{
		MOLog mMOLog = new MOLog(LocalConfig.mDBConfig_MSSQL);
		return mMOLog.Select(0);
	}	
	
	public static synchronized MyTableModel Get_mUnSubscriber() throws Exception
	{
		Subscriber mSub = new Subscriber(LocalConfig.mDBConfig_MSSQL);
		return mSub.Select(0);
	}
	
	public static synchronized MyTableModel Get_mChargeLog() throws Exception
	{
		ChargeLog mChargeLog = new ChargeLog(LocalConfig.mDBConfig_MSSQL);
		return mChargeLog.Select(0);
	}
	
	public static synchronized MyTableModel Get_mSubscriber() throws Exception
	{
		UnSubscriber mUnSub = new UnSubscriber(LocalConfig.mDBConfig_MSSQL);	
		return mUnSub.Select(0);
	}	
}
