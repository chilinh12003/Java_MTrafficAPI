package api.process;

import java.io.StringReader;
import java.util.Calendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import uti.utility.MyConfig;
import uti.utility.VNPApplication;
import uti.utility.MyConvert;
import uti.utility.MyLogger;
import dat.service.ChargeLog;
import dat.service.ServiceObject;
import db.define.MyDataRow;
import db.define.MyTableModel;

public class Charge
{
	public enum Reason
	{
		REG(1), RENEW(2), UNREG(3), ;

		private int value;

		private Reason(int value)
		{
			this.value = value;
		}

		public Integer GetValue()
		{
			return this.value;
		}

		public static Reason FromInt(int iValue)
		{
			for (Reason type : Reason.values())
			{
				if (type.GetValue() == iValue)
					return type;
			}
			return REG;
		}
	}

	public enum ErrorCode
	{

		ChargeSuccess(0), BlanceTooLow(1), ChargeNotComplete(2), UserNotExist(3), OtherError(4), InvalidSubscriptionState(5), VNPAPIError(-1);

		private int value;

		private ErrorCode(int value)
		{
			this.value = value;
		}

		public Integer GetValue()
		{
			return this.value;
		}

		public static ErrorCode FromInt(int iValue)
		{
			for (ErrorCode type : ErrorCode.values())
			{
				if (type.GetValue() == iValue)
					return type;
			}
			return OtherError;
		}
	}

	static MyLogger mLog = new MyLogger(LocalConfig.LogConfigPath, Charge.class.toString());

