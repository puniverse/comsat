/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co.paralleluniverse.comsat.webactors.servlet;

import co.paralleluniverse.comsat.webactors.WebActorCookie;
import co.paralleluniverse.comsat.webactors.WebActorHttpMessage;
import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.channels.SendPort;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.Response;
import org.omg.PortableServer.REQUEST_PROCESSING_POLICY_ID;

public class WebHttpMessage implements WebActorHttpMessage {
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    @Override
    public Enumeration<WebActorCookie> getCookies() {
        final Cookie[] cookies = request.getCookies();
        return new Enumeration<WebActorCookie>() {
            int i = 0;

            @Override
            public boolean hasMoreElements() {
                return cookies.length > i;
            }

            @Override
            public WebActorCookie nextElement() {
                final Cookie c = cookies[i++];
                return new WebActorCookie() {
                    @Override
                    public String getName() {
                        return c.getName();
                    }

                    @Override
                    public String getValue() {
                        return c.getValue();
                    }

                    @Override
                    public WebActorCookie setValue(String value) {
                        c.setValue(value);
                        return this;
                    }

                    @Override
                    public String getPath() {
                        return c.getPath();
                    }

                    @Override
                    public WebActorCookie setPath(String path) {
                        c.setPath(path);
                        return this;
                    }

                    @Override
                    public String getDomain() {
                        return c.getDomain();
                    }

                    @Override
                    public WebActorCookie setDomain(String domain) {
                        c.setDomain(domain);
                        return this;
                    }

                    @Override
                    public Integer getMaxAge() {
                        return c.getMaxAge();
                    }

                    @Override
                    public WebActorCookie setMaxAge(Integer maxAge) {
                        c.setMaxAge(maxAge);
                        return this;
                    }

                    @Override
                    public boolean isDiscard() {
                        throw new UnsupportedOperationException("not supported yet");
                    }

                    @Override
                    public WebActorCookie setDiscard(boolean discard) {
                        throw new UnsupportedOperationException("not supported yet");
                    }

                    @Override
                    public boolean isSecure() {
                        throw new UnsupportedOperationException("not supported yet");
                    }

                    @Override
                    public WebActorCookie setSecure(boolean secure) {
                        throw new UnsupportedOperationException("not supported yet");
                    }

                    @Override
                    public int getVersion() {
                        return c.getVersion();
                    }

                    @Override
                    public WebActorCookie setVersion(int version) {
                        c.setVersion(version);
                        return this;
                    }

                    @Override
                    public boolean isHttpOnly() {
                        return c.isHttpOnly();
                    }

                    @Override
                    public WebActorCookie setHttpOnly(boolean httpOnly) {
                        c.setHttpOnly(httpOnly);
                        return this;
                    }

                    @Override
                    public Date getExpires() {
                        throw new UnsupportedOperationException("not supported yet");
                    }

                    @Override
                    public WebActorCookie setExpires(Date expires) {
                        throw new UnsupportedOperationException("not supported yet");
                    }

                    @Override
                    public String getComment() {
                        return c.getComment();
                    }

                    @Override
                    public WebActorCookie setComment(String comment) {
                        c.setComment(comment);
                        return this;
                    }
                };
            }
        };
    }

    public interface EnumGettersForRoMap<K, V> {
        public V get(K key);

        public Enumeration<K> getKeys();
    }

