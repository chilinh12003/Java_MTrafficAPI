package api.process;

import java.util.Calendar;

import uti.utility.MyConfig;
import uti.utility.MyConvert;
import uti.utility.MyLogger;
import dat.service.ServiceObject;
import dat.sub.Subscriber;
import dat.sub.SubscriberObject;
import dat.sub.UnSubscriber;
import db.define.MyTableModel;

public class ProGetInfoSub
{

	public enum InfoSubResult
	{
		// 0 Đăng ký thành công dịch vụ
		Success(0),

		// 1xx Đều là đăng ký không thành công
		Fail(100),
		// Lỗi hệ thống
		SystemError(101),
		// Thông tin nhập vào không hợp lệ
		InputInvalid(102);

		private int value;

		private InfoSubResult(int value)
		{
			this.value = value;
		}

		public Integer GetValue()
		{
			return this.value;
		}

		public static InfoSubResult FromInt(int iValue)
		{
			for (InfoSubResult type : InfoSubResult.values())
			{
				if (type.GetValue() == iValue)
					return type;
			}
			return Fail;
		}
	}

	public enum Status
	{
		// 0 Đăng ký thành công dịch vụ
		NotReg(0), Register(1), NotExist(2), NotSpecify(3), ;

		private int value;

		private Status(int value)
		{
			this.value = value;
		}

		public Integer GetValue()
		{
			return this.value;
		}

		public static Status FromInt(int iValue)
		{
			for (Status type : Status.values())
			{
				if (type.GetValue() == iValue)
					return type;
			}
			return NotSpecify;
		}
	}

	MyLogger mLog = new MyLogger(LocalConfig.LogConfigPath, this.getClass().toString());

	SubscriberObject mSubObj = new SubscriberObject();

	ServiceObject mServiceObj = new ServiceObject();
	Calendar CurrentDate = Calendar.getInstance();

	Subscriber mSub = null;
	UnSubscriber mUnSub = null;

	InfoSubResult mInfoSubResult = InfoSubResult.Fail;
	Status mStatus = Status.NotSpecify;

	String MSISDN = "";
	String RequestID = "";
	String PacketName = "";
	String Channel = "";
	
	String AppName = "";
	String UserName = "";
	String IP = "";
	
