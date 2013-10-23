package co.paralleluniverse.comsat.webactors;

import co.paralleluniverse.strands.channels.SendPort;
import java.util.Enumeration;
import java.util.Map;

public interface WebActorHttpMessage extends WebMessage {
    public Enumeration<WebActorCookie> getCookies();

    public long getDateHeader(String name);

    public String getHeader(String name);

    public Enumeration<String> getHeaders(String name);

    public Enumeration<String> getHeaderNames();

    public String getMethod();

    public String getPathInfo();

    public String getContextPath();

    public String getQueryString();

    public String getRequestURI();

    public StringBuffer getRequestURL();

    public String getServletPath();

    public Object getAttribute(String name);

    public Enumeration<String> getAttributeNames();

    public int getContentLength();

    public String getContentType();

    public String getParameter(String name);

    public Enumeration<String> getParameterNames();

    public String[] getParameterValues(String name);

    public Map<String, String[]> getParameterMap();

    public String getServerName();

    public int getServerPort();

    public void setAttribute(String name, Object o);

    public void removeAttribute(String name);

    ////
    public Map<String, String> getHeaderMap();
    public Map<String, String> getAtrributesMap();

    //////////////////
    /// response methods
    public SendPort<String> getResponseStringPort();
}