    public <K, V> Map<K, V> getMapFromEnums(final EnumGettersForRoMap<K, V> etrm) {
        return new Map<K, V>() {
            @Override
            public int size() {
                int i = 0;
                Enumeration keys = etrm.getKeys();
                while (keys.hasMoreElements()) {
                    i++;
                    keys.nextElement();
                }
                return i;
            }

            @Override
            public boolean isEmpty() {
                return !etrm.getKeys().hasMoreElements();
            }

            @Override
            public boolean containsKey(Object key) {
                Enumeration keys = etrm.getKeys();
                while (keys.hasMoreElements())
                    if (keys.nextElement().equals(key))
                        return true;
                return false;
            }

            @Override
            public boolean containsValue(Object value) {
                Enumeration<K> keys = etrm.getKeys();
                while (keys.hasMoreElements())
                    if (etrm.get(keys.nextElement()).equals(value))
                        return true;
                return false;
            }

            @Override
            public V get(Object key) {
                return etrm.get((K) key);
            }

            @Override
            public V put(K key, V value) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public V remove(Object key) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void putAll(Map<? extends K, ? extends V> m) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Set<K> keySet() {
                return new HashSet<>(Collections.list(etrm.getKeys()));
            }

            @Override
            public Collection<V> values() {
                ArrayList<V> values = new ArrayList<>();
                Enumeration<K> keys = etrm.getKeys();
                while (keys.hasMoreElements())
                    values.add(etrm.get(keys.nextElement()));
                return values;
            }

            @Override
            public Set<Entry<K, V>> entrySet() {
                final Set<Entry<K, V>> hashSet = new HashSet<>();
                Enumeration<K> keys = etrm.getKeys();
                while (keys.hasMoreElements()) {
                    final K key = keys.nextElement();
                    hashSet.add(new Entry<K, V>() {
                        @Override
                        public K getKey() {
                            return key;
                        }

                        @Override
                        public V getValue() {
                            return etrm.get(key);
                        }

                        @Override
                        public V setValue(V value) {
                            throw new UnsupportedOperationException("Not supported yet.");
                        }
                    });
                }
                return hashSet;
            }
        };
    }

    public WebHttpMessage(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public Map<String, String> getHeaderMap() {

        return getMapFromEnums(new EnumGettersForRoMap<String, String>() {
            @Override
            public String get(String key) {
                return request.getHeader(key);
            }

            @Override
            public Enumeration<String> getKeys() {
                return request.getHeaderNames();
            }
        });
    }

    @Override
    public long getDateHeader(String name) {
        return request.getDateHeader(name);
    }

    @Override
    public String getHeader(String name) {
        return request.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return request.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return request.getHeaderNames();
    }

    @Override
    public String getMethod() {
        return request.getMethod();
    }

    @Override
    public String getQueryString() {
        return request.getQueryString();
    }

    @Override
    public String getServletPath() {
        return request.getServletPath();
    }

    @Override
    public String getParameter(String name) {
        return request.getParameter(name);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return request.getParameterNames();
    }

    @Override
    public String[] getParameterValues(String name) {
        return request.getParameterValues(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return request.getParameterMap();
    }

    @Override
    public String getServerName() {
        return request.getServerName();
    }

    @Override
    public int getServerPort() {
        return request.getServerPort();
    }

    @Override
    public void setAttribute(String name, Object o) {
        request.setAttribute(name, o);
    }

    @Override
    public void removeAttribute(String name) {
        request.removeAttribute(name);
    }

    @Override
    public String getContextPath() {
        return request.getContextPath();
    }

    @Override
    public String getRequestURI() {
        return request.getRequestURI();
    }

    @Override
    public StringBuffer getRequestURL() {
        return request.getRequestURL();
    }

    @Override
    public int getContentLength() {
        return request.getContentLength();
    }

    @Override
    public String getContentType() {
        return request.getContentType();
    }

    @Override
    public String getPathInfo() {
        return request.getPathInfo();
    }

    @Override
    public Object getAttribute(String name) {
        return request.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return request.getAttributeNames();
    }

    @Override
    public Map<String, String> getAtrributesMap() {
        return getMapFromEnums(new EnumGettersForRoMap<String, String>() {
            @Override
            public String get(String key) {
                return request.getParameter(key);
            }

            @Override
            public Enumeration<String> getKeys() {
                return request.getParameterNames();
            }
        });
    }

    @Override
    public SendPort<String> getResponseStringPort() {
        try {
            final PrintWriter writer = response.getWriter();
            return new SendPort<String>() {
                @Override
                public void send(String message) throws SuspendExecution, InterruptedException {
                    writer.print(message);
                }

                @Override
                public boolean send(String message, long timeout, TimeUnit unit) throws SuspendExecution, InterruptedException {
                    send(message);
                    return true;
                }

                @Override
                public boolean trySend(String message) {
                    try {
                        send(message);
                    } catch (SuspendExecution | InterruptedException ex) {
                        Logger.getLogger(WebHttpMessage.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return true;
                }

                @Override
                public void close() {
                    writer.close();
                    request.getAsyncContext().complete();
                }
            };
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }


    }
}
