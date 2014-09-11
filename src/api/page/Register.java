package api.page;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uti.utility.MyCheck;
import uti.utility.MyConfig;
import uti.utility.MyLogger;
import api.process.Common;
import api.process.LocalConfig;
import api.process.ProRegister;
import api.process.ProRegister.RegResult;
import dat.service.DefineMT;
import dat.service.DefineMT.MTType;

public class Register extends HttpServlet
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	MyLogger mLog = new MyLogger(LocalConfig.LogConfigPath, this.getClass().toString());

	/**
	 * Constructor of the object.
	 */
	public Register()
	{
		super();
	}

	/**
	 * Destruction of the servlet. <br>
	 */
	public void destroy()
	{
		super.destroy(); // Just puts "destroy" string in log
		// Put your code here
	}

	/**
	 * The doGet method of the servlet. <br>
	 * 
	 * This method is called when a form has its tag value method equals to get.
	 * 
	 * @param request
	 *            the request send by the client to the server
	 * @param response
	 *            the response send by the server to the client
	 * @throws ServletException
	 *             if an error occurred
	 * @throws IOException
	 *             if an error occurred
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		try
		{
			response.setContentType("text/xml");
			PrintWriter out = response.getWriter();
			out.println(GetVNPInfo(request));

			out.flush();
			out.close();
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
		}
	}

	/**
	 * The doPost method of the servlet. <br>
	 * 
	 * This method is called when a form has its tag value method equals to
	 * post.
	 * 
	 * @param request
	 *            the request send by the client to the server
	 * @param response
	 *            the response send by the server to the client
	 * @throws ServletException
	 *             if an error occurred
	 * @throws IOException
	 *             if an error occurred
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		try
		{
			response.setContentType("text/xml");
			PrintWriter out = response.getWriter();
			out.println(GetVNPInfo(request));
			out.flush();
			out.close();
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
		}
	}

	/**
	 * Initialization of the servlet. <br>
	 * 
	 * @throws ServletException
	 *             if an error occurs
	 */
	public void init() throws ServletException
	{
		// Put your code here
	}

	private String GetVNPInfo(HttpServletRequest request)
	{
		String XMLResponse = "";
		String XMLRequest = "";
		MTType mMTType = MTType.Invalid;
		try
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));

			StringBuilder builder = new StringBuilder();
			String aux = "";

			while ((aux = reader.readLine()) != null)
			{
				builder.append(aux);
			}

			if (builder.length() == 0)
			{
				mMTType = MTType.Invalid;
				return GetResult(mMTType);
			}
			
			XMLRequest = builder.toString();

			String RequestID = "";
			String MSISDN = "";
			String PacketName = "";
			String Promotion = "";
			String Trial = "";
			String Bundle = "";
			String Note = "";
			String Channel = "";
			String application = "";
			String username = "";
			String userip = "";

			try
			{
				RequestID = Common.GetValueNode(XMLRequest, "requestid").trim();
				MSISDN = Common.GetValueNode(XMLRequest, "msisdn").trim();
				PacketName = Common.GetValueNode(XMLRequest, "packagename").trim();
				Promotion = Common.GetValueNode(XMLRequest, "promotion").trim();
				Trial = Common.GetValueNode(XMLRequest, "trial").trim();
				Bundle = Common.GetValueNode(XMLRequest, "bundle").trim();
				Note = Common.GetValueNode(XMLRequest, "note").trim();
				Channel = Common.GetValueNode(XMLRequest, "channel").trim();
				application = Common.GetValueNode(XMLRequest, "application").trim();
				username = Common.GetValueNode(XMLRequest, "username").trim();
				userip = Common.GetValueNode(XMLRequest, "userip").trim();
			}
			catch (Exception ex)
			{
				mLog.log.error(ex);
				mMTType = MTType.Invalid;
				XMLResponse = GetResult(mMTType);
				return XMLResponse;
			}

			if(MyCheck.GetTelco(MSISDN) != MyConfig.Telco.GPC)
			{
				mMTType = MTType.Invalid;
				XMLResponse = GetResult(mMTType);
				return XMLResponse;
			}		
			
			ProRegister mProcess = new ProRegister(MSISDN, RequestID, PacketName, Promotion, Trial, Bundle, Note, Channel, application, username, userip);

			mMTType = mProcess.Process();

		}
		catch (Exception ex)
		{
			mMTType = MTType.SystemError;
			mLog.log.error(ex);

		}
		finally
		{
			XMLResponse = GetResult(mMTType);
			MyLogger.WriteDataLog(LocalConfig.LogDataFolder, "_API_VNP", "REQUEST REGISTER --> " + XMLRequest);
			MyLogger.WriteDataLog(LocalConfig.LogDataFolder, "_API_VNP", "RESPONSE REGISTER --> " + XMLResponse + "|| mMTType:" + mMTType.toString());
		}
		return XMLResponse;
	}

	private String GetResult(DefineMT.MTType mMTType)
	{
		ProRegister.RegResult mRegResult = RegResult.Fail;
		switch (mMTType)
		{
			case RegAgainSuccessFree:
				mRegResult = RegResult.Repeat;
				break;
			case RegNewSuccess:
				mRegResult = RegResult.SuccessFree;
				break;
			case RegFromVNPSuccess_Case_1:
				mRegResult = RegResult.SuccessFree;
				break;
			case RegFromVNPSuccess_Case_2:
				mRegResult = RegResult.SucessPay;
				break;
			case RegAgainSuccessNotFree:
				mRegResult = RegResult.SucessPay;
				break;
			case RegFail:
				mRegResult = RegResult.Fail;
				break;
			case Invalid:
				mRegResult = RegResult.InputInvalid;
				break;
			case RegNotEnoughMoney:
				mRegResult = RegResult.EnoughMoney;
				break;
			case RegRepeatFree:
				mRegResult = RegResult.ExistSub;
				break;
			case RegRepeatNotFree:
				mRegResult = RegResult.ExistSub;
				break;
			case SystemError:
				mRegResult = RegResult.SystemError;
				break;
			default:
				mRegResult = RegResult.Fail;
				break;
		}
		String XMLReturn = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<RESPONSE>" + "<ERRORID>" + mRegResult.GetValue() + "</ERRORID>" + "<ERRORDESC>"
				+ mRegResult.toString() + "</ERRORDESC>" + "</RESPONSE>";
		return XMLReturn;
	}

}
