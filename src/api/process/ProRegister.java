package api.process;

import java.util.Calendar;

import uti.utility.MyConfig;
import uti.utility.MyConfig.VNPApplication;
import uti.utility.MyConvert;
import uti.utility.MyLogger;
import api.process.Charge.ErrorCode;
import api.process.PromotionObject.AdvertiseType;
import api.process.PromotionObject.BundleType;
import api.process.PromotionObject.PromotionType;
import api.process.PromotionObject.TrialType;
import dat.service.DefineMT;
import dat.service.DefineMT.MTType;
import dat.service.MOLog;
import dat.service.ServiceObject;
import dat.sub.Subscriber;
import dat.sub.SubscriberObject;
import dat.sub.UnSubscriber;
import db.define.MyDataRow;
import db.define.MyTableModel;

public class ProRegister
{
	public enum RegResult
	{
		// 0 Đăng ký thành công dịch vụ
		Success(0),
		// 1 Thuê bao này đã tồn tại
		ExistSub(1),
		// 2 Đăng ký rồi và đăng ký lại dịch vụ
		Repeat(2),
		// 3 Đăng ký thành công dịch vụ và không bị trừ cước đăng ký
		SuccessFree(3),
		// 4 Đăng ký thành công dịch vụ và bị trừ cước đăng ký
		SucessPay(4),
		// 5 Đăng ký không thành công do không đủ tiền trong tài khoản
		EnoughMoney(5),
		// 1xx Đều là đăng ký không thành công
		Fail(100),
		// Lỗi hệ thống
		SystemError(101),
		// Thông tin nhập vào không hợp lệ
		InputInvalid(102), ;

		private int value;

		private RegResult(int value)
		{
			this.value = value;
		}

		public Integer GetValue()
		{
			return this.value;
		}

