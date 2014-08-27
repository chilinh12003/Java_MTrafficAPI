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
import api.process.ProGetTransaction;
import api.process.ProGetTransaction.InfoTranResult;

public class GetTransaction extends HttpServlet
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6324341941039511778L;
	MyLogger mLog = new MyLogger(LocalConfig.LogConfigPath, this.getClass().toString());
	
	/**
	 * Constructor of the object.
	 */
	public GetTransaction()
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
	 * @param request the request send by the client to the server
	 * @param response the response send by the server to the client
	 * @throws ServletException if an error occurred
	 * @throws IOException if an error occurred
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
	 * This method is called when a form has its tag value method equals to post.
	 * 
	 * @param request the request send by the client to the server
	 * @param response the response send by the server to the client
	 * @throws ServletException if an error occurred
	 * @throws IOException if an error occurred
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
	 * @throws ServletException if an error occurs
	 */
	public void init() throws ServletException
	{
		// Put your code here
	}


	private String GetVNPInfo(HttpServletRequest request)
	{
		String XMLResponse = "";
		String XMLRequest = "";
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
				XMLResponse = GetResult(InfoTranResult.InputInvalid);
				return XMLResponse;
			}

			XMLRequest = builder.toString();


			String RequestID = "";
			String MSISDN = "";
			String Channel = "";
			String fromdate = "";
			String todate = "";
			String pagesize = "";
			String pageindex = "";
			String application = "";
			String username = "";
			String userip = "";

			try
			{
				RequestID = Common.GetValueNode(XMLRequest, "requestid").trim();
				MSISDN = Common.GetValueNode(XMLRequest, "msisdn").trim();
				Channel = Common.GetValueNode(XMLRequest, "channel").trim();
				fromdate = Common.GetValueNode(XMLRequest, "fromdate").trim();
				todate = Common.GetValueNode(XMLRequest, "todate").trim();
				pagesize = Common.GetValueNode(XMLRequest, "pagesize").trim();
				pageindex = Common.GetValueNode(XMLRequest, "pageindex").trim();
				application = Common.GetValueNode(XMLRequest, "application").trim();
				username = Common.GetValueNode(XMLRequest, "username").trim();
				userip = Common.GetValueNode(XMLRequest, "userip").trim();
			}
			catch (Exception ex)
			{
				mLog.log.error(ex);
				XMLResponse = GetResult(InfoTranResult.InputInvalid);
				return XMLResponse;
			}
			
			if(MyCheck.GetTelco(MSISDN) != MyConfig.Telco.GPC)
			{
				XMLResponse = GetResult(InfoTranResult.InputInvalid);
				return XMLResponse;
			}
			
			ProGetTransaction mProcess = new ProGetTransaction(MSISDN, RequestID, Channel,
					MyConfig.Get_DateFormat_yyyymmddhhmmss().parse(fromdate),MyConfig.Get_DateFormat_yyyymmddhhmmss().parse(todate),
					Integer.parseInt(pagesize), Integer.parseInt(pageindex)
					);

			XMLResponse= mProcess.Process();
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
			XMLResponse = GetResult(InfoTranResult.SystemError);
		}
		finally
		{
			MyLogger.WriteDataLog(LocalConfig.LogDataFolder, "_API_VNP", "REQUEST GetTransaction --> " + XMLRequest);
			MyLogger.WriteDataLog(LocalConfig.LogDataFolder, "_API_VNP", "RESPONSE GetTransaction --> " + XMLResponse );
		}
		return XMLResponse;
	}

	private String GetResult(InfoTranResult mInfoTranResult)
	{
		String Format = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><RESPONSE><ERRORID>%s</ERRORID><ERRORDESC>%s</ERRORDESC><TOTALPAGE>%s</TOTALPAGE><TRANSACTION></TRANSACTION></RESPONSE>";
		return String.format(Format, new Object[] { mInfoTranResult.GetValue().toString(), mInfoTranResult.toString(), "0" });
	}

}
