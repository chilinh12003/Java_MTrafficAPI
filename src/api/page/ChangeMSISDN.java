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
import api.process.ProChangeMSISDN;
import api.process.ProChangeMSISDN.ChangeMSISDNResult;

public class ChangeMSISDN extends HttpServlet
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	MyLogger mLog = new MyLogger(LocalConfig.LogConfigPath, this.getClass().toString());
	
	/**
	 * Constructor of the object.
	 */
	public ChangeMSISDN()
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
				XMLResponse = GetResult(ChangeMSISDNResult.InputInvalid);
				return XMLResponse;
			}

			XMLRequest = builder.toString();

			String RequestID = "";
			String msisdnA = "";
			String msisdnB = "";
			String Channel = "";
			String application = "";
			String username = "";
			String userip = "";

			try
			{
				RequestID = Common.GetValueNode(XMLRequest, "requestid").trim();
				msisdnA = Common.GetValueNode(XMLRequest, "msisdnA").trim();
				msisdnB = Common.GetValueNode(XMLRequest, "msisdnB").trim();
				Channel = Common.GetValueNode(XMLRequest, "channel").trim();
				application = Common.GetValueNode(XMLRequest, "application").trim();
				username = Common.GetValueNode(XMLRequest, "username").trim();
				userip = Common.GetValueNode(XMLRequest, "userip").trim();
			}
			catch (Exception ex)
			{
				mLog.log.error(ex);
				XMLResponse = GetResult(ChangeMSISDNResult.InputInvalid);
				return XMLResponse;
			}
			
			if((MyCheck.GetTelco(msisdnA) != MyConfig.Telco.GPC) || (MyCheck.GetTelco(msisdnB) != MyConfig.Telco.GPC))
			{
				XMLResponse = GetResult(ChangeMSISDNResult.InputInvalid);
				return XMLResponse;
			}

			ProChangeMSISDN mProcess = new ProChangeMSISDN(msisdnA, msisdnB, RequestID, Channel, application, username, userip);

			XMLResponse= mProcess.Process();
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
			XMLResponse = GetResult(ChangeMSISDNResult.SystemError);
		}
		finally
		{
			MyLogger.WriteDataLog(LocalConfig.LogDataFolder, "_API_VNP", "REQUEST ChangeMSISDN --> " + XMLRequest);
			MyLogger.WriteDataLog(LocalConfig.LogDataFolder, "_API_VNP", "RESPONSE ChangeMSISDN --> " + XMLResponse );
		}
		return XMLResponse;
	}
	
	private String GetResult(ChangeMSISDNResult mDeregAllResult)
	{
		String XMLReturn = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<RESPONSE>" + "<ERRORID>" + mDeregAllResult.GetValue() + "</ERRORID>"
				+ "<ERRORDESC>" + mDeregAllResult.toString() + "</ERRORDESC>" + "</RESPONSE>";
		return XMLReturn;
	}

	
}
