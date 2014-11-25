package api.process;

import java.util.Calendar;

import uti.utility.MyConfig;
import uti.utility.VNPApplication;
import uti.utility.MyConvert;
import uti.utility.MyLogger;
import api.process.Charge.ErrorCode;
import dat.service.DefineMT;
import dat.service.DefineMT.MTType;
import dat.service.MOLog;
import dat.service.ServiceObject;
import dat.sub.Subscriber;
import dat.sub.SubscriberObject;
import dat.sub.UnSubscriber;
import db.define.MyDataRow;
import db.define.MyTableModel;

public class ProDeregister
{
	public enum DeregResult
	{
		// 0 Đăng ký thành công dịch vụ
		Success(0),
		// 1 Thuê bao này đã tồn tại
		NotExistSub(1),
		// 1xx Đều là đăng ký không thành công
		Fail(100),
		// Lỗi hệ thống
		SystemError(101),
		// Thông tin nhập vào không hợp lệ
		InputInvalid(102), ;

		private int value;

		private DeregResult(int value)
		{
			this.value = value;
		}

		public Integer GetValue()
		{
			return this.value;
		}

		public static DeregResult FromInt(int iValue)
		{
			for (DeregResult type : DeregResult.values())
			{
				if (type.GetValue() == iValue)
					return type;
			}
			return Fail;
		}
	}

	MyLogger mLog = new MyLogger(LocalConfig.LogConfigPath, this.getClass().toString());

	SubscriberObject mSubObj = new SubscriberObject();

	ServiceObject mServiceObj = new ServiceObject();

	Calendar mCal_Current = Calendar.getInstance();
	Calendar mCal_SendMO = Calendar.getInstance();
	Calendar mCal_Expire = Calendar.getInstance();

	Subscriber mSub = null;
	UnSubscriber mUnSub = null;

	DefineMT.MTType mMTType = MTType.RegFail;

	MyTableModel mTable_MOLog = null;

	String MTContent = "";

	String Keyword ="";
	String MSISDN = "";
	String RequestID = "";
	String PacketName = "";
	String Note ="";
	String Channel = "";

	String AppName = "";
	String UserName = "";
	String IP = "";

	public ProDeregister(String MSISDN, String RequestID, String PacketName,String Note, String Channel, String AppName, String UserName, String IP)
	{
		this.MSISDN = MSISDN;
		this.RequestID = RequestID;
		this.PacketName = PacketName;
		this.Note = Note;
		this.Channel = Channel.toUpperCase().trim();

		this.AppName = AppName;
		this.UserName = UserName;
		this.IP = IP;
	}

	/**
	 * Lấy thông tin MO từ VNP gửi sang
	 */
	private void GetMO()
	{
		try
		{
			String[] arr = Note.split("\\|");
			if(arr.length >=2)
			{
				Keyword = arr[1];
			}
			if(Keyword.equalsIgnoreCase(""))
			{
				Keyword = mServiceObj.DeregKeyword +" API";
			}
		}
		catch(Exception ex)
		{
			mLog.log.error(ex);
		}
	}
	
