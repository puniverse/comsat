package co.paralleluniverse.comsat.webactors;

import java.util.Date;


public interface WebCookie {

    String getName();

    String getValue();

    WebCookie setValue(final String value);

    String getPath();

    WebCookie setPath(final String path);

    String getDomain();

    WebCookie setDomain(final String domain);

    Integer getMaxAge();

    WebCookie setMaxAge(final Integer maxAge);

    boolean isDiscard();

    WebCookie setDiscard(final boolean discard);

    boolean isSecure();

    WebCookie setSecure(final boolean secure);

    int getVersion();

    WebCookie setVersion(final int version);

    boolean isHttpOnly();

    WebCookie setHttpOnly(final boolean httpOnly);

    Date getExpires();

    WebCookie setExpires(final Date expires);

    String getComment();

    WebCookie setComment(final String comment);
}
