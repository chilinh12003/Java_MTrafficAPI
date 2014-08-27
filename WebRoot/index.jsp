<%@page import="uti.utility.*"%>
<%@ page language="java" import="java.util.*" pageEncoding="ISO-8859-1"%>
<%
String path = request.getContextPath();
String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";
String ClassPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
String rootPath = System.getProperty("catalina.base");
String CurrentPath = MyCurrent.GetCurrentPath();
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <base href="<%=basePath%>">
    
    <title>My JSP 'index.jsp' starting page</title>
	<meta http-equiv="pragma" content="no-cache">
	<meta http-equiv="cache-control" content="no-cache">
	<meta http-equiv="expires" content="0">    
	<meta http-equiv="keywords" content="keyword1,keyword2,keyword3">
	<meta http-equiv="description" content="This is my page">
	
  </head>
  
  <body>
    This is my HBCom Webservice page. <br>
    <div>
    basePath:<%=basePath%>
    </div>
      <div>
    ClassPath:<%=ClassPath%>
    </div>
     <div>
    rootPath:<%=rootPath%>
    </div>
     <div>
    CurrentPath:<%=CurrentPath%>
    </div>
     <div>
    Web.xml Info:<%=getServletContext().getInitParameter("VNPCPName")%>
    </div>
    <div>
    	Mtraffic
    </div>
  </body>
</html>