	public static ErrorCode ChargeRegFree(Integer PartnerID, ServiceObject mServiceObj,String MSISDN, String Keyword, MyConfig.ChannelType mChannel,VNPApplication mVNPApp, String VNPUserName, String IP) throws Exception
	{
		try
		{
			return ChargeVNP(PartnerID,mServiceObj,MSISDN, 0, Reason.REG, 1000, 1, Keyword, mChannel,mVNPApp,VNPUserName,IP);
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	public static ErrorCode ChargeReg(Integer PartnerID, ServiceObject mServiceObj,String MSISDN, String Keyword, MyConfig.ChannelType mChannel,VNPApplication mVNPApp, String VNPUserName, String IP) throws Exception
	{
		try
		{
			return ChargeVNP(PartnerID,mServiceObj,MSISDN, 1000, Reason.REG, 1000, 0, Keyword, mChannel,mVNPApp,VNPUserName,IP);
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	public static ErrorCode ChargeRenew(Integer PartnerID, ServiceObject mServiceObj,String MSISDN,Integer Price, String Keyword, MyConfig.ChannelType mChannel,VNPApplication mVNPApp, String VNPUserName, String IP) throws Exception
	{
		try
		{
			return ChargeVNP(PartnerID,mServiceObj,MSISDN, Price, Reason.RENEW, Price, 0, Keyword, mChannel,mVNPApp,VNPUserName,IP);
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	public static ErrorCode ChargeDereg( Integer PartnerID, ServiceObject mServiceObj,String MSISDN, String Keyword, MyConfig.ChannelType mChannel,VNPApplication mVNPApp, String VNPUserName, String IP) throws Exception
	{
		try
		{
			return ChargeVNP(PartnerID,mServiceObj, MSISDN, 0, Reason.UNREG, 0, 0, Keyword, mChannel,mVNPApp,VNPUserName,IP);
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}

	private static ErrorCode GetErrorCode(String XML) throws Exception
	{
		ErrorCode Result = ErrorCode.OtherError;
		try
		{
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(XML));

			Document doc = db.parse(is);
			NodeList nodes = doc.getElementsByTagName("Error");

			Element line = (Element) nodes.item(0);

			Node child = line.getFirstChild();

			if (child instanceof CharacterData)
			{
				CharacterData cd = (CharacterData) child;
				String Code = cd.getData();

				Result = ErrorCode.FromInt(Integer.parseInt(Code.trim()));
			}

		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
		}
		return Result;
	}

	private static boolean InsertChargeLog(Integer PartnerID,ServiceObject mServiceObj, String MSISDN, Integer PRICE, Reason REASON, Integer ORIGINALPRICE, Integer PROMOTION,
			MyConfig.ChannelType CHANNEL,VNPApplication mVNPApp, String VNPUserName, String IP, ErrorCode mResult)
	{
		try
		{
			
			Calendar CurrentDate = Calendar.getInstance();

			ChargeLog mChargeLog = new ChargeLog(LocalConfig.mDBConfig_MSSQL);
			MyTableModel mTableLog = mChargeLog.Select(0);
			mTableLog.Clear();
			MyDataRow mRow_Log = mTableLog.CreateNewRow();

			/*
			 * MSISDN, ChargeDate, Price, ChargeTypeID,
			 * ChargeTypeName, ChargeStatusID, ChargeStatusName, PID,
			 * IsPromotion, ChannelTypeID, ChannelTypeName
			 */
			mRow_Log.SetValueCell("ServiceID", mServiceObj.ServiceID);
			mRow_Log.SetValueCell("MSISDN", MSISDN);
			mRow_Log.SetValueCell("ChargeDate", MyConfig.Get_DateFormat_InsertDB().format(CurrentDate.getTime()));
			mRow_Log.SetValueCell("ChargeTypeID", REASON.GetValue());
			mRow_Log.SetValueCell("ChargeTypeName", REASON.toString());

			mRow_Log.SetValueCell("ChargeStatusID", mResult.GetValue());
			mRow_Log.SetValueCell("ChargeStatusName", mResult.toString());

			mRow_Log.SetValueCell("IsPromotion", PROMOTION.toString());

			mRow_Log.SetValueCell("ChannelTypeID", CHANNEL.GetValue());
			mRow_Log.SetValueCell("ChannelTypeName", CHANNEL.toString());
			mRow_Log.SetValueCell("Price", PRICE);

			mRow_Log.SetValueCell("PID", MyConvert.GetPIDByMSISDN(MSISDN,LocalConfig.MAX_PID));
			
			mRow_Log.SetValueCell("PartnerID", PartnerID);
			
			mRow_Log.SetValueCell("AppID", mVNPApp.GetValue());
			mRow_Log.SetValueCell("AppName", mVNPApp.toString());
			
			mRow_Log.SetValueCell("UserName", VNPUserName);
			mRow_Log.SetValueCell("IP", IP);
			
			mTableLog.AddNewRow(mRow_Log);

			return mChargeLog.Insert(0, mTableLog.GetXML());
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
			return false;
		}
	}

	private static ErrorCode ChargeVNP(Integer PartnerID, ServiceObject mServiceObj,String MSISDN, Integer PRICE, Reason REASON, Integer ORIGINALPRICE, Integer PROMOTION,
			String NOTE, MyConfig.ChannelType CHANNEL,VNPApplication mVNPApp, String VNPUserName, String IP) throws Exception
	{
		String XMLResponse = "";
		String XMLReqeust = "";
		String RequestID = Long.toString(System.currentTimeMillis());
		String UserName = LocalConfig.VNPUserName;
		String Password = LocalConfig.VNPPassword;
		String CPName = LocalConfig.VNPCPName;
		ErrorCode mResult = ErrorCode.OtherError;

		String SaveChargeLogStatus = "Save Chargelog Satus: OK";
		String ContentName = mServiceObj.PacketName;
		String ContentID = mServiceObj.ServiceID.toString();
		
		try
		{
			String Template = "<CCGWRequest servicename=\"%s\" username=\"%s\" password=\"%s\">"+
					"<RequestType>1</RequestType>"+
					"<SequenceNumber>%s</SequenceNumber>"+
					"<SubId>%s</SubId>"+
					"<Price>%s</Price>"+
					"<Reason>%s</Reason>"+
					"<ORIGINALPRICE>%s</ORIGINALPRICE>"+
					"<PROMOTION>%s</PROMOTION>"+
					"<NOTE>%s</NOTE>"+
					"<CHANNEL>%s</CHANNEL>"+
					"<Content>"+
					 "<item contenttype=\"SUBSCRIPTION\" subcontenttype=\"VI\" contentid=\"%s\" contentname=\"%s\" cpname=\"%s\" note=\"\" playtype=\"\" contentprice=\"\"/>"+
					"</Content>"+
				"</CCGWRequest>";

			XMLReqeust = String.format(Template, new Object[] {  CPName, UserName, Password,RequestID, MSISDN, PRICE, REASON.toString(), ORIGINALPRICE,
					PROMOTION, NOTE, CHANNEL.toString(),ContentID, ContentName ,CPName});

			HttpPost mPost = new HttpPost(LocalConfig.VNPURLCharging);

			StringEntity mEntity = new StringEntity(XMLReqeust, ContentType.create("text/xml", "UTF-8"));
			mPost.setEntity(mEntity);

			HttpClient mClient = new DefaultHttpClient();
			try
			{
				HttpResponse response = mClient.execute(mPost);
				HttpEntity entity = response.getEntity();
				XMLResponse = EntityUtils.toString(entity);
				EntityUtils.consume(entity);
			}
			catch (Exception ex)
			{
				throw ex;
			}
			finally
			{
				mClient.getConnectionManager().shutdown();
			}

			mResult = GetErrorCode(XMLResponse);

			if (!InsertChargeLog(PartnerID,mServiceObj,MSISDN, PRICE, REASON, ORIGINALPRICE, PROMOTION, CHANNEL,mVNPApp,VNPUserName,IP, mResult))
			{
				SaveChargeLogStatus = "Save Chargelog Satus: FAIL";
			}

		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
		}
		finally
		{
			MyLogger.WriteDataLog(LocalConfig.LogDataFolder, "_Charge_VNP", "REQUEST CHARGE --> " + XMLReqeust);
			MyLogger.WriteDataLog(LocalConfig.LogDataFolder, "_Charge_VNP", "RESPONSE CHARGE --> " + XMLResponse + " || " + SaveChargeLogStatus);
		}

		return mResult;
	}
}