	private void Init() throws Exception
	{
		try
		{
			mSub = new Subscriber(LocalConfig.mDBConfig_MSSQL);
			mUnSub = new UnSubscriber(LocalConfig.mDBConfig_MSSQL);

			mTable_MOLog = (MyTableModel) TableTemplate.Get_mMOLog().clone();
			mTable_MOLog.Clear();

		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	public MyConfig.ChannelType GetChannelType()
	{
		try
		{
			return MyConfig.ChannelType.valueOf(Channel);
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
			return MyConfig.ChannelType.NOTHING;
		}
	}

	public VNPApplication GetApplication()
	{
		try
		{
			return VNPApplication.valueOf(AppName.toUpperCase());
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
			return new VNPApplication();
		}
	}

	private MTType AddToList() throws Exception
	{
		try
		{
			// nêu la so test hiệu năng của VInaphone thì ko trả MT
			if (MSISDN.startsWith("8484"))
				return mMTType;

			MTContent = Common.GetDefineMT_Message(mMTType);
			MTContent = MTContent.replace("[TenDichVu]", mServiceObj.ServiceName);
			if (Common.SendMT(MSISDN, Keyword, MTContent, RequestID))
				AddToMOLog(mMTType, MTContent);
			return mMTType;
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	/**
	 * Thêm MT vào ta
	 * 
	 * @throws Exception
	 */
	private void AddToMOLog(MTType mMTType_Current, String MTContent_Current) throws Exception
	{
		try
		{
			MyDataRow mRow_Log = mTable_MOLog.CreateNewRow();

			mRow_Log.SetValueCell("ServiceID", mServiceObj.ServiceID);
			mRow_Log.SetValueCell("MSISDN", MSISDN);
			mRow_Log.SetValueCell("ReceiveDate", MyConfig.Get_DateFormat_InsertDB().format(mCal_Current.getTime()));
			mRow_Log.SetValueCell("LogDate", MyConfig.Get_DateFormat_InsertDB().format(mCal_Current.getTime()));
			mRow_Log.SetValueCell("ChannelTypeID", Common.GetChannelType(Channel).GetValue());
			mRow_Log.SetValueCell("ChannelTypeName", Common.GetChannelType(Channel).toString());
			mRow_Log.SetValueCell("MTTypeID", mMTType_Current.GetValue());
			mRow_Log.SetValueCell("MTTypeName", mMTType_Current.toString());
			mRow_Log.SetValueCell("MO", Keyword);
			mRow_Log.SetValueCell("MT", MTContent_Current);
			mRow_Log.SetValueCell("LogContent", "DKDV:" + mServiceObj.ServiceName);
			mRow_Log.SetValueCell("PID", MyConvert.GetPIDByMSISDN(MSISDN, LocalConfig.MAX_PID));
			mRow_Log.SetValueCell("RequestID", RequestID);

			mTable_MOLog.AddNewRow(mRow_Log);
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
		}
	}

	private void Insert_MOLog()
	{
		try
		{
			MOLog mMOLog = new MOLog(LocalConfig.mDBConfig_MSSQL);
			mMOLog.Insert(0, mTable_MOLog.GetXML());
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
		}
	}

	private MyTableModel AddInfo() throws Exception
	{
		try
		{
			MyTableModel mTable_UnSub = TableTemplate.Get_mUnSubscriber();
			mTable_UnSub.Clear();

			// Tạo row để insert vào Table Sub
			MyDataRow mNewRow = mTable_UnSub.CreateNewRow();
			mNewRow.SetValueCell("MSISDN", mSubObj.MSISDN);
			mNewRow.SetValueCell("ServiceID", mSubObj.ServiceID);

			mNewRow.SetValueCell("FirstDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.FirstDate));
			mNewRow.SetValueCell("EffectiveDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.EffectiveDate));
			mNewRow.SetValueCell("ExpiryDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.ExpiryDate));

			if (mSubObj.ChargeDate != null)
				mNewRow.SetValueCell("ChargeDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.ChargeDate));

			if (mSubObj.RetryChargeCount != null)
				mNewRow.SetValueCell("RetryChargeCount", mSubObj.RetryChargeCount);

			if (mSubObj.RetryChargeDate != null)
				mNewRow.SetValueCell("RetryChargeDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.RetryChargeDate));

			if (mSubObj.RenewChargeDate != null)
				mNewRow.SetValueCell("RenewChargeDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.RenewChargeDate));

			mNewRow.SetValueCell("ChannelTypeID", mSubObj.ChannelTypeID);
			mNewRow.SetValueCell("ChannelTypeName", mSubObj.ChannelTypeName);
			mNewRow.SetValueCell("StatusID", mSubObj.StatusID);
			mNewRow.SetValueCell("StatusName", mSubObj.StatusName);
			mNewRow.SetValueCell("PID", mSubObj.PID);
			mNewRow.SetValueCell("TotalMT", mSubObj.TotalMT);
			mNewRow.SetValueCell("TotalMTByDay", mSubObj.TotalMTByDay);
			mNewRow.SetValueCell("OrderID", mSubObj.OrderID);

			mNewRow.SetValueCell("AppID", mSubObj.AppID);
			mNewRow.SetValueCell("AppName", mSubObj.AppName);
			mNewRow.SetValueCell("UserName", mSubObj.UserName);
			mNewRow.SetValueCell("IP", mSubObj.IP);

			mNewRow.SetValueCell("PartnerID", mSubObj.PartnerID);

			if (mSubObj.LastUpdate != null)
				mNewRow.SetValueCell("LastUpdate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.LastUpdate));

			if (mSubObj.DeregDate != null)
				mNewRow.SetValueCell("DeregDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.DeregDate));

			mTable_UnSub.AddNewRow(mNewRow);
			return mTable_UnSub;
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	private boolean MoveToSub() throws Exception
	{
		try
		{
			MyTableModel mTable_UnSub = AddInfo();

			if (!mUnSub.Move(0, mTable_UnSub.GetXML()))
			{
				mLog.log.info(" Move Tu Sub Sang UnSub KHONG THANH CONG: XML Insert-->" + mTable_UnSub.GetXML());
				return false;
			}

			return true;
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	/**
	 * tạo dữ liệu cho những đăng ký lại (trước đó đã hủy dịch vụ)
	 * 
	 * @throws Exception
	 */
	private void CreateDeReg() throws Exception
	{
		try
		{
			mSubObj.ChannelTypeID = GetChannelType().GetValue();
			mSubObj.ChannelTypeName = GetChannelType().toString();
			mSubObj.DeregDate = mCal_Current.getTime();
			mSubObj.AppID = GetApplication().GetValue();
			mSubObj.AppName = GetApplication().toString();
			mSubObj.UserName = UserName;
			mSubObj.IP = IP;
		}
		catch (Exception ex)
		{
			throw ex;
		}

	}

	public MTType Process()
	{
		mMTType = MTType.RegFail;
		try
		{
			// Khoi tao
			Init();
			
			
			// Lấy service
			mServiceObj = Common.GetServiceByCode(PacketName);

			GetMO();
			
			
			if (mServiceObj.IsNull())
			{
				mLog.log.info("Dich vu khong ton tai.");
				mMTType = MTType.Invalid;
				return AddToList();
			}
			Integer PID = MyConvert.GetPIDByMSISDN(MSISDN, LocalConfig.MAX_PID);

			MyTableModel mTable_Sub = mSub.Select(2, PID.toString(), MSISDN, mServiceObj.ServiceID.toString());

			if (mTable_Sub.GetRowCount() > 0)
				mSubObj = SubscriberObject.Convert(mTable_Sub, false);

			mSubObj.PID = MyConvert.GetPIDByMSISDN(MSISDN, LocalConfig.MAX_PID);

			// Nếu chưa đăng ký dịch vụ
			if (mSubObj.IsNull())
			{
				mMTType = MTType.DeRegNotRegister;
				return AddToList();
			}

			CreateDeReg();
			ErrorCode mResult = Charge.ChargeDereg(mSubObj.PartnerID,mServiceObj, MSISDN, mServiceObj.DeregKeyword, GetChannelType(), GetApplication(), UserName, IP);
			if (mResult != ErrorCode.ChargeSuccess)
			{
				mMTType = MTType.RegFail;
				return AddToList();
			}

			if (MoveToSub())
			{
				mMTType = MTType.DeRegSuccess;
				return AddToList();
			}

			mMTType = MTType.DeRegFail;

		}
		catch (Exception ex)
		{
			mMTType = MTType.SystemError;
			mLog.log.error(ex);
		}
		finally
		{
			Insert_MOLog();
		}

		return mMTType;
	}

}
