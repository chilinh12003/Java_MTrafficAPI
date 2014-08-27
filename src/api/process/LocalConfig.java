package api.process;

import java.util.Vector;

import dat.service.DefineMT;
import dat.service.DefineMTObject;
import dat.service.Service;
import dat.service.ServiceObject;
import db.define.DBConfig;

public class LocalConfig
{
	public static String LogConfigPath = "log4j.properties";
	public static String LogDataFolder = ".\\LogFile\\";

	public static String  DBConfigPath = "ProxoolConfig.xml";
	public static String  MySQLPoolName = "MySQL";
	public static String  MSSQLPoolName = "MSSQL";
	
	public static DBConfig mDBConfig_MSSQL = new DBConfig("MySQL");	
	public static DBConfig mDBConfig_MySQL = new DBConfig("MSSQL");
	

	public static String SHORT_CODE = "1546";
	//----------------cau hinh Charging-----------------------------
	public static String VNPURLCharging = "";
	public static String VNPCPName ="MTRAFFIC";
	public static String VNPUserName = "mtraffic";
	public static String VNPPassword ="mtraffic#1235";	
	
	/**
	 * Cấu hình cho phép bắn MT dài theo content Type là gì
	 */
	public static Integer LONG_MESSAGE_CONTENT_TYPE = 21;
	
	public static Integer CHARGE_MAX_ERROR_RETRY = 1;
	
	public static Integer MAX_PID = 20;
	
	private static Vector<DefineMTObject> mListDefineMT =  new Vector<DefineMTObject>();
	
	public static Vector<DefineMTObject> GetListDefineMT() throws Exception
	{
		if(mListDefineMT == null || mListDefineMT.size() == 0)
		{
			DefineMT mDefineMT = new DefineMT(LocalConfig.mDBConfig_MSSQL);
			mListDefineMT = mDefineMT.GetAllMT();
		}
		return mListDefineMT;
	}
	
	private static Vector<ServiceObject> mListServiceObject = new Vector<ServiceObject>();

	public static Vector<ServiceObject> GetListService() throws Exception
	{
		if(mListServiceObject == null || mListServiceObject.size() == 0)
		{
			// Load các đối tượng dạng Từ điển
			Service mService = new Service(LocalConfig.mDBConfig_MSSQL);
			mListServiceObject = mService.GetAllService();
		}
		return mListServiceObject;
	}
	
}