	public ProGetInfoSub(String MSISDN, String RequestID, String PacketName, String Channel, String AppName, String UserName, String IP)
	{
		this.MSISDN = MSISDN;
		this.RequestID = RequestID;
		this.PacketName = PacketName;
		this.Channel = Channel.toUpperCase().trim();
		this.AppName = AppName;
		this.UserName = UserName;
		this.IP = IP;
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
	private String GetResponse(String last_time_subscribe, String last_time_unsubscribe, String last_time_renew, String last_time_retry, String expire_time)
	{
		String Format = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><RESPONSE><SERVICE><error>%s</error><error_desc>%s</error_desc><status>%s</status><last_time_subscribe>%s</last_time_subscribe><last_time_unsubscribe>%s</last_time_unsubscribe><last_time_renew>%s</last_time_renew><last_time_retry>%s</last_time_retry><expire_time>%s</expire_time></SERVICE></RESPONSE>";
		return String.format(Format, new Object[] { mInfoSubResult.GetValue().toString(), mInfoSubResult.toString(), mStatus.GetValue().toString(),
				last_time_subscribe, last_time_unsubscribe, last_time_renew, last_time_retry, expire_time });
	}

	public String Process()
	{
		mInfoSubResult = InfoSubResult.Fail;
		String last_time_subscribe = "NULL";
		String last_time_unsubscribe = "NULL";
		String last_time_renew = "NULL";
		String last_time_retry = "NULL";
		String expire_time = "NULL";

		try
		{
			mSub = new Subscriber(LocalConfig.mDBConfig_MSSQL);
			mUnSub = new UnSubscriber(LocalConfig.mDBConfig_MSSQL);

			// Lấy service
			mServiceObj = Common.GetServiceByCode(PacketName);

			if (mServiceObj.IsNull())
			{
				mLog.log.info("Dich vu khong ton tai.");
				mStatus = Status.NotSpecify;
				mInfoSubResult = InfoSubResult.InputInvalid;
				return GetResponse(last_time_subscribe, last_time_unsubscribe, last_time_renew, last_time_retry, expire_time);
			}
			Integer PID = MyConvert.GetPIDByMSISDN(MSISDN,LocalConfig.MAX_PID);
			// Lấy thông tin khách hàng đã đăng ký
			MyTableModel mTable_Sub = mSub.Select(2, PID.toString(), MSISDN, mServiceObj.ServiceID.toString());

			mSubObj = SubscriberObject.Convert(mTable_Sub,false);

			if (mSubObj.IsNull())
			{
				mTable_Sub = mUnSub.Select(2, PID.toString(), MSISDN, mServiceObj.ServiceID.toString());

				if (mTable_Sub.GetRowCount() > 0)
					mSubObj = SubscriberObject.Convert(mTable_Sub,true);
			}

			// Nếu chưa đăng ký dịch vụ
			if (mSubObj.IsNull() || (mSubObj.IsDereg && mSubObj.StatusID == Subscriber.Status.UndoSub.GetValue()))
			{
				mInfoSubResult = InfoSubResult.Success;
				mStatus = Status.NotExist;
				return GetResponse(last_time_subscribe, last_time_unsubscribe, last_time_renew, last_time_retry, expire_time);
			}
			
			if (!mSubObj.IsNull() && mSubObj.IsDereg == false)
			{
				// Đang sử dụng dịch vụ
				mInfoSubResult = InfoSubResult.Success;
				mStatus = Status.Register;

				if (mSubObj.EffectiveDate != null)
					last_time_subscribe = MyConfig.Get_DateFormat_yyyymmddhhmmss().format(mSubObj.EffectiveDate);

				if (mSubObj.RenewChargeDate != null)
					last_time_renew = MyConfig.Get_DateFormat_yyyymmddhhmmss().format(mSubObj.RenewChargeDate);
				
				if (mSubObj.RetryChargeDate != null)
					last_time_retry = MyConfig.Get_DateFormat_yyyymmddhhmmss().format(mSubObj.RetryChargeDate);

				if (mSubObj.ExpiryDate != null)
					expire_time = MyConfig.Get_DateFormat_yyyymmddhhmmss().format(mSubObj.ExpiryDate);
				
				if (mSubObj.DeregDate != null)
					last_time_unsubscribe = MyConfig.Get_DateFormat_yyyymmddhhmmss().format(mSubObj.DeregDate);
				
				return GetResponse(last_time_subscribe, last_time_unsubscribe, last_time_renew, last_time_retry, expire_time);
			}
			
			if (!mSubObj.IsNull() && mSubObj.IsDereg == true)
			{
				// Đã hủy dịch vụ
				mInfoSubResult = InfoSubResult.Success;
				mStatus = Status.NotReg;

				if (mSubObj.EffectiveDate != null)
					last_time_subscribe = MyConfig.Get_DateFormat_yyyymmddhhmmss().format(mSubObj.EffectiveDate);

				if (mSubObj.RenewChargeDate != null)
					last_time_renew = MyConfig.Get_DateFormat_yyyymmddhhmmss().format(mSubObj.RenewChargeDate);

				if (mSubObj.RetryChargeDate != null)
					last_time_retry = MyConfig.Get_DateFormat_yyyymmddhhmmss().format(mSubObj.RetryChargeDate);

				if (mSubObj.ExpiryDate != null)
					expire_time = MyConfig.Get_DateFormat_yyyymmddhhmmss().format(mSubObj.ExpiryDate);

				if (mSubObj.DeregDate != null)
					last_time_unsubscribe = MyConfig.Get_DateFormat_yyyymmddhhmmss().format(mSubObj.DeregDate);
				return GetResponse(last_time_subscribe, last_time_unsubscribe, last_time_renew, last_time_retry, expire_time);
			}
		}
		catch (Exception ex)
		{
			mInfoSubResult = InfoSubResult.SystemError;
			mLog.log.error(ex);
		}
		return GetResponse(last_time_subscribe, last_time_unsubscribe, last_time_renew, last_time_retry, expire_time);
	}

}
