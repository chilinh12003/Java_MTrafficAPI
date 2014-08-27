package api.page;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uti.utility.MyConfig;
import uti.utility.MyLogger;
import api.process.Common;
import api.process.LocalConfig;
import api.process.ProGetInteraction;
import api.process.ProGetInteraction.InteractionResult;
import api.process.ProGetInteraction.Status;

public class GetInteraction extends HttpServlet
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6562778281874022367L;
	MyLogger mLog = new MyLogger(LocalConfig.LogConfigPath, this.getClass().toString());
	
	/**
	 * Constructor of the object.
	 */
	public GetInteraction()
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
				XMLResponse = GetResult(InteractionResult.InputInvalid);
				return XMLResponse;
			}

			XMLRequest = builder.toString();

			String RequestID = Common.GetValueNode(XMLRequest, "requestid");
			String MSISDN = Common.GetValueNode(XMLRequest, "msisdn");
			String PacketName = Common.GetValueNode(XMLRequest, "packagename");
			String fromdate = Common.GetValueNode(XMLRequest, "fromdate");
			String todate = Common.GetValueNode(XMLRequest, "todate");
			String channel = Common.GetValueNode(XMLRequest, "channel");

			ProGetInteraction mProcess = new ProGetInteraction(MSISDN, RequestID, PacketName,channel, MyConfig.Get_DateFormat_yyyymmddhhmmss().parse(fromdate),MyConfig.Get_DateFormat_yyyymmddhhmmss().parse(todate));

			XMLResponse= mProcess.Process();
		}
		catch (Exception ex)
		{
			mLog.log.error(ex);
			XMLResponse = GetResult(InteractionResult.SystemError);
		}
		finally
		{
			MyLogger.WriteDataLog(LocalConfig.LogDataFolder, "_API_VNP", "REQUEST GetInteraction --> " + XMLRequest);
			MyLogger.WriteDataLog(LocalConfig.LogDataFolder, "_API_VNP", "RESPONSE GetInteraction --> " + XMLResponse);
		}
		return XMLResponse;
	}

	private String GetResult(InteractionResult mInteractionResult)
	{
		Status mStatus = Status.NotExist;
		
		String last_time_access = "NULL";
		String last_channel = "";
		String description = "";
		
		String Format = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><RESPONSE><SERVICE><error>%s</error><error_desc>%s</error_desc><status>%s</status><last_time_access>%s</last_time_access><last_channel>%s</last_channel><description>%s</description></SERVICE></RESPONSE>";
		return String.format(Format, new Object[] { mInteractionResult.GetValue().toString(), mInteractionResult.toString(), mStatus.GetValue().toString(),
				last_time_access, last_channel, description });
	}

}