		public static RegResult FromInt(int iValue)
		{
			for (RegResult type : RegResult.values())
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

	PromotionObject mPromoObj = new PromotionObject();

	Calendar mCal_Current = Calendar.getInstance();
	Calendar mCal_SendMO = Calendar.getInstance();
	Calendar mCal_Expire = Calendar.getInstance();

	Subscriber mSub = null;
	UnSubscriber mUnSub = null;

	DefineMT.MTType mMTType = MTType.RegFail;

	MyTableModel mTable_MOLog = null;

	private int FreeCount = 7;
	String MTContent = "";

	String MSISDN = "";
	String RequestID = "";
	String PacketName = "";
	String Channel = "";

	String AppName = "";
	String UserName = "";
	String IP = "";

	String Promotion = "";
	String Trial = "";
	String Note = "";
	String Bundle = "";

	// Thời gian miễn phí để chèn vào MT trả về cho khách hàng
	String FreeTime = "7 ngay";

	public ProRegister(String MSISDN, String RequestID, String PacketName, String Promotion, String Trial, String Bundle, String Note, String Channel,
			String AppName, String UserName, String IP)
	{
		this.MSISDN = MSISDN.trim();
		this.RequestID = RequestID.trim();
		this.PacketName = PacketName.trim();

		this.Promotion = Promotion.trim();
		this.Trial = Trial.trim();
		this.Bundle = Bundle.trim();
		this.Note = Note.trim();

		this.Channel = Channel.toUpperCase().trim();
		this.AppName = AppName.trim();
		this.UserName = UserName.trim();
		this.IP = IP.trim();
	}

	/**
	 * Tính toán trường hợp khuyến mãi
	 * 
	 * @throws Exception
	 */
	private void CalculatePromotion() throws Exception
	{
		try
		{
			if (Bundle.equalsIgnoreCase("1"))
			{
				mPromoObj.mAdvertiseType = AdvertiseType.Bundle;
				mPromoObj.mBundleType = BundleType.NeverExpire;
			}
			else if (!Trial.equalsIgnoreCase("") && !Trial.equalsIgnoreCase("0"))
			{
				if (Trial.indexOf("c") > 0)
				{
					mPromoObj.mTrialType = TrialType.Day;
				}
				else if (Trial.indexOf("d") > 0)
				{
					mPromoObj.mTrialType = TrialType.Day;
				}
				else if (Trial.indexOf("w") > 0)
				{
					mPromoObj.mTrialType = TrialType.Week;
				}
				else if (Trial.indexOf("m") > 0)
				{
					mPromoObj.mTrialType = TrialType.Month;
				}

				mPromoObj.TrialNumberFree = Integer.parseInt(Trial.substring(0, Trial.length() - 1));
				mPromoObj.mAdvertiseType = AdvertiseType.Trial;
			}
			else if (!Promotion.equalsIgnoreCase("") && !Promotion.equalsIgnoreCase("0"))
			{
				if (Promotion.indexOf("c") > 0)
				{
					mPromoObj.mPromotionType = PromotionType.Day;
				}
				else if (Promotion.indexOf("d") > 0)
				{
					mPromoObj.mPromotionType = PromotionType.Day;
				}
				else if (Promotion.indexOf("w") > 0)
				{
					mPromoObj.mPromotionType = PromotionType.Week;
				}
				else if (Promotion.indexOf("m") > 0)
				{
					mPromoObj.mPromotionType = PromotionType.Month;
				}

				mPromoObj.PromotionNumberFree = Integer.parseInt(Promotion.substring(0, Promotion.length() - 1));
				mPromoObj.mAdvertiseType = AdvertiseType.Promotion;
			}

		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	/**
	 * Thiết lập thông tin Promotion cho đối tượng sub
	 * 
	 * @throws Exception
	 */
	private void SetPromotionToSub() throws Exception
	{
		mCal_Expire = Calendar.getInstance();
		mCal_Expire.set(Calendar.MILLISECOND, 0);
		mCal_Expire.set(mCal_Current.get(Calendar.YEAR), mCal_Current.get(Calendar.MONTH), mCal_Current.get(Calendar.DATE), 23, 59, 59);
		if (mPromoObj.mAdvertiseType == AdvertiseType.Bundle)
		{
			if (mPromoObj.mBundleType == BundleType.NeverExpire)
			{
				mCal_Expire.set(2030, mCal_Current.get(Calendar.MONTH), mCal_Current.get(Calendar.DATE), 23, 59, 59);
			}
			mSubObj.StatusID = dat.sub.Subscriber.Status.ActiveBundle.GetValue();
		}
		else if (mPromoObj.mAdvertiseType == AdvertiseType.Trial)
		{
			if (mPromoObj.mTrialType == TrialType.Day)
			{
				mCal_Expire.add(Calendar.DATE, mPromoObj.TrialNumberFree - 1);
				FreeTime = "" + mPromoObj.TrialNumberFree + " ngay";
			}
			else if (mPromoObj.mTrialType == TrialType.Week)
			{
				mCal_Expire.add(Calendar.DATE, mPromoObj.TrialNumberFree * 7);
				FreeTime = "" + mPromoObj.TrialNumberFree + " tuan";
			}
			else if (mPromoObj.mTrialType == TrialType.Month)
			{
				mCal_Expire.add(Calendar.MONTH, mPromoObj.TrialNumberFree);
				FreeTime = "" + mPromoObj.TrialNumberFree + " thang";
			}
			mSubObj.StatusID = dat.sub.Subscriber.Status.ActiveTrial.GetValue();
		}
		else if (mPromoObj.mAdvertiseType == AdvertiseType.Promotion)
		{
			if (mPromoObj.mPromotionType == PromotionType.Day)
			{
				mCal_Expire.add(Calendar.DATE, mPromoObj.PromotionNumberFree - 1);
				FreeTime = "" + mPromoObj.PromotionNumberFree + " ngay";
			}
			else if (mPromoObj.mPromotionType == PromotionType.Week)
			{
				mCal_Expire.add(Calendar.DATE, mPromoObj.PromotionNumberFree * 7);
				FreeTime = "" + mPromoObj.PromotionNumberFree + " tuan";
			}
			else if (mPromoObj.mPromotionType == PromotionType.Month)
			{
				mCal_Expire.add(Calendar.MONTH, mPromoObj.PromotionNumberFree);
				FreeTime = "" + mPromoObj.PromotionNumberFree + " thang";
			}
			mSubObj.StatusID = dat.sub.Subscriber.Status.ActivePromotion.GetValue();
		}

		mSubObj.ExpiryDate = mCal_Expire.getTime();
	}

	private void Init() throws Exception
	{
		try
		{
			mSub = new Subscriber(LocalConfig.mDBConfig_MSSQL);
			mUnSub = new UnSubscriber(LocalConfig.mDBConfig_MSSQL);

			mTable_MOLog = (MyTableModel) TableTemplate.Get_mMOLog().clone();
			mTable_MOLog.Clear();

			mCal_Expire.set(Calendar.MILLISECOND, 0);
			mCal_Expire.set(mCal_Current.get(Calendar.YEAR), mCal_Current.get(Calendar.MONTH), mCal_Current.get(Calendar.DATE), 23, 59, 59);

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
			mRow_Log.SetValueCell("MO", mServiceObj.RegKeyword);
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
			MyTableModel mTable_Sub = mSub.Select(0);
			mTable_Sub.Clear();

			// Tạo row để insert vào Table Sub
			MyDataRow mRow_Sub = mTable_Sub.CreateNewRow();
			mRow_Sub.SetValueCell("MSISDN", mSubObj.MSISDN);
			mRow_Sub.SetValueCell("ServiceID", mSubObj.ServiceID);

			mRow_Sub.SetValueCell("FirstDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.FirstDate));
			mRow_Sub.SetValueCell("EffectiveDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.EffectiveDate));
			mRow_Sub.SetValueCell("ExpiryDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.ExpiryDate));

			mRow_Sub.SetValueCell("RetryChargeCount", mSubObj.RetryChargeCount);

			if (mSubObj.ChargeDate != null)
				mRow_Sub.SetValueCell("ChargeDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.ChargeDate));

			mRow_Sub.SetValueCell("ChannelTypeID", mSubObj.ChannelTypeID);
			mRow_Sub.SetValueCell("ChannelTypeName", mSubObj.ChannelTypeName);
			mRow_Sub.SetValueCell("StatusID", mSubObj.StatusID);
			mRow_Sub.SetValueCell("StatusName", mSubObj.StatusName);
			mRow_Sub.SetValueCell("PID", mSubObj.PID);
			mRow_Sub.SetValueCell("TotalMT", mSubObj.TotalMT);
			mRow_Sub.SetValueCell("TotalMTByDay", mSubObj.TotalMTByDay);

			mRow_Sub.SetValueCell("AppID", mSubObj.AppID);
			mRow_Sub.SetValueCell("AppName", mSubObj.AppName);
			mRow_Sub.SetValueCell("UserName", mSubObj.UserName);
			mRow_Sub.SetValueCell("IP", mSubObj.IP);

			if (mSubObj.LastUpdate != null)
				mRow_Sub.SetValueCell("LastUpdate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.LastUpdate));

			// Lấy lại thời gian Hủy
			if (mSubObj.DeregDate != null)
				mRow_Sub.SetValueCell("DeregDate", MyConfig.Get_DateFormat_InsertDB().format(mSubObj.DeregDate));

			mTable_Sub.AddNewRow(mRow_Sub);
			return mTable_Sub;
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	private boolean Insert_Sub() throws Exception
	{
		try
		{
			MyTableModel mTable_Sub = AddInfo();

			if (!mSub.Insert(0, mTable_Sub.GetXML()))
			{
				mLog.log.info("Insert vao table Subscriber KHONG THANH CONG: XML Insert-->" + mTable_Sub.GetXML());
				return false;
			}

			return true;
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	private boolean MoveToUnSub() throws Exception
	{
		try
		{
			MyTableModel mTable_Sub = AddInfo();

			if (!mSub.Move(0, mTable_Sub.GetXML()))
			{
				mLog.log.info("Move tu UnSub Sang Sub KHONG THANH CONG: XML Insert-->" + mTable_Sub.GetXML());
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
	private void CreateRegAgain() throws Exception
	{
		try
		{
			mSubObj.ChannelTypeID = Common.GetChannelType(Channel).GetValue();
			mSubObj.ChannelTypeName = Common.GetChannelType(Channel).toString();

			mSubObj.EffectiveDate = mCal_Current.getTime();
			mSubObj.ExpiryDate = mCal_Expire.getTime();

			mSubObj.LastUpdate = mCal_Current.getTime();
			mSubObj.MSISDN = MSISDN;
			mSubObj.PID = MyConvert.GetPIDByMSISDN(mSubObj.MSISDN, LocalConfig.MAX_PID);
			mSubObj.ServiceID = mServiceObj.ServiceID;
			mSubObj.StatusID = dat.sub.Subscriber.Status.Active.GetValue();
			mSubObj.StatusName = dat.sub.Subscriber.Status.Active.toString();
			mSubObj.TotalMTByDay = 0;

			mSubObj.AppID = Common.GetApplication(AppName).GetValue();
			mSubObj.AppName = Common.GetApplication(AppName).toString();
			mSubObj.UserName = UserName;
			mSubObj.IP = IP;

		}
		catch (Exception ex)
		{
			throw ex;
		}

	}

	/**
	 * Tạo dữ liệu cho một đăng ký mới
	 * 
	 * @throws Exception
	 */
	private void CreateNewReg() throws Exception
	{
		try
		{
			mSubObj.ChannelTypeID = Common.GetChannelType(Channel).GetValue();
			mSubObj.ChannelTypeName = Common.GetChannelType(Channel).toString();

			mSubObj.ChargeDate = null;
			mSubObj.EffectiveDate = mCal_Current.getTime();
			mSubObj.ExpiryDate = mCal_Expire.getTime();
			mSubObj.FirstDate = mCal_Current.getTime();
			mSubObj.IsDereg = false;
			mSubObj.LastUpdate = mCal_Current.getTime();
			mSubObj.MSISDN = MSISDN;
			mSubObj.PID = MyConvert.GetPIDByMSISDN(mSubObj.MSISDN, LocalConfig.MAX_PID);
			mSubObj.ServiceID = mServiceObj.ServiceID;
			mSubObj.StatusID = dat.sub.Subscriber.Status.Active.GetValue();
			mSubObj.StatusName = dat.sub.Subscriber.Status.Active.toString();
			mSubObj.TotalMT = 0;
			mSubObj.TotalMTByDay = 0;

			mSubObj.AppID = Common.GetApplication(AppName).GetValue();
			mSubObj.AppName = Common.GetApplication(AppName).toString();
			mSubObj.UserName = UserName;
			mSubObj.IP = IP;

		}
		catch (Exception ex)
		{
			throw ex;
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
			MTContent = MTContent.replace("[DeregKeyword]", mServiceObj.DeregKeyword);
			MTContent = MTContent.replace("[FreeTime]", FreeTime);

			if (Common.SendMT(MSISDN, mServiceObj.RegKeyword, MTContent, RequestID))
			{
				AddToMOLog(mMTType, MTContent);
			}
			return mMTType;
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

			// Tính toàn khuyến mãi
			CalculatePromotion();

			// Lấy service
			mServiceObj = Common.GetServiceByCode(PacketName);

			if (mServiceObj.IsNull())
			{
				mLog.log.info("Dich vu khong ton tai.");
				mMTType = MTType.Invalid;
				return mMTType;
			}

			Integer PID = MyConvert.GetPIDByMSISDN(MSISDN, LocalConfig.MAX_PID);

			// Lấy thông tin khách hàng đã đăng ký
			MyTableModel mTable_Sub = mSub.Select(2, PID.toString(), MSISDN, mServiceObj.ServiceID.toString());

			mSubObj = SubscriberObject.Convert(mTable_Sub, false);

			if (mSubObj.IsNull())
			{
				mTable_Sub = mUnSub.Select(2, PID.toString(), MSISDN, mServiceObj.ServiceID.toString());

				if (mTable_Sub.GetRowCount() > 0)
					mSubObj = SubscriberObject.Convert(mTable_Sub, true);
			}

			mSubObj.PID = MyConvert.GetPIDByMSISDN(MSISDN, LocalConfig.MAX_PID);

			// Nếu đã đăng ký rồi và tiếp tục đăng ký
			if (!mSubObj.IsNull() && mSubObj.IsDereg == false)
			{
				// Kiểm tra còn free hay không
				if (mSubObj.IsFreeReg(FreeCount))
				{
					mMTType = MTType.RegRepeatFree;
					return AddToList();
				}
				else
				{
					mMTType = MTType.RegRepeatNotFree;
					return AddToList();
				}
			}

			// nếu được quảng cáo Bundel, trial hoặc promotion
			if (mPromoObj.mAdvertiseType != AdvertiseType.Normal)
			{
				// Tiến hành xử lý đăng ký khuyến mãi
				// Tạo dữ liệu cho đăng ký mới
				if (mSubObj.IsNull())
				{
					CreateNewReg();
					SetPromotionToSub();

					ErrorCode mResult = Charge.ChargeRegFree(mServiceObj, MSISDN, mServiceObj.RegKeyword, Common.GetChannelType(Channel),
							Common.GetApplication(AppName), UserName, IP);

					if (mResult != ErrorCode.ChargeSuccess)
					{
						mMTType = MTType.RegFail;
						return AddToList();
					}

					if (Insert_Sub())
					{
						if (mSubObj.AppID == VNPApplication.CCOS.GetValue())
						{

							mMTType = MTType.RegFromVNPSuccess_Case_1;
						}
						else
						{
							mMTType = MTType.RegNewSuccess;
						}
					}
					else
					{
						mMTType = MTType.RegFail;
					}

					return AddToList();
				}
				else if (mSubObj.IsDereg && mSubObj.StatusID == dat.sub.Subscriber.Status.UndoSub.GetValue())
				{
					CreateNewReg();
					SetPromotionToSub();

					ErrorCode mResult = Charge.ChargeRegFree(mServiceObj, MSISDN, mServiceObj.RegKeyword, Common.GetChannelType(Channel),
							Common.GetApplication(AppName), UserName, IP);

					if (mResult != ErrorCode.ChargeSuccess)
					{
						mMTType = MTType.RegFail;
						return AddToList();
					}
					if (MoveToUnSub())
					{
						if (mSubObj.AppID == VNPApplication.CCOS.GetValue())
						{

							mMTType = MTType.RegFromVNPSuccess_Case_1;
						}
						else
						{
							mMTType = MTType.RegNewSuccess;
						}
					}
					else
					{
						mMTType = MTType.RegFail;
					}

					return AddToList();
				}
				else
				{
					// đã từng sử dụng dịch vụ trước đây
					CreateRegAgain();

					SetPromotionToSub();

					ErrorCode mResult = Charge.ChargeRegFree(mServiceObj, MSISDN, mServiceObj.RegKeyword, Common.GetChannelType(Channel),
							Common.GetApplication(AppName), UserName, IP);

					if (mResult != ErrorCode.ChargeSuccess)
					{
						mMTType = MTType.RegFail;
						return AddToList();
					}
					if (MoveToUnSub())
					{
						if (mSubObj.AppID == VNPApplication.CCOS.GetValue())
						{

							mMTType = MTType.RegFromVNPSuccess_Case_1;
						}
						else
						{
							mMTType = MTType.RegNewSuccess;
						}
					}
					else
					{
						mMTType = MTType.RegFail;
					}

					return AddToList();
				}
			}

			// Đăng ký mới (chưa từng đăng ký trước đây)
			if (mSubObj.IsNull())
			{
				mCal_Expire.add(Calendar.DATE, 7);
				FreeTime = "7 ngay";

				// Tạo dữ liệu cho đăng ký mới
				CreateNewReg();

				ErrorCode mResult = Charge.ChargeRegFree(mServiceObj, MSISDN, mServiceObj.RegKeyword, Common.GetChannelType(Channel),
						Common.GetApplication(AppName), UserName, IP);

				if (mResult != ErrorCode.ChargeSuccess)
				{
					mMTType = MTType.RegFail; // Đăng ký lại nhưng miễn phí
					return AddToList();
				}

				if (Insert_Sub())
				{
					if (mSubObj.AppID == VNPApplication.CCOS.GetValue())
					{

						mMTType = MTType.RegFromVNPSuccess_Case_1;
					}
					else
					{
						mMTType = MTType.RegNewSuccess;
					}
				}
				else
				{
					mMTType = MTType.RegFail;
				}

				return AddToList();
			}

			// nếu số điện thoại đã từng đăng ký trước đây nhưng bị Vinaphone
			// Hủy thuê bao
			if (mSubObj.IsDereg && mSubObj.StatusID == dat.sub.Subscriber.Status.UndoSub.GetValue())
			{
				mCal_Expire.add(Calendar.DATE, 7);
				FreeTime = "7 ngay";

				CreateRegAgain();

				ErrorCode mResult = Charge.ChargeRegFree(mServiceObj, MSISDN, mServiceObj.RegKeyword, Common.GetChannelType(Channel),
						Common.GetApplication(AppName), UserName, IP);

				if (mResult != ErrorCode.ChargeSuccess)
				{
					mMTType = MTType.RegFail;
					return AddToList();
				}

				if (MoveToUnSub())
				{
					if (mSubObj.AppID == VNPApplication.CCOS.GetValue())
					{

						mMTType = MTType.RegFromVNPSuccess_Case_1;
					}
					else
					{
						mMTType = MTType.RegNewSuccess;
					}
				}
				else
				{
					mMTType = MTType.RegFail;
				}

				
				return AddToList();
			}

			// Đã đăng ký trước đó nhưng đang hủy
			if (mSubObj.IsDereg)
			{
				// Nếu là đăng ký và hết thời gian khuyến mại thì tiến hành
				// charge trước
				/*
				 * if (mSubObj.IsFreeReg(FreeCount)) {
				 * mCal_Expire.setTime(mSubObj.ExpiryDate);
				 * 
				 * CreateRegAgain();
				 * 
				 * ErrorCode mResult = Charge.ChargeRegFree(mServiceObj, MSISDN,
				 * mServiceObj.RegKeyword, Common.GetChannelType(Channel),
				 * Common.GetApplication(AppName), UserName, IP); if (mResult !=
				 * ErrorCode.ChargeSuccess) { mMTType = MTType.RegFail; // Đăng
				 * ký lại nhưng miễn phí return AddToList(); } // Nếu xóa unsub
				 * hoặc Insert sub không thành công thì thông // báo lỗi if
				 * (MoveToUnSub()) { if (mSubObj.AppID ==
				 * VNPApplication.CCOS.GetValue()) { mMTType =
				 * MTType.RegFromVNPSuccess_Case_1; } else { mMTType =
				 * MTType.RegAgainSuccessFree; } return AddToList(); }
				 * 
				 * mMTType = MTType.RegFail; // Đăng ký lại nhưng miễn phí
				 * return AddToList(); } else {
				 */
				CreateRegAgain();

				// đồng bộ thuê bao sang Vinpahone
				ErrorCode mResult = Charge.ChargeReg(mServiceObj, MSISDN, mServiceObj.RegKeyword, Common.GetChannelType(Channel),
						Common.GetApplication(AppName), UserName, IP);

				// Charge
				if (mResult == ErrorCode.BlanceTooLow)
				{
					mMTType = MTType.RegNotEnoughMoney;
					return AddToList();
				}
				if (mResult != ErrorCode.ChargeSuccess)
				{
					mMTType = MTType.RegFail; // Đăng ký lại nhưng mất tiền
					return AddToList();
				}

				// Nếu xóa unsub hoặc Insert sub không thành công thì thông
				// báo lỗi
				if (MoveToUnSub())
				{
					if (mSubObj.AppID == VNPApplication.CCOS.GetValue())
					{

						mMTType = MTType.RegFromVNPSuccess_Case_2;
					}
					else
					{
						mMTType = MTType.RegAgainSuccessNotFree;
					}
					return AddToList();
				}

				mMTType = MTType.RegFail; // Đăng ký lại nhưng mất tiền
				return AddToList();
				/* } */
			}

			mMTType = MTType.RegFail;

		}
		catch (Exception ex)
		{
			mMTType = MTType.SystemError;
			mLog.log.error(ex);
		}
		finally
		{
			// Insert vao log
			Insert_MOLog();
		}
		return mMTType;
	}
}
