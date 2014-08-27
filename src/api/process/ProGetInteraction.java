package api.process;

import java.util.Calendar;
import java.util.Date;

import uti.utility.MyConfig;
import uti.utility.MyConvert;
import uti.utility.MyLogger;
import dat.service.ServiceObject;
import dat.sub.Subscriber;
import dat.sub.SubscriberObject;
import dat.sub.UnSubscriber;
import db.define.MyTableModel;

public class ProGetInteraction
{
	public enum InteractionResult
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

		private InteractionResult(int value)
		{
			this.value = value;
		}

		public Integer GetValue()
		{
			return this.value;
		}

		public static InteractionResult FromInt(int iValue)
		{
			for (InteractionResult type : InteractionResult.values())
			{
				if (type.GetValue() == iValue)
					return type;
			}
			return Fail;
		}
	}

	public enum Status
	{
		// 0 là không tương tác/sử dụng dịch vụ
		 //1 là có tương tác/sử dụng dịch vụ (phải có đủ các thông tin kèm theo: last_time_access, last_channel, description)
		Exist(1), NotExist(0), ;

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
			return NotExist;
		}
	}

	MyLogger mLog = new MyLogger(LocalConfig.LogConfigPath, this.getClass().toString());

	SubscriberObject mSubObj = new SubscriberObject();

	ServiceObject mServiceObj = new ServiceObject();
	Calendar CurrentDate = Calendar.getInstance();

	Subscriber mSub = null;
	UnSubscriber mUnSub = null;

	InteractionResult mInteractionResult = InteractionResult.Fail;
	Status mStatus = Status.NotExist;

	String MSISDN = "";
	String RequestID = "";
	String PacketName = "";
	Date fromdate = null;
	Date todate = null;
	String Channel = "";
	
	public ProGetInteraction(String MSISDN, String RequestID, String PacketName,String Channel,Date fromdate, Date todate)
	{
		this.MSISDN = MSISDN;
		this.RequestID = RequestID;
		this.PacketName = PacketName;
		this.fromdate = fromdate;
		this.todate = todate;
		this.Channel = Channel.toUpperCase().trim();
	}
	
	private String GetResponse(String last_time_access, String last_channel, String description)
	{
		String Format = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><RESPONSE><SERVICE><error>%s</error><error_desc>%s</error_desc><status>%s</status><last_time_access>%s</last_time_access><last_channel>%s</last_channel><description>%s</description></SERVICE></RESPONSE>";
		return String.format(Format, new Object[] { mInteractionResult.GetValue().toString(), mInteractionResult.toString(), mStatus.GetValue().toString(),
				last_time_access, last_channel, description });
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

	
	public String Process()
	{
		mInteractionResult = InteractionResult.Fail;
		String last_time_access = "NULL";
		String last_channel = "";
		String description = "";

		try
		{
			mSub = new Subscriber(LocalConfig.mDBConfig_MSSQL);
			mUnSub = new UnSubscriber(LocalConfig.mDBConfig_MSSQL);

			// Lấy service
			mServiceObj = Common.GetServiceByCode(PacketName);

			if (mServiceObj.IsNull())
			{
				mLog.log.info("Dich vu khong ton tai.");
				mStatus = Status.NotExist;
				mInteractionResult = InteractionResult.InputInvalid;
				return GetResponse(last_time_access, last_channel, description);
			}
			Integer PID = MyConvert.GetPIDByMSISDN(MSISDN,LocalConfig.MAX_PID);
			
			// Lấy thông tin khách hàng đã đăng ký
			MyTableModel mTable_Sub = mSub.Select(2, PID.toString(), MSISDN, mServiceObj.ServiceID.toString(),
					MyConfig.Get_DateFormat_InsertDB().format(fromdate),MyConfig.Get_DateFormat_InsertDB().format(todate));

			mSubObj = SubscriberObject.Convert(mTable_Sub,false);
			
			if (mSubObj.IsNull())
			{
				mTable_Sub = mUnSub.Select(2, PID.toString(), MSISDN, mServiceObj.ServiceID.toString(),
						MyConfig.Get_DateFormat_InsertDB().format(fromdate),MyConfig.Get_DateFormat_InsertDB().format(todate));

				if (mTable_Sub.GetRowCount() > 0)
				
					mSubObj = SubscriberObject.Convert(mTable_Sub,true);
			}
		
			// Nếu chưa đăng ký dịch vụ
			if (mSubObj.IsNull() || (!mSubObj.ChannelTypeID.equals(MyConfig.ChannelType.SMS.GetValue())&&  mSubObj.ChannelTypeID.equals(MyConfig.ChannelType.WAP.GetValue())))
			{
				mInteractionResult = InteractionResult.Success;
				mStatus = Status.NotExist;
				return GetResponse(last_time_access, last_channel, description);
			}
			
			if (!mSubObj.IsNull() && mSubObj.IsDereg == false)
			{
				// Đang sử dụng dịch vụ
				mInteractionResult = InteractionResult.Success;
				mStatus = Status.Exist;
				last_channel = mSubObj.ChannelTypeName;
				
				if (mSubObj.EffectiveDate != null)
					last_time_access = MyConfig.Get_DateFormat_yyyymmddhhmmss().format(mSubObj.EffectiveDate);
				if(mSubObj.ChannelTypeID == MyConfig.ChannelType.WAP.GetValue())
				{
					description = "Dang ky dich vu tu wapsite";
				}
				else if (mSubObj.ChannelTypeID == MyConfig.ChannelType.SMS.GetValue())
				{
					description = "Gui MO DK toi dau so 1546";
				}
				return GetResponse(last_time_access, last_channel, description);
			}
			if (!mSubObj.IsNull() && mSubObj.IsDereg == true)
			{
				// Đã hủy dịch vụ
				mInteractionResult = InteractionResult.Success;
				mStatus = Status.Exist;

				if (mSubObj.DeregDate != null)
					last_time_access = MyConfig.Get_DateFormat_yyyymmddhhmmss().format(mSubObj.DeregDate);

				last_channel = mSubObj.ChannelTypeName;
				if(mSubObj.ChannelTypeID == MyConfig.ChannelType.WAP.GetValue())
				{
					description = "Huy dich vu tu wapsite";
				}
				else if (mSubObj.ChannelTypeID == MyConfig.ChannelType.SMS.GetValue())
				{
					description = "Gui MO HUY toi dau so 1546";
				}
				
				return GetResponse(last_time_access, last_channel, description);
			}
		}
		catch (Exception ex)
		{
			mInteractionResult = InteractionResult.SystemError;
			mLog.log.error(ex);
		}
		return GetResponse(last_time_access, last_channel, description);
	}

}
